const http = require('http');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const ASSETS_DIR = path.join(__dirname, '../android/app/src/main/assets/sounds');
if (!fs.existsSync(ASSETS_DIR)) {
  fs.mkdirSync(ASSETS_DIR, { recursive: true });
}

let savedCount = 0;
const TOTAL_SOUNDS = 16; // 8 profiles x 2 states

const server = http.createServer((req, res) => {
  if (req.method === 'POST' && req.url.startsWith('/save_wav/')) {
    const filename = req.url.replace('/save_wav/', '');
    const filepath = path.join(ASSETS_DIR, filename);
    const writeStream = fs.createWriteStream(filepath);

    req.pipe(writeStream);

    req.on('end', () => {
      savedCount++;
      console.log(`[Audio Renderer] Saved (${savedCount}/${TOTAL_SOUNDS}): ${filename}`);
      res.writeHead(200, { 'Content-Type': 'text/plain' });
      res.end('OK');

      if (savedCount >= TOTAL_SOUNDS) {
        console.log('✅ ALL 16 WEBAUDIO SWITCH SOUNDS RENDERED & SAVED TO ASSETS!');
        setTimeout(() => process.exit(0), 1000);
      }
    });
  } else {
    // Serve HTML
    const htmlPath = path.join(__dirname, 'render_sounds.html');
    if (fs.existsSync(htmlPath)) {
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(fs.readFileSync(htmlPath));
    } else {
      res.writeHead(404);
      res.end('Not found');
    }
  }
});

server.listen(8899, () => {
  console.log('[Audio Renderer] Server running at http://localhost:8899');
  console.log('[Audio Renderer] Launching headless browser to synthesize exact WebAudio WAV files...');
  exec('firefox --headless http://localhost:8899');
});
