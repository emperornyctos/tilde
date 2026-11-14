package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.IconPackManager
import com.example.model.AppInfo
import com.example.model.DrawerLayoutMode
import com.example.model.DrawerSortOrder
import com.example.model.LauncherSettings
import com.example.ui.components.AppIconView
import com.example.ui.components.TuiCard
import com.example.ui.components.TuiCliLink
import com.example.ui.components.TuiSectionHeader
import com.example.ui.theme.LocalLauncherTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppDrawerSheet(
    settings: LauncherSettings,
    allApps: List<AppInfo>,
    searchQuery: String,
    iconPackManager: IconPackManager,
    onLaunchApp: (AppInfo) -> Unit,
    onAddAppToHome: (AppInfo) -> Unit,
    onTogglePinDock: (AppInfo) -> Unit,
    onEditApp: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onUninstallApp: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSettingsSection: (String) -> Unit,
    onOpenWidgets: () -> Unit,
    onCycleLayoutMode: () -> Unit,
    onCycleSortOrder: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onCloseDrawer() }

    val tokens = LocalLauncherTokens.current
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }

    val filteredApps = remember(allApps, searchQuery, settings.drawerSortOrder) {
        val visible = allApps.filter { !it.isHidden }
        val searched = if (searchQuery.isBlank()) {
            visible
        } else {
            val q = searchQuery.trim().lowercase()
            visible.filter {
                it.displayLabel.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
        when (settings.drawerSortOrder) {
            DrawerSortOrder.ALPHABETICAL -> searched.sortedBy { it.displayLabel.lowercase() }
            DrawerSortOrder.MOST_USED -> searched.sortedWith(
                compareByDescending<AppInfo> { it.launchCount }.thenBy { it.displayLabel.lowercase() }
            )
            DrawerSortOrder.RECENTLY_INSTALLED -> searched.sortedWith(
                compareByDescending<AppInfo> { it.installTime }.thenBy { it.displayLabel.lowercase() }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background.copy(alpha = 0.96f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("app_drawer_container")
    ) {
        // Drawer Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                TuiSectionHeader(text = "~/app drawer")
                Text(
                    text = "${filteredApps.size} installed · ${settings.drawerSortOrder.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textMuted
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TuiCliLink(label = settings.drawerLayoutMode.label.lowercase(), onClick = onCycleLayoutMode)
                TuiCliLink(label = settings.drawerSortOrder.label.lowercase(), onClick = onCycleSortOrder)
                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close App Drawer",
                        tint = tokens.textPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Settings Menu Card inside App Drawer (explicitly requested!)
        if (settings.showSettingsInDrawer) {
            TuiCard(
                backgroundColor = tokens.surface,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings)
                    .testTag("drawer_settings_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .background(tokens.pillBg)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Launcher Settings",
                                    tint = tokens.pillText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Launcher Settings & Studio",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = "Colors · Wallpaper · Icon Packs · Fonts · Grid",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.textMuted
                                )
                            }
                        }
                        TuiCliLink(label = "open", onClick = onOpenSettings)
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DrawerQuickPill(
                            icon = Icons.Default.Palette,
                            label = "themes & colors",
                            onClick = { onOpenSettingsSection("theme") }
                        )
                        DrawerQuickPill(
                            icon = Icons.Default.Tune,
                            label = "mono fonts",
                            onClick = { onOpenSettingsSection("fonts") }
                        )
                        DrawerQuickPill(
                            icon = Icons.Default.Settings,
                            label = "icon packs",
                            onClick = { onOpenSettingsSection("icons") }
                        )
                        DrawerQuickPill(
                            icon = Icons.Default.Widgets,
                            label = "widgets",
                            onClick = onOpenWidgets
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // Apps Content
        when (settings.drawerLayoutMode) {
            DrawerLayoutMode.GRID -> {
                val cols = settings.drawerColumns.coerceIn(3, 6)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (filteredApps.isEmpty()) {
                        item(span = { GridItemSpan(cols) }) {
                            EmptyDrawerMessage(searchQuery)
                        }
                    } else {
                        items(filteredApps, key = { it.componentKey }) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .combinedClickable(
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { selectedAppForMenu = app }
                                    )
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                            ) {
                                AppIconView(
                                    app = app,
                                    settings = settings,
                                    iconPackManager = iconPackManager
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = app.displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.textPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = settings.maxLabelLines.coerceIn(1, 2),
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            DrawerLayoutMode.LIST -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (filteredApps.isEmpty()) {
                        item { EmptyDrawerMessage(searchQuery) }
                    } else {
                        items(filteredApps, key = { it.componentKey }) { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .background(tokens.surface.copy(alpha = 0.65f))
                                    .border(
                                        0.75.dp,
                                        tokens.border,
                                        RoundedCornerShape(tokens.cardCornerRadius)
                                    )
                                    .combinedClickable(
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { selectedAppForMenu = app }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                AppIconView(
                                    app = app,
                                    settings = settings,
                                    iconPackManager = iconPackManager,
                                    sizeOverride = 40.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.displayLabel,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = tokens.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (app.launchCount > 0) "${app.launchCount} launches · ${app.packageName}"
                                        else app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TuiCliLink(label = "opt", onClick = { selectedAppForMenu = app })
                            }
                        }
                    }
                }
            }

            DrawerLayoutMode.COMPACT_INDEX -> {
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                val letters = remember(filteredApps) {
                    filteredApps.map {
                        it.displayLabel.firstOrNull()?.uppercaseChar()?.let { ch ->
                            if (ch in 'A'..'Z') ch else '#'
                        } ?: '#'
                    }.distinct()
                }

                Row(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (filteredApps.isEmpty()) {
                            item { EmptyDrawerMessage(searchQuery) }
                        } else {
                            items(filteredApps, key = { it.componentKey }) { app ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                        .combinedClickable(
                                            onClick = { onLaunchApp(app) },
                                            onLongClick = { selectedAppForMenu = app }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    AppIconView(
                                        app = app,
                                        settings = settings,
                                        iconPackManager = iconPackManager,
                                        sizeOverride = 32.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = app.displayLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // A-Z Quick Jump Rail
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(28.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        letters.forEach { ch ->
                            Text(
                                text = ch.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = tokens.accent,
                                modifier = Modifier
                                    .clickable {
                                        val idx = filteredApps.indexOfFirst {
                                            val first = it.displayLabel.firstOrNull()?.uppercaseChar() ?: '#'
                                            val normalized = if (first in 'A'..'Z') first else '#'
                                            normalized == ch
                                        }
                                        if (idx >= 0) {
                                            scope.launch { listState.animateScrollToItem(idx) }
                                        }
                                    }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Long-press app options dialog
    selectedAppForMenu?.let { app ->
        val isPinned = settings.pinnedDockPackages.contains(app.packageName)
        Dialog(onDismissRequest = { selectedAppForMenu = null }) {
            TuiCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIconView(app, settings, iconPackManager, sizeOverride = 42.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = app.displayLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.textPrimary
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    DrawerActionRow("Launch app") {
                        selectedAppForMenu = null
                        onLaunchApp(app)
                    }
                    DrawerActionRow("Add to home screen") {
                        selectedAppForMenu = null
                        onAddAppToHome(app)
                    }
                    DrawerActionRow(if (isPinned) "Unpin from bottom dock" else "Pin to bottom dock") {
                        selectedAppForMenu = null
                        onTogglePinDock(app)
                    }
                    DrawerActionRow("Edit icon & custom label") {
                        selectedAppForMenu = null
                        onEditApp(app)
                    }
                    DrawerActionRow("Hide from app drawer") {
                        selectedAppForMenu = null
                        onHideApp(app)
                    }
                    DrawerActionRow("App info") {
                        selectedAppForMenu = null
                        onAppInfo(app)
                    }
                    DrawerActionRow("Uninstall") {
                        selectedAppForMenu = null
                        onUninstallApp(app)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerQuickPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val tokens = LocalLauncherTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(tokens.cardCornerRadius))
            .background(tokens.background.copy(alpha = 0.7f))
            .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tokens.accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tokens.textPrimary)
    }
}

@Composable
private fun DrawerActionRow(label: String, onClick: () -> Unit) {
    val tokens = LocalLauncherTokens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(tokens.cardCornerRadius))
            .background(tokens.background.copy(alpha = 0.6f))
            .border(0.75.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = "> $label", style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
    }
}

@Composable
private fun EmptyDrawerMessage(query: String) {
    val tokens = LocalLauncherTokens.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Text(
            text = if (query.isNotBlank()) "No apps match \"$query\"" else "No visible apps",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textMuted
        )
    }
}
