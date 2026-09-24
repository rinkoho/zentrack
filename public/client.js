// --- Settings & Configuration State ---
const settings = {
  sensitivity: 1.2,
  scrollSensitivity: 1.0,
  vibration: true,
  keepAwake: true,
  highPolling: true,
  showButtons: true,
  showScroll: true,
  accelerationEnabled: true,
  accelerationIntensity: 1.0,
  naturalScroll: true,
  profile: 'control-total', // 'control-total' | 'trackpad-only' | 'keyboard-65'
  keySound: true,
  soundProfile: 'cherry-blue',
  soundVolume: 0.8,
  keyboardTheme: 'carbon',
  syncTheme: false,
  keyCaster: true,
  gamepadPreset: 'fps',
  gamepadPhysical: false,
  gamepadFullscreenTrackpad: true,
  gamepadMapping: {
    joyUp: 'w', joyDown: 's', joyLeft: 'a', joyRight: 'd',
    btnA: 'space', btnB: 'e', btnX: 'r', btnY: 'q',
    btnL: 'click_right', btnR: 'click_left',
    btnSelect: 'Escape', btnStart: 'Return'
  },
  gamepadLayout: {
    joystick: { x: 8, y: 40, scale: 1.0 },
    trackpad: { x: 44, y: 40, width: 170, height: 120 },
    abxy: { x: 74, y: 40, scale: 1.0 },
    bumperL: { x: 3, y: 15, scale: 1.0 },
    bumperR: { x: 85, y: 15, scale: 1.0 },
    centerNav: { x: 42, y: 15, scale: 1.0 }
  }
};

// Apply layout modifiers to body based on settings
function applyLayoutSettings() {
  const body = document.body;

  // 1. Show/Hide physical mouse buttons
  if (settings.showButtons) {
    body.classList.remove('hide-buttons');
  } else {
    body.classList.add('hide-buttons');
  }

  // 2. Show/Hide right scroll strip
  if (settings.showScroll) {
    body.classList.remove('hide-scroll');
  } else {
    body.classList.add('hide-scroll');
  }

  // 3. Switch profiles
  if (settings.profile === 'trackpad-only') {
    body.classList.remove('profile-control-total', 'profile-keyboard-65', 'profile-hybrid-65');
    body.classList.add('profile-trackpad-only');
    document.getElementById('trackpad-surface').style.display = 'flex';
    document.getElementById('keyboard-surface').style.display = 'none';
    document.getElementById('keyboard-dashboard').style.display = 'none';
    document.getElementById('gamepad-surface').style.display = 'none';
  } else if (settings.profile === 'keyboard-65') {
    body.classList.remove('profile-control-total', 'profile-trackpad-only', 'profile-hybrid-65', 'profile-gamepad-steam');
    body.classList.add('profile-keyboard-65');
    document.getElementById('trackpad-surface').style.display = 'none';
    document.getElementById('keyboard-surface').style.display = 'flex';
    document.getElementById('keyboard-dashboard').style.display = settings.keyCaster ? 'flex' : 'none';
    document.getElementById('gamepad-surface').style.display = 'none';
    renderKeyboard();
    document.getElementById('gamepad-sizes-section').style.display = 'none';
  } else if (settings.profile === 'hybrid-65') {
    body.classList.remove('profile-control-total', 'profile-trackpad-only', 'profile-keyboard-65', 'profile-gamepad-steam');
    body.classList.add('profile-hybrid-65');
    document.getElementById('trackpad-surface').style.display = 'flex';
    document.getElementById('keyboard-surface').style.display = 'flex';
    document.getElementById('keyboard-dashboard').style.display = 'none';
    document.getElementById('gamepad-surface').style.display = 'none';
    document.getElementById('gamepad-sizes-section').style.display = 'none';
    renderKeyboard();
  } else if (settings.profile === 'gamepad-steam') {
    body.classList.remove('profile-control-total', 'profile-trackpad-only', 'profile-keyboard-65', 'profile-hybrid-65');
    body.classList.add('profile-gamepad-steam');
    document.getElementById('trackpad-surface').style.display = 'none';
    document.getElementById('keyboard-surface').style.display = 'none';
    document.getElementById('keyboard-dashboard').style.display = 'none';
    document.getElementById('gamepad-surface').style.display = 'flex';
    document.getElementById('gamepad-sizes-section').style.display = 'block';
    applyGamepadLayout();
    initGamepadControls();
  } else {
    body.classList.remove('profile-trackpad-only', 'profile-keyboard-65', 'profile-hybrid-65', 'profile-gamepad-steam');
    body.classList.add('profile-control-total');
    document.getElementById('trackpad-surface').style.display = 'flex';
    document.getElementById('keyboard-surface').style.display = 'none';
    document.getElementById('keyboard-dashboard').style.display = 'none';
    document.getElementById('gamepad-surface').style.display = 'none';
    document.getElementById('gamepad-sizes-section').style.display = 'none';
  }

  // 4. Highlight active profile item in the sidebar
  document.querySelectorAll('.menu-item').forEach(item => {
    if (item.getAttribute('data-profile') === settings.profile) {
      item.classList.add('active');
    } else {
      item.classList.remove('active');
    }
  });

  // Apply keyboard theme class to surface, dashboard and HUD
  const kbSurface = document.getElementById('keyboard-surface');
  const kbDashboard = document.getElementById('keyboard-dashboard');
  const hudCaster = document.getElementById('hud-caster');
  const theme = settings.keyboardTheme || 'carbon';

  if (kbSurface) {
    const isThemeChanged = !kbSurface.classList.contains(`theme-${theme}`);
    kbSurface.className = 'keyboard-surface';
    if (isThemeChanged) {
      kbSurface.classList.add('theme-changing');
    }
    kbSurface.classList.add(`theme-${theme}`);
    
    if (isThemeChanged) {
      if (window.themeChangeTimeout) clearTimeout(window.themeChangeTimeout);
      window.themeChangeTimeout = setTimeout(() => {
        kbSurface.classList.remove('theme-changing');
      }, 500);
    }
  }
  if (kbDashboard) {
    kbDashboard.className = 'keyboard-dashboard';
    kbDashboard.classList.add(`theme-${theme}`);
    const dbStatusTheme = document.getElementById('db-status-theme');
    if (dbStatusTheme) {
      dbStatusTheme.innerText = theme;
    }
  }
  if (hudCaster) {
    hudCaster.className = 'hud-caster';
    hudCaster.classList.add(`theme-${theme}`);
  }

  // Force trigger browser layout calculations (essential for landscape swaps)
  window.dispatchEvent(new Event('resize'));
}

// Load saved settings from localStorage if available
function loadSettings() {
  const saved = localStorage.getItem('zentrack_settings');
  if (saved) {
    try {
      const parsed = JSON.parse(saved);
      Object.assign(settings, parsed);
      
      // Update UI elements to match loaded settings
      document.getElementById('slider-sensitivity').value = settings.sensitivity;
      document.getElementById('val-sensitivity').innerText = settings.sensitivity.toFixed(1) + 'x';
      
      document.getElementById('slider-scroll').value = settings.scrollSensitivity;
      document.getElementById('val-scroll').innerText = settings.scrollSensitivity.toFixed(1) + 'x';
      
      document.getElementById('slider-acceleration').value = settings.accelerationIntensity;
      document.getElementById('val-acceleration').innerText = settings.accelerationIntensity.toFixed(1) + 'x';
      
      document.getElementById('toggle-vibration').checked = settings.vibration;
      document.getElementById('toggle-wakelock').checked = settings.keepAwake;
      document.getElementById('toggle-highpolling').checked = settings.highPolling;
      
      document.getElementById('toggle-showbuttons').checked = settings.showButtons;
      document.getElementById('toggle-showscroll').checked = settings.showScroll;
      document.getElementById('toggle-acceleration').checked = settings.accelerationEnabled;
      document.getElementById('toggle-naturalscroll').checked = settings.naturalScroll;
      
      const toggleKeySound = document.getElementById('toggle-key-sound');
      if (toggleKeySound) {
        toggleKeySound.checked = settings.keySound;
      }
      
      const selectSoundProfile = document.getElementById('select-sound-profile');
      if (selectSoundProfile) {
        selectSoundProfile.value = settings.soundProfile || 'cherry-blue';
      }

      if (settings.soundVolume === undefined) {
        settings.soundVolume = 0.8;
      }
      const sliderSoundVolume = document.getElementById('slider-sound-volume');
      if (sliderSoundVolume) {
        sliderSoundVolume.value = settings.soundVolume;
      }
      const valSoundVolume = document.getElementById('val-sound-volume');
      if (valSoundVolume) {
        valSoundVolume.innerText = (settings.soundVolume * 100).toFixed(0) + '%';
      }

      const selectKeyboardTheme = document.getElementById('select-keyboard-theme');
      if (selectKeyboardTheme) {
        selectKeyboardTheme.value = settings.keyboardTheme || 'carbon';
      }

      const toggleSyncTheme = document.getElementById('toggle-sync-theme');
      if (toggleSyncTheme) {
        toggleSyncTheme.checked = !!settings.syncTheme;
      }

      if (settings.keyCaster === undefined) {
        settings.keyCaster = true;
      }
      const toggleKeyCaster = document.getElementById('toggle-keycaster');
      if (toggleKeyCaster) {
        toggleKeyCaster.checked = !!settings.keyCaster;
      }

      const selectGamepadPreset = document.getElementById('select-gamepad-preset');
      if (selectGamepadPreset) {
        selectGamepadPreset.value = settings.gamepadPreset || 'fps';
      }
      
      // Ensure layout exists defensively
      if (!settings.gamepadLayout) {
        settings.gamepadLayout = {
          joystick: { x: 8, y: 40, scale: 1.0 },
          trackpad: { x: 44, y: 40, width: 170, height: 120 },
          abxy: { x: 74, y: 40, scale: 1.0 },
          bumperL: { x: 3, y: 15, scale: 1.0 },
          bumperR: { x: 85, y: 15, scale: 1.0 },
          centerNav: { x: 42, y: 15, scale: 1.0 }
        };
      }
      
      // Synchronize mappings and slider sizes to keymap UI selects
      syncGamepadMappingUI();
      syncGamepadSizesUI();

      if (settings.gamepadPhysical === undefined) settings.gamepadPhysical = false;
      const toggleGpPhysical = document.getElementById('toggle-gamepad-physical');
      if (toggleGpPhysical) {
        toggleGpPhysical.checked = !!settings.gamepadPhysical;
      }

      if (settings.gamepadFullscreenTrackpad === undefined) settings.gamepadFullscreenTrackpad = true;
      const toggleGpFullscreen = document.getElementById('toggle-gamepad-fullscreen-trackpad');
      if (toggleGpFullscreen) {
        toggleGpFullscreen.checked = !!settings.gamepadFullscreenTrackpad;
      }
    } catch (e) {
      console.error('Error loading settings:', e);
    }
  }
  
  // Set UI disabled status and opacity based on accelerationEnabled setting
  document.getElementById('item-acc-intensity').style.opacity = settings.accelerationEnabled ? '1' : '0.4';
  document.getElementById('slider-acceleration').disabled = !settings.accelerationEnabled;

  applyLayoutSettings();
}

function saveSettings() {
  localStorage.setItem('zentrack_settings', JSON.stringify(settings));
}

// --- Haptic Feedback Utility ---
function triggerHaptic(type = 'click') {
  if (!settings.vibration || !('vibrate' in navigator)) return;
  
  if (type === 'click') {
    navigator.vibrate(12); // Short crisp vibration
  } else if (type === 'right-click') {
    navigator.vibrate(25); // Slightly heavier vibration
  } else if (type === 'middle-click') {
    navigator.vibrate(18);
  } else if (type === 'success') {
    navigator.vibrate([15, 30, 15]);
  }
}

// --- Low Latency Web Audio API Mechanical Switch Sound Synthesizer ---
let audioCtx = null;

