package com.example.devtools

object DevToolsScripts {

    /**
     * Injected into the document as early as possible to intercept console and network calls
     */
    val INJECTION_INIT_SCRIPT = """
        (function() {
            if (window.__devChromeInitialized) return;
            window.__devChromeInitialized = true;

            // 1. Console Interception
            const origConsole = {
                log: console.log,
                info: console.info,
                warn: console.warn,
                error: console.error,
                debug: console.debug
            };

            function serializeArg(arg) {
                if (arg === null) return "null";
                if (arg === undefined) return "undefined";
                if (typeof arg === "object") {
                    try {
                        return JSON.stringify(arg, null, 2);
                    } catch (e) {
                        return String(arg);
                    }
                }
                return String(arg);
            }

            function sendConsole(level, args, stack) {
                try {
                    const message = Array.from(args).map(serializeArg).join(' ');
                    if (window.DevChromeBridge && window.DevChromeBridge.onConsoleLog) {
                        window.DevChromeBridge.onConsoleLog(level, message, stack || "");
                    }
                } catch (e) {}
            }

            console.log = function() {
                origConsole.log.apply(console, arguments);
                sendConsole("LOG", arguments);
            };
            console.info = function() {
                origConsole.info.apply(console, arguments);
                sendConsole("INFO", arguments);
            };
            console.warn = function() {
                origConsole.warn.apply(console, arguments);
                sendConsole("WARN", arguments);
            };
            console.error = function() {
                origConsole.error.apply(console, arguments);
                sendConsole("ERROR", arguments, new Error().stack);
            };
            console.debug = function() {
                origConsole.debug.apply(console, arguments);
                sendConsole("DEBUG", arguments);
            };

            window.addEventListener('error', function(e) {
                sendConsole("ERROR", [e.message + " at " + e.filename + ":" + e.lineno], e.error ? e.error.stack : "");
            });

            window.addEventListener('unhandledrejection', function(e) {
                sendConsole("ERROR", ["Unhandled Promise Rejection: " + (e.reason ? (e.reason.message || e.reason) : "Unknown")], "");
            });

            // 2. Network Interception (Fetch & XHR)
            const origFetch = window.fetch;
            if (origFetch) {
                window.fetch = async function() {
                    const startTime = performance.now();
                    const args = arguments;
                    const url = typeof args[0] === 'string' ? args[0] : (args[0] && args[0].url ? args[0].url : 'unknown');
                    const method = (args[1] && args[1].method) ? args[1].method.toUpperCase() : 'GET';
                    
                    try {
                        const response = await origFetch.apply(this, args);
                        const duration = Math.round(performance.now() - startTime);
                        const clone = response.clone();
                        let preview = "";
                        try {
                            preview = (await clone.text()).substring(0, 500);
                        } catch (e) {}
                        
                        if (window.DevChromeBridge && window.DevChromeBridge.onNetworkRequest) {
                            window.DevChromeBridge.onNetworkRequest(
                                url, method, response.status, duration, "fetch", preview
                            );
                        }
                        return response;
                    } catch (err) {
                        const duration = Math.round(performance.now() - startTime);
                        if (window.DevChromeBridge && window.DevChromeBridge.onNetworkRequest) {
                            window.DevChromeBridge.onNetworkRequest(
                                url, method, 0, duration, "fetch", err.message || "Failed"
                            );
                        }
                        throw err;
                    }
                };
            }

            const origXhrOpen = XMLHttpRequest.prototype.open;
            const origXhrSend = XMLHttpRequest.prototype.send;
            XMLHttpRequest.prototype.open = function(method, url) {
                this._devUrl = url;
                this._devMethod = method ? method.toUpperCase() : 'GET';
                return origXhrOpen.apply(this, arguments);
            };
            XMLHttpRequest.prototype.send = function() {
                const startTime = performance.now();
                const xhr = this;
                this.addEventListener('loadend', function() {
                    const duration = Math.round(performance.now() - startTime);
                    let preview = "";
                    try {
                        preview = (xhr.responseText || "").substring(0, 500);
                    } catch(e) {}
                    if (window.DevChromeBridge && window.DevChromeBridge.onNetworkRequest) {
                        window.DevChromeBridge.onNetworkRequest(
                            xhr._devUrl || "", xhr._devMethod || "GET", xhr.status || 0, duration, "xhr", preview
                        );
                    }
                });
                return origXhrSend.apply(this, arguments);
            };
        })();
    """.trimIndent()

