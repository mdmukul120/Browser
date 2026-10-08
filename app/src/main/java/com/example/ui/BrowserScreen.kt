package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.model.BrowserTab
import com.example.viewmodel.BrowserViewModel

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isBookmarked by viewModel.isCurrentPageBookmarked.collectAsState()

    // Dialog & Drawer states
    val isDevToolsOpen by viewModel.isDevToolsOpen.collectAsState()
    val showTabsDialog by viewModel.showTabsDialog.collectAsState()
    val showMenuDialog by viewModel.showMenuDialog.collectAsState()
    val showOfflineDialog by viewModel.showOfflineDialog.collectAsState()
    val showBookmarksDialog by viewModel.showBookmarksDialog.collectAsState()
    val showSecurityDialog by viewModel.showSecurityDialog.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showDownloadPrompt by viewModel.showDownloadSitePrompt.collectAsState()

    val savedWebsites by viewModel.savedWebsites.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    val currentTab = activeTab ?: BrowserTab()

    // Android Hardware Back Navigation Handler
    BackHandler {
        when {
            isDevToolsOpen -> viewModel.toggleDevTools()
            showTabsDialog -> viewModel.setTabsDialogVisible(false)
            showMenuDialog -> viewModel.setMenuDialogVisible(false)
            showOfflineDialog -> viewModel.setOfflineDialogVisible(false)
            showBookmarksDialog -> viewModel.setBookmarksDialogVisible(false)
            showSecurityDialog -> viewModel.setSecurityDialogVisible(false)
            showSettingsDialog -> viewModel.setSettingsDialogVisible(false)
            showDownloadPrompt -> viewModel.closeDownloadDialog()
            currentTab.canGoBack -> viewModel.goBack()
        }
    }

    Scaffold(
        topBar = {
            ChromeOmnibox(
                viewModel = viewModel,
                tab = currentTab,
                tabCount = tabs.size,
                isDarkTheme = settings.appDarkTheme
            )
        },
        bottomBar = {
            ChromeBottomBar(
                viewModel = viewModel,
                tab = currentTab,
                isBookmarked = isBookmarked
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Active Tab WebView
            WebViewController(
                viewModel = viewModel,
                tab = currentTab,
                settings = settings,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // F12 Developer Tools Drawer
    if (isDevToolsOpen) {
        DevToolsDrawer(
            viewModel = viewModel
        )
    }

    // Tabs Switcher
    if (showTabsDialog) {
        TabsSwitcherDialog(
            viewModel = viewModel,
            tabs = tabs,
            activeTabId = activeTabId,
            onDismiss = { viewModel.setTabsDialogVisible(false) }
        )
    }

    // Overflow Menu
    if (showMenuDialog) {
        ChromeMenuDialog(
            viewModel = viewModel,
            tab = currentTab,
            settings = settings,
            onDismiss = { viewModel.setMenuDialogVisible(false) }
        )
    }

    // Offline Sites Library
    if (showOfflineDialog) {
        OfflineSitesDialog(
            viewModel = viewModel,
            savedSites = savedWebsites,
            downloadProgress = downloadProgress,
            onDismiss = { viewModel.setOfflineDialogVisible(false) }
        )
    }

    // Download Site Prompt
    if (showDownloadPrompt) {
        DownloadSitePromptDialog(
            viewModel = viewModel,
            targetUrl = currentTab.url,
            onDismiss = { viewModel.closeDownloadDialog() }
        )
    }

    // Bookmarks and History
    if (showBookmarksDialog) {
        val bookmarks by viewModel.bookmarks.collectAsState()
        val history by viewModel.history.collectAsState()
        BookmarksHistoryDialog(
            viewModel = viewModel,
            bookmarks = bookmarks,
            history = history,
            onDismiss = { viewModel.setBookmarksDialogVisible(false) }
        )
    }

    // Site Security Information
    if (showSecurityDialog) {
        SiteSecurityDialog(
            tab = currentTab,
            onDismiss = { viewModel.setSecurityDialogVisible(false) }
        )
    }

    // Settings
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            settings = settings,
            onDismiss = { viewModel.setSettingsDialogVisible(false) }
        )
    }
}