function initAudioContext() {
  if (!audioCtx) {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
  }
  if (audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
}

function playSwitchSound(isPress = true, keyCode = '') {
  if (!settings.keySound) return;
  try {
    initAudioContext();
    if (!audioCtx) return;

    const profile = settings.soundProfile || 'cherry-blue';
    const volume = settings.soundVolume !== undefined ? settings.soundVolume : 0.8;
    const now = audioCtx.currentTime;

    // --- Dynamic Acoustic Scaling Factors based on Keycap Dimensions ---
    let pitchScale = 1.0;
    let cutoffScale = 1.0;
    let gainScale = 1.0;
    let decayScale = 1.0;

    if (keyCode === 'space') {
      // Spacebar: deep hollow acoustics, high volume, slightly longer decay
      pitchScale = 0.68;
      cutoffScale = 0.70;
      gainScale = 1.35;
      decayScale = 1.25;
    } else if (['Return', 'BackSpace', 'Shift_L', 'Shift_R', 'Caps_Lock', 'Tab'].includes(keyCode)) {
      // Modifiers / Stabilized keys: mid-low pitch clacks, slightly louder
      pitchScale = 0.84;
      cutoffScale = 0.85;
      gainScale = 1.15;
      decayScale = 1.15;
    }

    if (profile === 'cherry-blue' || profile === 'cherry-blue-full') {
      // Cherry MX Blue: High frequency mechanical switch click + housing bottom-out clack
      const clickFreq = (isPress ? 900 : 1300) * pitchScale;
      const clickCutoff = (isPress ? 1400 : 2100) * cutoffScale;
      const clickGain = (isPress ? 0.35 : 0.22) * gainScale;

      // 1. High Frequency Actuation Click
      const oscClick = audioCtx.createOscillator();
      const gainClick = audioCtx.createGain();
      const filterClick = audioCtx.createBiquadFilter();

      filterClick.type = 'highpass';
      filterClick.frequency.setValueAtTime(clickCutoff, now);

      oscClick.type = 'triangle';
      oscClick.frequency.setValueAtTime(clickFreq, now);
      oscClick.frequency.exponentialRampToValueAtTime(isPress ? (clickFreq / 6) : (clickFreq / 5), now + 0.015 * decayScale);

      gainClick.gain.setValueAtTime(clickGain * volume, now);
      gainClick.gain.exponentialRampToValueAtTime(0.001, now + 0.015 * decayScale);

      oscClick.connect(filterClick);
      filterClick.connect(gainClick);
      gainClick.connect(audioCtx.destination);

      oscClick.start(now);
      oscClick.stop(now + 0.015 * decayScale + 0.005);

      // 2. Low Frequency Housing Bottom-out "Thock"
      if (isPress) {
        const thockFreqStart = 130 * pitchScale;
        const thockFreqEnd = 75 * pitchScale;
        const thockCutoff = 320 * cutoffScale;
        const thockGain = 0.5 * gainScale;

        const oscThock = audioCtx.createOscillator();
        const gainThock = audioCtx.createGain();
        const filterThock = audioCtx.createBiquadFilter();

        filterThock.type = 'lowpass';
        filterThock.frequency.setValueAtTime(thockCutoff, now);

        oscThock.type = 'sine';
        oscThock.frequency.setValueAtTime(thockFreqStart, now);
        oscThock.frequency.exponentialRampToValueAtTime(thockFreqEnd, now + 0.035 * decayScale);

        gainThock.gain.setValueAtTime(thockGain * volume, now);
        gainThock.gain.exponentialRampToValueAtTime(0.001, now + 0.045 * decayScale);

        oscThock.connect(filterThock);
        filterThock.connect(gainThock);
        gainThock.connect(audioCtx.destination);

        oscThock.start(now);
        oscThock.stop(now + 0.045 * decayScale + 0.005);
      }
    } else if (profile === 'chocolate') {
      // Chocolate Crujiente Snap: Dry, organic wooden snap with no metallic resonance
      const snapFreq = (isPress ? 1150 : 1300) * pitchScale;
      const snapCutoff = (isPress ? 1050 : 1200) * cutoffScale;
      const snapGain = (isPress ? 0.7 : 0.22) * gainScale;
      const snapDecay = (isPress ? 0.012 : 0.008) * decayScale;

      const clackFreqStart = 160 * pitchScale;
      const clackFreqEnd = 95 * pitchScale;
      const clackCutoff = 260 * cutoffScale;
      const clackGain = 0.6 * gainScale;
      const clackDecay = 0.024 * decayScale;

      // 1. High Frequency Dry Snap Click
      const oscSnap = audioCtx.createOscillator();
      const gainSnap = audioCtx.createGain();
      const filterSnap = audioCtx.createBiquadFilter();

      filterSnap.type = 'bandpass';
      filterSnap.frequency.setValueAtTime(snapCutoff, now);
      filterSnap.Q.setValueAtTime(4.0, now);

      oscSnap.type = 'triangle';
      oscSnap.frequency.setValueAtTime(snapFreq, now);
      oscSnap.frequency.exponentialRampToValueAtTime(isPress ? (snapFreq / 3.5) : (snapFreq / 2.5), now + snapDecay);

      gainSnap.gain.setValueAtTime(snapGain * volume, now);
      gainSnap.gain.exponentialRampToValueAtTime(0.001, now + snapDecay);

      oscSnap.connect(filterSnap);
      filterSnap.connect(gainSnap);
      gainSnap.connect(audioCtx.destination);

      oscSnap.start(now);
      oscSnap.stop(now + snapDecay + 0.005);

      // 2. Low Frequency Dense Woody Clack
      if (isPress) {
        const oscClack = audioCtx.createOscillator();
        const gainClack = audioCtx.createGain();
        const filterClack = audioCtx.createBiquadFilter();

        filterClack.type = 'lowpass';
        filterClack.frequency.setValueAtTime(clackCutoff, now);

        oscClack.type = 'sine';
        oscClack.frequency.setValueAtTime(clackFreqStart, now);
        oscClack.frequency.exponentialRampToValueAtTime(clackFreqEnd, now + clackDecay);

        gainClack.gain.setValueAtTime(clackGain * volume, now);
        gainClack.gain.exponentialRampToValueAtTime(0.001, now + clackDecay);

        oscClack.connect(filterClack);
        filterClack.connect(gainClack);
        gainClack.connect(audioCtx.destination);

        oscClack.start(now);
        oscClack.stop(now + clackDecay + 0.005);
      }
    } else if (profile === 'cherry-red') {
      // Lubed Linear Switch "Thock" (No click transient, just deep bottom-out housing thock)
      if (isPress) {
        const thockFreqStart = 110 * pitchScale;
        const thockFreqEnd = 65 * pitchScale;
        const thockCutoff = 250 * cutoffScale;
        const thockGain = 0.7 * gainScale;

        const oscThock = audioCtx.createOscillator();
        const gainThock = audioCtx.createGain();
        const filterThock = audioCtx.createBiquadFilter();

        filterThock.type = 'lowpass';
        filterThock.frequency.setValueAtTime(thockCutoff, now);

        oscThock.type = 'triangle';
        oscThock.frequency.setValueAtTime(thockFreqStart, now);
        oscThock.frequency.exponentialRampToValueAtTime(thockFreqEnd, now + 0.04 * decayScale);

        gainThock.gain.setValueAtTime(thockGain * volume, now);
        gainThock.gain.exponentialRampToValueAtTime(0.001, now + 0.045 * decayScale);

        oscThock.connect(filterThock);
        filterThock.connect(gainThock);
        gainThock.connect(audioCtx.destination);

        oscThock.start(now);
        oscThock.stop(now + 0.045 * decayScale + 0.005);
      } else {
        const thockFreqStart = 140 * pitchScale;
        const thockFreqEnd = 90 * pitchScale;
        const thockCutoff = 300 * cutoffScale;
        const thockGain = 0.2 * gainScale;

        const oscThock = audioCtx.createOscillator();
        const gainThock = audioCtx.createGain();
        const filterThock = audioCtx.createBiquadFilter();

        filterThock.type = 'lowpass';
        filterThock.frequency.setValueAtTime(thockCutoff, now);

        oscThock.type = 'sine';
        oscThock.frequency.setValueAtTime(thockFreqStart, now);
        oscThock.frequency.exponentialRampToValueAtTime(thockFreqEnd, now + 0.02 * decayScale);

        gainThock.gain.setValueAtTime(thockGain * volume, now);
        gainThock.gain.exponentialRampToValueAtTime(0.001, now + 0.025 * decayScale);

        oscThock.connect(filterThock);
        filterThock.connect(gainThock);
        gainThock.connect(audioCtx.destination);

        oscThock.start(now);
        oscThock.stop(now + 0.025 * decayScale + 0.005);
      }
    } else if (profile === 'box-navy') {
      // Kailh Box Navy: Double-click feel, extremely loud and sharp click bar clack
      const clickFreq = (isPress ? 1500 : 1400) * pitchScale;
      const clickCutoff = (isPress ? 2200 : 2000) * cutoffScale;
      const clickGain = (isPress ? 0.55 : 0.42) * gainScale;
      const clickDecay = 0.012 * decayScale;

      const oscClick = audioCtx.createOscillator();
      const gainClick = audioCtx.createGain();
      const filterClick = audioCtx.createBiquadFilter();

      filterClick.type = 'highpass';
      filterClick.frequency.setValueAtTime(clickCutoff, now);

      oscClick.type = 'triangle';
      oscClick.frequency.setValueAtTime(clickFreq, now);
      oscClick.frequency.exponentialRampToValueAtTime(250 * pitchScale, now + clickDecay);

      gainClick.gain.setValueAtTime(clickGain * volume, now);
      gainClick.gain.exponentialRampToValueAtTime(0.001, now + clickDecay);

      oscClick.connect(filterClick);
      filterClick.connect(gainClick);
      gainClick.connect(audioCtx.destination);

      oscClick.start(now);
      oscClick.stop(now + clickDecay + 0.005);

      if (isPress) {
        const thockFreqStart = 140 * pitchScale;
        const thockFreqEnd = 80 * pitchScale;
        const thockCutoff = 450 * cutoffScale;
        const thockGain = 0.6 * gainScale;
        const thockDecay = 0.035 * decayScale;

        const oscThock = audioCtx.createOscillator();
        const gainThock = audioCtx.createGain();
        const filterThock = audioCtx.createBiquadFilter();

        filterThock.type = 'lowpass';
        filterThock.frequency.setValueAtTime(thockCutoff, now);

        oscThock.type = 'sine';
        oscThock.frequency.setValueAtTime(thockFreqStart, now);
        oscThock.frequency.exponentialRampToValueAtTime(thockFreqEnd, now + thockDecay);

        gainThock.gain.setValueAtTime(thockGain * volume, now);
        gainThock.gain.exponentialRampToValueAtTime(0.001, now + thockDecay + 0.005);

        oscThock.connect(filterThock);
        filterThock.connect(gainThock);
        gainThock.connect(audioCtx.destination);

        oscThock.start(now);
        oscThock.stop(now + thockDecay + 0.01);
      }
    } else if (profile === 'buckling-spring') {
      // Buckling Spring (IBM Model M): Crisp click + steel spring ringing resonance (ping)
      const clickFreq = (isPress ? 950 : 1300) * pitchScale;
      const clickCutoff = (isPress ? 1100 : 1500) * cutoffScale;
      const clickGain = (isPress ? 0.32 : 0.18) * gainScale;
      const clickDecay = 0.016 * decayScale;

      const oscClick = audioCtx.createOscillator();
      const gainClick = audioCtx.createGain();
      const filterClick = audioCtx.createBiquadFilter();

      filterClick.type = 'bandpass';
      filterClick.frequency.setValueAtTime(clickCutoff, now);

      oscClick.type = 'triangle';
      oscClick.frequency.setValueAtTime(clickFreq, now);
      oscClick.frequency.exponentialRampToValueAtTime(isPress ? 200 * pitchScale : 300 * pitchScale, now + clickDecay);

      gainClick.gain.setValueAtTime(clickGain * volume, now);
      gainClick.gain.exponentialRampToValueAtTime(0.001, now + clickDecay);

      oscClick.connect(filterClick);
      filterClick.connect(gainClick);
      gainClick.connect(audioCtx.destination);

      oscClick.start(now);
      oscClick.stop(now + clickDecay + 0.005);

      // Spring ring resonance ping (sine oscillator with longer decay)
      if (isPress) {
        const pingFreq = 2500 * pitchScale;
        const pingCutoff = 2800 * cutoffScale;
        const pingGain = 0.045 * gainScale;
        const pingDecay = 0.12 * decayScale;

        const oscPing = audioCtx.createOscillator();
        const gainPing = audioCtx.createGain();
        const filterPing = audioCtx.createBiquadFilter();

        filterPing.type = 'bandpass';
        filterPing.frequency.setValueAtTime(pingCutoff, now);
        filterPing.Q.setValueAtTime(3.0, now);

        oscPing.type = 'sine';
        oscPing.frequency.setValueAtTime(pingFreq, now);

        gainPing.gain.setValueAtTime(pingGain * volume, now);
        gainPing.gain.exponentialRampToValueAtTime(0.001, now + pingDecay);

        oscPing.connect(filterPing);
        filterPing.connect(gainPing);
        gainPing.connect(audioCtx.destination);

        oscPing.start(now);
        oscPing.stop(now + pingDecay + 0.01);

        // Buckling bottom-out housing clack
        const thockFreqStart = 120 * pitchScale;
        const thockFreqEnd = 70 * pitchScale;
        const thockCutoff = 300 * cutoffScale;
        const thockGain = 0.4 * gainScale;
        const thockDecay = 0.04 * decayScale;

        const oscThock = audioCtx.createOscillator();
        const gainThock = audioCtx.createGain();
        const filterThock = audioCtx.createBiquadFilter();

        filterThock.type = 'lowpass';
        filterThock.frequency.setValueAtTime(thockCutoff, now);

        oscThock.type = 'sine';
        oscThock.frequency.setValueAtTime(thockFreqStart, now);
        oscThock.frequency.exponentialRampToValueAtTime(thockFreqEnd, now + thockDecay);

        gainThock.gain.setValueAtTime(thockGain * volume, now);
        gainThock.gain.exponentialRampToValueAtTime(0.001, now + thockDecay + 0.005);

        oscThock.connect(filterThock);
        filterThock.connect(gainThock);
        gainThock.connect(audioCtx.destination);

        oscThock.start(now);
        oscThock.stop(now + thockDecay + 0.01);
      }
    } else if (profile === 'topre') {
      // Topre (Capacitive Plop): Warm, rounded, dome-over-spring sound. No click, just "plop".
      const plopFreqStart = (isPress ? 140 : 180) * pitchScale;
      const plopFreqEnd = (isPress ? 85 : 110) * pitchScale;
      const plopCutoff = (isPress ? 220 : 280) * cutoffScale;
      const plopGain = (isPress ? 0.7 : 0.3) * gainScale;
      const plopDecay = (isPress ? 0.035 : 0.03) * decayScale;

      const oscThock = audioCtx.createOscillator();
      const gainThock = audioCtx.createGain();
      const filterThock = audioCtx.createBiquadFilter();

      filterThock.type = 'lowpass';
      filterThock.frequency.setValueAtTime(plopCutoff, now);

      oscThock.type = 'sine';
      oscThock.frequency.setValueAtTime(plopFreqStart, now);
      oscThock.frequency.exponentialRampToValueAtTime(plopFreqEnd, now + plopDecay);

      gainThock.gain.setValueAtTime(plopGain * volume, now);
      gainThock.gain.exponentialRampToValueAtTime(0.001, now + plopDecay + 0.003);

      oscThock.connect(filterThock);
      filterThock.connect(gainThock);
      gainThock.connect(audioCtx.destination);

      oscThock.start(now);
      oscThock.stop(now + plopDecay + 0.005);
    } else if (profile === 'membrane') {
      // Soft rubber dome membrane sound (very quiet, low-pass filtered click)
      const domeFreqStart = (isPress ? 150 : 200) * pitchScale;
      const domeFreqEnd = (isPress ? 80 : 120) * pitchScale;
      const domeCutoff = 400 * cutoffScale;
      const domeGain = (isPress ? 0.4 : 0.15) * gainScale;
      const domeDecay = 0.02 * decayScale;

      const oscClick = audioCtx.createOscillator();
      const gainClick = audioCtx.createGain();
      const filterClick = audioCtx.createBiquadFilter();

      filterClick.type = 'lowpass';
      filterClick.frequency.setValueAtTime(domeCutoff, now);

      oscClick.type = 'sine';
      oscClick.frequency.setValueAtTime(domeFreqStart, now);
      oscClick.frequency.exponentialRampToValueAtTime(domeFreqEnd, now + domeDecay);

      gainClick.gain.setValueAtTime(domeGain * volume, now);
      gainClick.gain.exponentialRampToValueAtTime(0.001, now + domeDecay + 0.005);

      oscClick.connect(filterClick);
      filterClick.connect(gainClick);
      gainClick.connect(audioCtx.destination);

      oscClick.start(now);
      oscClick.stop(now + domeDecay + 0.01);
    } else if (profile === 'bubble-wrap') {
      // Bubble Wrap Popping!
      if (isPress) {
        const popFreqStart = 450 * pitchScale;
        const popFreqEnd = 1100 * pitchScale;
        const popCutoff = 750 * cutoffScale;
        const popGain = 0.5 * gainScale;
        const popDecay = 0.015 * decayScale;

        const oscPop = audioCtx.createOscillator();
        const gainPop = audioCtx.createGain();
        const filterPop = audioCtx.createBiquadFilter();

        filterPop.type = 'bandpass';
        filterPop.frequency.setValueAtTime(popCutoff, now);

        oscPop.type = 'sine';
        oscPop.frequency.setValueAtTime(popFreqStart, now);
        oscPop.frequency.exponentialRampToValueAtTime(popFreqEnd, now + popDecay);

        gainPop.gain.setValueAtTime(popGain * volume, now);
        gainPop.gain.exponentialRampToValueAtTime(0.001, now + popDecay);

        oscPop.connect(filterPop);
        filterPop.connect(gainPop);
        gainPop.connect(audioCtx.destination);

        oscPop.start(now);
        oscPop.stop(now + popDecay + 0.005);
      }
    }
  } catch (err) {
    console.error('[Audio] Error synthesizing mechanical sound:', err);
  }
}

// --- Screen Wake Lock (Keep Screen On) ---
let wakeLock = null;
async function requestWakeLock() {
  if (!settings.keepAwake) {
    releaseWakeLock();
    return;
  }
  try {
    if ('wakeLock' in navigator) {
      wakeLock = await navigator.wakeLock.request('screen');
      console.log('[WakeLock] Screen Wake Lock active');
    }
  } catch (err) {
    console.warn(`[WakeLock] Failed: ${err.name}, ${err.message}`);
  }
}

function releaseWakeLock() {
  if (wakeLock !== null) {
    wakeLock.release()
      .then(() => {
        wakeLock = null;
        console.log('[WakeLock] Screen Wake Lock released');
      });
  }
}

// Auto-reacquire WakeLock if browser tab goes to background and comes back
document.addEventListener('visibilitychange', async () => {
  if (wakeLock !== null && document.visibilityState === 'visible') {
    await requestWakeLock();
  }
});

// --- WebSocket Communication ---
let socket = null;
let reconnectTimer = null;
let pingInterval = null;
let lastPingTime = 0;
const statusBadge = document.getElementById('status-badge');
const statusText = document.getElementById('status-text');
const latencyBadge = document.getElementById('latency-badge');
const latencyValue = document.getElementById('latency-value');
const ipDisplay = document.getElementById('sidebar-ip-display');

function showTokenPrompt() {
  if (document.getElementById('token-security-overlay')) return;

  const overlay = document.createElement('div');
  overlay.id = 'token-security-overlay';
  overlay.style.position = 'fixed';
  overlay.style.top = '0';
  overlay.style.left = '0';
  overlay.style.width = '100%';
  overlay.style.height = '100%';
  overlay.style.backgroundColor = '#080b11';
  overlay.style.display = 'flex';
  overlay.style.flexDirection = 'column';
  overlay.style.alignItems = 'center';
  overlay.style.justifyContent = 'center';
  overlay.style.zIndex = '9999';
  overlay.style.padding = '20px';
  overlay.style.boxSizing = 'border-box';
  overlay.style.textAlign = 'center';
  overlay.style.fontFamily = 'system-ui, -apple-system, sans-serif';

  overlay.innerHTML = `
    <div style="max-width: 400px; width: 100%; padding: 32px; border: 1px solid rgba(0, 242, 254, 0.15); border-radius: 24px; background: rgba(16, 22, 35, 0.9); backdrop-filter: blur(20px); box-shadow: 0 12px 40px rgba(0,0,0,0.5);">
      <h1 style="color: #00f2fe; margin-top: 0; font-size: 24px; font-weight: 700; margin-bottom: 12px;">Vincular Dispositivo 🔒</h1>
      <p style="color: #90a0b7; line-height: 1.6; font-size: 14px; margin-bottom: 24px;">Este trackpad está protegido. Escanea el código QR de la computadora o introduce el token de seguridad manualmente:</p>
      <input type="text" id="security-token-input" placeholder="Introduce el token aquí..." style="width: 100%; padding: 14px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.08); background: rgba(28, 38, 57, 0.6); color: #fff; text-align: center; font-size: 16px; margin-bottom: 16px; box-sizing: border-box; outline: none; transition: border-color 0.3s;">
      <button id="save-token-btn" style="width: 100%; padding: 14px; border-radius: 12px; border: none; background: linear-gradient(135deg, #00f2fe, #b927fc); color: #fff; font-size: 16px; font-weight: 700; cursor: pointer; transition: opacity 0.2s;">Vincular Trackpad</button>
    </div>
  `;

  document.body.appendChild(overlay);

  const input = document.getElementById('security-token-input');
  const btn = document.getElementById('save-token-btn');

  // Input styling focus states
  input.addEventListener('focus', () => {
    input.style.borderColor = '#00f2fe';
  });
  input.addEventListener('blur', () => {
    input.style.borderColor = 'rgba(255,255,255,0.08)';
  });

  const saveToken = () => {
    const enteredToken = input.value.trim();
    if (enteredToken) {
      localStorage.setItem('zentrack_token', enteredToken);
      overlay.remove();
      connectWebSocket();
    } else {
      input.style.borderColor = '#ff3b30';
      triggerHaptic('error');
    }
  };

  btn.addEventListener('click', saveToken);
  input.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') saveToken();
  });
}

