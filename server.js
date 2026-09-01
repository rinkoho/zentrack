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

// SHA-256 AES-256-GCM key derivation for E2EE keyboard events
const aesKey = crypto.createHash('sha256').update(config.token).digest();

function decryptKeyboardPayload(payload) {
  try {
    const iv = Buffer.from(payload.iv, 'base64');
    const data = Buffer.from(payload.data, 'base64');
    const tag = Buffer.from(payload.tag, 'base64');

    const decipher = crypto.createDecipheriv('aes-256-gcm', aesKey, iv);
    decipher.setAuthTag(tag);

    const decrypted = Buffer.concat([decipher.update(data), decipher.final()]);
    return JSON.parse(decrypted.toString('utf8'));
  } catch (e) {
    console.error('[AES-256-GCM Decryption Error]:', e.message);
    return null;
  }
}

const PORT = process.env.PORT || 3000;
let xdotool = null;
let gamepadProcess = null;
let gamepadEnabled = false;

// Function to start the persistent Python virtual gamepad process
function startGamepadProcess() {
  if (gamepadProcess) return;
  console.log('[Gamepad] Spawning Python virtual gamepad uinput script...');
  gamepadProcess = spawn('python', [path.join(__dirname, 'virtual_gamepad.py')]);

  gamepadProcess.stdout.on('data', (data) => {
    console.log(`[VirtualGamepad stdout]: ${data.toString().trim()}`);
  });

  gamepadProcess.stderr.on('data', (data) => {
    console.error(`[VirtualGamepad stderr]: ${data.toString().trim()}`);
  });

  gamepadProcess.on('close', (code) => {
    console.log(`[VirtualGamepad] Process exited with code ${code}`);
    gamepadProcess = null;
  });
}

function stopGamepadProcess() {
  if (gamepadProcess) {
    console.log('[Gamepad] Terminating virtual gamepad process...');
    try {
      gamepadProcess.kill();
    } catch (e) {}
    gamepadProcess = null;
  }
}

function sendGamepadCommand(cmd) {
  if (gamepadProcess && gamepadProcess.stdin.writable) {
    gamepadProcess.stdin.write(cmd + '\n');
  }
}

// High-polling & Interpolation State
let highPolling = true;
let interpolationQueue = [];
let interpolationInterval = null;
const activeModifiers = new Set();

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

// --- uinput High-End Touchpad Smooth Scroll Driver ---
let uinputDaemon = null;

function startUInputDaemon() {
  const scriptPath = path.join(__dirname, 'uinput_device.py');
  uinputDaemon = spawn('python3', [scriptPath]);

  uinputDaemon.stdout.on('data', (data) => {
    console.log(`[uinput-touchpad] ${data.toString().trim()}`);
  });

  uinputDaemon.stderr.on('data', (data) => {
    console.error(`[uinput-touchpad err] ${data.toString().trim()}`);
  });

  uinputDaemon.on('close', (code) => {
    console.log(`[uinput-touchpad] Daemon exited code ${code}, restarting...`);
    setTimeout(startUInputDaemon, 1000);
  });
}

startUInputDaemon();

function sendUInput(payload) {
  if (uinputDaemon && uinputDaemon.stdin && uinputDaemon.stdin.writable) {
    uinputDaemon.stdin.write(JSON.stringify(payload) + '\n');
  }
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
      currentRice: config.currentRice || 'tokyo-night',
      connectedCount: clients.length,
      connectedIPs: clients
    }));
    return;
  }

  // Instant Rice Sync API endpoint
  if (pathname === '/api/rice') {
    const riceName = parsedUrl.query.name || 'tokyo-night';
    console.log(`[RiceSync API] Instant Rice Broadcast: ${riceName}`);
    config.currentRice = riceName;

    if (typeof wss !== 'undefined' && wss.clients) {
      const msg = JSON.stringify({ type: 'rice_sync', name: riceName });
      wss.clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
          client.send(msg);
        }
      });
    }

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true, rice: riceName }));
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

// Path to the active bspwm rice configuration file
const RICE_FILE = path.join(os.homedir(), '.config', 'bspwm', '.rice');

// Read the currently active rice from the file
function getActiveRice() {
  if (fs.existsSync(RICE_FILE)) {
    try {
      return fs.readFileSync(RICE_FILE, 'utf8').trim();
    } catch (e) {
      console.error('[ThemeSync] Error reading .rice file:', e);
    }
  }
  return null;
}

// Broadcast JSON message to all open WebSocket clients
function broadcast(payload) {
  const msg = JSON.stringify(payload);
  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(msg);
    }
  });
}

// Watch for changes in active bspwm rice
let watchTimeout = null;
if (fs.existsSync(RICE_FILE)) {
  console.log(`[ThemeSync] Watching for active rice changes in ${RICE_FILE}`);
  
  const handleRiceChange = () => {
    const activeRice = getActiveRice();
    if (activeRice) {
      console.log(`[ThemeSync] Active rice changed to: ${activeRice}`);
      broadcast({ type: 'rice_update', rice: activeRice });
    }
  };

  fs.watch(RICE_FILE, (eventType) => {
    if (watchTimeout) clearTimeout(watchTimeout);
    watchTimeout = setTimeout(handleRiceChange, 100);
  });
} else {
  console.warn(`[ThemeSync] Active rice file not found at ${RICE_FILE}`);
}

