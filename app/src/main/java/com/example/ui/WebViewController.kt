package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.adblock.AdBlockEngine
import com.example.devtools.ConsoleLogEntry
import com.example.devtools.ConsoleLogLevel
import com.example.devtools.DevToolsBridge
import com.example.devtools.DevToolsScripts
import com.example.model.BrowserTab
import com.example.model.BrowserSettings
import com.example.model.UserAgentType
import com.example.viewmodel.BrowserViewModel

@Suppress("DEPRECATION")
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewController(
    viewModel: BrowserViewModel,
    tab: BrowserTab,
    settings: BrowserSettings,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Configure WebView Settings
                this.settings.apply {
                    javaScriptEnabled = settings.javascriptEnabled
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    allowFileAccess = true
                    allowContentAccess = true

                    // Allow local file browsing for downloaded offline sites
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true

                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                    // Cache mode
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                // Attach Javascript Bridge for DevTools
                val bridge = DevToolsBridge(viewModel)
                addJavascriptInterface(bridge, "DevChromeBridge")

                // WebChromeClient for Progress, Title, and native Console messages
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        viewModel.updateActiveTabProgress(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        val currentUrl = view?.url ?: tab.url
                        viewModel.updateActiveTabInfo(
                            title = title ?: "",
                            url = currentUrl,
                            canGoBack = view?.canGoBack() == true,
                            canGoForward = view?.canGoForward() == true
                        )
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let { msg ->
                            val level = when (msg.messageLevel()) {
                                ConsoleMessage.MessageLevel.ERROR -> ConsoleLogLevel.ERROR
                                ConsoleMessage.MessageLevel.WARNING -> ConsoleLogLevel.WARN
                                ConsoleMessage.MessageLevel.LOG -> ConsoleLogLevel.LOG
                                ConsoleMessage.MessageLevel.TIP -> ConsoleLogLevel.INFO
                                ConsoleMessage.MessageLevel.DEBUG -> ConsoleLogLevel.DEBUG
                                else -> ConsoleLogLevel.LOG
                            }
                            viewModel.onConsoleLogReceived(
                                ConsoleLogEntry(
                                    level = level,
                                    message = "${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})"
                                )
                            )
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                // WebViewClient for Ad Blocking, Error Handling, and Script Injection
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        if (settings.adBlockEnabled && request != null) {
                            val reqUrl = request.url.toString()
                            if (AdBlockEngine.isAdOrTracker(reqUrl)) {
                                viewModel.incrementTabBlockedAds()
                                return AdBlockEngine.createEmptyResponse()
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        // Inject early bridge script
                        view?.evaluateJavascript(DevToolsScripts.INJECTION_INIT_SCRIPT, null)

                        // If Force Dark Web is on, inject early CSS
                        if (settings.forceDarkWeb) {
                            view?.evaluateJavascript(DevToolsScripts.getApplyForceDarkScript(true), null)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val finalUrl = url ?: tab.url
                        viewModel.updateActiveTabInfo(
                            title = view?.title ?: "",
                            url = finalUrl,
                            canGoBack = view?.canGoBack() == true,
                            canGoForward = view?.canGoForward() == true
                        )

                        // Re-inject DevTools bridge
                        view?.evaluateJavascript(DevToolsScripts.INJECTION_INIT_SCRIPT, null)

                        // Re-apply Force Dark Web if enabled
                        if (settings.forceDarkWeb) {
                            view?.evaluateJavascript(DevToolsScripts.getApplyForceDarkScript(true), null)
                        }

                        // Re-apply Element Inspector if active
                        if (tab.isInspectorActive) {
                            view?.evaluateJavascript(DevToolsScripts.getElementInspectorScript(true), null)
                        }

                        // Cosmetic adblock style injection
                        if (settings.adBlockEnabled) {
                            val cosmeticScript = """
                                (function() {
                                    let adStyle = document.getElementById('__dev_cosmetic_adblock');
                                    if (!adStyle) {
                                        adStyle = document.createElement('style');
                                        adStyle.id = '__dev_cosmetic_adblock';
                                        adStyle.innerHTML = `${AdBlockEngine.COSMETIC_ADBLOCK_CSS}`;
                                        document.head.appendChild(adStyle);
                                    }
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(cosmeticScript, null)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request?.isForMainFrame == true) {
                            viewModel.onConsoleLogReceived(
                                ConsoleLogEntry(
                                    level = ConsoleLogLevel.ERROR,
                                    message = "Navigation Error: ${error?.description} (code ${error?.errorCode})"
                                )
                            )
                        }
                    }
                }

                // Initial load
                if (tab.url.isNotBlank() && tab.url != "about:blank") {
                    loadUrl(tab.url)
                }

                webViewInstance = this
                viewModel.attachWebView(this)
            }
        },
        update = { webView ->
            viewModel.attachWebView(webView)

            // Update user agent string if desktop mode or setting changed
            val targetUserAgent = when {
                tab.isDesktopMode -> UserAgentType.DESKTOP_CHROME.userAgentString
                settings.userAgentType != UserAgentType.DEFAULT_MOBILE -> settings.userAgentType.userAgentString
                else -> null
            }
            if (targetUserAgent != null && webView.settings.userAgentString != targetUserAgent) {
                webView.settings.userAgentString = targetUserAgent
            } else if (targetUserAgent == null && webView.settings.userAgentString != null) {
                webView.settings.userAgentString = null // Reset to default
            }

            // Sync JavaScript setting
            if (webView.settings.javaScriptEnabled != settings.javascriptEnabled) {
                webView.settings.javaScriptEnabled = settings.javascriptEnabled
            }

            // Load URL if changed externally and not matching current
            if (webView.url != tab.url && tab.url.isNotBlank() && tab.url != "about:blank") {
                webView.loadUrl(tab.url)
            }
        }
    )
}
