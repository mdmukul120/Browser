package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserTab
import com.example.ui.theme.DevConsoleRed
import com.example.viewmodel.BrowserViewModel

@Composable
fun ChromeBottomBar(
    viewModel: BrowserViewModel,
    tab: BrowserTab,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = if (tab.isIncognito) Color(0xFF1F1F1F) else MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = barColor,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back
            IconButton(
                onClick = { viewModel.goBack() },
                enabled = tab.canGoBack,
                modifier = Modifier.testTag("nav_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (tab.canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Forward
            IconButton(
                onClick = { viewModel.goForward() },
                enabled = tab.canGoForward,
                modifier = Modifier.testTag("nav_forward_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (tab.canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Home
            IconButton(
                onClick = { viewModel.navigateTo("https://www.google.com") },
                modifier = Modifier.testTag("nav_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // F12 Developer Tools (Console / Inspector) button with error badge!
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = { viewModel.toggleDevTools() },
                    modifier = Modifier.testTag("nav_devtools_f12_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Developer Tools (F12)",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (tab.jsErrorsCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-4).dp, y = 4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(DevConsoleRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tab.jsErrorsCount > 99) "99+" else tab.jsErrorsCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bookmark Toggle
            IconButton(
                onClick = { viewModel.toggleBookmark() },
                modifier = Modifier.testTag("nav_bookmark_button")
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isBookmarked) "Remove Bookmark" else "Add Bookmark",
                    tint = if (isBookmarked) Color(0xFFFBBC04) else MaterialTheme.colorScheme.onSurface
                )
            }

            // Quick Offline Downloader button
            IconButton(
                onClick = { viewModel.openDownloadDialog() },
                modifier = Modifier.testTag("nav_offline_download_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadForOffline,
                    contentDescription = "Download Website Offline",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