function connectWebSocket() {
  if (reconnectTimer) clearTimeout(reconnectTimer);
  if (pingInterval) clearInterval(pingInterval);

  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  
  // 1. Check for token in URL query
  const urlParams = new URLSearchParams(window.location.search);
  let token = urlParams.get('token');

  if (token) {
    // Save to localStorage so PWA can load it later without query parameters
    localStorage.setItem('zentrack_token', token);
    
    // Clean URL query parameter so it looks clean (and doesn't clutter PWA URL)
    const cleanUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
    window.history.replaceState({}, document.title, cleanUrl);
  } else {
    // 2. Fallback to localStorage
    token = localStorage.getItem('zentrack_token') || '';
  }

  // If no token exists, display the link overlay and abort connection
  if (!token) {
    showTokenPrompt();
    return;
  }

  const wsUrl = `${wsProtocol}//${window.location.host}/?token=${encodeURIComponent(token)}`;
  
  console.log(`[WebSocket] Connecting to ${wsUrl}...`);
  socket = new WebSocket(wsUrl);

  // Show host in the sidebar active server display
  ipDisplay.innerText = window.location.host;

  socket.onopen = () => {
    console.log('[WebSocket] Connected successfully!');
    statusBadge.className = 'status-badge connected';
    statusText.innerText = 'Conectado';
    latencyBadge.style.display = 'flex';

    // Update hybrid indicators
    const hybridStatusBadge = document.getElementById('hybrid-status-badge');
    const hybridStatusText = document.getElementById('hybrid-status-text');
    const hybridPingValue = document.getElementById('hybrid-ping-value');
    if (hybridStatusBadge) hybridStatusBadge.className = 'hybrid-status connected';
    if (hybridStatusText) hybridStatusText.innerText = 'Conectado';
    if (hybridPingValue) hybridPingValue.style.display = 'inline';
    
    triggerHaptic('success');
    requestWakeLock();
    
    // Sync current settings to the server
    syncSettingsToServer();
    
    // Notify server of physical gamepad mode if enabled
    if (settings.profile === 'gamepad-steam' && settings.gamepadPhysical) {
      sendSocket({ type: 'gamepad_mode', enabled: true });
    }
    
    // Start Ping/Pong mechanism to measure latency
    startPingTimer();
  };

  socket.onclose = () => {
    console.log('[WebSocket] Connection lost. Reconnecting in 1.5s...');
    statusBadge.className = 'status-badge disconnected';
    statusText.innerText = 'Desconectado';
    latencyBadge.style.display = 'none';

    // Update hybrid indicators
    const hybridStatusBadge = document.getElementById('hybrid-status-badge');
    const hybridStatusText = document.getElementById('hybrid-status-text');
    const hybridPingValue = document.getElementById('hybrid-ping-value');
    if (hybridStatusBadge) hybridStatusBadge.className = 'hybrid-status disconnected';
    if (hybridStatusText) hybridStatusText.innerText = 'Desconectado';
    if (hybridPingValue) hybridPingValue.style.display = 'none';
    
    releaseWakeLock();
    if (pingInterval) clearInterval(pingInterval);
    
    reconnectTimer = setTimeout(connectWebSocket, 1500);
  };

  socket.onerror = (error) => {
    console.error('[WebSocket] Error:', error);
  };

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      if (data.type === 'pong') {
        const rtt = Date.now() - data.id; // rtt in ms
        latencyValue.innerText = `${rtt}ms`;
        
        // Update hybrid ping indicator
        const hybridPingValue = document.getElementById('hybrid-ping-value');
        if (hybridPingValue) hybridPingValue.innerText = `${rtt}ms`;
      } else if (data.type === 'sync_settings') {
        if (data.highPolling !== undefined && settings.highPolling !== data.highPolling) {
          settings.highPolling = data.highPolling;
          document.getElementById('toggle-highpolling').checked = settings.highPolling;
          saveSettings();
        }
      } else if (data.type === 'rice_update') {
        // Cache the latest theme received from the server
        window.lastServerRice = data.rice;
        
        if (settings.syncTheme) {
          settings.keyboardTheme = data.rice;
          const selectKeyboardTheme = document.getElementById('select-keyboard-theme');
          if (selectKeyboardTheme) {
            selectKeyboardTheme.value = data.rice;
          }
          saveSettings();
          applyLayoutSettings();
        }
      }
    } catch (e) {
      console.error('[WebSocket] Message parsing error:', e);
    }
  };
}

function sendSocket(payload) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(payload));
  }
}

// ⚡ Fast 6-Byte Int16Array Binary Protocol (CMD, X, Y)
function sendSocketBinary(cmd, x, y) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    const buffer = new Int16Array(3);
    buffer[0] = cmd;
    buffer[1] = Math.round(x);
    buffer[2] = Math.round(y);
    socket.send(buffer.buffer);
  }
}

function syncSettingsToServer() {
  sendSocket({
    type: 'settings',
    highPolling: settings.highPolling
  });
}

function startPingTimer() {
  pingInterval = setInterval(() => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      sendSocket({ type: 'ping', id: Date.now() });
    }
  }, 2000);
}

// --- Ripple Effect inside Trackpad ---
const pointerArea = document.getElementById('pointer-area');
function showRipple(clientX, clientY, isDouble = false) {
  const rect = pointerArea.getBoundingClientRect();
  const x = clientX - rect.left;
  const y = clientY - rect.top;

  const ripple = document.createElement('div');
  ripple.className = isDouble ? 'ripple double-ripple' : 'ripple';
  ripple.style.left = `${x}px`;
  ripple.style.top = `${y}px`;
  
  pointerArea.appendChild(ripple);
  
  setTimeout(() => {
    ripple.remove();
  }, 450);
}

function showDragIndicator(clientX, clientY) {
  const rect = pointerArea.getBoundingClientRect();
  const x = clientX - rect.left;
  const y = clientY - rect.top;

  const ring = document.createElement('div');
  ring.className = 'drag-ring';
  ring.style.left = `${x}px`;
  ring.style.top = `${y}px`;
  
  pointerArea.appendChild(ring);
  
  setTimeout(() => {
    ring.remove();
  }, 600);
}

// --- Pointer Events Gestures State Machine ---
const trackpadSurface = document.getElementById('trackpad-surface');
const activePointers = new Map();

// Tap and drag parameters
let isTapCandidate = false;
let tapStartTime = 0;
let tapStartX = 0;
let tapStartY = 0;
const TAP_MOVEMENT_THRESHOLD = 8; // pixels
const TAP_TIMEOUT = 220; // ms

// Acceleration tracking variables
let lastMoveTime = 0;
let remainderX = 0;
let remainderY = 0;

// Gestures timers
let dragTimer = null;
let clickTimeout = null;
let hasStartedMoving = false;

// Double tap & Drag state
let isDraggingMode = false;

// Scroll & Two-Finger Tap state
let isTwoFingerScroll = false;
let lastScrollX = 0;
let lastScrollY = 0;
let scrollAccumulatorX = 0;
let scrollAccumulatorY = 0;
const SCROLL_STEP_THRESHOLD = 6; // ultra-responsive smooth scroll

let twoFingerTapStartTime = 0;
let isTwoFingerTapCandidate = false;
let twoFingerStartX = 0;
let twoFingerStartY = 0;
let hasHadTwoFingers = false;

// Three-finger swipe state
let isThreeFingerSwipe = false;
let isThreeFingerSwipeCandidate = false;
let threeFingerStartX = 0;
let threeFingerStartY = 0;

// Add css touch action styling dynamically to ensure touch-action none
pointerArea.style.touchAction = 'none';

pointerArea.addEventListener('pointerdown', (e) => {
  e.preventDefault();
  pointerArea.setPointerCapture(e.pointerId);
  trackpadSurface.classList.add('active');

  const pointerState = {
    startX: e.clientX,
    startY: e.clientY,
    lastX: e.clientX,
    lastY: e.clientY,
    startTime: Date.now()
  };
  activePointers.set(e.pointerId, pointerState);

  const activeCount = activePointers.size;

  if (activeCount === 1) {
    // Single finger init
    isTapCandidate = true;
    tapStartTime = Date.now();
    tapStartX = e.clientX;
    tapStartY = e.clientY;
    hasStartedMoving = false;
    lastMoveTime = e.timeStamp || Date.now();
    remainderX = 0;
    remainderY = 0;

    // Clear any existing dragTimer
    if (dragTimer) {
      clearTimeout(dragTimer);
      dragTimer = null;
    }

    // Setup held-click drag activation:
    // If the finger is held down still for 350ms, we trigger mousedown
    dragTimer = setTimeout(() => {
      if (activePointers.size === 1 && !hasStartedMoving && isTapCandidate) {
        isDraggingMode = true;
        isTapCandidate = false; // no longer a single tap candidate
        triggerHaptic('middle-click'); // distinct vibration for drag lock
        sendSocket({ type: 'mousedown', button: 1 });
        showDragIndicator(tapStartX, tapStartY);
      }
    }, 350);

  } else if (activeCount === 2) {
    // Cancel single-finger drag/click timers immediately on multi-touch
    if (dragTimer) {
      clearTimeout(dragTimer);
      dragTimer = null;
    }
    if (clickTimeout) {
      clearTimeout(clickTimeout);
      clickTimeout = null;
    }

    // Release drag lock if it was active
    if (isDraggingMode) {
      sendSocket({ type: 'mouseup', button: 1 });
      isDraggingMode = false;
    }

    // Initialize two-finger scroll and right click gesture detection
    isTwoFingerScroll = true;
    isTapCandidate = false;
    isTwoFingerTapCandidate = true;
    hasHadTwoFingers = true;
    twoFingerTapStartTime = Date.now();

    const pointers = Array.from(activePointers.values());
    // Calculate initial midpoint of both fingers
    twoFingerStartX = (pointers[0].lastX + pointers[1].lastX) / 2;
    twoFingerStartY = (pointers[0].lastY + pointers[1].lastY) / 2;
    
    lastScrollX = twoFingerStartX;
    lastScrollY = twoFingerStartY;
    
    scrollAccumulatorX = 0;
    scrollAccumulatorY = 0;
  } else if (activeCount === 4) {
    // 4 fingers: Workspace navigation swipe
    sendSocket({ type: 'log', message: 'Three fingers detected on pointerdown!' });
    if (dragTimer) {
      clearTimeout(dragTimer);
      dragTimer = null;
    }
    if (clickTimeout) {
      clearTimeout(clickTimeout);
      clickTimeout = null;
    }
    isTapCandidate = false;
    isTwoFingerTapCandidate = false;
    isTwoFingerScroll = false;

    isThreeFingerSwipe = true;
    isThreeFingerSwipeCandidate = true;

    const pointers = Array.from(activePointers.values());
    threeFingerStartX = (pointers[0].lastX + pointers[1].lastX + pointers[2].lastX + pointers[3].lastX) / 4;
    threeFingerStartY = (pointers[0].lastY + pointers[1].lastY + pointers[2].lastY + pointers[3].lastY) / 4;
    sendSocket({ type: 'log', message: `threeFingerStartX initialized to: ${threeFingerStartX.toFixed(1)}` });
  } else {
    // 4 or more fingers, disable gestures
    if (dragTimer) {
      clearTimeout(dragTimer);
      dragTimer = null;
    }
    if (clickTimeout) {
      clearTimeout(clickTimeout);
      clickTimeout = null;
    }
    isTapCandidate = false;
    isTwoFingerTapCandidate = false;
    isTwoFingerScroll = false;
    isThreeFingerSwipe = false;
    isThreeFingerSwipeCandidate = false;
  }
});

