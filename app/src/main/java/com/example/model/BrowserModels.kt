package com.example.model

data class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "about:blank",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isIncognito: Boolean = false,
    val isDesktopMode: Boolean = false,
    val adsBlockedCount: Int = 0,
    val jsErrorsCount: Int = 0,
    val isInspectorActive: Boolean = false
)

enum class UserAgentType(val displayName: String, val userAgentString: String) {
    DEFAULT_MOBILE(
        "Chrome Mobile (Default)",
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    ),
    DESKTOP_CHROME(
        "Chrome Desktop (PC)",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    ),
    SAFARI_IOS(
        "Safari (iPhone iOS)",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"
    ),
    FIREFOX_DESKTOP(
        "Firefox (Desktop)",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0"
    )
}

data class BrowserSettings(
    val adBlockEnabled: Boolean = true,
    val forceDarkWeb: Boolean = false,
    val appDarkTheme: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val desktopModeDefault: Boolean = false,
    val userAgentType: UserAgentType = UserAgentType.DEFAULT_MOBILE,
    val searchEngineUrl: String = "https://www.google.com/search?q="
)
