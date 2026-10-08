package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.devtools.*
import com.example.ui.theme.*
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.DevToolsTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsDrawer(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeDevToolsTab.collectAsState()
    val consoleLogs by viewModel.consoleLogs.collectAsState()
    val consoleFilter by viewModel.consoleFilter.collectAsState()
    val networkRequests by viewModel.networkRequests.collectAsState()
    val selectedElement by viewModel.selectedDomElement.collectAsState()
    val storageEntries by viewModel.storageEntries.collectAsState()
    val performanceMetrics by viewModel.performanceMetrics.collectAsState()
    val pageSource by viewModel.pageSource.collectAsState()
    val currentTabState by viewModel.activeTab.collectAsState()

    ModalBottomSheet(
        onDismissRequest = { viewModel.toggleDevTools() },
        containerColor = DevConsoleBackground,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(36.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = Color.Gray.copy(alpha = 0.5f)
            ) {}
        },
        modifier = modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Header Bar: Title & Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DevConsoleHeader)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "F12 DevTools",
                        tint = DevConsoleBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DevTools (F12)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Inject Eruda floating console
                    AssistChip(
                        onClick = { viewModel.injectErudaWidget() },
                        label = { Text("Eruda Console", fontSize = 11.sp, color = Color.White) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = DevConsoleBorder
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.toggleDevTools() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Tab Selector Scrollable Row
            ScrollableTabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = DevConsoleHeader,
                contentColor = DevConsoleBlue,
                edgePadding = 8.dp,
                divider = {}
            ) {
                DevToolsTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { viewModel.setDevToolsTab(tab) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (activeTab == tab) DevConsoleBlue else Color.Gray
                                )
                                if (tab == DevToolsTab.CONSOLE && (currentTabState?.jsErrorsCount ?: 0) > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(DevConsoleRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentTabState?.jsErrorsCount.toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            HorizontalDivider(color = DevConsoleBorder, thickness = 1.dp)

            // Content Panel according to active tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    DevToolsTab.CONSOLE -> ConsoleTabContent(
                        logs = consoleLogs,
                        filter = consoleFilter,
                        onFilterChange = { viewModel.setConsoleFilter(it) },
                        onClear = { viewModel.clearConsole() },
                        onExecuteJs = { viewModel.executeJsInPage(it) }
                    )
                    DevToolsTab.ELEMENTS -> ElementsTabContent(
                        selectedElement = selectedElement,
                        isInspectorActive = currentTabState?.isInspectorActive == true,
                        onToggleInspector = { viewModel.toggleElementInspector() }
                    )
                    DevToolsTab.NETWORK -> NetworkTabContent(
                        requests = networkRequests,
                        onClear = { viewModel.clearNetwork() }
                    )
                    DevToolsTab.STORAGE -> StorageTabContent(
                        entries = storageEntries,
                        onRefresh = { viewModel.requestStorageInspection() }
                    )
                    DevToolsTab.PERFORMANCE -> PerformanceTabContent(
                        metrics = performanceMetrics,
                        onRefresh = { viewModel.requestPerformanceInspection() }
                    )
                    DevToolsTab.SOURCES -> SourcesTabContent(
                        source = pageSource,
                        onRefresh = { viewModel.requestPageSource() }
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. CONSOLE TAB
// ==========================================
@Composable
private fun ConsoleTabContent(
    logs: List<ConsoleLogEntry>,
    filter: ConsoleLogLevel?,
    onFilterChange: (ConsoleLogLevel?) -> Unit,
    onClear: () -> Unit,
    onExecuteJs: (String) -> Unit
) {
    var jsInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, filter) {
        if (filter == null) logs else logs.filter { it.level == filter }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Console Toolbar (Filter chips & Clear button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DevConsoleHeader.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Clear Console",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Filter Chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChipItem(
                    label = "All (${logs.size})",
                    selected = filter == null,
                    onClick = { onFilterChange(null) }
                )
                FilterChipItem(
                    label = "Errors (${logs.count { it.level == ConsoleLogLevel.ERROR }})",
                    selected = filter == ConsoleLogLevel.ERROR,
                    activeColor = DevConsoleRed,
                    onClick = { onFilterChange(ConsoleLogLevel.ERROR) }
                )
                FilterChipItem(
                    label = "Warnings (${logs.count { it.level == ConsoleLogLevel.WARN }})",
                    selected = filter == ConsoleLogLevel.WARN,
                    activeColor = DevConsoleYellow,
                    onClick = { onFilterChange(ConsoleLogLevel.WARN) }
                )
                FilterChipItem(
                    label = "Info",
                    selected = filter == ConsoleLogLevel.INFO,
                    activeColor = DevConsoleBlue,
                    onClick = { onFilterChange(ConsoleLogLevel.INFO) }
                )
            }
        }

        HorizontalDivider(color = DevConsoleBorder, thickness = 0.5.dp)

        // Logs Output List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Console is empty.\nExecute JavaScript or trigger actions to view logs.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    ConsoleLogItem(log)
                    HorizontalDivider(color = DevConsoleBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }

        // Quick JS Snippet Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DevConsoleHeader.copy(alpha = 0.8f))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val snippets = listOf(
                "document.title",
                "location.href",
                "document.cookie",
                "localStorage.length",
                "navigator.userAgent"
            )
            snippets.forEach { snippet ->
                Text(
                    text = snippet,
                    color = DevConsoleBlue,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DevConsoleBorder)
                        .clickable { onExecuteJs(snippet) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        // REPL Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DevConsoleHeader)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">",
                color = DevConsoleBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(6.dp))
            TextField(
                value = jsInput,
                onValueChange = { jsInput = it },
                placeholder = {
                    Text(
                        "Evaluate JavaScript (e.g. alert('test'))",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("console_js_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (jsInput.isNotBlank()) {
                        onExecuteJs(jsInput)
                        jsInput = ""
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("console_run_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run",
                    tint = DevConsoleGreen
                )
            }
        }
    }
}

@Composable
private fun ConsoleLogItem(log: ConsoleLogEntry) {
    val (bgColor, textColor, icon) = when (log.level) {
        ConsoleLogLevel.ERROR -> Triple(DevConsoleRed.copy(alpha = 0.15f), DevConsoleRed, Icons.Default.ErrorOutline)
        ConsoleLogLevel.WARN -> Triple(DevConsoleYellow.copy(alpha = 0.15f), DevConsoleYellow, Icons.Default.WarningAmber)
        ConsoleLogLevel.INFO -> Triple(DevConsoleBlue.copy(alpha = 0.1f), DevConsoleBlue, Icons.Default.Info)
        ConsoleLogLevel.RESULT -> Triple(DevConsoleGreen.copy(alpha = 0.1f), DevConsoleGreen, Icons.Default.Check)
        else -> Triple(Color.Transparent, Color(0xFFE0E0E0), null)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = log.level.name,
                tint = textColor,
                modifier = Modifier
                    .size(14.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        SelectionContainer(modifier = Modifier.weight(1f)) {
            Column {
                Text(
                    text = log.message,
                    color = textColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!log.stack.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = log.stack,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    activeColor: Color = DevConsoleBlue,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) activeColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) activeColor else DevConsoleBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = if (selected) activeColor else Color.Gray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ==========================================
// 2. ELEMENTS TAB (DOM INSPECTOR)
// ==========================================
@Composable
private fun ElementsTabContent(
    selectedElement: DomSelectedElement?,
    isInspectorActive: Boolean,
    onToggleInspector: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Toggle Element Touch Inspector Button
        Button(
            onClick = onToggleInspector,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isInspectorActive) DevConsoleBlue else DevConsoleHeader
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = "Inspect",
                tint = if (isInspectorActive) Color.Black else DevConsoleBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isInspectorActive) "Inspecting Active (Touch element on webpage)" else "Inspect Element (Touch on Webpage)",
                color = if (isInspectorActive) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedElement == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap 'Inspect Element' above and touch any button, image, or text on the page to view its HTML & CSS properties.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                item {
                    // Tag and ID badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DevConsoleHeader)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "<${selectedElement.tagName}${if (selectedElement.id.isNotBlank()) " #${selectedElement.id}" else ""}${if (selectedElement.className.isNotBlank()) " .${selectedElement.className.replace(" ", " .")}" else ""}>",
                            color = DevConsoleBlue,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(selectedElement.outerHtml)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy HTML",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Computed CSS Styles Card
                item {
                    Text(
                        text = "Computed CSS Styles",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DevConsoleHeader),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            selectedElement.computedStyles.forEach { (prop, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = prop,
                                        color = DevConsoleYellow,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = value,
                                        color = Color(0xFFD4D4D4),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Attributes Card
                if (selectedElement.attributes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Attributes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DevConsoleHeader),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                selectedElement.attributes.forEach { (k, v) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = k,
                                            color = DevConsoleGreen,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = v,
                                            color = Color(0xFFD4D4D4),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Outer HTML View
                item {
                    Text(
                        text = "Outer HTML",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DevConsoleHeader)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = selectedElement.outerHtml,
                                color = Color(0xFFCE9178),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. NETWORK TAB
// ==========================================
@Composable
private fun NetworkTabContent(
    requests: List<NetworkRequestEntry>,
    onClear: () -> Unit
) {
    var selectedRequest by remember { mutableStateOf<NetworkRequestEntry?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DevConsoleHeader.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Clear Network",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${requests.size} Requests",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        HorizontalDivider(color = DevConsoleBorder, thickness = 0.5.dp)

        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No network requests captured yet.\nBrowse websites or trigger API calls to inspect requests.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(requests.reversed(), key = { it.id }) { req ->
                    val statusColor = when {
                        req.status in 200..299 -> DevConsoleGreen
                        req.status in 300..399 -> DevConsoleYellow
                        req.status >= 400 || req.status == 0 -> DevConsoleRed
                        else -> Color.Gray
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRequest = req }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (req.status == 0) "FAIL" else req.status.toString(),
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Method Badge
                        Text(
                            text = req.method,
                            color = DevConsoleBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // URL path
                        Text(
                            text = req.url,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Duration
                        Text(
                            text = "${req.durationMs}ms",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    HorizontalDivider(color = DevConsoleBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }

    // Network Request Detail Dialog
    selectedRequest?.let { req ->
        AlertDialog(
            onDismissRequest = { selectedRequest = null },
            title = {
                Text(
                    text = "${req.method} ${req.status}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            text = {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        Text("URL:", fontWeight = FontWeight.Bold, color = DevConsoleBlue, fontSize = 12.sp)
                        Text(req.url, fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Duration:", fontWeight = FontWeight.Bold, color = DevConsoleBlue, fontSize = 12.sp)
                        Text("${req.durationMs} ms", fontSize = 11.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (!req.responseBody.isNullOrBlank()) {
                            Text("Response Preview:", fontWeight = FontWeight.Bold, color = DevConsoleGreen, fontSize = 12.sp)
                            Text(
                                text = req.responseBody,
                                fontSize = 10.sp,
                                color = Color(0xFFD4D4D4),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRequest = null }) {
                    Text("Close")
                }
            },
            containerColor = DevConsoleHeader
        )
    }
}

// ==========================================
// 4. STORAGE TAB
// ==========================================
@Composable
private fun StorageTabContent(
    entries: List<StorageEntry>,
    onRefresh: () -> Unit
) {
    var selectedType by remember { mutableStateOf(StorageType.LOCAL_STORAGE) }
    val filtered = entries.filter { it.storageType == selectedType }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // Storage Type Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChipItem(
                    label = "LocalStorage (${entries.count { it.storageType == StorageType.LOCAL_STORAGE }})",
                    selected = selectedType == StorageType.LOCAL_STORAGE,
                    onClick = { selectedType = StorageType.LOCAL_STORAGE }
                )
                FilterChipItem(
                    label = "SessionStorage (${entries.count { it.storageType == StorageType.SESSION_STORAGE }})",
                    selected = selectedType == StorageType.SESSION_STORAGE,
                    onClick = { selectedType = StorageType.SESSION_STORAGE }
                )
                FilterChipItem(
                    label = "Cookies (${entries.count { it.storageType == StorageType.COOKIE }})",
                    selected = selectedType == StorageType.COOKIE,
                    onClick = { selectedType = StorageType.COOKIE }
                )
            }

            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = DevConsoleBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No ${selectedType.name.lowercase().replace('_', ' ')} keys found on this page.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DevConsoleHeader),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        SelectionContainer {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = item.key,
                                    color = DevConsoleYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.value,
                                    color = Color(0xFFD4D4D4),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. PERFORMANCE & AUDIT TAB
// ==========================================
@Composable
private fun PerformanceTabContent(
    metrics: PerformanceMetrics?,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Performance & Optimization Audit",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = DevConsoleBlue)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (metrics == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = DevConsoleBlue)
                ) {
                    Text("Analyze Current Page Performance")
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                // Score card
                item {
                    val scoreColor = when {
                        metrics.lighthouseScore >= 80 -> DevConsoleGreen
                        metrics.lighthouseScore >= 50 -> DevConsoleYellow
                        else -> DevConsoleRed
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DevConsoleHeader),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Lighthouse Performance Score", color = Color.White, fontSize = 13.sp)
                                Text("Navigation Timing API", color = Color.Gray, fontSize = 11.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(scoreColor.copy(alpha = 0.2f))
                                    .border(2.dp, scoreColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${metrics.lighthouseScore}",
                                    color = scoreColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Timing Breakdown Grid
                item {
                    Text("Timing Waterfall", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DevConsoleHeader),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            TimingRow("DNS Lookup", "${metrics.dnsLookupMs} ms")
                            TimingRow("TCP Connection", "${metrics.tcpConnectMs} ms")
                            TimingRow("TTFB (Time to First Byte)", "${metrics.ttfbMs} ms")
                            TimingRow("DOMContentLoaded", "${metrics.domContentLoadedMs} ms")
                            TimingRow("Total Page Load", "${metrics.fullLoadMs} ms")
                            TimingRow("Total HTTP Resources", "${metrics.totalResources} assets")
                            TimingRow("DOM Elements Count", "${metrics.domNodeCount} nodes")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Optimization Suggestions
                item {
                    Text("Optimization Tips", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        metrics.suggestions.forEach { tip ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DevConsoleHeader)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Tip",
                                    tint = DevConsoleYellow,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tip,
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// 6. SOURCES TAB
// ==========================================
@Composable
private fun SourcesTabContent(
    source: String?,
    onRefresh: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page Source HTML",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Row {
                if (!source.isNullOrBlank()) {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(source)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = DevConsoleBlue, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (source.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = DevConsoleBlue)) {
                    Text("Load HTML Page Source")
                }
            }
        } else {
            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DevConsoleHeader)
                    .padding(10.dp)
            ) {
                LazyColumn {
                    val lines = source.lines()
                    items(lines.size) { index ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${index + 1}".padStart(4, ' '),
                                color = Color.DarkGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lines[index],
                                color = Color(0xFFD4D4D4),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