pointerArea.addEventListener('pointermove', (e) => {
  e.preventDefault();
  
  if (!activePointers.has(e.pointerId)) return;
  const state = activePointers.get(e.pointerId);

  const activeCount = activePointers.size;

  if (activeCount === 1 && !isTwoFingerScroll) {
    // Single-finger movement (high polling rate support)
    const events = e.getCoalescedEvents ? e.getCoalescedEvents() : [e];
    const moveBatch = [];

    for (const ev of events) {
      const dxRaw = ev.clientX - state.lastX;
      const dyRaw = ev.clientY - state.lastY;

      // Verify if finger moved too much (cancels tap and cancels long-press dragTimer if not fired yet)
      const distFromStart = Math.hypot(ev.clientX - tapStartX, ev.clientY - tapStartY);
      if (distFromStart > TAP_MOVEMENT_THRESHOLD) {
        isTapCandidate = false;
        hasStartedMoving = true;
        if (dragTimer) {
          clearTimeout(dragTimer);
          dragTimer = null;
        }
      }

      // Calculate time delta for velocity tracking
      const now = ev.timeStamp || Date.now();
      let dt = now - lastMoveTime;
      if (dt <= 0) dt = 8;
      if (dt > 120) dt = 8; // Reset baseline on large gaps
      lastMoveTime = now;

      // Calculate velocity and acceleration multiplier
      const distance = Math.hypot(dxRaw, dyRaw);
      const velocity = distance / dt; // pixels per millisecond
      
      let multiplier = 1.0;
      if (settings.accelerationEnabled) {
        const threshold = 0.12; // px/ms
        if (velocity > threshold) {
          // Exponential mouse acceleration curve
          multiplier = 1.0 + settings.accelerationIntensity * Math.pow(velocity - threshold, 1.35);
        }
      }

      // Apply sensitivity, acceleration multiplier, and accumulate remainders (prevents pixel drop)
      const targetX = dxRaw * settings.sensitivity * multiplier + remainderX;
      const targetY = dyRaw * settings.sensitivity * multiplier + remainderY;

      const moveX = Math.round(targetX);
      const moveY = Math.round(targetY);

      remainderX = targetX - moveX;
      remainderY = targetY - moveY;

      if (moveX !== 0 || moveY !== 0) {
        sendSocketBinary(1, moveX, moveY);
      }

      state.lastX = ev.clientX;
      state.lastY = ev.clientY;
    }

  } else if (activeCount === 2 && isTwoFingerScroll) {
    // Update coordinates for the current moving pointer
    state.lastX = e.clientX;
    state.lastY = e.clientY;

    const pointers = Array.from(activePointers.values());
    const currentScrollX = (pointers[0].lastX + pointers[1].lastX) / 2;
    const currentScrollY = (pointers[0].lastY + pointers[1].lastY) / 2;

    const deltaX = currentScrollX - lastScrollX;
    const deltaY = currentScrollY - lastScrollY;

    // Track total sliding distance of the midpoint since the second finger touched down
    const slideDist = Math.hypot(currentScrollX - twoFingerStartX, currentScrollY - twoFingerStartY);
    
    // Increased slide tolerance threshold (16px) for squishy fingers during quick double taps
    if (slideDist > 16) {
      isTwoFingerTapCandidate = false;
    }

    if (deltaX !== 0 || deltaY !== 0) {
      let dy = deltaY * settings.scrollSensitivity;
      let dx = deltaX * settings.scrollSensitivity;
      if (settings.naturalScroll) {
        dy = -dy;
        dx = -dx;
      }
      sendSocketBinary(2, Math.round(dx * 10), Math.round(dy * 10));
    }

    lastScrollX = currentScrollX;
    lastScrollY = currentScrollY;

  } else if (activeCount === 4 && isThreeFingerSwipe) {
    // Update coordinates for the current moving pointer
    state.lastX = e.clientX;
    state.lastY = e.clientY;

    if (isThreeFingerSwipeCandidate) {
      const pointers = Array.from(activePointers.values());
      if (pointers.length === 4) {
        const currentX = (pointers[0].lastX + pointers[1].lastX + pointers[2].lastX + pointers[3].lastX) / 4;
        const dx = currentX - threeFingerStartX;
        
        // Log every 10px of movement to avoid spamming too much
        if (Math.floor(Math.abs(dx)) % 10 === 0) {
          sendSocket({ type: 'log', message: `4-finger swipe dx: ${dx.toFixed(1)} (threshold: 40)` });
        }

        // Threshold: 40px swipe to trigger workspace switch
        if (Math.abs(dx) > 40) {
          isThreeFingerSwipeCandidate = false; // Trigger once per gesture
          sendSocket({ type: 'log', message: `Swipe threshold exceeded! dx = ${dx.toFixed(1)}. Sending action...` });
          
          if (dx > 0) {
            // Swipe Right: switch to left workspace (Super + Left)
            sendSocket({ type: 'shortcut', action: 'workspace_left' });
            triggerHaptic('middle-click');
            showRipple(currentX, window.innerHeight / 2, true); // Visual feedback indicator
          } else {
            // Swipe Left: switch to right workspace (Super + Right)
            sendSocket({ type: 'shortcut', action: 'workspace_right' });
            triggerHaptic('middle-click');
            showRipple(currentX, window.innerHeight / 2, true);
          }
        }
      } else {
        sendSocket({ type: 'log', message: `Warning: pointers length is ${pointers.length} inside activeCount===3 move!` });
      }
    }
  }
});

pointerArea.addEventListener('pointerup', (e) => {
  e.preventDefault();
  
  if (!activePointers.has(e.pointerId)) return;
  const state = activePointers.get(e.pointerId);
  const duration = Date.now() - state.startTime;

  // Clear long-press timer if it hasn't fired yet
  if (dragTimer) {
    clearTimeout(dragTimer);
    dragTimer = null;
  }

  if (isDraggingMode) {
    // Lift dragging finger -> Release Left click
    sendSocket({ type: 'mouseup', button: 1 });
    isDraggingMode = false;
    activePointers.delete(e.pointerId);
    trackpadSurface.classList.remove('active');
    triggerHaptic('click');
    return;
  }

  // Handle click outputs
  if (hasHadTwoFingers) {
    // Two-finger tap -> Right Click (3)
    // If fingers are lifted within 300ms of two-finger touch start and didn't slide much
    if (isTwoFingerTapCandidate && (Date.now() - twoFingerTapStartTime < 300)) {
      // Clear click timeouts to prevent accidental clicks
      if (clickTimeout) {
        clearTimeout(clickTimeout);
        clickTimeout = null;
      }
      sendSocket({ type: 'click', button: 3 }); // Right Click
      triggerHaptic('right-click');
      isTwoFingerTapCandidate = false; // consume click immediately
    }
  } else {
    // Single finger interaction
    if (isTapCandidate && duration < TAP_TIMEOUT) {
      const clickX = tapStartX;
      const clickY = tapStartY;

      // Single click vs double click logic
      if (clickTimeout) {
        // A second tap occurred within 200ms -> Double Click
        clearTimeout(clickTimeout);
        clickTimeout = null;
        sendSocket({ type: 'click', button: 1, double: true });
        triggerHaptic('right-click');
        showRipple(clickX, clickY, true); // double ripple (visual highlight)
      } else {
        // Start click timeout to wait for a potential double click
        clickTimeout = setTimeout(() => {
          clickTimeout = null;
          sendSocket({ type: 'click', button: 1 });
          triggerHaptic('click');
          showRipple(clickX, clickY, false); // single ripple
        }, 200);
      }
    }
  }

  activePointers.delete(e.pointerId);
  sendSocket({ type: 'log', message: `pointerup for ID: ${e.pointerId}, remaining: ${activePointers.size}` });
  
  if (activePointers.size === 0) {
    trackpadSurface.classList.remove('active');
    isTwoFingerScroll = false;
    isTapCandidate = false;
    hasHadTwoFingers = false;
    isThreeFingerSwipe = false;
    isThreeFingerSwipeCandidate = false;
  }
});

pointerArea.addEventListener('pointercancel', (e) => {
  if (dragTimer) {
    clearTimeout(dragTimer);
    dragTimer = null;
  }
  if (clickTimeout) {
    clearTimeout(clickTimeout);
    clickTimeout = null;
  }
  activePointers.delete(e.pointerId);
  sendSocket({ type: 'log', message: `pointercancel for ID: ${e.pointerId}, remaining: ${activePointers.size}` });
  
  if (activePointers.size === 0) {
    trackpadSurface.classList.remove('active');
    if (isDraggingMode) {
      sendSocket({ type: 'mouseup', button: 1 });
      isDraggingMode = false;
    }
    isTwoFingerScroll = false;
    isTapCandidate = false;
    hasHadTwoFingers = false;
    isThreeFingerSwipe = false;
    isThreeFingerSwipeCandidate = false;
  }
});

// --- Scroll Wheel Strip Handling (Single finger scroll) ---
const scrollWheelStrip = document.getElementById('scroll-wheel-strip');
let lastStripY = 0;
let stripAccumulator = 0;

scrollWheelStrip.addEventListener('touchstart', (e) => {
  e.preventDefault();
  if (e.touches.length > 0) {
    lastStripY = e.touches[0].clientY;
    stripAccumulator = 0;
    triggerHaptic('click');
  }
}, { passive: false });

scrollWheelStrip.addEventListener('touchmove', (e) => {
  e.preventDefault();
  if (e.touches.length > 0) {
    const currentY = e.touches[0].clientY;
    const deltaY = currentY - lastStripY;
    
    stripAccumulator += deltaY * settings.scrollSensitivity;
    
    const SCROLL_STRIP_STEP = 12;
    if (Math.abs(stripAccumulator) >= SCROLL_STRIP_STEP) {
      const steps = Math.floor(Math.abs(stripAccumulator) / SCROLL_STRIP_STEP);
      const direction = stripAccumulator > 0 ? 'down' : 'up';
      
      sendSocket({ type: 'scroll', direction, steps });
      
      triggerHaptic('click'); // micro vibration tick
      stripAccumulator = stripAccumulator % SCROLL_STRIP_STEP;
    }
    
    lastStripY = currentY;
  }
}, { passive: false });

// --- Bottom Mouse Buttons Clicks & Holds ---
const btnLeft = document.getElementById('mouse-btn-left');
const btnMiddle = document.getElementById('mouse-btn-middle');
const btnRight = document.getElementById('mouse-btn-right');

function setupHoldButton(btn, buttonCode, hapticType) {
  const startHandler = (e) => {
    e.preventDefault();
    btn.classList.add('active');
    triggerHaptic(hapticType);
    sendSocket({ type: 'mousedown', button: buttonCode });
  };
  
  const endHandler = (e) => {
    e.preventDefault();
    if (btn.classList.contains('active')) {
      btn.classList.remove('active');
      sendSocket({ type: 'mouseup', button: buttonCode });
    }
  };
  
  btn.addEventListener('touchstart', startHandler, { passive: false });
  btn.addEventListener('touchend', endHandler, { passive: false });
  btn.addEventListener('touchcancel', endHandler, { passive: false });
  
  // Fallbacks for testing in browser (mouse simulation)
  btn.addEventListener('mousedown', (e) => {
    if (e.button === 0) {
      btn.classList.add('active');
      sendSocket({ type: 'mousedown', button: buttonCode });
    }
  });
  btn.addEventListener('mouseup', () => {
    btn.classList.remove('active');
    sendSocket({ type: 'mouseup', button: buttonCode });
  });
}

setupHoldButton(btnLeft, 1, 'click');
setupHoldButton(btnMiddle, 2, 'middle-click');
setupHoldButton(btnRight, 3, 'right-click');

// --- BSPWM Modifier & Shortcut Buttons ---
const modifierButtons = document.querySelectorAll('.mod-btn');
modifierButtons.forEach(btn => {
  btn.addEventListener('touchstart', (e) => {
    e.preventDefault();
    const key = btn.getAttribute('data-key');
    const isActive = btn.classList.toggle('active');
    
    triggerHaptic('click');
    
    const commandType = isActive ? 'keydown' : 'keyup';
    sendSocket({
      type: 'key',
      key: `${commandType} ${key}`
    });
  }, { passive: false });
});

// Launch short actions
const launchButtons = document.querySelectorAll('.launch-btn');
let polybarVisible = true;

launchButtons.forEach(btn => {
  btn.addEventListener('touchstart', (e) => {
    e.preventDefault();
    triggerHaptic('right-click');
    
    const action = btn.getAttribute('data-action');
    
    if (btn.id === 'btn-toggle-bar') {
      if (polybarVisible) {
        sendSocket({ type: 'shortcut', action: 'toggle_bar_hide' });
        btn.classList.remove('polybar-shown');
        btn.classList.add('polybar-hidden');
        polybarVisible = false;
      } else {
        sendSocket({ type: 'shortcut', action: 'toggle_bar_show' });
        btn.classList.remove('polybar-hidden');
        btn.classList.add('polybar-shown');
        polybarVisible = true;
      }
    } else if (action) {
      sendSocket({ type: 'shortcut', action: action });
      
      // Auto reset modifiers after action to prevent system locks
      modifierButtons.forEach(modBtn => {
        if (modBtn.classList.contains('active')) {
          modBtn.classList.remove('active');
          const modKey = modBtn.getAttribute('data-key');
          sendSocket({ type: 'key', key: `keyup ${modKey}` });
        }
      });
    }
  }, { passive: false });
});

// --- Settings Drawer Interactions ---
const settingsToggle = document.getElementById('settings-toggle');
const settingsClose = document.getElementById('settings-close');
const settingsDrawer = document.getElementById('settings-drawer');
const drawerOverlay = document.getElementById('drawer-overlay');

settingsToggle.addEventListener('touchstart', (e) => {
  e.preventDefault();
  triggerHaptic('click');
  settingsDrawer.classList.add('active');
  drawerOverlay.classList.add('active');
}, { passive: false });

const closeDrawer = (e) => {
  if (e) e.preventDefault();
  triggerHaptic('click');
  settingsDrawer.classList.remove('active');
  drawerOverlay.classList.remove('active');
};

settingsClose.addEventListener('touchstart', closeDrawer, { passive: false });
drawerOverlay.addEventListener('touchstart', closeDrawer, { passive: false });

// Slider adjustments
const sliderSensitivity = document.getElementById('slider-sensitivity');
const valSensitivity = document.getElementById('val-sensitivity');

sliderSensitivity.addEventListener('input', (e) => {
  const val = parseFloat(e.target.value);
  settings.sensitivity = val;
  valSensitivity.innerText = val.toFixed(1) + 'x';
  saveSettings();
});

const sliderScroll = document.getElementById('slider-scroll');
const valScroll = document.getElementById('val-scroll');

sliderScroll.addEventListener('input', (e) => {
  const val = parseFloat(e.target.value);
  settings.scrollSensitivity = val;
  valScroll.innerText = val.toFixed(1) + 'x';
  saveSettings();
});

const sliderSoundVolume = document.getElementById('slider-sound-volume');
const valSoundVolume = document.getElementById('val-sound-volume');
if (sliderSoundVolume && valSoundVolume) {
  sliderSoundVolume.addEventListener('input', (e) => {
    const val = parseFloat(e.target.value);
    settings.soundVolume = val;
    valSoundVolume.innerText = (val * 100).toFixed(0) + '%';
    saveSettings();
  });
  sliderSoundVolume.addEventListener('change', () => {
    playSwitchSound(true); // Preview volume change
  });
}

// Toggles in settings
const toggleVibration = document.getElementById('toggle-vibration');
toggleVibration.addEventListener('change', (e) => {
  settings.vibration = e.target.checked;
  saveSettings();
  triggerHaptic('click');
});

const toggleWakelock = document.getElementById('toggle-wakelock');
toggleWakelock.addEventListener('change', (e) => {
  settings.keepAwake = e.target.checked;
  saveSettings();
  if (settings.keepAwake) {
    requestWakeLock();
  } else {
    releaseWakeLock();
  }
});

