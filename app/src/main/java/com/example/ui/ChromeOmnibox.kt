package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserTab
import com.example.viewmodel.BrowserViewModel

@Composable
fun ChromeOmnibox(
    viewModel: BrowserViewModel,
    tab: BrowserTab,
    tabCount: Int,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputText by remember(tab.url) { mutableStateOf(if (tab.url == "about:blank") "" else tab.url) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val isSecure = tab.url.startsWith("https://")
    val isOfflineFile = tab.url.startsWith("file://")

    val surfaceColor = if (tab.isIncognito) Color(0xFF1F1F1F) else MaterialTheme.colorScheme.surface
    val omniboxColor = if (tab.isIncognito) Color(0xFF2D2E30) else MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Omnibox pill container
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(omniboxColor)
                    .clickable {
                        isEditing = true
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Security / protocol icon
                IconButton(
                    onClick = { viewModel.setSecurityDialogVisible(true) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("security_info_button")
                ) {
                    Icon(
                        imageVector = when {
                            isOfflineFile -> Icons.Default.FolderOpen
                            isSecure -> Icons.Default.Lock
                            else -> Icons.Default.WarningAmber
                        },
                        contentDescription = "Site Security Info",
                        tint = when {
                            isOfflineFile -> Color(0xFF81C995)
                            isSecure -> if (isDarkTheme) Color(0xFF8AB4F8) else Color(0xFF1A73E8)
                            else -> Color(0xFFF28B82)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Address field
                Box(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (!it.isFocused) isEditing = false
                                }
                                .testTag("omnibox_input"),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                isEditing = false
                                focusManager.clearFocus()
                                viewModel.navigateTo(inputText)
                            })
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    } else {
                        val displayUrl = when {
                            tab.url.startsWith("file://") -> "Offline: ${tab.title.ifBlank { "Saved Website" }}"
                            tab.url == "about:blank" -> "Search or type URL"
                            else -> tab.url.removePrefix("https://").removePrefix("http://")
                        }
                        Text(
                            text = displayUrl,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = if (tab.url == "about:blank") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Clear or Reload/Stop action inside omnibox
                if (isEditing && inputText.isNotBlank()) {
                    IconButton(
                        onClick = { inputText = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (tab.isLoading) viewModel.stopLoading() else viewModel.reload()
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("reload_button")
                    ) {
                        Icon(
                            imageVector = if (tab.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (tab.isLoading) "Stop" else "Reload",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Tabs button with badge count
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { viewModel.setTabsDialogVisible(true) }
                    .testTag("tabs_switcher_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabCount.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Overflow Menu button
            IconButton(
                onClick = { viewModel.setMenuDialogVisible(true) },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("menu_overflow_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Animated Loading Progress Bar
        val animatedProgress by animateFloatAsState(
            targetValue = if (tab.isLoading) tab.progress / 100f else 0f,
            label = "ProgressBarAnimation"
        )
        AnimatedVisibility(visible = tab.isLoading && tab.progress < 100) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }
}
