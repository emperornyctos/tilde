package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.IconPackManager
import com.example.model.AppInfo
import com.example.model.BuiltInWidgetType
import com.example.model.HomeGridItem
import com.example.model.HomeItemType
import com.example.model.IconPackInfo
import com.example.model.LauncherSettings
import com.example.ui.components.AppIconView
import com.example.ui.components.TuiCard
import com.example.ui.components.TuiCliLink
import com.example.ui.components.TuiSectionHeader
import com.example.ui.components.toImageBitmap
import com.example.ui.theme.LocalLauncherTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditDialog(
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onPickAvatar: () -> Unit,
    onPickBanner: () -> Unit,
    onSave: (name: String, pronouns: String, subtitle: String, bio: String, quote: String) -> Unit
) {
    val tokens = LocalLauncherTokens.current
    var name by remember { mutableStateOf(settings.profileDisplayName) }
    var pronouns by remember { mutableStateOf(settings.profilePronouns) }
    var subtitle by remember { mutableStateOf(settings.profileSubtitle) }
    var bio by remember { mutableStateOf(settings.profileBio) }
    var quote by remember { mutableStateOf(settings.profileWelcomeQuote) }

    Dialog(onDismissRequest = onDismiss) {
        TuiCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TuiSectionHeader(text = "Edit profile")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TuiCliLink(label = "pick avatar", onClick = onPickAvatar)
                    TuiCliLink(label = "pick banner", onClick = onPickBanner)
                }
                TuiTextField(value = name, onValueChange = { name = it }, label = "Display Name")
                TuiTextField(value = pronouns, onValueChange = { pronouns = it }, label = "Pronouns / Tag")
                TuiTextField(value = subtitle, onValueChange = { subtitle = it }, label = "Subtitle")
                TuiTextField(value = bio, onValueChange = { bio = it }, label = "Bio", singleLine = false)
                TuiTextField(value = quote, onValueChange = { quote = it }, label = "Welcome Banner Quote")
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = tokens.textMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(name, pronouns, subtitle, bio, quote)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.pillBg,
                            contentColor = tokens.pillText
                        ),
                        shape = RoundedCornerShape(tokens.cardCornerRadius)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeQuoteDialog(
    initialQuote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val tokens = LocalLauncherTokens.current
    var quote by remember { mutableStateOf(initialQuote) }
    Dialog(onDismissRequest = onDismiss) {
        TuiCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TuiSectionHeader(text = "Welcome quote")
                TuiTextField(value = quote, onValueChange = { quote = it }, label = "Status Message", singleLine = false)
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = tokens.textMuted) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(quote); onDismiss() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.pillBg,
                            contentColor = tokens.pillText
                        ),
                        shape = RoundedCornerShape(tokens.cardCornerRadius)
                    ) { Text("Update") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppEditDialog(
    app: AppInfo,
    settings: LauncherSettings,
    iconPacks: List<IconPackInfo>,
    iconPackManager: IconPackManager,
    onPickCustomImage: () -> Unit,
    onSave: (
        newLabel: String?,
        customIconUri: String?,
        packPkg: String?,
        packDrawable: String?,
        clearIcon: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLauncherTokens.current
    var labelText by remember(app.componentKey) { mutableStateOf(app.displayLabel) }
    var selectedCustomIconUri by remember(app.componentKey, app.customIconUri) {
        mutableStateOf(app.customIconUri)
    }
    var selectedPackPkg by remember(app.componentKey, app.customPackPackage) {
        mutableStateOf(app.customPackPackage)
    }
    var selectedPackDrawable by remember(app.componentKey, app.customPackDrawable) {
        mutableStateOf(app.customPackDrawable)
    }
    var clearIcon by remember(app.componentKey) { mutableStateOf(false) }

    val externalPacks = remember(iconPacks) { iconPacks.filter { !it.isBuiltIn && it.packageName != null } }
    var activePackForBrowse by remember(externalPacks, app.customPackPackage) {
        mutableStateOf(app.customPackPackage ?: externalPacks.firstOrNull()?.packageName)
    }
    var packDrawables by remember { mutableStateOf<List<String>>(emptyList()) }
    var glyphFilter by remember { mutableStateOf("") }

    LaunchedEffect(activePackForBrowse) {
        val pkg = activePackForBrowse
        packDrawables = if (pkg != null) {
            iconPackManager.getSampleDrawableNames(pkg, limit = 500)
        } else {
            emptyList()
        }
    }

    val filteredDrawables = remember(packDrawables, glyphFilter) {
        val q = glyphFilter.trim().lowercase()
        if (q.isEmpty()) packDrawables.take(150)
        else packDrawables.filter { it.lowercase().contains(q) }.take(150)
    }

    Dialog(onDismissRequest = onDismiss) {
        TuiCard(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.heightIn(max = 680.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                TuiSectionHeader(text = "Customize app shortcut")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIconView(
                        app = app.copy(
                            customIconUri = if (clearIcon) null else selectedCustomIconUri,
                            customPackPackage = if (clearIcon) null else selectedPackPkg,
                            customPackDrawable = if (clearIcon) null else selectedPackDrawable
                        ),
                        settings = settings,
                        iconPackManager = iconPackManager,
                        sizeOverride = 52.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = when {
                                clearIcon -> "Default icon"
                                !selectedCustomIconUri.isNullOrBlank() -> "Custom cropped photo"
                                !selectedPackDrawable.isNullOrBlank() -> "Pack: $selectedPackDrawable"
                                else -> app.packageName
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TuiTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = "Custom Label"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TuiCliLink(label = "pick & crop image", onClick = onPickCustomImage)
                    TuiCliLink(
                        label = "reset icon",
                        onClick = {
                            clearIcon = true
                            selectedCustomIconUri = null
                            selectedPackPkg = null
                            selectedPackDrawable = null
                        }
                    )
                }

                if (externalPacks.isNotEmpty()) {
                    TuiSectionHeader(text = "Icon pack glyphs (${packDrawables.size})")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        externalPacks.forEach { pack ->
                            TuiCliLink(
                                label = pack.label,
                                color = if (activePackForBrowse == pack.packageName) tokens.accent else tokens.textMuted,
                                onClick = { activePackForBrowse = pack.packageName }
                            )
                        }
                    }
                    TuiTextField(
                        value = glyphFilter,
                        onValueChange = { glyphFilter = it },
                        label = "Search pack icons (e.g. browser, music...)"
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        items(filteredDrawables) { drawableName ->
                            val pkg = activePackForBrowse ?: return@items
                            val drawable by produceState<Drawable?>(null, pkg, drawableName) {
                                value = iconPackManager.loadNamedDrawableFromPack(pkg, drawableName)
                            }
                            val isSelected = selectedPackPkg == pkg &&
                                selectedPackDrawable == drawableName &&
                                selectedCustomIconUri.isNullOrBlank() &&
                                !clearIcon
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) tokens.accent else tokens.border,
                                        shape = RoundedCornerShape(tokens.cardCornerRadius)
                                    )
                                    .clickable {
                                        clearIcon = false
                                        selectedCustomIconUri = null
                                        selectedPackPkg = pkg
                                        selectedPackDrawable = drawableName
                                    }
                                    .padding(4.dp)
                            ) {
                                val bmp = remember(drawable) { drawable?.toImageBitmap() }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = drawableName,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Tip: Pick & crop any image above, or install any Android icon pack APK to choose custom glyphs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = tokens.textMuted) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                labelText,
                                if (clearIcon) null else selectedCustomIconUri,
                                if (clearIcon) null else selectedPackPkg,
                                if (clearIcon) null else selectedPackDrawable,
                                clearIcon
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.pillBg,
                            contentColor = tokens.pillText
                        ),
                        shape = RoundedCornerShape(tokens.cardCornerRadius)
                    ) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
fun FolderModalDialog(
    folderItem: HomeGridItem,
    allApps: List<AppInfo>,
    settings: LauncherSettings,
    iconPackManager: IconPackManager,
    onLaunchApp: (AppInfo) -> Unit,
    onSaveFolder: (newTitle: String, newKeys: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLauncherTokens.current
    var isEditing by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(folderItem.folderTitle ?: "~/folder") }
    var selectedKeys by remember { mutableStateOf(folderItem.folderAppKeys.toSet()) }

    val folderApps = remember(folderItem.folderAppKeys, allApps) {
        val map = allApps.associateBy { it.componentKey }
        folderItem.folderAppKeys.mapNotNull { map[it] }
    }

    Dialog(onDismissRequest = onDismiss) {
        TuiCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.accent
                    )
                    TuiCliLink(
                        label = if (isEditing) "done" else "edit",
                        onClick = {
                            if (isEditing) {
                                onSaveFolder(title, selectedKeys.toList())
                            }
                            isEditing = !isEditing
                        }
                    )
                }

                if (!isEditing) {
                    if (folderApps.isEmpty()) {
                        Text(
                            text = "Empty folder · tap [edit] to add apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 320.dp)
                        ) {
                            items(folderApps, key = { it.componentKey }) { app ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                        .clickable {
                                            onLaunchApp(app)
                                            onDismiss()
                                        }
                                        .padding(6.dp)
                                ) {
                                    AppIconView(
                                        app = app,
                                        settings = settings,
                                        iconPackManager = iconPackManager,
                                        sizeOverride = 44.dp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = app.displayLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    TuiTextField(value = title, onValueChange = { title = it }, label = "Folder Name")
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(allApps.filter { !it.isHidden }, key = { it.componentKey }) { app ->
                            val checked = selectedKeys.contains(app.componentKey)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedKeys = if (checked) {
                                            selectedKeys - app.componentKey
                                        } else {
                                            selectedKeys + app.componentKey
                                        }
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                AppIconView(app, settings, iconPackManager, sizeOverride = 30.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = app.displayLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (checked) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = tokens.accent)
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
fun AddToCellDialog(
    page: Int,
    col: Int,
    row: Int,
    allApps: List<AppInfo>,
    settings: LauncherSettings,
    iconPackManager: IconPackManager,
    onAddApp: (AppInfo) -> Unit,
    onAddFolder: (String, List<AppInfo>) -> Unit,
    onAddBuiltInWidget: (BuiltInWidgetType) -> Unit,
    onOpenSystemWidgets: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLauncherTokens.current
    var tab by remember { mutableIntStateOf(0) } // 0 = Apps, 1 = Widgets, 2 = Folder
    var folderTitle by remember { mutableStateOf("~/tools") }
    var folderSelection by remember { mutableStateOf<Set<String>>(emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        TuiCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TuiSectionHeader(text = "Add to cell (${col + 1},${row + 1}) · page ${page + 1}")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TuiCliLink(
                        label = "app",
                        color = if (tab == 0) tokens.accent else tokens.textMuted,
                        onClick = { tab = 0 }
                    )
                    TuiCliLink(
                        label = "widget",
                        color = if (tab == 1) tokens.accent else tokens.textMuted,
                        onClick = { tab = 1 }
                    )
                    TuiCliLink(
                        label = "folder",
                        color = if (tab == 2) tokens.accent else tokens.textMuted,
                        onClick = { tab = 2 }
                    )
                }

                when (tab) {
                    0 -> {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(allApps.filter { !it.isHidden }, key = { it.componentKey }) { app ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAddApp(app); onDismiss() }
                                        .padding(vertical = 6.dp)
                                ) {
                                    AppIconView(app, settings, iconPackManager, sizeOverride = 34.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = app.displayLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.textPrimary
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 320.dp)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                        .background(tokens.pillBg)
                                        .clickable { onDismiss(); onOpenSystemWidgets() }
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "+ Browse Android System Widgets...",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = tokens.pillText
                                    )
                                }
                            }
                            items(BuiltInWidgetType.entries) { widget ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                                        .clickable { onAddBuiltInWidget(widget); onDismiss() }
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "${widget.title} (${widget.defaultColSpan}x${widget.defaultRowSpan})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = tokens.accent
                                    )
                                    Text(
                                        text = widget.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textMuted
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        TuiTextField(value = folderTitle, onValueChange = { folderTitle = it }, label = "Folder Name")
                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                            items(allApps.filter { !it.isHidden }, key = { it.componentKey }) { app ->
                                val selected = folderSelection.contains(app.componentKey)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            folderSelection = if (selected) folderSelection - app.componentKey else folderSelection + app.componentKey
                                        }
                                        .padding(vertical = 5.dp)
                                ) {
                                    AppIconView(app, settings, iconPackManager, sizeOverride = 28.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(app.displayLabel, style = MaterialTheme.typography.bodySmall, color = tokens.textPrimary, modifier = Modifier.weight(1f))
                                    if (selected) Icon(Icons.Default.Check, null, tint = tokens.accent)
                                }
                            }
                        }
                        Button(
                            onClick = {
                                val selectedApps = allApps.filter { folderSelection.contains(it.componentKey) }
                                onAddFolder(folderTitle, selectedApps)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.pillBg, contentColor = tokens.pillText),
                            shape = RoundedCornerShape(tokens.cardCornerRadius),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Create Folder (${folderSelection.size})") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemContextDialog(
    item: HomeGridItem,
    app: AppInfo?,
    isPinnedToDock: Boolean,
    gridCols: Int,
    gridRows: Int,
    onMove: (newPage: Int, newCol: Int, newRow: Int) -> Unit,
    onResize: (dCol: Int, dRow: Int) -> Unit,
    onEditApp: () -> Unit,
    onPickWidgetPhoto: () -> Unit,
    onTogglePhotoWidgetBorder: () -> Unit,
    onTogglePinDock: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLauncherTokens.current
    Dialog(onDismissRequest = onDismiss) {
        TuiCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val title = when (item.type) {
                    HomeItemType.APP -> app?.displayLabel ?: item.packageName ?: "App"
                    HomeItemType.FOLDER -> item.folderTitle ?: "Folder"
                    HomeItemType.BUILT_IN_WIDGET -> item.builtInWidgetType?.title ?: "Widget"
                    HomeItemType.SYSTEM_WIDGET -> "System Widget"
                }
                TuiSectionHeader(text = "Item · $title")
                Text(
                    text = "Page ${item.page + 1} · Col ${item.col + 1}, Row ${item.row + 1} · Size ${item.colSpan}x${item.rowSpan}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textMuted
                )

                // Move controls
                TuiSectionHeader(text = "Position & page")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TuiCliLink(label = "←", onClick = { onMove(item.page, item.col - 1, item.row) })
                    TuiCliLink(label = "→", onClick = { onMove(item.page, item.col + 1, item.row) })
                    TuiCliLink(label = "↑", onClick = { onMove(item.page, item.col, item.row - 1) })
                    TuiCliLink(label = "↓", onClick = { onMove(item.page, item.col, item.row + 1) })
                    if (item.page > 0) {
                        TuiCliLink(label = "prev page", onClick = { onMove(item.page - 1, item.col, item.row) })
                    }
                    TuiCliLink(label = "next page", onClick = { onMove(item.page + 1, item.col, item.row) })
                }

                // Resize controls for widgets
                if (item.type == HomeItemType.BUILT_IN_WIDGET || item.type == HomeItemType.SYSTEM_WIDGET) {
                    TuiSectionHeader(text = "Resize (${item.colSpan}/${gridCols} × ${item.rowSpan}/${gridRows})")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TuiCliLink(label = "-width", onClick = { onResize(-1, 0) })
                        TuiCliLink(label = "+width", onClick = { onResize(1, 0) })
                        TuiCliLink(label = "-height", onClick = { onResize(0, -1) })
                        TuiCliLink(label = "+height", onClick = { onResize(0, 1) })
                    }
                }

                // Photo Frame widget actions
                if (item.type == HomeItemType.BUILT_IN_WIDGET &&
                    (item.builtInWidgetType == BuiltInWidgetType.PHOTO_FRAME ||
                        item.builtInWidgetType == BuiltInWidgetType.PHOTO_FRAME_WIDE)
                ) {
                    TuiSectionHeader(text = "Photo / GIF frame actions")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TuiCliLink(label = "pick photo / GIF", onClick = { onDismiss(); onPickWidgetPhoto() })
                        TuiCliLink(label = "toggle border", onClick = { onTogglePhotoWidgetBorder(); onDismiss() })
                    }
                }

                // App-specific actions
                if (item.type == HomeItemType.APP && app != null) {
                    TuiSectionHeader(text = "App actions")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TuiCliLink(label = "edit icon/label", onClick = { onDismiss(); onEditApp() })
                        TuiCliLink(
                            label = if (isPinnedToDock) "unpin dock" else "pin to dock",
                            onClick = { onTogglePinDock(); onDismiss() }
                        )
                        TuiCliLink(label = "app info", onClick = { onAppInfo(); onDismiss() })
                        TuiCliLink(label = "uninstall", onClick = { onUninstall(); onDismiss() })
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TuiCliLink(
                        label = "remove from home",
                        color = MaterialTheme.colorScheme.error,
                        onClick = { onRemove(); onDismiss() }
                    )
                    TuiCliLink(label = "close", onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
fun TuiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = tokens.textPrimary,
            unfocusedTextColor = tokens.textPrimary,
            focusedBorderColor = tokens.accent,
            unfocusedBorderColor = tokens.border,
            focusedLabelColor = tokens.accent,
            unfocusedLabelColor = tokens.textMuted,
            cursorColor = tokens.accent
        ),
        shape = RoundedCornerShape(tokens.cardCornerRadius),
        modifier = modifier.fillMaxWidth()
    )
}