const toggleHighpolling = document.getElementById('toggle-highpolling');
toggleHighpolling.addEventListener('change', (e) => {
  settings.highPolling = e.target.checked;
  saveSettings();
  syncSettingsToServer();
  triggerHaptic('click');
});

const toggleShowButtons = document.getElementById('toggle-showbuttons');
toggleShowButtons.addEventListener('change', (e) => {
  settings.showButtons = e.target.checked;
  saveSettings();
  applyLayoutSettings();
  triggerHaptic('click');
});

const toggleShowScroll = document.getElementById('toggle-showscroll');
toggleShowScroll.addEventListener('change', (e) => {
  settings.showScroll = e.target.checked;
  saveSettings();
  applyLayoutSettings();
  triggerHaptic('click');
});

const toggleAcceleration = document.getElementById('toggle-acceleration');
toggleAcceleration.addEventListener('change', (e) => {
  settings.accelerationEnabled = e.target.checked;
  document.getElementById('item-acc-intensity').style.opacity = settings.accelerationEnabled ? '1' : '0.4';
  document.getElementById('slider-acceleration').disabled = !settings.accelerationEnabled;
  saveSettings();
  triggerHaptic('click');
});

const sliderAcceleration = document.getElementById('slider-acceleration');
const valAcceleration = document.getElementById('val-acceleration');
sliderAcceleration.addEventListener('input', (e) => {
  const val = parseFloat(e.target.value);
  settings.accelerationIntensity = val;
  valAcceleration.innerText = val.toFixed(1) + 'x';
  saveSettings();
});

const toggleNaturalScroll = document.getElementById('toggle-naturalscroll');
toggleNaturalScroll.addEventListener('change', (e) => {
  settings.naturalScroll = e.target.checked;
  saveSettings();
  triggerHaptic('click');
});

const toggleFullscreen = document.getElementById('toggle-fullscreen');
toggleFullscreen.addEventListener('change', (e) => {
  triggerHaptic('click');
  if (e.target.checked) {
    const docEl = document.documentElement;
    if (docEl.requestFullscreen) {
      docEl.requestFullscreen();
    } else if (docEl.webkitRequestFullscreen) {
      docEl.webkitRequestFullscreen();
    } else if (docEl.msRequestFullscreen) {
      docEl.msRequestFullscreen();
    }
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen();
    } else if (document.webkitExitFullscreen) {
      document.webkitExitFullscreen();
    } else if (document.msExitFullscreen) {
      document.msExitFullscreen();
    }
  }
});

const toggleKeySoundElement = document.getElementById('toggle-key-sound');
if (toggleKeySoundElement) {
  toggleKeySoundElement.addEventListener('change', (e) => {
    settings.keySound = e.target.checked;
    saveSettings();
    triggerHaptic('click');
  });
}

const selectSoundProfileElement = document.getElementById('select-sound-profile');
if (selectSoundProfileElement) {
  selectSoundProfileElement.addEventListener('change', (e) => {
    settings.soundProfile = e.target.value;
    saveSettings();
    triggerHaptic('click');
    // Play a preview click of the new switch sound profile
    setTimeout(() => playSwitchSound(true), 50);
  });
}

const selectKeyboardThemeElement = document.getElementById('select-keyboard-theme');
if (selectKeyboardThemeElement) {
  selectKeyboardThemeElement.addEventListener('change', (e) => {
    settings.keyboardTheme = e.target.value;
    saveSettings();
    applyLayoutSettings();
    triggerHaptic('click');
  });
}

const toggleSyncThemeElement = document.getElementById('toggle-sync-theme');
if (toggleSyncThemeElement) {
  toggleSyncThemeElement.addEventListener('change', (e) => {
    settings.syncTheme = e.target.checked;
    
    if (settings.syncTheme && window.lastServerRice) {
      settings.keyboardTheme = window.lastServerRice;
      const selectKeyboardTheme = document.getElementById('select-keyboard-theme');
      if (selectKeyboardTheme) {
        selectKeyboardTheme.value = window.lastServerRice;
      }
      applyLayoutSettings();
    }
    
    saveSettings();
    triggerHaptic('click');
  });
}

const toggleKeyCasterElement = document.getElementById('toggle-keycaster');
if (toggleKeyCasterElement) {
  toggleKeyCasterElement.addEventListener('change', (e) => {
    settings.keyCaster = e.target.checked;
    saveSettings();
    applyLayoutSettings();
    triggerHaptic('click');
  });
}

const syncFullscreenCheckbox = () => {
  const isFS = !!(document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement || document.msFullscreenElement);
  toggleFullscreen.checked = isFS;
};
document.addEventListener('fullscreenchange', syncFullscreenCheckbox);
document.addEventListener('webkitfullscreenchange', syncFullscreenCheckbox);
document.addEventListener('mozfullscreenchange', syncFullscreenCheckbox);
document.addEventListener('MSFullscreenChange', syncFullscreenCheckbox);

// --- Sidebar Menu Panel Interaction & Profile Switcher ---
const sidebarToggle = document.getElementById('sidebar-toggle');
const sidebarClose = document.getElementById('sidebar-close');
const sidebar = document.getElementById('sidebar');
const sidebarOverlay = document.getElementById('sidebar-overlay');

sidebarToggle.addEventListener('touchstart', (e) => {
  e.preventDefault();
  triggerHaptic('click');
  sidebar.classList.add('active');
  sidebarOverlay.classList.add('active');
}, { passive: false });

const closeSidebar = (e) => {
  if (e) e.preventDefault();
  triggerHaptic('click');
  sidebar.classList.remove('active');
  sidebarOverlay.classList.remove('active');
};

sidebarClose.addEventListener('touchstart', closeSidebar, { passive: false });
sidebarOverlay.addEventListener('touchstart', closeSidebar, { passive: false });

// Profile selector item event handlers
document.querySelectorAll('.sidebar .menu-item').forEach(item => {
  item.addEventListener('touchstart', (e) => {
    e.preventDefault();
    const profile = item.getAttribute('data-profile');
    if (profile) {
      settings.profile = profile;
      saveSettings();
      applyLayoutSettings();
      triggerHaptic('right-click');
      closeSidebar();
    }
  }, { passive: false });
});

// ========================================================
//               65% MECHANICAL KEYBOARD LAYOUT
// ========================================================
const KEYBOARD_LAYOUT = [
  // ROW 1
  [
    { label: 'Esc', code: 'Escape', width: 1.0, type: 'key-red' },
    { label: '1', sublabel: '!', code: '1', width: 1.0, type: 'key-white' },
    { label: '2', sublabel: '@', code: '2', width: 1.0, type: 'key-white' },
    { label: '3', sublabel: '#', code: '3', width: 1.0, type: 'key-white' },
    { label: '4', sublabel: '$', code: '4', width: 1.0, type: 'key-white' },
    { label: '5', sublabel: '%', code: '5', width: 1.0, type: 'key-white' },
    { label: '6', sublabel: '^', code: '6', width: 1.0, type: 'key-white' },
    { label: '7', sublabel: '&', code: '7', width: 1.0, type: 'key-white' },
    { label: '8', sublabel: '*', code: '8', width: 1.0, type: 'key-white' },
    { label: '9', sublabel: '(', code: '9', width: 1.0, type: 'key-white' },
    { label: '0', sublabel: ')', code: '0', width: 1.0, type: 'key-white' },
    { label: '-', sublabel: '_', code: 'minus', width: 1.0, type: 'key-white' },
    { label: '=', sublabel: '+', code: 'equal', width: 1.0, type: 'key-white' },
    { label: 'Backspace', code: 'BackSpace', width: 2.0, type: 'key-grey' },
    { label: 'DEL', code: 'Delete', width: 1.0, type: 'key-grey' }
  ],
  // ROW 2
  [
    { label: 'Tab', code: 'Tab', width: 1.5, type: 'key-grey' },
    { label: 'Q', code: 'q', width: 1.0, type: 'key-white' },
    { label: 'W', code: 'w', width: 1.0, type: 'key-white' },
    { label: 'E', code: 'e', width: 1.0, type: 'key-white' },
    { label: 'R', code: 'r', width: 1.0, type: 'key-white' },
    { label: 'T', code: 't', width: 1.0, type: 'key-white' },
    { label: 'Y', code: 'y', width: 1.0, type: 'key-white' },
    { label: 'U', code: 'u', width: 1.0, type: 'key-white' },
    { label: 'I', code: 'i', width: 1.0, type: 'key-white' },
    { label: 'O', code: 'o', width: 1.0, type: 'key-white' },
    { label: 'P', code: 'p', width: 1.0, type: 'key-white' },
    { label: '[', sublabel: '{', code: 'bracketleft', width: 1.0, type: 'key-white' },
    { label: ']', sublabel: '}', code: 'bracketright', width: 1.0, type: 'key-white' },
    { label: '\\', sublabel: '|', code: 'backslash', width: 1.5, type: 'key-white' },
    { label: 'PGUP', code: 'Prior', width: 1.0, type: 'key-grey' }
  ],
  // ROW 3
  [
    { label: 'Caps Lock', code: 'Caps_Lock', width: 1.75, type: 'key-grey' },
    { label: 'A', code: 'a', width: 1.0, type: 'key-white' },
    { label: 'S', code: 's', width: 1.0, type: 'key-white' },
    { label: 'D', code: 'd', width: 1.0, type: 'key-white' },
    { label: 'F', code: 'f', width: 1.0, type: 'key-white' },
    { label: 'G', code: 'g', width: 1.0, type: 'key-white' },
    { label: 'H', code: 'h', width: 1.0, type: 'key-white' },
    { label: 'J', code: 'j', width: 1.0, type: 'key-white' },
    { label: 'K', code: 'k', width: 1.0, type: 'key-white' },
    { label: 'L', code: 'l', width: 1.0, type: 'key-white' },
    { label: ';', sublabel: ':', code: 'semicolon', width: 1.0, type: 'key-white' },
    { label: '\'', sublabel: '"', code: 'apostrophe', width: 1.0, type: 'key-white' },
    { label: 'Enter', code: 'Return', width: 2.25, type: 'key-red' },
    { label: 'PGDN', code: 'Next', width: 1.0, type: 'key-grey' }
  ],
  // ROW 4
  [
    { label: 'Shift', code: 'Shift_L', width: 2.25, type: 'key-grey' },
    { label: 'Z', code: 'z', width: 1.0, type: 'key-white' },
    { label: 'X', code: 'x', width: 1.0, type: 'key-white' },
    { label: 'C', code: 'c', width: 1.0, type: 'key-white' },
    { label: 'V', code: 'v', width: 1.0, type: 'key-white' },
    { label: 'B', code: 'b', width: 1.0, type: 'key-white' },
    { label: 'N', code: 'n', width: 1.0, type: 'key-white' },
    { label: 'M', code: 'm', width: 1.0, type: 'key-white' },
    { label: ',', sublabel: '<', code: 'comma', width: 1.0, type: 'key-white' },
    { label: '.', sublabel: '>', code: 'period', width: 1.0, type: 'key-white' },
    { label: '/', sublabel: '?', code: 'slash', width: 1.0, type: 'key-white' },
    { label: 'Shift', code: 'Shift_R', width: 1.75, type: 'key-grey' },
    { label: '↑', code: 'Up', width: 1.0, type: 'key-red' },
    { label: 'END', code: 'End', width: 1.0, type: 'key-grey' }
  ],
  // ROW 5
  [
    { label: 'Ctrl', code: 'Control_L', width: 1.25, type: 'key-grey' },
    { label: 'Win', code: 'Super_L', width: 1.25, type: 'key-grey' },
    { label: 'Alt', code: 'Alt_L', width: 1.25, type: 'key-grey' },
    { label: '', code: 'space', width: 6.25, type: 'key-white' }, // Spacebar
    { label: 'Alt', code: 'Alt_R', width: 1.0, type: 'key-grey' },
    { label: 'Fn', code: 'Fn', width: 1.0, type: 'key-grey' },
    { label: 'Ctrl', code: 'Control_R', width: 1.0, type: 'key-grey' },
    { label: '←', code: 'Left', width: 1.0, type: 'key-red' },
    { label: '↓', code: 'Down', width: 1.0, type: 'key-red' },
    { label: '→', code: 'Right', width: 1.0, type: 'key-red' }
  ]
];

// --- Key Caster / HUD Overlay Logic ---
const activeModifiers = new Set();
let typedBuffer = '';
const recentShortcuts = [];
let hudFadeTimeout = null;
let casterClearTimeout = null;
let capsLockActive = false;

const KEY_LABELS = {
  'Escape': 'Esc',
  'BackSpace': 'Backspace',
  'Delete': 'DEL',
  'Tab': 'Tab',
  'Caps_Lock': 'Caps',
  'Return': 'Enter',
  'Shift_L': 'Shift',
  'Shift_R': 'Shift',
  'Control_L': 'Ctrl',
  'Control_R': 'Ctrl',
  'Alt_L': 'Alt',
  'Alt_R': 'Alt',
  'Super_L': 'Super',
  'space': 'Espacio',
  'Left': '←',
  'Right': '→',
  'Up': '↑',
  'Down': '↓',
  'Prior': 'PgUp',
  'Next': 'PgDn',
  'End': 'End',
  'minus': '-',
  'equal': '+',
  'bracketleft': '[',
  'bracketright': ']',
  'backslash': '\\',
  'semicolon': ';',
  'apostrophe': '\'',
  'comma': ',',
  'period': '.',
  'slash': '/'
};

function isModifier(code) {
  return ['Shift_L', 'Shift_R', 'Control_L', 'Control_R', 'Alt_L', 'Alt_R', 'Super_L', 'Fn'].includes(code);
}

function isPrintable(code, label) {
  if (['Left', 'Right', 'Up', 'Down'].includes(code)) return false;
  if (code === 'space') return false;
  return label.length === 1;
}

function hasActiveCommandModifiers() {
  return Array.from(activeModifiers).some(m => ['Ctrl', 'Alt', 'Super', 'Fn'].includes(m));
}

