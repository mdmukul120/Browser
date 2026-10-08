package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserTab
import com.example.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsSwitcherDialog(
    viewModel: BrowserViewModel,
    tabs: List<BrowserTab>,
    activeTabId: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tabs.size} Tabs Open",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // New Tab Button
                    FilledTonalButton(
                        onClick = { viewModel.createTab() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("new_tab_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Tab", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Tab")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tabs, key = { it.id }) { tab ->
                    val isActive = tab.id == activeTabId
                    TabCardItem(
                        tab = tab,
                        isActive = isActive,
                        onSelect = { viewModel.selectTab(tab.id) },
                        onClose = { viewModel.closeTab(tab.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Incognito Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = { viewModel.createTab(isIncognito = true) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("new_incognito_tab_button")
                ) {
                    Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = "Incognito", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Incognito Tab")
                }
            }
        }
    }
}

@Composable
private fun TabCardItem(
    tab: BrowserTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val cardBg = if (tab.isIncognito) Color(0xFF232428) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Header with Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (tab.isIncognito) Color(0xFF1B1B1E) else MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tab.isIncognito) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Incognito",
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = tab.title.ifBlank { "New Tab" },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tab",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Tab Thumbnail / Content Preview Placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (tab.url.startsWith("file://")) Icons.Default.FolderOpen else Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.url.removePrefix("https://").removePrefix("http://"),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
