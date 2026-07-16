const http = require('http');
const fs = require('fs');
const path = require('path');
const os = require('os');
const { spawn, exec } = require('child_process');
const WebSocket = require('ws');
const qrcode = require('qrcode-terminal');
const url = require('url');
const crypto = require('crypto');

// Load or generate a persistent security token to prevent unauthorized access
const CONFIG_FILE = path.join(__dirname, 'config.json');
let config = { token: '' };
if (fs.existsSync(CONFIG_FILE)) {
  try {
    config = JSON.parse(fs.readFileSync(CONFIG_FILE, 'utf8'));
  } catch (e) {
    console.error('[Security] Error reading config.json, generating new one...');
  }
}
if (!config.token) {
  config.token = crypto.randomBytes(16).toString('hex');
  fs.writeFileSync(CONFIG_FILE, JSON.stringify(config, null, 2), 'utf8');
}

const PORT = process.env.PORT || 3000;
let xdotool = null;

// High-polling & Interpolation State
let highPolling = true;
let interpolationQueue = [];
let interpolationInterval = null;

// Function to start the persistent xdotool process in stdin mode
function startXdotool() {
  if (xdotool) {
    try {
      xdotool.kill();
    } catch (e) {
      // Ignore
    }
  }

  // Running xdotool with '-' enables reading commands from stdin,
  // which avoids process spawning overhead for every cursor movement.
  xdotool = spawn('xdotool', ['-']);

  xdotool.on('close', (code) => {
    console.log(`[xdotool] process closed with code ${code}, restarting...`);
    setTimeout(startXdotool, 1000);
  });

  xdotool.on('error', (err) => {
    console.error('[xdotool] process error:', err);
  });

  // Pipe stdout and stderr to the server console for transparency
  xdotool.stdout.on('data', (data) => {
    console.log(`[xdotool stdout]: ${data.toString().trim()}`);
  });
  xdotool.stderr.on('data', (data) => {
    console.error(`[xdotool stderr]: ${data.toString().trim()}`);
  });
}

// Start xdotool process
startXdotool();

function sendXdotoolCommand(command) {
  if (xdotool && xdotool.stdin.writable) {
    xdotool.stdin.write(command + '\n');
  } else {
    console.warn('[xdotool] Warning: stdin is not writable, restarting xdotool...');
    startXdotool();
  }
}

// High Polling Rate Interpolation Engine
function processMove(dx, dy) {
  if (!highPolling) {
    sendXdotoolCommand(`mousemove_relative -- ${dx} ${dy}`);
    return;
  }

  // Split movements into micro-steps of 1-2 pixels to achieve ultra-smooth 500Hz+ glide
  const maxStep = 2;
  const stepsCount = Math.max(1, Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / maxStep));

  const stepX = dx / stepsCount;
  const stepY = dy / stepsCount;

  for (let i = 0; i < stepsCount; i++) {
    const currentX = Math.round(stepX * (i + 1)) - Math.round(stepX * i);
    const currentY = Math.round(stepY * (i + 1)) - Math.round(stepY * i);
    if (currentX !== 0 || currentY !== 0) {
      interpolationQueue.push({ dx: currentX, dy: currentY });
    }
  }

  // Prevent queue growth during rapid swipes (preserves real-time responsiveness)
  if (interpolationQueue.length > 25) {
    let extraDx = 0;
    let extraDy = 0;
    // Drain excess items immediately
    while (interpolationQueue.length > 6) {
      const m = interpolationQueue.shift();
      extraDx += m.dx;
      extraDy += m.dy;
    }
    if (extraDx !== 0 || extraDy !== 0) {
      sendXdotoolCommand(`mousemove_relative -- ${extraDx} ${extraDy}`);
    }
  }

  startInterpolationWorker();
}

function startInterpolationWorker() {
  if (interpolationInterval) return;

  // Drain the queue at roughly 600Hz (every 1.6ms)
  interpolationInterval = setInterval(() => {
    if (interpolationQueue.length === 0) {
      clearInterval(interpolationInterval);
      interpolationInterval = null;
      return;
    }

    const move = interpolationQueue.shift();
    sendXdotoolCommand(`mousemove_relative -- ${move.dx} ${move.dy}`);
  }, 1.5);
}