function handleCasterPress(key) {
  if (!settings.keyCaster) return;
  const code = key.code;
  const label = key.label;
  const sublabel = key.sublabel;

  const hudCaster = document.getElementById('hud-caster');
  const hudText = document.getElementById('hud-text');
  const dbText = document.getElementById('db-text');
  const dbHistory = document.getElementById('db-history');

  // Trigger HUD fade-in for trackpad overlay profiles
  if (hudCaster) {
    hudCaster.style.opacity = '1';
    hudCaster.style.transform = 'translateX(-50%) translateY(0)';
    if (hudFadeTimeout) clearTimeout(hudFadeTimeout);
    hudFadeTimeout = setTimeout(() => {
      hudCaster.style.opacity = '0';
      hudCaster.style.transform = 'translateX(-50%) translateY(10px)';
    }, 3000);
  }

  // Auto-clear buffer after 1.2s of inactivity (no key typed)
  if (casterClearTimeout) clearTimeout(casterClearTimeout);
  casterClearTimeout = setTimeout(() => {
    typedBuffer = '';
    updateDisplay();
  }, 1200);

  // 1. Caps Lock State
  if (code === 'Caps_Lock') {
    capsLockActive = !capsLockActive;
    const dbCaps = document.getElementById('db-status-caps');
    if (dbCaps) {
      if (capsLockActive) {
        dbCaps.innerText = 'ON';
        dbCaps.classList.add('caps-on');
      } else {
        dbCaps.innerText = 'OFF';
        dbCaps.classList.remove('caps-on');
      }
    }
  }

  // 2. Modifiers
  if (isModifier(code)) {
    const modLabel = KEY_LABELS[code] || label;
    activeModifiers.add(modLabel);
    updateDisplay();
    return;
  }

  // 3. Normal typing or shortcuts
  let displayValue = '';

  if (activeModifiers.size > 0 && hasActiveCommandModifiers()) {
    const modsArray = Array.from(activeModifiers);
    const keyName = KEY_LABELS[code] || label;
    displayValue = modsArray.map(m => `[${m}]`).join(' + ') + ` + [${keyName}]`;
    
    recentShortcuts.unshift(displayValue);
    if (recentShortcuts.length > 3) recentShortcuts.pop();
    
    if (dbHistory) {
      dbHistory.innerHTML = '';
      recentShortcuts.forEach(sc => {
        const item = document.createElement('div');
        item.className = 'db-history-item';
        item.innerText = sc;
        dbHistory.appendChild(item);
      });
    }

    renderAnimatedText('hud-text', displayValue);
    renderAnimatedText('db-text', displayValue);
    
    // In full keyboard profile, keep the Live Text area showing the typedBuffer instead of shortcut combo
    if (settings.profile === 'keyboard-65') {
      updateDisplay();
    }
  } else {
    if (code === 'BackSpace') {
      typedBuffer = typedBuffer.slice(0, -1);
    } else if (code === 'Return') {
      typedBuffer = '';
    } else if (code === 'space') {
      typedBuffer += ' ';
    } else if (code === 'Escape') {
      typedBuffer = '';
    } else if (isPrintable(code, label)) {
      let char = label;
      const shiftActive = activeModifiers.has('Shift');
      
      if (shiftActive && sublabel) {
        char = sublabel; // Use shifted character layout (e.g. '1' -> '!', '-' -> '_')
      } else if (/[a-zA-Z]/.test(char)) {
        const shouldBeUpper = capsLockActive !== shiftActive;
        char = shouldBeUpper ? char.toUpperCase() : char.toLowerCase();
      }
      typedBuffer += char;
    } else {
      const friendlyName = KEY_LABELS[code] || label;
      displayValue = `[${friendlyName}]`;
      renderAnimatedText('hud-text', displayValue);
      renderAnimatedText('db-text', displayValue);
      return;
    }

    // Limit buffer to a sliding window of max 40 characters
    if (typedBuffer.length > 40) {
      typedBuffer = typedBuffer.slice(-40);
    }

    updateDisplay();
  }
}

function handleCasterRelease(code) {
  if (isModifier(code)) {
    const modLabel = KEY_LABELS[code] || code;
    activeModifiers.delete(modLabel);
    updateDisplay();
  }
}

function updateDisplay() {
  let textToDisplay = typedBuffer;

  if (textToDisplay === '' && activeModifiers.size > 0) {
    textToDisplay = Array.from(activeModifiers).map(m => `[${m}]`).join(' + ') + ' + ...';
  }

  if (textToDisplay === '') {
    textToDisplay = 'Listo para escribir...';
  }

  renderAnimatedText('hud-text', textToDisplay);
  renderAnimatedText('db-text', textToDisplay);
}

function renderAnimatedText(containerId, text) {
  const container = document.getElementById(containerId);
  if (!container) return;

  // Render whole combinations/shortcuts/placeholders as single bouncing elements
  const isShortcut = text.startsWith('[') || text === 'Listo para escribir...';

  if (isShortcut) {
    container.innerHTML = `<span class="char-appear">${text}</span>`;
    return;
  }

  // Normal character typing: append or remove individual char spans to keep animations clean
  const chars = Array.from(text);
  const currentSpans = container.querySelectorAll('.char-span');
  const N = chars.length;
  const M = currentSpans.length;

  if (N > M) {
    // If the container holds placeholder or shortcuts, clear it first
    if (container.firstElementChild && !container.firstElementChild.classList.contains('char-span')) {
      container.innerHTML = '';
    }
    // Append only newly added character spans to prevent re-triggering existing ones
    for (let i = M; i < N; i++) {
      const span = document.createElement('span');
      span.className = 'char-span char-appear';
      span.innerHTML = chars[i] === ' ' ? '&nbsp;' : chars[i];
      container.appendChild(span);
    }
  } else if (N < M) {
    // Character deleted: remove from the end
    for (let i = M - 1; i >= N; i--) {
      if (currentSpans[i]) {
        currentSpans[i].remove();
      }
    }
  } else {
    // N === M: check if characters are identical
    let match = true;
    for (let i = 0; i < N; i++) {
      const currentText = currentSpans[i].innerHTML === '&nbsp;' ? ' ' : currentSpans[i].innerText;
      if (currentText !== chars[i]) {
        match = false;
        break;
      }
    }
    if (!match) {
      container.innerHTML = '';
      chars.forEach(c => {
        const span = document.createElement('span');
        span.className = 'char-span char-appear';
        span.innerHTML = c === ' ' ? '&nbsp;' : c;
        container.appendChild(span);
      });
    }
  }
}

function renderKeyboard() {
  const container = document.getElementById('keyboard-surface');
  if (!container) return;

  container.innerHTML = ''; // Clear previous elements

  KEYBOARD_LAYOUT.forEach((row) => {
    const rowEl = document.createElement('div');
    rowEl.className = 'keyboard-row';
    
    row.forEach((key) => {
      // Create contiguous hitbox container
      const containerEl = document.createElement('div');
      containerEl.className = 'keycap-container';
      containerEl.style.flex = `${key.width} 0 0%`;

      // Create visual keycap button
      const keyEl = document.createElement('div');
      keyEl.className = `keycap ${key.type}`;
      keyEl.setAttribute('data-code', key.code);

      // Create Key Legend (Label & Sublabel)
      const labelEl = document.createElement('span');
      labelEl.className = 'keycap-label';
      labelEl.innerText = key.label;

      if (key.sublabel) {
        const sublabelEl = document.createElement('span');
        sublabelEl.className = 'keycap-sublabel';
        sublabelEl.innerText = key.sublabel;
        labelEl.appendChild(sublabelEl);
      }

      keyEl.appendChild(labelEl);
      containerEl.appendChild(keyEl);

      // Bind PointerEvents on containerEl for expanded gapless hitbox
      containerEl.addEventListener('pointerdown', (e) => {
        e.preventDefault();
        try {
          containerEl.setPointerCapture(e.pointerId);
        } catch (err) {}
        
        keyEl.classList.add('active');
        triggerHaptic('click');
        playSwitchSound(true, key.code); // Actuate synthetic switch click & bottom-out thock
        
        // Trigger HUD / Dashboard visual keycaster press
        handleCasterPress(key);

        if (key.code !== 'Fn') {
          sendSocket({ type: 'keydown', key: key.code });
        }

        // Setup key repeat simulation for client-side caster (buffer, delete, etc.)
        if (containerEl.repeatTimeout) clearTimeout(containerEl.repeatTimeout);
        if (containerEl.repeatInterval) clearInterval(containerEl.repeatInterval);

        if (!isModifier(key.code)) {
          containerEl.repeatTimeout = setTimeout(() => {
            containerEl.repeatInterval = setInterval(() => {
              handleCasterPress(key);
            }, 80);
          }, 400);
        }
      });

      containerEl.addEventListener('pointerup', (e) => {
        e.preventDefault();
        try {
          containerEl.releasePointerCapture(e.pointerId);
        } catch (err) {}
        
        keyEl.classList.remove('active');
        playSwitchSound(false, key.code); // Return synthetic switch click
        
        // Clear repeat timers
        if (containerEl.repeatTimeout) clearTimeout(containerEl.repeatTimeout);
        if (containerEl.repeatInterval) clearInterval(containerEl.repeatInterval);

        // Trigger HUD / Dashboard visual keycaster release
        handleCasterRelease(key.code);

        if (key.code !== 'Fn') {
          sendSocket({ type: 'keyup', key: key.code });
        }
      });

      containerEl.addEventListener('pointercancel', (e) => {
        e.preventDefault();
        try {
          containerEl.releasePointerCapture(e.pointerId);
        } catch (err) {}
        
        keyEl.classList.remove('active');
        playSwitchSound(false, key.code); // Return synthetic switch click
        
        // Clear repeat timers
        if (containerEl.repeatTimeout) clearTimeout(containerEl.repeatTimeout);
        if (containerEl.repeatInterval) clearInterval(containerEl.repeatInterval);

        // Trigger HUD / Dashboard visual keycaster release
        handleCasterRelease(key.code);

        if (key.code !== 'Fn') {
          sendSocket({ type: 'keyup', key: key.code });
        }
      });

      rowEl.appendChild(containerEl);
    });

    container.appendChild(rowEl);
  });
}

// --- Floating Hybrid Mode Controls & Actions ---
const hybridSidebarToggle = document.getElementById('hybrid-sidebar-toggle');
const hybridSettingsToggle = document.getElementById('hybrid-settings-toggle');
const hybridControls = document.getElementById('hybrid-controls');

if (hybridControls) {
  // Prevent trackpad touches when clicking hybrid controls
  const stopProp = (e) => e.stopPropagation();
  ['pointerdown', 'pointermove', 'pointerup', 'pointercancel', 
   'touchstart', 'touchmove', 'touchend', 'touchcancel'].forEach(evt => {
    hybridControls.addEventListener(evt, stopProp, { passive: true });
  });
}

if (hybridSidebarToggle) {
  hybridSidebarToggle.addEventListener('touchstart', (e) => {
    e.preventDefault();
    e.stopPropagation();
    triggerHaptic('click');
    sidebar.classList.add('active');
    sidebarOverlay.classList.add('active');
  }, { passive: false });
}

if (hybridSettingsToggle) {
  const settingsDrawer = document.getElementById('settings-drawer');
  const drawerOverlay = document.getElementById('drawer-overlay');
  
  hybridSettingsToggle.addEventListener('touchstart', (e) => {
    e.preventDefault();
    e.stopPropagation();
    triggerHaptic('click');
    settingsDrawer.classList.add('active');
    drawerOverlay.classList.add('active');
  }, { passive: false });
}

// ========================================================
//                STEAM CONTROLLER GAMEPAD LOGIC
// ========================================================

const GAMEPAD_PRESETS = {
  fps: {
    joyUp: 'w', joyDown: 's', joyLeft: 'a', joyRight: 'd',
    btnA: 'space', btnB: 'e', btnX: 'r', btnY: 'q',
    btnL: 'click_right', btnR: 'click_left',
    btnSelect: 'Escape', btnStart: 'Return'
  },
  retro: {
    joyUp: 'Up', joyDown: 'Down', joyLeft: 'Left', joyRight: 'Right',
    btnA: 'z', btnB: 'x', btnX: 'c', btnY: 'v',
    btnL: 'Shift_L', btnR: 'Control_L',
    btnSelect: 'Escape', btnStart: 'Return'
  }
};

const GP_LABEL_MAPS = {
  joyUp: 'Joystick ARRIBA ⬆️',
  joyDown: 'Joystick ABAJO ⬇️',
  joyLeft: 'Joystick IZQUIERDA ⬅️',
  joyRight: 'Joystick DERECHA ➡️',
  btnA: 'Botón A (Saltar)',
  btnB: 'Botón B (Acción)',
  btnX: 'Botón X (Recargar)',
  btnY: 'Botón Y (Arma)',
  btnL: 'Gatillo L (Bump)',
  btnR: 'Gatillo R (Shoot)',
  btnSelect: 'Botón SELECT',
  btnStart: 'Botón START'
};

const MAP_OPTIONS = {
  'w': 'W', 'a': 'A', 's': 'S', 'd': 'D',
  'q': 'Q', 'e': 'E', 'r': 'R', 'f': 'F',
  'g': 'G', 'c': 'C', 'x': 'X', 'z': 'Z',
  'v': 'V', 't': 'T', 'y': 'Y', 'm': 'M',
  'space': 'Espacio (Space)',
  'Return': 'Enter / Intro',
  'Escape': 'Escape',
  'Tab': 'Tabulator (Tab)',
  'Shift_L': 'Shift (Izquierdo)',
  'Control_L': 'Ctrl (Izquierdo)',
  'Alt_L': 'Alt (Izquierdo)',
  'Up': 'Flecha Arriba',
  'Down': 'Flecha Abajo',
  'Left': 'Flecha Izquierda',
  'Right': 'Flecha Derecha',
  'click_left': 'Click Izquierdo Mouse',
  'click_right': 'Click Derecho Mouse',
  'click_middle': 'Click Medio Mouse'
};

function initGamepadMappingUI() {
  const container = document.getElementById('gamepad-mapping-list');
  if (!container) return;

  container.innerHTML = '';
  
  Object.keys(GP_LABEL_MAPS).forEach(key => {
    const item = document.createElement('div');
    item.className = 'map-item';
    
    const label = document.createElement('span');
    label.className = 'map-label';
    label.innerText = GP_LABEL_MAPS[key];
    
    const select = document.createElement('select');
    select.className = 'map-select';
    select.setAttribute('data-key', key);
    
    Object.keys(MAP_OPTIONS).forEach(optValue => {
      const option = document.createElement('option');
      option.value = optValue;
      option.innerText = MAP_OPTIONS[optValue];
      select.appendChild(option);
    });
    
    select.addEventListener('change', (e) => {
      settings.gamepadMapping[key] = e.target.value;
      settings.gamepadPreset = 'custom';
      
      const presetSelect = document.getElementById('select-gamepad-preset');
      if (presetSelect) presetSelect.value = 'custom';
      
      updatePresetBanner();
      saveSettings();
    });
    
    item.appendChild(label);
    item.appendChild(select);
    container.appendChild(item);
  });

  const presetSelect = document.getElementById('select-gamepad-preset');
  if (presetSelect) {
    presetSelect.addEventListener('change', (e) => {
      const val = e.target.value;
      settings.gamepadPreset = val;
      
      if (val !== 'custom' && GAMEPAD_PRESETS[val]) {
        settings.gamepadMapping = JSON.parse(JSON.stringify(GAMEPAD_PRESETS[val]));
      }
      
      syncGamepadMappingUI();
      updatePresetBanner();
      saveSettings();
      triggerHaptic('click');
    });
  }

  const toggleGpPhysical = document.getElementById('toggle-gamepad-physical');
  if (toggleGpPhysical) {
    toggleGpPhysical.addEventListener('change', (e) => {
      settings.gamepadPhysical = !!e.target.checked;
      saveSettings();
      triggerHaptic('click');
      sendSocket({ type: 'gamepad_mode', enabled: settings.gamepadPhysical });
    });
  }

  const toggleGpFullscreen = document.getElementById('toggle-gamepad-fullscreen-trackpad');
  if (toggleGpFullscreen) {
    toggleGpFullscreen.addEventListener('change', (e) => {
      settings.gamepadFullscreenTrackpad = !!e.target.checked;
      saveSettings();
      triggerHaptic('click');
      updateGamepadTrackpadVisualMode();
      syncGamepadSizesUI();
    });
  }

  updatePresetBanner();
}

function updateGamepadTrackpadVisualMode() {
  const gpSurface = document.getElementById('gamepad-surface');
  if (!gpSurface) return;

  if (settings.gamepadFullscreenTrackpad) {
    gpSurface.classList.add('fullscreen-trackpad-mode');
  } else {
    gpSurface.classList.remove('fullscreen-trackpad-mode');
  }
}

