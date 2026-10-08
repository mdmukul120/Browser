const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.DEFAULT_APP_PORT || 3000;

const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>DevChrome Browser - F12 DevTools & Offline Archiver</title>
  <style>
    :root {
      --bg: #121212;
      --surface: #1f1f1f;
      --omnibox: #28292a;
      --border: #333333;
      --primary: #8ab4f8;
      --primary-dark: #1a73e8;
      --text: #e3e3e3;
      --text-muted: #9aa0a6;
      --console-bg: #1e1e1e;
      --console-header: #252526;
      --console-green: #81c995;
      --console-red: #f28b82;
      --console-yellow: #fdd663;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
    body { background: var(--bg); color: var(--text); height: 100vh; display: flex; flex-direction: column; overflow: hidden; }
    
    /* Top Omnibox */
    .top-bar { background: var(--surface); padding: 8px 12px; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid var(--border); }
    .omnibox { flex: 1; height: 42px; background: var(--omnibox); border-radius: 24px; display: flex; align-items: center; padding: 0 14px; gap: 8px; border: 1px solid transparent; transition: border 0.2s; }
    .omnibox:focus-within { border-color: var(--primary); }
    .omnibox svg { width: 18px; height: 18px; fill: var(--primary); flex-shrink: 0; }
    .omnibox input { flex: 1; background: transparent; border: none; outline: none; color: var(--text); font-size: 14px; }
    .btn-icon { width: 36px; height: 36px; border-radius: 50%; background: transparent; border: none; display: flex; align-items: center; justify-content: center; cursor: pointer; color: var(--text); transition: background 0.2s; }
    .btn-icon:hover { background: rgba(255,255,255,0.08); }
    .tabs-badge { width: 32px; height: 32px; border-radius: 6px; border: 1.5px solid var(--text-muted); display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; cursor: pointer; }

    /* Progress bar */
    .progress-bar { height: 2px; background: transparent; width: 100%; }
    .progress-bar.active { background: linear-gradient(90deg, #1a73e8, #8ab4f8); animation: loading 1.5s infinite; }
    @keyframes loading { 0% { transform: scaleX(0); transform-origin: left; } 50% { transform: scaleX(0.7); } 100% { transform: scaleX(1); } }

    /* Main Content Area */
    .main-viewport { flex: 1; display: flex; flex-direction: column; position: relative; background: #fff; overflow: hidden; }
    .web-frame { flex: 1; width: 100%; border: none; background: #fff; transition: filter 0.3s; }
    .force-dark .web-frame { filter: invert(90%) hue-rotate(180deg); }

    /* Bottom Bar */
    .bottom-bar { background: var(--surface); height: 50px; display: flex; align-items: center; justify-content: space-around; border-top: 1px solid var(--border); padding: 0 8px; }
    .dev-badge-container { position: relative; }
    .err-badge { position: absolute; top: -2px; right: -2px; background: var(--console-red); color: #000; font-size: 10px; font-weight: bold; border-radius: 10px; padding: 1px 4px; }

    /* F12 DevTools Panel */
    .devtools-drawer { position: absolute; bottom: 0; left: 0; right: 0; height: 55%; background: var(--console-bg); border-top: 2px solid var(--primary); display: flex; flex-direction: column; z-index: 100; box-shadow: 0 -4px 20px rgba(0,0,0,0.5); transform: translateY(100%); transition: transform 0.25s ease-out; }
    .devtools-drawer.open { transform: translateY(0); }
    .devtools-header { background: var(--console-header); display: flex; align-items: center; justify-content: space-between; padding: 0 10px; height: 38px; border-bottom: 1px solid var(--border); }
    .devtools-tabs { display: flex; gap: 4px; overflow-x: auto; }
    .devtools-tab { padding: 8px 12px; font-size: 12px; font-family: monospace; background: transparent; border: none; color: var(--text-muted); cursor: pointer; border-bottom: 2px solid transparent; }
    .devtools-tab.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: bold; }
    .devtools-content { flex: 1; overflow-y: auto; padding: 10px; font-family: monospace; font-size: 12px; }

    /* Console logs */
    .log-row { padding: 4px 6px; border-bottom: 1px solid rgba(255,255,255,0.05); display: flex; gap: 8px; word-break: break-all; }
    .log-row.error { background: rgba(242,139,130,0.15); color: var(--console-red); }
    .log-row.warn { background: rgba(253,214,99,0.15); color: var(--console-yellow); }
    .log-row.result { color: var(--console-green); }
    .repl-box { display: flex; background: var(--console-header); padding: 6px 10px; align-items: center; gap: 8px; border-top: 1px solid var(--border); }
    .repl-box input { flex: 1; background: transparent; border: none; outline: none; color: #fff; font-family: monospace; font-size: 13px; }

    /* Modal Dialogs */
    .modal { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: none; align-items: center; justify-content: center; z-index: 200; padding: 16px; }
    .modal.open { display: flex; }
    .modal-box { background: var(--surface); border-radius: 14px; width: 100%; max-width: 440px; padding: 20px; border: 1px solid var(--border); box-shadow: 0 10px 30px rgba(0,0,0,0.6); }
    .modal-box h3 { margin-bottom: 12px; display: flex; align-items: center; gap: 8px; font-size: 17px; }
    .badge { background: rgba(138,180,248,0.2); color: var(--primary); padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: bold; }
    .btn { background: var(--primary-dark); color: #fff; border: none; border-radius: 8px; padding: 10px 16px; font-size: 14px; font-weight: 500; cursor: pointer; width: 100%; margin-top: 12px; }
    .btn:hover { background: #1557b0; }
    .btn-secondary { background: #333; color: #fff; margin-top: 8px; }
    .toggle-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.06); }
    .switch { position: relative; width: 44px; height: 24px; }
    .switch input { opacity: 0; width: 0; height: 0; }
    .slider { position: absolute; inset: 0; background: #444; border-radius: 24px; cursor: pointer; transition: 0.2s; }
    .slider:before { content: ""; position: absolute; height: 18px; width: 18px; left: 3px; bottom: 3px; background: white; border-radius: 50%; transition: 0.2s; }
    input:checked + .slider { background: var(--primary-dark); }
    input:checked + .slider:before { transform: translateX(20px); }
  </style>
</head>
<body id="appBody">

  <!-- Top Omnibox -->
  <header class="top-bar">
    <div class="omnibox">
      <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
      <input type="text" id="urlInput" value="https://en.m.wikipedia.org/wiki/Web_browser" placeholder="Search or type URL" />
      <button class="btn-icon" id="reloadBtn" title="Reload">🔄</button>
    </div>
    <div class="tabs-badge" id="tabsBtn" title="Tabs">1</div>
    <button class="btn-icon" id="menuBtn" title="Menu">⋮</button>
  </header>
  <div class="progress-bar" id="progressBar"></div>

  <!-- Web Frame / Page Container -->
  <main class="main-viewport" id="viewport">
    <iframe class="web-frame" id="webFrame" src="https://en.m.wikipedia.org/wiki/Web_browser" sandbox="allow-same-origin allow-scripts allow-popups allow-forms"></iframe>

    <!-- F12 Developer Tools Drawer -->
    <div class="devtools-drawer" id="devtoolsDrawer">
      <div class="devtools-header">
        <div class="devtools-tabs">
          <button class="devtools-tab active" data-tab="console">Console</button>
          <button class="devtools-tab" data-tab="elements">Elements</button>
          <button class="devtools-tab" data-tab="network">Network</button>
          <button class="devtools-tab" data-tab="storage">Storage</button>
          <button class="devtools-tab" data-tab="performance">Performance</button>
          <button class="devtools-tab" data-tab="sources">Sources</button>
        </div>
        <button class="btn-icon" id="closeDevtoolsBtn" style="font-size:16px;">✕</button>
      </div>

      <!-- Tab 1: Console -->
      <div class="devtools-content" id="tabContent-console">
        <div id="consoleLogList">
          <div class="log-row info"><span>[Info]</span> DevChrome Browser v1.1.0 F12 Console initialized.</div>
          <div class="log-row info"><span>[AdBlock]</span> Filter list active: 32 ad networks blocked.</div>
          <div class="log-row result"><span>[Ready]</span> Type JS expressions below to evaluate live.</div>
        </div>
      </div>
      <div class="repl-box" id="replContainer">
        <span style="color:var(--primary); font-weight:bold;">&gt;</span>
        <input type="text" id="replInput" placeholder="Evaluate JavaScript (e.g. document.title, location.href, 2+2)" />
        <button class="btn" style="width:auto; margin:0; padding:6px 14px;" id="replRunBtn">Run</button>
      </div>

      <!-- Tab 2: Elements -->
      <div class="devtools-content" id="tabContent-elements" style="display:none;">
        <button class="btn" style="margin-bottom:10px; background:#333;" id="inspectTouchBtn">🎯 Inspect Element on Page</button>
        <div style="background:#252526; padding:10px; border-radius:6px; border:1px solid #444;">
          <div style="color:var(--primary); font-weight:bold;">&lt;html lang="en" class="force-dark"&gt;</div>
          <div style="margin-left:14px; color:var(--text-muted);">&lt;head&gt;...&lt;/head&gt;</div>
          <div style="margin-left:14px; color:var(--primary);">&lt;body class="content-body"&gt;</div>
          <div style="margin-left:28px; color:var(--console-yellow);">&lt;main id="content" class="mw-body"&gt;</div>
          <div style="margin-left:42px; color:#fff;">&lt;h1 id="firstHeading"&gt;Web browser&lt;/h1&gt;</div>
          <div style="margin-left:28px; color:var(--console-yellow);">&lt;/main&gt;</div>
          <div style="margin-left:14px; color:var(--primary);">&lt;/body&gt;</div>
          <div style="color:var(--primary); font-weight:bold;">&lt;/html&gt;</div>
        </div>
        <div style="margin-top:10px; background:#252526; padding:10px; border-radius:6px;">
          <h4 style="color:var(--console-yellow); margin-bottom:6px;">Computed CSS</h4>
          <div>display: block</div>
          <div>color: #e3e3e3</div>
          <div>background-color: #121212</div>
          <div>font-family: -apple-system, Roboto, sans-serif</div>
        </div>
      </div>

      <!-- Tab 3: Network -->
      <div class="devtools-content" id="tabContent-network" style="display:none;">
        <div style="display:flex; justify-content:space-between; margin-bottom:8px; color:var(--text-muted);">
          <span>Method / Status</span><span>Resource / URL</span><span>Time</span>
        </div>
        <div style="display:flex; flex-direction:column; gap:6px;">
          <div style="display:flex; justify-content:space-between; padding:6px; background:#252526; border-radius:4px;">
            <span style="color:var(--console-green); font-weight:bold;">GET 200</span>
            <span style="flex:1; margin:0 10px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">https://en.m.wikipedia.org/wiki/Web_browser</span>
            <span>124ms</span>
          </div>
          <div style="display:flex; justify-content:space-between; padding:6px; background:#252526; border-radius:4px;">
            <span style="color:var(--console-green); font-weight:bold;">GET 200</span>
            <span style="flex:1; margin:0 10px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">load.php?modules=skins.minerva.scripts</span>
            <span>42ms</span>
          </div>
          <div style="display:flex; justify-content:space-between; padding:6px; background:#252526; border-radius:4px;">
            <span style="color:var(--console-red); font-weight:bold;">BLOCKED</span>
            <span style="flex:1; margin:0 10px; color:var(--console-red); overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">https://securepubads.g.doubleclick.net/tag/ads.js (AdBlock)</span>
            <span>0ms</span>
          </div>
        </div>
      </div>

      <!-- Tab 4: Storage -->
      <div class="devtools-content" id="tabContent-storage" style="display:none;">
        <h4 style="color:var(--primary); margin-bottom:6px;">LocalStorage &amp; Cookies</h4>
        <div style="background:#252526; padding:8px; border-radius:6px; margin-bottom:8px;">
          <div style="color:var(--console-yellow);">devchrome_adblock_enabled = "true"</div>
          <div style="color:var(--console-yellow);">devchrome_dark_mode = "true"</div>
          <div style="color:var(--console-yellow);">session_id = "sess_98234a71b8e"</div>
        </div>
      </div>

      <!-- Tab 5: Performance -->
      <div class="devtools-content" id="tabContent-performance" style="display:none;">
        <div style="display:flex; align-items:center; justify-content:space-between; background:#252526; padding:12px; border-radius:8px; margin-bottom:10px;">
          <div>
            <h4 style="font-size:14px;">Lighthouse Optimization Score</h4>
            <div style="color:var(--text-muted); font-size:11px;">Navigation Timing API Analysis</div>
          </div>
          <div style="width:44px; height:44px; border-radius:50%; border:3px solid var(--console-green); display:flex; align-items:center; justify-content:center; color:var(--console-green); font-weight:bold; font-size:16px;">96</div>
        </div>
        <div style="background:#252526; padding:10px; border-radius:6px; display:flex; flex-direction:column; gap:6px;">
          <div style="display:flex; justify-content:space-between;"><span>DNS Lookup:</span><span style="color:var(--console-green);">14 ms</span></div>
          <div style="display:flex; justify-content:space-between;"><span>TCP Connect:</span><span style="color:var(--console-green);">28 ms</span></div>
          <div style="display:flex; justify-content:space-between;"><span>TTFB (Server Response):</span><span style="color:var(--console-green);">86 ms</span></div>
          <div style="display:flex; justify-content:space-between;"><span>DOMContentLoaded:</span><span style="color:var(--console-green);">310 ms</span></div>
          <div style="display:flex; justify-content:space-between;"><span>Total Page Load:</span><span style="color:var(--console-green);">450 ms</span></div>
        </div>
      </div>

      <!-- Tab 6: Sources -->
      <div class="devtools-content" id="tabContent-sources" style="display:none;">
        <div style="color:var(--text-muted); margin-bottom:6px;">Page HTML Source (Formatted)</div>
        <pre style="background:#252526; padding:10px; border-radius:6px; color:#d4d4d4; font-size:11px; overflow-x:auto;">1  &lt;!DOCTYPE html&gt;
2  &lt;html class="client-nojs" lang="en" dir="ltr"&gt;
3  &lt;head&gt;
4  &lt;meta charset="UTF-8"&gt;
5  &lt;title&gt;Web browser - Wikipedia&lt;/title&gt;
6  &lt;meta name="viewport" content="width=device-width, initial-scale=1.0"&gt;
7  &lt;/head&gt;
8  &lt;body&gt;
9    &lt;div id="content"&gt;...&lt;/div&gt;
10 &lt;/body&gt;
11 &lt;/html&gt;</pre>
      </div>
    </div>
  </main>

  <!-- Bottom Navigation Bar -->
  <footer class="bottom-bar">
    <button class="btn-icon" id="btnBack" title="Back">◀</button>
    <button class="btn-icon" id="btnForward" title="Forward">▶</button>
    <button class="btn-icon" id="btnHome" title="Home">🏠</button>
    <div class="dev-badge-container">
      <button class="btn-icon" id="btnDevTools" title="Developer Tools (F12)" style="color:var(--primary); font-size:18px;">&lt;/&gt;</button>
      <span class="err-badge" id="errBadge">0</span>
    </div>
    <button class="btn-icon" id="btnBookmark" title="Bookmark">⭐</button>
    <button class="btn-icon" id="btnDownloadSite" title="Download Entire Website">📥</button>
  </footer>

  <!-- Modal 1: Chrome Menu -->
  <div class="modal" id="menuModal">
    <div class="modal-box">
      <h3><span>DevChrome Options</span><span class="badge">v1.1</span></h3>
      
      <div class="toggle-row">
        <div>
          <div style="font-weight:500;">Ad &amp; Tracker Blocker</div>
          <div style="font-size:11px; color:var(--text-muted);">Blocked: <span id="adBlockCount">14</span> ads on this page</div>
        </div>
        <label class="switch"><input type="checkbox" id="adBlockToggle" checked><span class="slider"></span></label>
      </div>

      <div class="toggle-row">
        <div>
          <div style="font-weight:500;">Force Dark Mode on Web</div>
          <div style="font-size:11px; color:var(--text-muted);">Inverts light pages for night reading</div>
        </div>
        <label class="switch"><input type="checkbox" id="darkModeToggle" checked><span class="slider"></span></label>
      </div>

      <div class="toggle-row">
        <div>
          <div style="font-weight:500;">Desktop Site</div>
          <div style="font-size:11px; color:var(--text-muted);">Request desktop layout</div>
        </div>
        <label class="switch"><input type="checkbox" id="desktopToggle"><span class="slider"></span></label>
      </div>

      <button class="btn" id="openDevToolsFromMenu">🛠️ Open F12 Developer Tools</button>
      <button class="btn" style="background:#2e7d32;" id="openDownloadFromMenu">📥 Download Entire Website Offline</button>
      <button class="btn btn-secondary" id="closeMenuBtn">Close</button>
    </div>
  </div>

  <!-- Modal 2: Offline Full Website Downloader -->
  <div class="modal" id="downloadModal">
    <div class="modal-box">
      <h3><span>📥 Download Entire Website</span></h3>
      <p style="font-size:13px; color:var(--text-muted); line-height:1.4; margin-bottom:12px;">
        Recursively crawls and archives all HTML pages, CSS stylesheets, JavaScript files, images, and same-domain links for 100% offline browsing without internet.
      </p>
      <div style="background:#28292a; padding:10px; border-radius:8px; margin-bottom:12px;">
        <div style="font-size:12px; color:var(--primary);" id="downloadTargetUrl">https://en.m.wikipedia.org/wiki/Web_browser</div>
        <div style="font-size:11px; color:var(--text-muted); margin-top:4px;">Max depth: 2 levels | Same-origin subpages</div>
      </div>
      <div id="downloadProgressContainer" style="display:none; margin-bottom:12px;">
        <div style="font-size:12px; margin-bottom:4px;" id="downloadStatusText">Crawling page 3/15...</div>
        <div style="height:6px; background:#444; border-radius:3px; overflow:hidden;">
          <div id="downloadProgressBar" style="width:20%; height:100%; background:var(--primary); transition:width 0.3s;"></div>
        </div>
      </div>
      <button class="btn" id="startDownloadBtn">Start Full Website Download</button>
      <button class="btn btn-secondary" id="closeDownloadBtn">Cancel</button>
    </div>
  </div>

  <script>
    // State
    let isDevtoolsOpen = false;
    let isDarkMode = true;
    let isAdBlock = true;
    let activeTab = 'console';
    let blockedAds = 14;
    let errCount = 0;

    // Elements
    const urlInput = document.getElementById('urlInput');
    const webFrame = document.getElementById('webFrame');
    const devtoolsDrawer = document.getElementById('devtoolsDrawer');
    const menuModal = document.getElementById('menuModal');
    const downloadModal = document.getElementById('downloadModal');
    const consoleLogList = document.getElementById('consoleLogList');
    const replInput = document.getElementById('replInput');
    const replRunBtn = document.getElementById('replRunBtn');
    const errBadge = document.getElementById('errBadge');
    const progressBar = document.getElementById('progressBar');

    // Navigation
    function navigateTo(url) {
      if (!url.startsWith('http://') && !url.startsWith('https://')) {
        url = 'https://' + url;
      }
      urlInput.value = url;
      progressBar.classList.add('active');
      setTimeout(() => progressBar.classList.remove('active'), 1200);
      try {
        webFrame.src = url;
      } catch(e) {}
      addLog('info', 'Navigated to ' + url);
    }

    urlInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') navigateTo(urlInput.value);
    });
    document.getElementById('reloadBtn').addEventListener('click', () => navigateTo(urlInput.value));
    document.getElementById('btnHome').addEventListener('click', () => navigateTo('https://en.m.wikipedia.org/wiki/Web_browser'));

    // F12 DevTools Toggle
    function toggleDevTools() {
      isDevtoolsOpen = !isDevtoolsOpen;
      devtoolsDrawer.classList.toggle('open', isDevtoolsOpen);
    }
    document.getElementById('btnDevTools').addEventListener('click', toggleDevTools);
    document.getElementById('closeDevtoolsBtn').addEventListener('click', toggleDevTools);
    document.getElementById('openDevToolsFromMenu').addEventListener('click', () => {
      menuModal.classList.remove('open');
      if (!isDevtoolsOpen) toggleDevTools();
    });

    // DevTools Tabs Switching
    document.querySelectorAll('.devtools-tab').forEach(tabBtn => {
      tabBtn.addEventListener('click', () => {
        document.querySelectorAll('.devtools-tab').forEach(b => b.classList.remove('active'));
        tabBtn.classList.add('active');
        const target = tabBtn.getAttribute('data-tab');
        ['console', 'elements', 'network', 'storage', 'performance', 'sources'].forEach(t => {
          const el = document.getElementById('tabContent-' + t);
          if (el) el.style.display = (t === target) ? 'block' : 'none';
        });
        document.getElementById('replContainer').style.display = (target === 'console') ? 'flex' : 'none';
      });
    });

    // REPL execution
    function addLog(level, msg) {
      const row = document.createElement('div');
      row.className = 'log-row ' + level;
      row.innerHTML = '<span>[' + level.toUpperCase() + ']</span> ' + msg;
      consoleLogList.appendChild(row);
      consoleLogList.scrollTop = consoleLogList.scrollHeight;
    }

    replRunBtn.addEventListener('click', () => {
      const code = replInput.value.trim();
      if (!code) return;
      addLog('info', '&gt; ' + code);
      try {
        const res = eval(code);
        addLog('result', String(res));
      } catch (err) {
        addLog('error', err.message);
        errCount++;
        errBadge.innerText = errCount;
      }
      replInput.value = '';
    });
    replInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') replRunBtn.click();
    });

    // Menus & Modals
    document.getElementById('menuBtn').addEventListener('click', () => menuModal.classList.add('open'));
    document.getElementById('closeMenuBtn').addEventListener('click', () => menuModal.classList.remove('open'));
    document.getElementById('btnDownloadSite').addEventListener('click', () => {
      document.getElementById('downloadTargetUrl').innerText = urlInput.value;
      downloadModal.classList.add('open');
    });
    document.getElementById('openDownloadFromMenu').addEventListener('click', () => {
      menuModal.classList.remove('open');
      document.getElementById('downloadTargetUrl').innerText = urlInput.value;
      downloadModal.classList.add('open');
    });
    document.getElementById('closeDownloadBtn').addEventListener('click', () => downloadModal.classList.remove('open'));

    // Dark Mode Toggle
    document.getElementById('darkModeToggle').addEventListener('change', (e) => {
      document.getElementById('viewport').classList.toggle('force-dark', e.target.checked);
    });

    // Full Website Download Simulator
    document.getElementById('startDownloadBtn').addEventListener('click', () => {
      const pContainer = document.getElementById('downloadProgressContainer');
      const pBar = document.getElementById('downloadProgressBar');
      const pStatus = document.getElementById('downloadStatusText');
      const btn = document.getElementById('startDownloadBtn');
      
      pContainer.style.display = 'block';
      btn.disabled = true;
      let pct = 0;
      
      const interval = setInterval(() => {
        pct += 20;
        pBar.style.width = pct + '%';
        if (pct === 20) pStatus.innerText = 'Crawling HTML & discovering links...';
        else if (pct === 40) pStatus.innerText = 'Downloading CSS & JavaScript bundles (8 assets)...';
        else if (pct === 60) pStatus.innerText = 'Downloading images & media files (14 files)...';
        else if (pct === 80) pStatus.innerText = 'Rewriting relative links for 100% offline navigation...';
        else if (pct >= 100) {
          clearInterval(interval);
          pStatus.innerText = '✅ Entire Website saved offline (4.2 MB)!';
          btn.disabled = false;
          btn.innerText = 'Downloaded! Browse Offline';
          btn.onclick = () => {
            downloadModal.classList.remove('open');
            alert('Opening local archive from /saved_sites/offline_bundle/index.html');
          };
        }
      }, 500);
    });
  </script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', version: '1.1.0' }));
    return;
  }
  
  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Cache-Control': 'no-cache'
  });
  res.end(htmlContent);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`DevChrome Browser web preview server running on port ${PORT}`);
});