// Helper to determine the local IPv4 address
function getLocalIP() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      // Skip loopback and non-IPv4 addresses
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return 'localhost';
}

// Create the HTTP server to serve static files from the 'public' directory
const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  let pathname = parsedUrl.pathname;

  // Local status API for CLI / Eww widget monitoring
  if (pathname === '/status') {
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    const clients = [];
    if (typeof wss !== 'undefined' && wss.clients) {
      wss.clients.forEach(client => {
        if (client._socket) {
          // Clean IPv4-mapped IPv6 addresses (e.g. ::ffff:192.168.1.1 -> 192.168.1.1)
          const ip = client._socket.remoteAddress.replace(/^.*:/, '');
          clients.push(ip);
        }
      });
    }
    res.end(JSON.stringify({
      status: 'active',
      port: PORT,
      token: config.token,
      connectedCount: clients.length,
      connectedIPs: clients
    }));
    return;
  }

  let filePath = pathname === '/' ? '/index.html' : pathname;
  const fullPath = path.join(__dirname, 'public', filePath);

  const extname = String(path.extname(fullPath)).toLowerCase();
  const mimeTypes = {
    '.html': 'text/html',
    '.js': 'text/javascript',
    '.css': 'text/css',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
  };

  const contentType = mimeTypes[extname] || 'application/octet-stream';

  fs.readFile(fullPath, (error, content) => {
    if (error) {
      if (error.code === 'ENOENT') {
        res.writeHead(404, { 'Content-Type': 'text/html' });
        res.end('<h1>404 File Not Found</h1>', 'utf-8');
      } else {
        res.writeHead(500);
        res.end(`Server Error: ${error.code}\n`);
      }
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content, 'utf-8');
    }
  });
});

// Setup the WebSocket Server on top of the HTTP server
const wss = new WebSocket.Server({ server });

wss.on('connection', (ws, req) => {
  const clientIP = req.socket.remoteAddress;

  // Extract and validate token from WebSocket connection request URL
  const parsedUrl = url.parse(req.url, true);
  const clientToken = parsedUrl.query.token;

  if (clientToken !== config.token) {
    console.warn(`[Security] Unauthorized WebSocket connection attempt from IP: ${clientIP}. Invalid token.`);
    ws.close(4001, 'Unauthorized: Invalid security token');
    return;
  }

  console.log(`[WebSocket] Mobile client connected from IP: ${clientIP}`);

  // Send current settings state on connect
  ws.send(JSON.stringify({ type: 'sync_settings', highPolling }));

  ws.on('message', (message) => {
    try {
      const payload = JSON.parse(message);
      
      switch (payload.type) {
        case 'ping':
          // Echo ping back with the identifier for latency calculations
          ws.send(JSON.stringify({ type: 'pong', id: payload.id }));
          break;

        case 'move':
          processMove(payload.dx, payload.dy);
          break;

        case 'move_batch':
          // Process coalesced high-frequency movements
          if (highPolling) {
            for (const ev of payload.events) {
              processMove(ev.dx, ev.dy);
            }
          } else {
            let cmds = '';
            for (const ev of payload.events) {
              cmds += `mousemove_relative -- ${ev.dx} ${ev.dy}\n`;
            }
            sendXdotoolCommand(cmds.trim());
          }
          break;

        case 'click':
          // Button mappings: 1 = Left click, 2 = Middle, 3 = Right
          if (payload.double) {
            sendXdotoolCommand(`click --repeat 2 --delay 100 ${payload.button}`);
          } else {
            sendXdotoolCommand(`click ${payload.button}`);
          }
          break;

        case 'mousedown':
          sendXdotoolCommand(`mousedown ${payload.button}`);
          break;

        case 'mouseup':
          sendXdotoolCommand(`mouseup ${payload.button}`);
          break;

        case 'scroll':
          // Scroll up (button 4), scroll down (button 5), scroll left (button 6), scroll right (button 7)
          let scrollBtn = 5;
          if (payload.direction === 'up') scrollBtn = 4;
          else if (payload.direction === 'down') scrollBtn = 5;
          else if (payload.direction === 'left') scrollBtn = 6;
          else if (payload.direction === 'right') scrollBtn = 7;
          
          const steps = payload.steps || 1;
          sendXdotoolCommand(`click --repeat ${steps} --delay 0 ${scrollBtn}`);
          break;

        case 'key':
          // Trigger a keyboard shortcut
          sendXdotoolCommand(`key ${payload.key}`);
          break;

        case 'keydown':
          // Press and hold a key down
          sendXdotoolCommand(`keydown ${payload.key}`);
          break;

        case 'keyup':
          // Release a key
          sendXdotoolCommand(`keyup ${payload.key}`);
          break;

        case 'shortcut':
          handleSystemShortcut(payload.action);
          break;

        case 'log':
          console.log(`[Client Log] ${payload.message}`);
          break;

        case 'settings':
          if (payload.highPolling !== undefined) {
            highPolling = payload.highPolling;
            console.log(`[WebSocket] Settings updated: highPolling = ${highPolling}`);
          }
          break;

        default:
          console.warn('[WebSocket] Unknown payload type:', payload.type);
      }
    } catch (err) {
      console.error('[WebSocket] Error parsing socket message:', err);
    }
  });

  ws.on('close', () => {
    console.log('[WebSocket] Mobile client disconnected');
  });
});