    /**
     * Element Inspector mode toggle script
     */
    fun getElementInspectorScript(enable: Boolean): String {
        return if (enable) """
            (function() {
                if (window.__devChromeInspectorActive) return;
                window.__devChromeInspectorActive = true;

                let highlightBox = document.getElementById('__dev_highlight_box');
                if (!highlightBox) {
                    highlightBox = document.createElement('div');
                    highlightBox.id = '__dev_highlight_box';
                    highlightBox.style.position = 'fixed';
                    highlightBox.style.pointerEvents = 'none';
                    highlightBox.style.zIndex = '2147483647';
                    highlightBox.style.border = '2px solid #1A73E8';
                    highlightBox.style.backgroundColor = 'rgba(26, 115, 232, 0.2)';
                    highlightBox.style.transition = 'all 0.1s ease';
                    highlightBox.style.boxSizing = 'border-box';
                    document.body.appendChild(highlightBox);
                }
                highlightBox.style.display = 'block';

                function onTouchMove(e) {
                    const touch = e.touches ? e.touches[0] : e;
                    const el = document.elementFromPoint(touch.clientX, touch.clientY);
                    if (el && el !== highlightBox) {
                        const rect = el.getBoundingClientRect();
                        highlightBox.style.top = rect.top + 'px';
                        highlightBox.style.left = rect.left + 'px';
                        highlightBox.style.width = rect.width + 'px';
                        highlightBox.style.height = rect.height + 'px';
                    }
                }

                function onTouchEnd(e) {
                    const touch = e.changedTouches ? e.changedTouches[0] : e;
                    const el = document.elementFromPoint(touch.clientX, touch.clientY);
                    if (el && el !== highlightBox) {
                        e.preventDefault();
                        e.stopPropagation();

                        const style = window.getComputedStyle(el);
                        const stylesObj = {
                            "display": style.display,
                            "color": style.color,
                            "background-color": style.backgroundColor,
                            "font-size": style.fontSize,
                            "font-family": style.fontFamily,
                            "width": style.width,
                            "height": style.height,
                            "margin": style.margin,
                            "padding": style.padding,
                            "border": style.border,
                            "position": style.position
                        };

                        const attrs = {};
                        for (let i = 0; i < el.attributes.length; i++) {
                            const a = el.attributes[i];
                            attrs[a.name] = a.value;
                        }

                        if (window.DevChromeBridge && window.DevChromeBridge.onDomElementSelected) {
                            window.DevChromeBridge.onDomElementSelected(
                                el.tagName.toLowerCase(),
                                el.id || "",
                                el.className || "",
                                (el.innerText || "").substring(0, 100),
                                (el.outerHTML || "").substring(0, 3000),
                                JSON.stringify(attrs),
                                JSON.stringify(stylesObj)
                            );
                        }
                    }
                }

                window.__devInspectorMove = onTouchMove;
                window.__devInspectorEnd = onTouchEnd;

                window.addEventListener('touchmove', onTouchMove, { passive: true });
                window.addEventListener('touchend', onTouchEnd, { capture: true });
                window.addEventListener('click', onTouchEnd, { capture: true });
            })();
        """.trimIndent()
        else """
            (function() {
                window.__devChromeInspectorActive = false;
                const box = document.getElementById('__dev_highlight_box');
                if (box) box.style.display = 'none';
                if (window.__devInspectorMove) {
                    window.removeEventListener('touchmove', window.__devInspectorMove);
                }
                if (window.__devInspectorEnd) {
                    window.removeEventListener('touchend', window.__devInspectorEnd, { capture: true });
                    window.removeEventListener('click', window.__devInspectorEnd, { capture: true });
                }
            })();
        """.trimIndent()
    }