function syncGamepadMappingUI() {
  const selectElements = document.querySelectorAll('.map-select');
  selectElements.forEach(select => {
    const key = select.getAttribute('data-key');
    if (key && settings.gamepadMapping[key]) {
      select.value = settings.gamepadMapping[key];
    }
  });
  
  const presetSelect = document.getElementById('select-gamepad-preset');
  if (presetSelect) {
    presetSelect.value = settings.gamepadPreset || 'fps';
  }
}

function updatePresetBanner() {
  const banner = document.getElementById('gp-preset-banner');
  if (banner) {
    const label = settings.gamepadPreset === 'fps' ? 'FPS PROFILE' : 
                  settings.gamepadPreset === 'retro' ? 'RETRO PROFILE' : 'CUSTOM PROFILE';
    banner.innerText = label;
  }
}

let gamepadInitialized = false;
let joystickTouchId = null;
let trackpadTouchId = null;
let trackpadDecayTimeout = null;

function initGamepadControls() {
  if (gamepadInitialized) return;
  gamepadInitialized = true;

  console.log('[Gamepad] Initializing Touch Controller Listeners...');

  // 1. Left Joystick Control (Dynamic Position & Multitouch isolated)
  const gpLeft = document.querySelector('.gp-left');
  const boundary = document.getElementById('joystick-boundary');
  const knob = document.getElementById('joystick-knob');
  
  if (gpLeft && boundary && knob) {
    let center = { x: 0, y: 0 };
    const joyKeysState = { up: false, down: false, left: false, right: false };

    const updateJoystickState = (deltaX, deltaY) => {
      const maxD = 35; // Clamped maximum knob travel
      const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
      
      let moveX = deltaX;
      let moveY = deltaY;
      
      if (distance > maxD) {
        moveX = (deltaX / distance) * maxD;
        moveY = (deltaY / distance) * maxD;
      }
      
      knob.style.transform = `translate(${moveX}px, ${moveY}px)`;
      
      const nx = moveX / maxD;
      const ny = moveY / maxD;
      const thresh = 0.35;
      
      if (settings.gamepadPhysical) {
        // Physical gamepad mode: send ABS_X, ABS_Y (Left Stick values 0..255, center 128)
        const rxVal = Math.round(128 + nx * 127);
        const ryVal = Math.round(128 + ny * 127);
        sendSocket({ type: 'gp_axis', name: 'X', value: rxVal });
        sendSocket({ type: 'gp_axis', name: 'Y', value: ryVal });
      } else {
        // Keyboard emulation mode: W, A, S, D
        const targetKeys = {
          up: ny < -thresh,
          down: ny > thresh,
          left: nx < -thresh,
          right: nx > thresh
        };
        
        const mapKeys = {
          up: settings.gamepadMapping.joyUp,
          down: settings.gamepadMapping.joyDown,
          left: settings.gamepadMapping.joyLeft,
          right: settings.gamepadMapping.joyRight
        };
        
        Object.keys(targetKeys).forEach(dir => {
          if (targetKeys[dir] !== joyKeysState[dir]) {
            joyKeysState[dir] = targetKeys[dir];
            const keyVal = mapKeys[dir];
            if (keyVal) {
              if (joyKeysState[dir]) {
                sendSocket({ type: 'keydown', key: keyVal });
              } else {
                sendSocket({ type: 'keyup', key: keyVal });
              }
            }
          }
        });
      }
    };

    const resetJoystick = () => {
      knob.style.transform = 'translate(0, 0)';
      
      // Reset boundary position back to CSS default layouts
      boundary.style.position = '';
      boundary.style.left = '';
      boundary.style.top = '';
      
      if (settings.gamepadPhysical) {
        sendSocket({ type: 'gp_axis', name: 'X', value: 128 });
        sendSocket({ type: 'gp_axis', name: 'Y', value: 128 });
      } else {
        const mapKeys = {
          up: settings.gamepadMapping.joyUp,
          down: settings.gamepadMapping.joyDown,
          left: settings.gamepadMapping.joyLeft,
          right: settings.gamepadMapping.joyRight
        };
        
        Object.keys(joyKeysState).forEach(dir => {
          if (joyKeysState[dir]) {
            joyKeysState[dir] = false;
            const keyVal = mapKeys[dir];
            if (keyVal) {
              sendSocket({ type: 'keyup', key: keyVal });
            }
          }
        });
      }
    };

    // Touch handle with multi-touch isolation
    gpLeft.addEventListener('touchstart', (e) => {
      e.preventDefault();
      if (isEditingGamepadLayout) return;
      if (joystickTouchId !== null) return;
      
      const touch = e.changedTouches[0];
      joystickTouchId = touch.identifier;
      
      // Dynamic positioning: center joystick boundary at finger client location
      const wrapperRect = document.getElementById('gamepad-surface').getBoundingClientRect();
      const posX = touch.clientX - wrapperRect.left;
      const posY = touch.clientY - wrapperRect.top;
      
      boundary.style.position = 'absolute';
      boundary.style.left = `${posX - 65}px`; // center (130px / 2)
      boundary.style.top = `${posY - 65}px`;  // center
      
      center = { x: touch.clientX, y: touch.clientY };
      updateJoystickState(0, 0);
      triggerHaptic('light');
    }, { passive: false });

    window.addEventListener('touchmove', (e) => {
      if (joystickTouchId === null) return;
      
      let activeTouch = null;
      for (let i = 0; i < e.touches.length; i++) {
        if (e.touches[i].identifier === joystickTouchId) {
          activeTouch = e.touches[i];
          break;
        }
      }
      
      if (!activeTouch) return;
      
      const dx = activeTouch.clientX - center.x;
      const dy = activeTouch.clientY - center.y;
      updateJoystickState(dx, dy);
    }, { passive: false });

    const handleJoystickTouchEnd = (e) => {
      if (joystickTouchId === null) return;
      
      let matched = false;
      for (let i = 0; i < e.changedTouches.length; i++) {
        if (e.changedTouches[i].identifier === joystickTouchId) {
          matched = true;
          break;
        }
      }
      
      if (matched) {
        joystickTouchId = null;
        resetJoystick();
      }
    };

    window.addEventListener('touchend', handleJoystickTouchEnd, { passive: false });
    window.addEventListener('touchcancel', handleJoystickTouchEnd, { passive: false });

    // Fallback Mouse actions for desktop
    let mouseActive = false;
    gpLeft.addEventListener('mousedown', (e) => {
      if (e.pointerType === 'touch') return;
      if (isEditingGamepadLayout) return;
      mouseActive = true;
      const rect = boundary.getBoundingClientRect();
      center = { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 };
      
      const dx = e.clientX - center.x;
      const dy = e.clientY - center.y;
      updateJoystickState(dx, dy);
    });

    window.addEventListener('mousemove', (e) => {
      if (!mouseActive) return;
      const dx = e.clientX - center.x;
      const dy = e.clientY - center.y;
      updateJoystickState(dx, dy);
    });

    window.addEventListener('mouseup', () => {
      if (!mouseActive) return;
      mouseActive = false;
      resetJoystick();
    });
  }

  // 2. Right Aim Trackpad Control (Steam Controller style trackpad with gestures and uinput support)
  const trackpad = document.getElementById('gp-aim-trackpad');
  if (trackpad) {
    let lastTouchX = null;
    let lastTouchY = null;
    
    // Gesture parameters
    let lastTouchEndT = 0;
    let lastReleaseX = null;
    let lastReleaseY = null;
    let longPressTimeout = null;
    let isLongPressActive = false;
    let isDoubleTapHold = false;
    let trackpadStartX = 0;
    let trackpadStartY = 0;
    let trackpadHasMoved = false;
    let tapClicksDisabled = false;

    trackpad.addEventListener('touchstart', (e) => {
      e.preventDefault();
      if (isEditingGamepadLayout) return;
      if (trackpadTouchId !== null) return;
      
      const touch = e.changedTouches[0];
      trackpadTouchId = touch.identifier;
      lastTouchX = touch.clientX;
      lastTouchY = touch.clientY;
      trackpadStartX = touch.clientX;
      trackpadStartY = touch.clientY;
      trackpadHasMoved = false;
      
      isLongPressActive = false;
      isDoubleTapHold = false;

      // Check if any face buttons or triggers are currently being held down
      const buttonsActive = document.querySelectorAll('.gp-btn.active, .gp-shoulder.active').length > 0;
      tapClicksDisabled = buttonsActive;

      if (tapClicksDisabled) {
        // If buttons are active, force trackpadHasMoved = true to bypass any release clicks, but DO NOT return!
        trackpadHasMoved = true;
      } else {
        // Double tap hold -> triggers Left Click drag (holds down button 1)
        const now = Date.now();
        const timeDelta = now - lastTouchEndT;
        let isDoubleTap = false;

        if (timeDelta < 160 && lastReleaseX !== null && lastReleaseY !== null) {
          const tapDist = Math.sqrt(
            Math.pow(touch.clientX - lastReleaseX, 2) + 
            Math.pow(touch.clientY - lastReleaseY, 2)
          );
          if (tapDist < 40) { // Only count as double tap if within 40px radius!
            isDoubleTap = true;
          }
        }

        if (isDoubleTap) {
          isDoubleTapHold = true;
          sendSocket({ type: 'mousedown', button: 1 });
          playSwitchSound(true, 'space');
          triggerHaptic('light');
        } else {
          // Long Press -> triggers Right Click (holds down button 3) after 500ms
          longPressTimeout = setTimeout(() => {
            if (!trackpadHasMoved && trackpadTouchId !== null) {
              isLongPressActive = true;
              sendSocket({ type: 'mousedown', button: 3 });
              playSwitchSound(true, 'space');
              triggerHaptic('medium');
            }
          }, 500);
        }
      }
    }, { passive: false });
    
    window.addEventListener('touchmove', (e) => {
      if (trackpadTouchId === null) return;
      
      let activeTouch = null;
      for (let i = 0; i < e.touches.length; i++) {
        if (e.touches[i].identifier === trackpadTouchId) {
          activeTouch = e.touches[i];
          break;
        }
      }
      
      if (!activeTouch) return;
      
      // Cancel long press immediately on any touch move event
      clearTimeout(longPressTimeout);

      const dx = activeTouch.clientX - lastTouchX;
      const dy = activeTouch.clientY - lastTouchY;
      
      lastTouchX = activeTouch.clientX;
      lastTouchY = activeTouch.clientY;
      
      const totalDist = Math.sqrt(
        Math.pow(activeTouch.clientX - trackpadStartX, 2) + 
        Math.pow(activeTouch.clientY - trackpadStartY, 2)
      );
      
      if (totalDist > 6) {
        trackpadHasMoved = true;
      }
      
      if (settings.gamepadPhysical) {
        // Physical Gamepad: Translate swipes to Right Stick axes (RX, RY values 0..255)
        const rxVal = Math.max(0, Math.min(255, Math.round(128 + dx * 9)));
        const ryVal = Math.max(0, Math.min(255, Math.round(128 + dy * 9)));
        
        sendSocket({ type: 'gp_axis', name: 'RX', value: rxVal });
        sendSocket({ type: 'gp_axis', name: 'RY', value: ryVal });
        
        // Auto decay axis back to center (128) when swipe motion pauses
        clearTimeout(trackpadDecayTimeout);
        trackpadDecayTimeout = setTimeout(() => {
          sendSocket({ type: 'gp_axis', name: 'RX', value: 128 });
          sendSocket({ type: 'gp_axis', name: 'RY', value: 128 });
        }, 50);
      } else {
        // Keyboard/Mouse Mode: Move mouse pointer
        const sens = settings.sensitivity || 1.2;
        sendSocket({ type: 'move', dx: Math.round(dx * sens), dy: Math.round(dy * sens) });
      }
    }, { passive: false });
    
    const handleTrackpadTouchEnd = (e) => {
      if (trackpadTouchId === null) return;
      
      let matched = false;
      let releasedTouch = null;
      for (let i = 0; i < e.changedTouches.length; i++) {
        if (e.changedTouches[i].identifier === trackpadTouchId) {
          matched = true;
          releasedTouch = e.changedTouches[i];
          break;
        }
      }
      
      if (matched) {
        trackpadTouchId = null;
        clearTimeout(longPressTimeout);
        
        if (settings.gamepadPhysical) {
          sendSocket({ type: 'gp_axis', name: 'RX', value: 128 });
          sendSocket({ type: 'gp_axis', name: 'RY', value: 128 });
        }
        
        if (!tapClicksDisabled) {
          if (isDoubleTapHold) {
            sendSocket({ type: 'mouseup', button: 1 });
            playSwitchSound(false, 'space');
            isDoubleTapHold = false;
          } else if (isLongPressActive) {
            sendSocket({ type: 'mouseup', button: 3 });
            playSwitchSound(false, 'space');
            isLongPressActive = false;
          } else if (!trackpadHasMoved) {
            // Quick Tap -> Emulates Left click click
            sendSocket({ type: 'mousedown', button: 1 });
            playSwitchSound(true, 'space');
            triggerHaptic('light');
            setTimeout(() => {
              sendSocket({ type: 'mouseup', button: 1 });
              playSwitchSound(false, 'space');
            }, 45);
          }
        }
        
        if (!trackpadHasMoved && !tapClicksDisabled) {
          lastTouchEndT = Date.now();
          if (releasedTouch) {
            lastReleaseX = releasedTouch.clientX;
            lastReleaseY = releasedTouch.clientY;
          }
        } else {
          lastTouchEndT = 0;
          lastReleaseX = null;
          lastReleaseY = null;
        }
        lastTouchX = null;
        lastTouchY = null;
      }
    };
    
    window.addEventListener('touchend', handleTrackpadTouchEnd, { passive: false });
    window.addEventListener('touchcancel', handleTrackpadTouchEnd, { passive: false });
  }

  // 3. Action Buttons & Bumpers Control (A/B/X/Y, Start, Select, L, R)
  const gamepadButtons = document.querySelectorAll('.gp-btn, .gp-shoulder');
  
  const gpBtnNamesMap = {
    btnA: 'A', btnB: 'B', btnX: 'X', btnY: 'Y',
    btnL: 'L', btnR: 'R', btnSelect: 'select', btnStart: 'start'
  };

  gamepadButtons.forEach(btn => {
    const btnKey = btn.getAttribute('data-btn');
    if (!btnKey) return;
    
    let btnTouchId = null;
    let lastBtnTouchX = null;
    let lastBtnTouchY = null;
    let btnHasDragged = false;

    const handlePress = (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (isEditingGamepadLayout) return;
      
      btn.classList.add('active');
      triggerHaptic('light');
      playSwitchSound(true, 'space');
      
      const touch = e.touches ? e.changedTouches[0] : null;
      if (touch) {
        btnTouchId = touch.identifier;
        lastBtnTouchX = touch.clientX;
        lastBtnTouchY = touch.clientY;
        btnHasDragged = false;
      }

      if (settings.gamepadPhysical) {
        const btnName = gpBtnNamesMap[btnKey];
        if (btnName) {
          sendSocket({ type: 'gp_btn', name: btnName, value: 1 });
        }
      } else {
        const action = settings.gamepadMapping[btnKey];
        if (action) {
          if (action.startsWith('click_')) {
            const btnNum = action === 'click_left' ? 1 : action === 'click_right' ? 3 : 2;
            sendSocket({ type: 'mousedown', button: btnNum });
          } else {
            sendSocket({ type: 'keydown', key: action });
          }
        }
      }
    };
    
    const handleRelease = (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (isEditingGamepadLayout) return;
      
      let matched = false;
      if (e.changedTouches) {
        for (let i = 0; i < e.changedTouches.length; i++) {
          if (e.changedTouches[i].identifier === btnTouchId) {
            matched = true;
            break;
          }
        }
      } else {
        matched = true;
      }

      if (matched) {
        btnTouchId = null;
        lastBtnTouchX = null;
        lastBtnTouchY = null;
        btnHasDragged = false;

        btn.classList.remove('active');
        playSwitchSound(false, 'space');
        
        if (settings.gamepadPhysical) {
          const btnName = gpBtnNamesMap[btnKey];
          if (btnName) {
            sendSocket({ type: 'gp_btn', name: btnName, value: 0 });
          }
        } else {
          const action = settings.gamepadMapping[btnKey];
          if (action) {
            if (action.startsWith('click_')) {
              const btnNum = action === 'click_left' ? 1 : action === 'click_right' ? 3 : 2;
              sendSocket({ type: 'mouseup', button: btnNum });
            } else {
              sendSocket({ type: 'keyup', key: action });
            }
          }
        }
      }
    };

    const handleButtonTouchMove = (e) => {
      if (btnTouchId === null) return;
      
      let activeTouch = null;
      for (let i = 0; i < e.touches.length; i++) {
        if (e.touches[i].identifier === btnTouchId) {
          activeTouch = e.touches[i];
          break;
        }
      }
      if (!activeTouch) return;
      
      const dx = activeTouch.clientX - lastBtnTouchX;
      const dy = activeTouch.clientY - lastBtnTouchY;
      
      lastBtnTouchX = activeTouch.clientX;
      lastBtnTouchY = activeTouch.clientY;
      
      const dist = Math.sqrt(dx*dx + dy*dy);
      if (dist > 1 || btnHasDragged) {
        btnHasDragged = true;
        
        if (settings.gamepadPhysical) {
          const rxVal = Math.max(0, Math.min(255, Math.round(128 + dx * 9)));
          const ryVal = Math.max(0, Math.min(255, Math.round(128 + dy * 9)));
          sendSocket({ type: 'gp_axis', name: 'RX', value: rxVal });
          sendSocket({ type: 'gp_axis', name: 'RY', value: ryVal });
          
          clearTimeout(trackpadDecayTimeout);
          trackpadDecayTimeout = setTimeout(() => {
            sendSocket({ type: 'gp_axis', name: 'RX', value: 128 });
            sendSocket({ type: 'gp_axis', name: 'RY', value: 128 });
          }, 50);
        } else {
          const sens = settings.sensitivity || 1.2;
          sendSocket({ type: 'move', dx: Math.round(dx * sens), dy: Math.round(dy * sens) });
        }
      }
    };

    btn.addEventListener('touchstart', handlePress, { passive: false });
    btn.addEventListener('touchend', handleRelease, { passive: false });
    btn.addEventListener('touchcancel', handleRelease, { passive: false });
    window.addEventListener('touchmove', handleButtonTouchMove, { passive: false });
    
    btn.addEventListener('mousedown', handlePress);
    btn.addEventListener('mouseup', handleRelease);
    btn.addEventListener('mouseleave', handleRelease);
  });

  // 4. Sidebar & Settings overlay bindings for Gamepad Controls
  const gamepadControls = document.getElementById('gamepad-controls');
  if (gamepadControls) {
    const stopProp = (e) => e.stopPropagation();
    ['pointerdown', 'pointermove', 'pointerup', 'pointercancel', 
     'touchstart', 'touchmove', 'touchend', 'touchcancel'].forEach(evt => {
      gamepadControls.addEventListener(evt, stopProp, { passive: true });
    });
  }

  const gamepadSidebarToggle = document.getElementById('gamepad-sidebar-toggle');
  const gamepadSettingsToggle = document.getElementById('gamepad-settings-toggle');

  if (gamepadSidebarToggle) {
    gamepadSidebarToggle.addEventListener('touchstart', (e) => {
      e.preventDefault();
      e.stopPropagation();
      triggerHaptic('click');
      sidebar.classList.add('active');
      sidebarOverlay.classList.add('active');
    }, { passive: false });
  }

  if (gamepadSettingsToggle) {
    const settingsDrawer = document.getElementById('settings-drawer');
    const drawerOverlay = document.getElementById('drawer-overlay');
    
    gamepadSettingsToggle.addEventListener('touchstart', (e) => {
      e.preventDefault();
      e.stopPropagation();
      triggerHaptic('click');
      settingsDrawer.classList.add('active');
      drawerOverlay.classList.add('active');
    }, { passive: false });
  }
}

