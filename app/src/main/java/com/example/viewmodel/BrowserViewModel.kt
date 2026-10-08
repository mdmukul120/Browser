package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adblock.AdBlockEngine
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.SavedWebsiteEntity
import com.example.devtools.*
import com.example.model.BrowserTab
import com.example.model.BrowserSettings
import com.example.model.UserAgentType
import com.example.offline.OfflineSiteDownloader
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class DevToolsTab {
    CONSOLE, ELEMENTS, NETWORK, STORAGE, PERFORMANCE, SOURCES
}

class BrowserViewModel(application: Application) : AndroidViewModel(application), DevToolsListener {

    private val db = AppDatabase.getDatabase(application)
    private val bookmarkDao = db.bookmarkDao
    private val historyDao = db.historyDao
    private val savedWebsiteDao = db.savedWebsiteDao

    val offlineDownloader = OfflineSiteDownloader(application)
    val downloadProgress = offlineDownloader.progress

    val savedWebsites: StateFlow<List<SavedWebsiteEntity>> = savedWebsiteDao.getAllSavedWebsites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = historyDao.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tabs state
    private val initialTab = BrowserTab(url = "https://www.google.com")
    private val _tabs = MutableStateFlow<List<BrowserTab>>(listOf(initialTab))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val activeTab: StateFlow<BrowserTab?> = combine(_tabs, _activeTabId) { tabList, id ->
        tabList.find { it.id == id } ?: tabList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialTab)

    // Browser Settings
    private val _settings = MutableStateFlow(BrowserSettings())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    // Current page bookmark status
    private val _isCurrentPageBookmarked = MutableStateFlow(false)
    val isCurrentPageBookmarked: StateFlow<Boolean> = _isCurrentPageBookmarked.asStateFlow()

    // F12 Developer Tools State
    private val _isDevToolsOpen = MutableStateFlow(false)
    val isDevToolsOpen: StateFlow<Boolean> = _isDevToolsOpen.asStateFlow()

    private val _activeDevToolsTab = MutableStateFlow(DevToolsTab.CONSOLE)
    val activeDevToolsTab: StateFlow<DevToolsTab> = _activeDevToolsTab.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<ConsoleLogEntry>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLogEntry>> = _consoleLogs.asStateFlow()

    private val _consoleFilter = MutableStateFlow<ConsoleLogLevel?>(null)
    val consoleFilter: StateFlow<ConsoleLogLevel?> = _consoleFilter.asStateFlow()

    private val _networkRequests = MutableStateFlow<List<NetworkRequestEntry>>(emptyList())
    val networkRequests: StateFlow<List<NetworkRequestEntry>> = _networkRequests.asStateFlow()

    private val _selectedDomElement = MutableStateFlow<DomSelectedElement?>(null)
    val selectedDomElement: StateFlow<DomSelectedElement?> = _selectedDomElement.asStateFlow()

    private val _storageEntries = MutableStateFlow<List<StorageEntry>>(emptyList())
    val storageEntries: StateFlow<List<StorageEntry>> = _storageEntries.asStateFlow()

    private val _performanceMetrics = MutableStateFlow<PerformanceMetrics?>(null)
    val performanceMetrics: StateFlow<PerformanceMetrics?> = _performanceMetrics.asStateFlow()

    private val _pageSource = MutableStateFlow<String?>(null)
    val pageSource: StateFlow<String?> = _pageSource.asStateFlow()

    // UI Dialogs
    private val _showTabsDialog = MutableStateFlow(false)
    val showTabsDialog: StateFlow<Boolean> = _showTabsDialog.asStateFlow()

    private val _showMenuDialog = MutableStateFlow(false)
    val showMenuDialog: StateFlow<Boolean> = _showMenuDialog.asStateFlow()

    private val _showOfflineDialog = MutableStateFlow(false)
    val showOfflineDialog: StateFlow<Boolean> = _showOfflineDialog.asStateFlow()

    private val _showBookmarksDialog = MutableStateFlow(false)
    val showBookmarksDialog: StateFlow<Boolean> = _showBookmarksDialog.asStateFlow()

