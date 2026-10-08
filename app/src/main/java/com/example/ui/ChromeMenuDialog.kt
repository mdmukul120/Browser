package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adblock.AdBlockEngine
import com.example.model.BrowserTab
import com.example.model.BrowserSettings
import com.example.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromeMenuDialog(
    viewModel: BrowserViewModel,
    tab: BrowserTab,
    settings: BrowserSettings,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Browser Options",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Primary Dev & Offline Actions (Prominent cards)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        viewModel.toggleDevTools()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "F12 DevTools",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Developer Tools (F12)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Console, DOM Inspector, Network, Storage, Performance",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Download Entire Website
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        viewModel.openDownloadDialog()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadForOffline,
                        contentDescription = "Download Full Website",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download Entire Website",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Save full multi-page site with CSS, JS & images for 100% offline browsing",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Toggles Section
            MenuItemToggle(
                icon = Icons.Default.Shield,
                title = "Ad & Tracker Blocker",
                subtitle = "Total blocked: ${AdBlockEngine.getBlockedCount()} (This tab: ${tab.adsBlockedCount})",
                checked = settings.adBlockEnabled,
                onCheckedChange = { viewModel.toggleAdBlock() }
            )

            MenuItemToggle(
                icon = Icons.Default.DarkMode,
                title = "Force Dark Mode on Web",
                subtitle = "Inverts light web content comfortably for night reading",
                checked = settings.forceDarkWeb,
                onCheckedChange = { viewModel.toggleForceDarkWeb() }
            )

            MenuItemToggle(
                icon = Icons.Default.Laptop,
                title = "Desktop Site",
                subtitle = "Request desktop PC version of webpage",
                checked = tab.isDesktopMode,
                onCheckedChange = { viewModel.toggleDesktopMode() }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Actions
            MenuItemRow(
                icon = Icons.Default.Add,
                title = "New Tab",
                onClick = {
                    onDismiss()
                    viewModel.createTab()
                }
            )

            MenuItemRow(
                icon = Icons.Default.VisibilityOff,
                title = "New Incognito Tab",
                onClick = {
                    onDismiss()
                    viewModel.createTab(isIncognito = true)
                }
            )

            MenuItemRow(
                icon = Icons.Default.FolderSpecial,
                title = "Saved Websites (Offline Library)",
                onClick = {
                    onDismiss()
                    viewModel.setOfflineDialogVisible(true)
                }
            )

            MenuItemRow(
                icon = Icons.Default.BookmarkBorder,
                title = "Bookmarks",
                onClick = {
                    onDismiss()
                    viewModel.setBookmarksDialogVisible(true)
                }
            )

            MenuItemRow(
                icon = Icons.Default.History,
                title = "History",
                onClick = {
                    onDismiss()
                    viewModel.setBookmarksDialogVisible(true)
                }
            )

            MenuItemRow(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = {
                    onDismiss()
                    viewModel.setSettingsDialogVisible(true)
                }
            )
        }
    }
}

@Composable
private fun MenuItemToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