wss.on('connection', (ws, req) => {
  const clientIP = req.socket.remoteAddress;

  // Disable Nagle's buffering algorithm for 0ms Wi-Fi packet transmission
  if (req.socket && req.socket.setNoDelay) {
    req.socket.setNoDelay(true);
  }
  if (req.socket && req.socket.setKeepAlive) {
    req.socket.setKeepAlive(true, 1000);
  }

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

  // Send the currently active rice upon connection so the client can synchronize if desired
  const activeRice = getActiveRice();
  if (activeRice) {
    ws.send(JSON.stringify({ type: 'rice_update', rice: activeRice }));
  }

  ws.on('message', (message) => {
    try {
      // ⚡ FAST BINARY PROTOCOL (6-byte Int16Array: [CMD, X, Y])
      if (Buffer.isBuffer(message) && message.length >= 6) {
        const cmd = message.readInt16LE(0);
        const x = message.readInt16LE(2);
        const y = message.readInt16LE(4);

        if (cmd === 1) { // MOVE
          sendUInput({ type: 'move', dx: x, dy: y });
          return;
        } else if (cmd === 2) { // SMOOTH SCROLL
          sendUInput({ type: 'smooth_scroll', dx: x / 10.0, dy: y / 10.0 });
          return;
        }
      }

      let payload = JSON.parse(message);

      if (payload.type === 'enc_key') {
        payload = decryptKeyboardPayload(payload);
        if (!payload) return;
      }
      
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

        case 'smooth_scroll':
          sendUInput({ type: 'smooth_scroll', dx: payload.dx, dy: payload.dy });
          break;

        case 'key':
          // Trigger a keyboard shortcut
          sendXdotoolCommand(`key ${mapXdotoolKey(payload.key)}`);
          break;

        case 'keydown':
          {
            const k = payload.key;
            if (['Alt_L', 'Alt_R', 'Shift_L', 'Shift_R', 'Control_L', 'Control_R', 'Super_L'].includes(k)) {
              activeModifiers.add(k);
            }

            // 1. Emit Hardware Kernel Input Event via /dev/uinput (100% Real Physical Keyboard)
            const uinputOk = uinputDaemon && uinputDaemon.stdin && uinputDaemon.stdin.writable;
            if (uinputOk) {
              sendUInput({ type: 'keydown', key: k });
            } else {
              // 2. Fallback X11 xdotool handling ONLY if uinput is not available
              const isAltGrPressed = activeModifiers.has('Alt_R');
              const isShiftPressed = activeModifiers.has('Shift_L') || activeModifiers.has('Shift_R');

              if (isAltGrPressed) {
                if (k === 'n' || k === 'N') {
                  sendXdotoolCommand(isShiftPressed ? 'key Ntilde' : 'key ntilde');
                  break;
                } else if (k === 'a' || k === 'A') {
                  sendXdotoolCommand(isShiftPressed ? 'key Aacute' : 'key aacute');
                  break;
                } else if (k === 'e' || k === 'E') {
                  sendXdotoolCommand(isShiftPressed ? 'key Eacute' : 'key eacute');
                  break;
                } else if (k === 'i' || k === 'I') {
                  sendXdotoolCommand(isShiftPressed ? 'key Iacute' : 'key iacute');
                  break;
                } else if (k === 'o' || k === 'O') {
                  sendXdotoolCommand(isShiftPressed ? 'key Oacute' : 'key oacute');
                  break;
                } else if (k === 'u' || k === 'U') {
                  sendXdotoolCommand(isShiftPressed ? 'key Uacute' : 'key uacute');
                  break;
                }
              }

              sendXdotoolCommand(`keydown ${mapXdotoolKey(k)}`);
            }
          }
          break;

        case 'keyup':
          {
            const k = payload.key;
            if (['Alt_L', 'Alt_R', 'Shift_L', 'Shift_R', 'Control_L', 'Control_R', 'Super_L'].includes(k)) {
              activeModifiers.delete(k);
            }
            const uinputOk = uinputDaemon && uinputDaemon.stdin && uinputDaemon.stdin.writable;
            if (uinputOk) {
              sendUInput({ type: 'keyup', key: k });
            } else {
              sendXdotoolCommand(`keyup ${mapXdotoolKey(k)}`);
            }
          }
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

        case 'gamepad_mode':
          gamepadEnabled = !!payload.enabled;
          if (gamepadEnabled) {
            startGamepadProcess();
          } else {
            stopGamepadProcess();
          }
          break;

        case 'gp_btn':
          console.log(`[Gamepad Button] ${payload.name} = ${payload.value}`);
          sendGamepadCommand(`btn ${payload.name} ${payload.value}`);
          break;

        case 'gp_axis':
          console.log(`[Gamepad Axis] ${payload.name} = ${payload.value}`);
          sendGamepadCommand(`axis ${payload.name} ${payload.value}`);
          break;

        default:
          console.warn('[WebSocket] Unknown payload type:', payload.type);
      }
    } catch (err) {
      console.error('[WebSocket] Error parsing socket message:', err);
    }
  });

  ws.on('close', () => {
    console.log('[WebSocket] Mobile client disconnected. Releasing modifier keys for safety...');
    sendXdotoolCommand('keyup Super_L keyup Super_R keyup Control_L keyup Control_R keyup Alt_L keyup Alt_R keyup ISO_Level3_Shift keyup Shift_L keyup Shift_R');
    stopGamepadProcess();
  });
});

function mapXdotoolKey(key) {
  if (!key) return '';
  if (key === 'Alt_R') return 'ISO_Level3_Shift';
  return key;
}

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