// State variable for Edit Layout mode
let isEditingGamepadLayout = false;

// Apply scale and absolute positions from settings to DOM elements
function applyGamepadLayout() {
  updateGamepadTrackpadVisualMode();
  const layout = settings.gamepadLayout || {
    joystick: { x: 8, y: 40, scale: 1.0 },
    trackpad: { x: 44, y: 40, width: 170, height: 120 },
    abxy: { x: 74, y: 40, scale: 1.0 },
    bumperL: { x: 3, y: 15, scale: 1.0 },
    bumperR: { x: 85, y: 15, scale: 1.0 },
    centerNav: { x: 42, y: 15, scale: 1.0 }
  };

  const joy = document.getElementById('joystick-boundary');
  const track = document.getElementById('gp-aim-trackpad');
  const abxy = document.querySelector('.gp-face-buttons');
  const bumpL = document.getElementById('gp-btn-l');
  const bumpR = document.getElementById('gp-btn-r');
  const nav = document.querySelector('.gp-nav-buttons');

  if (joy) {
    joy.style.left = `${layout.joystick.x}%`;
    joy.style.top = `${layout.joystick.y}%`;
    joy.style.transform = `scale(${layout.joystick.scale})`;
  }
  if (track) {
    track.style.left = `${layout.trackpad.x}%`;
    track.style.top = `${layout.trackpad.y}%`;
    track.style.width = `${layout.trackpad.width}px`;
    track.style.height = `${layout.trackpad.height}px`;
  }
  if (abxy) {
    abxy.style.left = `${layout.abxy.x}%`;
    abxy.style.top = `${layout.abxy.y}%`;
    abxy.style.transform = `scale(${layout.abxy.scale})`;
  }
  if (bumpL) {
    bumpL.style.left = `${layout.bumperL.x}%`;
    bumpL.style.top = `${layout.bumperL.y}%`;
    bumpL.style.transform = `scale(${layout.bumperL.scale})`;
  }
  if (bumpR) {
    bumpR.style.left = `${layout.bumperR.x}%`;
    bumpR.style.top = `${layout.bumperR.y}%`;
    bumpR.style.transform = `scale(${layout.bumperR.scale})`;
  }
  if (nav) {
    nav.style.left = `${layout.centerNav.x}%`;
    nav.style.top = `${layout.centerNav.y}%`;
    nav.style.transform = `scale(${layout.centerNav.scale})`;
  }
}

// Synchronize scale range sliders in settings drawer to values loaded from settings
function syncGamepadSizesUI() {
  const layout = settings.gamepadLayout;
  if (!layout) return;

  const sliderJoy = document.getElementById('slider-gp-joy-scale');
  const valJoy = document.getElementById('val-gp-joy-scale');
  if (sliderJoy && valJoy) {
    sliderJoy.value = layout.joystick.scale;
    valJoy.innerText = layout.joystick.scale.toFixed(1) + 'x';
  }

  const sliderABXY = document.getElementById('slider-gp-abxy-scale');
  const valABXY = document.getElementById('val-gp-abxy-scale');
  if (sliderABXY && valABXY) {
    sliderABXY.value = layout.abxy.scale;
    valABXY.innerText = layout.abxy.scale.toFixed(1) + 'x';
  }

  const sliderWidth = document.getElementById('slider-gp-pad-width');
  const valWidth = document.getElementById('val-gp-pad-width');
  const rowWidth = document.getElementById('row-gp-pad-width');
  if (sliderWidth && valWidth) {
    sliderWidth.value = layout.trackpad.width;
    valWidth.innerText = layout.trackpad.width + 'px';
    sliderWidth.disabled = !!settings.gamepadFullscreenTrackpad;
    if (rowWidth) rowWidth.style.opacity = settings.gamepadFullscreenTrackpad ? '0.4' : '1';
  }

  const sliderHeight = document.getElementById('slider-gp-pad-height');
  const valHeight = document.getElementById('val-gp-pad-height');
  const rowHeight = document.getElementById('row-gp-pad-height');
  if (sliderHeight && valHeight) {
    sliderHeight.value = layout.trackpad.height;
    valHeight.innerText = layout.trackpad.height + 'px';
    sliderHeight.disabled = !!settings.gamepadFullscreenTrackpad;
    if (rowHeight) rowHeight.style.opacity = settings.gamepadFullscreenTrackpad ? '0.4' : '1';
  }

  const sliderBump = document.getElementById('slider-gp-bump-scale');
  const valBump = document.getElementById('val-gp-bump-scale');
  if (sliderBump && valBump) {
    sliderBump.value = layout.bumperL.scale;
    valBump.innerText = layout.bumperL.scale.toFixed(1) + 'x';
  }
}

// Initialize layout drag-to-position event handlers and settings sliders
function initGamepadLayoutEditor() {
  const btnEdit = document.getElementById('btn-edit-layout');
  const gpSurface = document.getElementById('gamepad-surface');

  if (btnEdit && gpSurface) {
    btnEdit.addEventListener('touchstart', (e) => {
      e.preventDefault();
      e.stopPropagation();
      triggerHaptic('click');

      isEditingGamepadLayout = !isEditingGamepadLayout;
      if (isEditingGamepadLayout) {
        gpSurface.classList.add('editing-layout');
        btnEdit.classList.add('saving-layout');
        btnEdit.innerText = '💾 Guardar';
      } else {
        gpSurface.classList.remove('editing-layout');
        btnEdit.classList.remove('saving-layout');
        btnEdit.innerText = '📐 Editar';
        saveSettings();
      }
    }, { passive: false });

    // Enable drag positioning for each component in Edit Mode
    const draggableElements = [
      { id: 'joystick-boundary', key: 'joystick' },
      { id: 'gp-aim-trackpad', key: 'trackpad' },
      { className: 'gp-face-buttons', key: 'abxy' },
      { id: 'gp-btn-l', key: 'bumperL' },
      { id: 'gp-btn-r', key: 'bumperR' },
      { className: 'gp-nav-buttons', key: 'centerNav' }
    ];

    draggableElements.forEach(item => {
      const el = item.id ? document.getElementById(item.id) : document.querySelector('.' + item.className);
      if (!el) return;

      let dragTouchId = null;
      let startX = 0;
      let startY = 0;
      let startLayoutX = 0;
      let startLayoutY = 0;

      el.addEventListener('touchstart', (e) => {
        if (!isEditingGamepadLayout) return;
        
        // Stop propagation to prevent dynamic joystick trigger or trackpad move
        e.stopPropagation();
        e.preventDefault();

        const touch = e.changedTouches[0];
        dragTouchId = touch.identifier;

        startX = touch.clientX;
        startY = touch.clientY;
        
        const layout = settings.gamepadLayout[item.key];
        startLayoutX = layout ? layout.x : 0;
        startLayoutY = layout ? layout.y : 0;
      }, { passive: false });

      window.addEventListener('touchmove', (e) => {
        if (!isEditingGamepadLayout || dragTouchId === null) return;

        let activeTouch = null;
        for (let i = 0; i < e.touches.length; i++) {
          if (e.touches[i].identifier === dragTouchId) {
            activeTouch = e.touches[i];
            break;
          }
        }
        if (!activeTouch) return;

        const wrapperRect = gpSurface.getBoundingClientRect();
        
        const deltaX = activeTouch.clientX - startX;
        const deltaY = activeTouch.clientY - startY;

        let pctX = startLayoutX + (deltaX / wrapperRect.width) * 100;
        let pctY = startLayoutY + (deltaY / wrapperRect.height) * 100;

        pctX = Math.max(0, Math.min(95, pctX));
        pctY = Math.max(0, Math.min(95, pctY));

        // Update layout configuration
        settings.gamepadLayout[item.key].x = parseFloat(pctX.toFixed(1));
        settings.gamepadLayout[item.key].y = parseFloat(pctY.toFixed(1));

        applyGamepadLayout();
      }, { passive: false });

      const handleDragEnd = (e) => {
        if (dragTouchId === null) return;
        let matched = false;
        for (let i = 0; i < e.changedTouches.length; i++) {
          if (e.changedTouches[i].identifier === dragTouchId) {
            matched = true;
            break;
          }
        }
        if (matched) {
          dragTouchId = null;
          saveSettings();
        }
      };

      window.addEventListener('touchend', handleDragEnd, { passive: false });
      window.addEventListener('touchcancel', handleDragEnd, { passive: false });
    });
  }

  // Bind scale range sliders input events
  const sliderJoy = document.getElementById('slider-gp-joy-scale');
  const valJoy = document.getElementById('val-gp-joy-scale');
  if (sliderJoy && valJoy) {
    sliderJoy.addEventListener('input', (e) => {
      const val = parseFloat(e.target.value);
      settings.gamepadLayout.joystick.scale = val;
      valJoy.innerText = val.toFixed(1) + 'x';
      applyGamepadLayout();
      saveSettings();
    });
  }

  const sliderABXY = document.getElementById('slider-gp-abxy-scale');
  const valABXY = document.getElementById('val-gp-abxy-scale');
  if (sliderABXY && valABXY) {
    sliderABXY.addEventListener('input', (e) => {
      const val = parseFloat(e.target.value);
      settings.gamepadLayout.abxy.scale = val;
      valABXY.innerText = val.toFixed(1) + 'x';
      applyGamepadLayout();
      saveSettings();
    });
  }

  const sliderWidth = document.getElementById('slider-gp-pad-width');
  const valWidth = document.getElementById('val-gp-pad-width');
  if (sliderWidth && valWidth) {
    sliderWidth.addEventListener('input', (e) => {
      const val = parseInt(e.target.value);
      settings.gamepadLayout.trackpad.width = val;
      valWidth.innerText = val + 'px';
      applyGamepadLayout();
      saveSettings();
    });
  }

  const sliderHeight = document.getElementById('slider-gp-pad-height');
  const valHeight = document.getElementById('val-gp-pad-height');
  if (sliderHeight && valHeight) {
    sliderHeight.addEventListener('input', (e) => {
      const val = parseInt(e.target.value);
      settings.gamepadLayout.trackpad.height = val;
      valHeight.innerText = val + 'px';
      applyGamepadLayout();
      saveSettings();
    });
  }

  const sliderBump = document.getElementById('slider-gp-bump-scale');
  const valBump = document.getElementById('val-gp-bump-scale');
  if (sliderBump && valBump) {
    sliderBump.addEventListener('input', (e) => {
      const val = parseFloat(e.target.value);
      settings.gamepadLayout.bumperL.scale = val;
      settings.gamepadLayout.bumperR.scale = val;
      valBump.innerText = val.toFixed(1) + 'x';
      applyGamepadLayout();
      saveSettings();
    });
  }
}

// --- Initialize App ---
initGamepadMappingUI();
initGamepadLayoutEditor();
loadSettings();
connectWebSocket();
if (settings.keepAwake) {
  setTimeout(requestWakeLock, 1000);
}

// Register PWA Service Worker for standalone fullscreen installation support
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js')
      .then((reg) => {
        console.log('[PWA] Service Worker registered successfully, scope:', reg.scope);
      })
      .catch((err) => {
        console.error('[PWA] Service Worker registration failed:', err);
      });
  });
}
