package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.model.BrowserSettings
import com.example.model.BrowserTab
import com.example.model.UserAgentType
import com.example.viewmodel.BrowserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksHistoryDialog(
    viewModel: BrowserViewModel,
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bookmarks, 1: History

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bookmarks (${bookmarks.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("History (${history.size})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Bookmarks List
                if (bookmarks.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No bookmarks yet. Tap the star icon on any webpage to save it.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bookmarks, key = { it.id }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        viewModel.navigateTo(item.url)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { viewModel.deleteBookmark(item) }) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // History List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAllHistory() }) {
                            Text("Clear History", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (history.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Browsing history is empty.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(history, key = { it.id }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        viewModel.navigateTo(item.url)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(item.title, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(sdf.format(Date(item.visitedAt)), fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SiteSecurityDialog(
    tab: BrowserTab,
    onDismiss: () -> Unit
) {
    val isSecure = tab.url.startsWith("https://")
    val isOffline = tab.url.startsWith("file://")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when {
                    isOffline -> Icons.Default.FolderOpen
                    isSecure -> Icons.Default.Lock
                    else -> Icons.Default.WarningAmber
                },
                contentDescription = null,
                tint = when {
                    isOffline -> Color(0xFF81C995)
                    isSecure -> Color(0xFF8AB4F8)
                    else -> Color(0xFFF28B82)
                },
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = when {
                    isOffline -> "Offline Local Archive"
                    isSecure -> "Connection is Secure"
                    else -> "Connection is Not Secure"
                },
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                Text(
                    text = when {
                        isOffline -> "This page is served from your local offline archive. No network connectivity is required."
                        isSecure -> "Your information (for example, passwords or credit card numbers) is private when it is sent to this site (HTTPS)."
                        else -> "You should not enter any sensitive information on this site because it may be seen by attackers (HTTP)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Page Details:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("URL: ${tab.url}", fontSize = 11.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Ads Blocked: ${tab.adsBlockedCount}", fontSize = 11.sp, color = Color.Gray)
                Text("JS Errors: ${tab.jsErrorsCount}", fontSize = 11.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: BrowserViewModel,
    settings: BrowserSettings,
    onDismiss: () -> Unit
) {
    var expandedUa by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Browser Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Dark Theme Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("App Dark Theme", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("Chrome deep dark mode UI", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.appDarkTheme,
                    onCheckedChange = { viewModel.toggleAppDarkTheme() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Web Force Dark Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Web Force Dark Mode", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("Invert light webpages to dark mode for comfortable reading", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.forceDarkWeb,
                    onCheckedChange = { viewModel.toggleForceDarkWeb() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ad Blocker Global
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ad & Tracker Blocker", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("Block ad networks, telemetry & banners", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.adBlockEnabled,
                    onCheckedChange = { viewModel.toggleAdBlock() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Agent Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("User Agent String", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("Controls how websites identify your browser", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                Box {
                    OutlinedButton(
                        onClick = { expandedUa = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(settings.userAgentType.displayName)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = expandedUa,
                        onDismissRequest = { expandedUa = false }
                    ) {
                        UserAgentType.entries.forEach { ua ->
                            DropdownMenuItem(
                                text = { Text(ua.displayName) },
                                onClick = {
                                    viewModel.setUserAgentType(ua)
                                    expandedUa = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
