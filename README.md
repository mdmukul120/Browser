# DevChrome Browser (v1.1.0) 🌐🛠️

A modern, Chrome-styled developer browser for Android equipped with full **F12 Developer Tools**, built-in **Ad & Tracker Blocker**, comfortable **Web Force Dark Mode**, and a **Recursive Full Website Offline Downloader**.

---

## 🚀 Key Features

### 1. F12 Developer Tools & Console Terminal
- **Console Terminal**: Interactive JavaScript REPL prompt (`>`) executing live code on active webpages with real-time logs (`log`, `warn`, `error`, `info`) and unhandled promise/error traps.
- **DOM Elements Inspector**: Tap-to-inspect mode highlighting in-page elements with computed CSS styles, attributes, and outerHTML.
- **Network Inspector**: Intercepts `window.fetch` and `XMLHttpRequest` capturing HTTP methods, status codes, response times, and payloads.
- **Storage Inspector**: View and inspect `localStorage`, `sessionStorage`, and `document.cookie`.
- **Performance Audit**: Real-time Navigation Timing API analysis calculating TTFB, DNS lookup, DOMContentLoaded, full page load time, and Lighthouse-style optimization recommendations.
- **Sources View**: Formatted HTML page source viewer with line numbers.
- **Eruda Suite**: Quick floating console widget toggle.

### 2. Built-in Ad & Tracker Blocker
- Blocks intrusive ad networks, popups, and user-tracking analytics scripts.
- Cosmetic CSS rule injection hiding empty ad containers and banners.
- Live blocked count per tab and globally.

### 3. User-Friendly Dark Mode
- Deep Chrome dark palette (`#1F1F1F`, `#28292A`, `#8AB4F8`).
- **Web Force Dark Mode**: Smart CSS color inversion preserving media/images for comfortable night reading on any website.

### 4. Full Website Offline Downloader
- Recursively crawls and archives multi-page sites including HTML subpages, CSS stylesheets, JavaScript files, and images.
- Localizes internal links for 100% offline navigation from internal storage.
- Offline Library manager with site preview and size metrics.

---

## 🤖 GitHub Automated Build & Release CI/CD

This repository includes a pre-configured GitHub Actions workflow in `.github/workflows/build-and-release.yml`.

### How It Works:
1. **Push to GitHub**:
   ```bash
   git add .
   git commit -m "Release DevChrome Browser v1.1.0"
   git push origin main
   ```
2. **Automatic Build**:
   GitHub Actions automatically:
   - Sets up JDK 17
   - Runs unit tests (`./gradlew testDebugUnitTest`)
   - Builds signed debug and release APKs (`./gradlew assembleRelease assembleDebug`)
   - Builds Android App Bundle (`./gradlew bundleRelease`)
3. **Automatic GitHub Release**:
   - Creates a GitHub Release tagged `v1.1.0` (or timestamp tag)
   - Attaches `app-release.apk`, `app-debug.apk`, and `app-release.aab` directly to the release page.
4. **Download & Install**:
   Go to the **Releases** tab in your GitHub repository and download `app-release.apk` directly onto your Android device!