    private val _showSecurityDialog = MutableStateFlow(false)
    val showSecurityDialog: StateFlow<Boolean> = _showSecurityDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showDownloadSitePrompt = MutableStateFlow(false)
    val showDownloadSitePrompt: StateFlow<Boolean> = _showDownloadSitePrompt.asStateFlow()

    // WebView reference to evaluate JS
    private var currentWebView: WebView? = null

    fun attachWebView(webView: WebView?) {
        this.currentWebView = webView
    }

    // --- TAB MANAGEMENT ---
    fun createTab(url: String = "https://www.google.com", isIncognito: Boolean = false) {
        val newTab = BrowserTab(url = url, isIncognito = isIncognito)
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        _showTabsDialog.value = false
        clearDevToolsForNewPage()
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value
        if (currentList.size <= 1) {
            // Keep at least one tab open
            val newTab = BrowserTab(url = "https://www.google.com")
            _tabs.value = listOf(newTab)
            _activeTabId.value = newTab.id
            return
        }
        val remaining = currentList.filter { it.id != tabId }
        _tabs.value = remaining
        if (_activeTabId.value == tabId) {
            _activeTabId.value = remaining.last().id
        }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
        _showTabsDialog.value = false
        checkBookmarkStatus()
    }

    // --- NAVIGATION ACTIONS ---
    fun navigateTo(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val url = if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            "${_settings.value.searchEngineUrl}${Uri.encode(trimmed)}"
        }