// Execute system-specific commands using xdotool based on the user's BSPWM config
function handleSystemShortcut(action) {
  console.log(`[Shortcut] Triggering system shortcut: ${action}`);
  switch (action) {
    case 'terminal':
      // Super + Enter (Opens default terminal)
      sendXdotoolCommand('key super+Return');
      break;
    case 'browser':
      // Super + b (Opens Thorium web browser)
      sendXdotoolCommand('key super+b');
      break;
    case 'file_manager':
      // Super + f (Opens Thunar File Manager)
      sendXdotoolCommand('key super+f');
      break;
    case 'rofi':
      // Super + Space (Opens Rofi Application menu)
      sendXdotoolCommand('key super+space');
      break;
    case 'close_window':
      // Super + x (Closes current active window)
      sendXdotoolCommand('key super+x');
      break;
    case 'workspace_left':
      // Emulate Super + Left shortcut keypress using xdotool
      sendXdotoolCommand('key super+Left');
      break;
    case 'workspace_right':
      // Emulate Super + Right shortcut keypress using xdotool
      sendXdotoolCommand('key super+Right');
      break;
    case 'powermenu':
      // Super + Alt + p (Opens the Rofi Power menu)
      sendXdotoolCommand('key super+alt+p');
      break;
    case 'wallselect':
      // Super + Alt + w (Opens the Wallpaper select menu)
      sendXdotoolCommand('key super+alt+w');
      break;
    case 'toggle_bar_hide':
      // Super + Alt + h (Hides Polybar / Eww)
      sendXdotoolCommand('key super+alt+h');
      break;
    case 'toggle_bar_show':
      // Super + Alt + u (Unhides Polybar / Eww)
      sendXdotoolCommand('key super+alt+u');
      break;
    default:
      console.warn('[Shortcut] Unknown system shortcut action:', action);
  }
}

// Start Server and display connection info
server.listen(PORT, '0.0.0.0', () => {
  const localIP = getLocalIP();
  const address = `http://${localIP}:${PORT}/?token=${config.token}`;
  
  console.clear();
  console.log('========================================================');
  console.log('                 ZEN-TRACKPAD SERVER                    ');
  console.log('========================================================');
  console.log(`Running locally at: http://localhost:${PORT}`);
  console.log(`Mobile connection URL: ${address}`);
  console.log('--------------------------------------------------------');
  console.log('Scan the QR code below from your phone to connect:');
  
  qrcode.generate(address, { small: true });
  
  console.log('========================================================');
  console.log('Logs:');
});