    /**
     * Storage extraction script
     */
    val EXTRACT_STORAGE_SCRIPT = """
        (function() {
            try {
                const local = {};
                for (let i = 0; i < localStorage.length; i++) {
                    const key = localStorage.key(i);
                    local[key] = localStorage.getItem(key);
                }

                const session = {};
                for (let i = 0; i < sessionStorage.length; i++) {
                    const key = sessionStorage.key(i);
                    session[key] = sessionStorage.getItem(key);
                }

                const cookies = {};
                if (document.cookie) {
                    document.cookie.split(';').forEach(c => {
                        const parts = c.trim().split('=');
                        if (parts[0]) cookies[parts[0]] = parts.slice(1).join('=');
                    });
                }

                if (window.DevChromeBridge && window.DevChromeBridge.onStorageData) {
                    window.DevChromeBridge.onStorageData(
                        JSON.stringify(local),
                        JSON.stringify(session),
                        JSON.stringify(cookies)
                    );
                }
            } catch (e) {
                if (window.DevChromeBridge && window.DevChromeBridge.onConsoleLog) {
                    window.DevChromeBridge.onConsoleLog("ERROR", "Storage access failed: " + e.message, "");
                }
            }
        })();
    """.trimIndent()

    /**
     * Performance Timing extraction script
     */
    val EXTRACT_PERFORMANCE_SCRIPT = """
        (function() {
            try {
                const t = window.performance ? window.performance.timing : null;
                const nav = performance.getEntriesByType ? performance.getEntriesByType('navigation')[0] : null;
                
                let dns = 0, tcp = 0, ttfb = 0, domReady = 0, loadTime = 0;

                if (nav) {
                    dns = Math.round(nav.domainLookupEnd - nav.domainLookupStart);
                    tcp = Math.round(nav.connectEnd - nav.connectStart);
                    ttfb = Math.round(nav.responseStart - nav.requestStart);
                    domReady = Math.round(nav.domContentLoadedEventEnd - nav.startTime);
                    loadTime = Math.round(nav.loadEventEnd - nav.startTime);
                } else if (t) {
                    dns = Math.max(0, t.domainLookupEnd - t.domainLookupStart);
                    tcp = Math.max(0, t.connectEnd - t.connectStart);
                    ttfb = Math.max(0, t.responseStart - t.requestStart);
                    domReady = Math.max(0, t.domContentLoadedEventEnd - t.navigationStart);
                    loadTime = Math.max(0, t.loadEventEnd - t.navigationStart);
                }

                const resourceCount = performance.getEntriesByType ? performance.getEntriesByType('resource').length : 0;
                const domNodeCount = document.getElementsByTagName('*').length;
                let memoryMb = 0;
                if (performance.memory && performance.memory.usedJSHeapSize) {
                    memoryMb = (performance.memory.usedJSHeapSize / (1024 * 1024)).toFixed(2);
                }

                const data = {
                    dns: Math.max(0, dns),
                    tcp: Math.max(0, tcp),
                    ttfb: Math.max(0, ttfb),
                    domReady: Math.max(0, domReady),
                    loadTime: Math.max(0, loadTime),
                    resources: resourceCount,
                    domNodes: domNodeCount,
                    memory: parseFloat(memoryMb)
                };

                if (window.DevChromeBridge && window.DevChromeBridge.onPerformanceMetrics) {
                    window.DevChromeBridge.onPerformanceMetrics(JSON.stringify(data));
                }
            } catch (e) {}
        })();
    """.trimIndent()

    /**
     * Web Force Dark Mode CSS injection
     */
    const val FORCE_DARK_CSS = """
        html {
            filter: invert(90%) hue-rotate(180deg) !important;
            background: #121212 !important;
        }
        img, video, canvas, svg, [style*="background-image"], iframe {
            filter: invert(100%) hue-rotate(180deg) !important;
        }
    """

    fun getApplyForceDarkScript(enable: Boolean): String {
        return if (enable) """
            (function() {
                let style = document.getElementById('__devchrome_force_dark');
                if (!style) {
                    style = document.createElement('style');
                    style.id = '__devchrome_force_dark';
                    style.innerHTML = `$FORCE_DARK_CSS`;
                    document.documentElement.appendChild(style);
                }
            })();
        """.trimIndent()
        else """
            (function() {
                const style = document.getElementById('__devchrome_force_dark');
                if (style) style.remove();
            })();
        """.trimIndent()
    }

    /**
     * Injects the Eruda developer console suite as an optional floating tool
     */
    val INJECT_ERUDA_SCRIPT = """
        (function() {
            if (window.eruda) {
                window.eruda.show();
                return;
            }
            const script = document.createElement('script');
            script.src = 'https://cdn.jsdelivr.net/npm/eruda';
            script.onload = function() {
                window.eruda.init();
                window.eruda.show();
            };
            document.body.appendChild(script);
        })();
    """.trimIndent()
}