        updateActiveTab { it.copy(url = url, isLoading = true, progress = 10) }
        currentWebView?.loadUrl(url)
        clearDevToolsForNewPage()
    }

    fun reload() {
        currentWebView?.reload()
    }

    fun stopLoading() {
        currentWebView?.stopLoading()
    }

    fun goBack() {
        if (currentWebView?.canGoBack() == true) {
            currentWebView?.goBack()
        }
    }

    fun goForward() {
        if (currentWebView?.canGoForward() == true) {
            currentWebView?.goForward()
        }
    }

    // --- ACTIVE TAB STATE UPDATES ---
    fun updateActiveTabProgress(progress: Int) {
        updateActiveTab { it.copy(progress = progress, isLoading = progress < 100) }
    }

    fun updateActiveTabInfo(title: String, url: String, canGoBack: Boolean, canGoForward: Boolean) {
        updateActiveTab {
            it.copy(
                title = title.ifBlank { "Untitled" },
                url = url,
                canGoBack = canGoBack,
                canGoForward = canGoForward
            )
        }
        checkBookmarkStatus()

        // Save to history if not incognito and not blank
        val active = activeTab.value
        if (active != null && !active.isIncognito && url.isNotBlank() && !url.startsWith("about:")) {
            viewModelScope.launch {
                historyDao.insertHistory(HistoryEntity(title = title.ifBlank { url }, url = url))
            }
        }
    }

    fun incrementTabBlockedAds() {
        updateActiveTab { it.copy(adsBlockedCount = it.adsBlockedCount + 1) }
    }

    private fun updateActiveTab(transform: (BrowserTab) -> BrowserTab) {
        val currentId = _activeTabId.value
        _tabs.value = _tabs.value.map {
            if (it.id == currentId) transform(it) else it
        }
    }

    // --- DEVELOPER TOOLS ACTIONS ---
    fun toggleDevTools() {
        _isDevToolsOpen.value = !_isDevToolsOpen.value
        if (_isDevToolsOpen.value) {
            requestStorageInspection()
            requestPerformanceInspection()
        }
    }

    fun setDevToolsTab(tab: DevToolsTab) {
        _activeDevToolsTab.value = tab
        when (tab) {
            DevToolsTab.STORAGE -> requestStorageInspection()
            DevToolsTab.PERFORMANCE -> requestPerformanceInspection()
            DevToolsTab.SOURCES -> requestPageSource()
            else -> {}
        }
    }

    fun executeJsInPage(jsCode: String) {
        if (jsCode.isBlank()) return
        val wrapped = """
            (function() {
                try {
                    const result = eval(${android.net.Uri.encode(jsCode).let { "\"$it\"" }}.replace(/%([0-9A-F]{2})/g, function(m, p) { return String.fromCharCode('0x' + p); }));
                    return result !== undefined ? String(result) : "undefined";
                } catch(e) {
                    return "Error: " + e.message;
                }
            })();
        """.trimIndent()

        currentWebView?.evaluateJavascript(wrapped) { result ->
            val cleanResult = result?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: "null"
            val logEntry = ConsoleLogEntry(
                level = if (cleanResult.startsWith("Error:")) ConsoleLogLevel.ERROR else ConsoleLogLevel.RESULT,
                message = "> $jsCode\n$cleanResult"
            )
            onConsoleLogReceived(logEntry)
        }
    }

    fun toggleElementInspector() {
        val current = activeTab.value?.isInspectorActive ?: false
        val newState = !current
        updateActiveTab { it.copy(isInspectorActive = newState) }
        val script = DevToolsScripts.getElementInspectorScript(newState)
        currentWebView?.evaluateJavascript(script, null)
        if (newState) {
            _isDevToolsOpen.value = true
            _activeDevToolsTab.value = DevToolsTab.ELEMENTS
        }
    }

    fun injectErudaWidget() {
        currentWebView?.evaluateJavascript(DevToolsScripts.INJECT_ERUDA_SCRIPT, null)
    }

    fun requestStorageInspection() {
        currentWebView?.evaluateJavascript(DevToolsScripts.EXTRACT_STORAGE_SCRIPT, null)
    }

    fun requestPerformanceInspection() {
        currentWebView?.evaluateJavascript(DevToolsScripts.EXTRACT_PERFORMANCE_SCRIPT, null)
    }

    fun requestPageSource() {
        currentWebView?.evaluateJavascript(
            "(function() { return document.documentElement.outerHTML; })();"
        ) { html ->
            val unescaped = html?.removeSurrounding("\"")
                ?.replace("\\\"", "\"")
                ?.replace("\\n", "\n")
                ?.replace("\\t", "\t")
            _pageSource.value = unescaped
        }
    }

    fun clearConsole() {
        _consoleLogs.value = emptyList()
        updateActiveTab { it.copy(jsErrorsCount = 0) }
    }

    fun clearNetwork() {
        _networkRequests.value = emptyList()
    }

    fun setConsoleFilter(level: ConsoleLogLevel?) {
        _consoleFilter.value = level
    }

    private fun clearDevToolsForNewPage() {
        _consoleLogs.value = emptyList()
        _networkRequests.value = emptyList()
        _selectedDomElement.value = null
        _performanceMetrics.value = null
        _pageSource.value = null
        updateActiveTab { it.copy(jsErrorsCount = 0, isInspectorActive = false, adsBlockedCount = 0) }
    }

    // --- DevToolsListener Callbacks ---
    override fun onConsoleLogReceived(entry: ConsoleLogEntry) {
        _consoleLogs.value = _consoleLogs.value + entry
        if (entry.level == ConsoleLogLevel.ERROR) {
            updateActiveTab { it.copy(jsErrorsCount = it.jsErrorsCount + 1) }
        }
    }

    override fun onNetworkRequestCaptured(entry: NetworkRequestEntry) {
        _networkRequests.value = _networkRequests.value + entry
    }

    override fun onElementSelected(element: DomSelectedElement) {
        _selectedDomElement.value = element
        _isDevToolsOpen.value = true
        _activeDevToolsTab.value = DevToolsTab.ELEMENTS
    }

    override fun onStorageExtracted(entries: List<StorageEntry>) {
        _storageEntries.value = entries
    }

    override fun onPerformanceMetricsUpdated(metrics: PerformanceMetrics) {
        _performanceMetrics.value = metrics
    }

    override fun onPageSourceReady(source: String) {
        _pageSource.value = source
    }

    // --- SETTINGS TOGGLES ---
    fun toggleAdBlock() {
        val newSetting = !_settings.value.adBlockEnabled
        _settings.value = _settings.value.copy(adBlockEnabled = newSetting)
        reload()
    }

    fun toggleForceDarkWeb() {
        val newSetting = !_settings.value.forceDarkWeb
        _settings.value = _settings.value.copy(forceDarkWeb = newSetting)
        currentWebView?.evaluateJavascript(DevToolsScripts.getApplyForceDarkScript(newSetting), null)
    }

    fun toggleAppDarkTheme() {
        _settings.value = _settings.value.copy(appDarkTheme = !_settings.value.appDarkTheme)
    }

    fun toggleDesktopMode() {
        val current = activeTab.value?.isDesktopMode ?: false
        val newMode = !current
        updateActiveTab { it.copy(isDesktopMode = newMode) }
        reload()
    }

    fun setUserAgentType(type: UserAgentType) {
        _settings.value = _settings.value.copy(userAgentType = type)
        reload()
    }

    // --- OFFLINE FULL WEBSITE DOWNLOADER ---
    fun openDownloadDialog() {
        _showMenuDialog.value = false
        _showDownloadSitePrompt.value = true
    }

    fun closeDownloadDialog() {
        _showDownloadSitePrompt.value = false
    }

    fun startDownloadingCurrentWebsite(maxPages: Int = 20) {
        val url = activeTab.value?.url ?: return
        if (url.startsWith("http://") || url.startsWith("https://")) {
            _showDownloadSitePrompt.value = false
            _showOfflineDialog.value = true
            viewModelScope.launch {
                val result = offlineDownloader.downloadEntireWebsite(url, maxPagesLimit = maxPages)
                result.onSuccess { site ->
                    savedWebsiteDao.insertSavedWebsite(
                        SavedWebsiteEntity(
                            id = site.id,
                            title = site.title,
                            originalUrl = site.originalUrl,
                            localIndexPath = site.localIndexPath,
                            totalPages = site.totalPages,
                            totalAssets = site.totalAssets,
                            totalBytes = site.totalBytes
                        )
                    )
                }
            }
        }
    }

    fun openSavedOfflineWebsite(site: SavedWebsiteEntity) {
        _showOfflineDialog.value = false
        val fileUrl = "file://${site.localIndexPath}"
        createTab(url = fileUrl)
    }

    fun deleteSavedWebsite(site: SavedWebsiteEntity) {
        viewModelScope.launch {
            try {
                val file = java.io.File(site.localIndexPath).parentFile
                if (file != null && file.exists()) {
                    file.deleteRecursively()
                }
            } catch (_: Exception) {}
            savedWebsiteDao.deleteSavedWebsite(site)
        }
    }

    // --- BOOKMARKS & HISTORY ---
    fun toggleBookmark() {
        val active = activeTab.value ?: return
        val url = active.url
        if (url.isBlank() || url.startsWith("about:")) return

        viewModelScope.launch {
            if (_isCurrentPageBookmarked.value) {
                bookmarkDao.deleteByUrl(url)
                _isCurrentPageBookmarked.value = false
            } else {
                bookmarkDao.insertBookmark(
                    BookmarkEntity(title = active.title.ifBlank { url }, url = url)
                )
                _isCurrentPageBookmarked.value = true
            }
        }
    }

    private fun checkBookmarkStatus() {
        val url = activeTab.value?.url ?: return
        viewModelScope.launch {
            _isCurrentPageBookmarked.value = bookmarkDao.isBookmarked(url)
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            bookmarkDao.deleteBookmark(bookmark)
            checkBookmarkStatus()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyDao.clearHistory()
        }
    }

    // --- DIALOG VISIBILITY ---
    fun setTabsDialogVisible(visible: Boolean) { _showTabsDialog.value = visible }
    fun setMenuDialogVisible(visible: Boolean) { _showMenuDialog.value = visible }
    fun setOfflineDialogVisible(visible: Boolean) { _showOfflineDialog.value = visible }
    fun setBookmarksDialogVisible(visible: Boolean) { _showBookmarksDialog.value = visible }
    fun setSecurityDialogVisible(visible: Boolean) { _showSecurityDialog.value = visible }
    fun setSettingsDialogVisible(visible: Boolean) { _showSettingsDialog.value = visible }
}
