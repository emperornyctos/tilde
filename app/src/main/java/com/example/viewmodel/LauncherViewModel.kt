package com.example.viewmodel

import android.app.Application
import android.app.WallpaperManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.AppSearchIndex
import com.example.data.HomeRepository
import com.example.data.IconPackManager
import com.example.data.LauncherDatabase
import com.example.data.MediaPlaybackState
import com.example.data.MediaSessionMonitor
import com.example.data.QuickNoteEntity
import com.example.data.SettingsDataStore
import com.example.data.WidgetHostManager
import org.json.JSONObject
import com.example.model.AppInfo
import com.example.model.BuiltInWidgetType
import com.example.model.HomeGridItem
import com.example.model.HomeItemType
import com.example.model.IconPackInfo
import com.example.model.LauncherRoute
import com.example.model.LauncherSettings
import com.example.model.SystemWidgetGroup
import com.example.model.ThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val db = LauncherDatabase.getInstance(appContext)
    val settingsStore = SettingsDataStore(appContext)
    val appRepository = AppRepository(appContext, db.appCustomizationDao())
    val homeRepository = HomeRepository(db.homeItemDao(), db.quickNoteDao())
    val iconPackManager = IconPackManager(appContext)
    val widgetHostManager = WidgetHostManager(appContext)
    val mediaMonitor = MediaSessionMonitor(appContext)

    val mediaState: StateFlow<MediaPlaybackState> = mediaMonitor.mediaState

    val settings: StateFlow<LauncherSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())

    val allApps: StateFlow<List<AppInfo>> = appRepository.allAppsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val homeItems: StateFlow<List<HomeGridItem>> = homeRepository.homeItemsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val quickNotes: StateFlow<List<QuickNoteEntity>> = homeRepository.quickNotesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _iconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val iconPacks: StateFlow<List<IconPackInfo>> = _iconPacks.asStateFlow()

    private val _systemWidgetGroups = MutableStateFlow<List<SystemWidgetGroup>>(emptyList())
    val systemWidgetGroups: StateFlow<List<SystemWidgetGroup>> = _systemWidgetGroups.asStateFlow()

    private val _activeRoute = MutableStateFlow(LauncherRoute.HOME)
    val activeRoute: StateFlow<LauncherRoute> = _activeRoute.asStateFlow()

    private val _settingsFocusSection = MutableStateFlow("theme")
    val settingsFocusSection: StateFlow<String> = _settingsFocusSection.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _isZenMode = MutableStateFlow(false)
    val isZenMode: StateFlow<Boolean> = _isZenMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val appSearchIndex: StateFlow<AppSearchIndex> = allApps
        .map { apps -> AppSearchIndex.build(apps) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSearchIndex.build(emptyList()))

    private val _currentHomePage = MutableStateFlow(0)
    val currentHomePage: StateFlow<Int> = _currentHomePage.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    private val packageReceiver = appRepository.registerPackageReceiver {
        viewModelScope.launch {
            appRepository.refreshInstalledApps()
            _iconPacks.value = iconPackManager.getAvailableIconPacks()
            _systemWidgetGroups.value = widgetHostManager.getInstalledSystemWidgets()
        }
    }

    init {
        viewModelScope.launch {
            appRepository.refreshInstalledApps()
            val apps = appRepository.allAppsFlow.first { it.isNotEmpty() || true }
            val currentSettings = settingsStore.settingsFlow.first()
            homeRepository.seedDefaultsIfEmpty(
                installedApps = apps,
                cols = currentSettings.gridColumns,
                rows = currentSettings.gridRows
            )
            _iconPacks.value = iconPackManager.getAvailableIconPacks()
            _systemWidgetGroups.value = widgetHostManager.getInstalledSystemWidgets()
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { appContext.unregisterReceiver(packageReceiver) }
        mediaMonitor.release()
    }

    fun clearToast() {
        _statusToast.value = null
    }

    fun showMessage(msg: String) {
        _statusToast.value = msg
    }

    fun navigateTo(route: LauncherRoute) {
        _isDrawerOpen.value = false
        _activeRoute.value = route
    }

    fun openSettingsSection(section: String) {
        _settingsFocusSection.value = section
        _isDrawerOpen.value = false
        _activeRoute.value = LauncherRoute.SETTINGS
    }

    fun openDrawer() {
        _isDrawerOpen.value = true
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
    }

    fun toggleDrawer() {
        _isDrawerOpen.value = !_isDrawerOpen.value
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun toggleZenMode() {
        _isZenMode.value = !_isZenMode.value
    }

    fun setCurrentHomePage(page: Int) {
        _currentHomePage.value = page.coerceAtLeast(0)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun launchApp(app: AppInfo) {
        viewModelScope.launch {
            val ok = appRepository.launchApp(app)
            if (!ok) {
                _statusToast.value = "Unable to launch ${app.displayLabel}"
            } else {
                _searchQuery.value = ""
                _isDrawerOpen.value = false
            }
        }
    }

    fun openAppInfo(app: AppInfo) {
        appRepository.openAppInfo(app.packageName)
    }

    fun uninstallApp(app: AppInfo) {
        appRepository.uninstallApp(app.packageName)
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch {
            settingsStore.updateSettings(transform)
        }
    }

    fun applyThemePreset(preset: ThemePreset) {
        updateSettings { s ->
            s.copy(
                themePreset = preset,
                accentColor = preset.accentHex,
                backgroundColor = preset.bgHex,
                surfaceColor = preset.surfaceHex,
                borderColor = preset.borderHex,
                textColor = preset.textHex,
                mutedTextColor = preset.mutedHex,
                pillBackgroundColor = preset.pillBgHex,
                pillTextColor = preset.pillTextHex,
                isLightTheme = preset.isLight
            )
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            settingsStore.replaceSettings(LauncherSettings())
            _statusToast.value = "Reset settings to Terminal Violet default"
        }
    }

    fun exportSettingsJson(): String {
        return settingsStore.exportToJson(settings.value)
    }

    fun importSettingsJson(json: String): Boolean {
        val parsed = settingsStore.importFromJson(json) ?: return false
        viewModelScope.launch {
            settingsStore.replaceSettings(parsed)
            _statusToast.value = "Configuration imported"
        }
        return true
    }

    fun togglePinnedDockApp(packageName: String) {
        updateSettings { s ->
            val current = s.pinnedDockPackages.toMutableList()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else if (current.size < 4) {
                current.add(packageName)
            } else {
                _statusToast.value = "Dock holds up to 4 pinned apps"
            }
            s.copy(pinnedDockPackages = current)
        }
    }

    fun addAppToHome(app: AppInfo, targetPage: Int = _currentHomePage.value, targetCell: Pair<Int, Int>? = null) {
        viewModelScope.launch {
            val s = settings.value
            val (page, pos) = if (targetCell != null && homeRepository.canPlaceAt(
                    targetPage, targetCell.first, targetCell.second, 1, 1, s.gridColumns, s.gridRows, -1L
                )
            ) {
                targetPage to targetCell
            } else {
                homeRepository.findFirstOpenSlot(targetPage, 1, 1, s.gridColumns, s.gridRows)
            }
            homeRepository.addItem(
                HomeGridItem(
                    page = page,
                    col = pos.first,
                    row = pos.second,
                    colSpan = 1,
                    rowSpan = 1,
                    type = HomeItemType.APP,
                    packageName = app.packageName,
                    activityName = app.activityName
                )
            )
            _statusToast.value = "Added ${app.displayLabel} to page ${page + 1}"
        }
    }

    fun addFolderToHome(
        title: String,
        apps: List<AppInfo>,
        targetPage: Int = _currentHomePage.value,
        targetCell: Pair<Int, Int>? = null
    ) {
        viewModelScope.launch {
            val s = settings.value
            val (page, pos) = if (targetCell != null && homeRepository.canPlaceAt(
                    targetPage, targetCell.first, targetCell.second, 1, 1, s.gridColumns, s.gridRows, -1L
                )
            ) {
                targetPage to targetCell
            } else {
                homeRepository.findFirstOpenSlot(targetPage, 1, 1, s.gridColumns, s.gridRows)
            }
            homeRepository.addItem(
                HomeGridItem(
                    page = page,
                    col = pos.first,
                    row = pos.second,
                    colSpan = 1,
                    rowSpan = 1,
                    type = HomeItemType.FOLDER,
                    folderTitle = title.ifBlank { "~/folder" },
                    folderAppKeys = apps.map { it.componentKey }
                )
            )
            _statusToast.value = "Created folder on page ${page + 1}"
        }
    }

    fun addBuiltInWidgetToHome(
        widgetType: BuiltInWidgetType,
        targetPage: Int = _currentHomePage.value,
        targetCell: Pair<Int, Int>? = null
    ) {
        viewModelScope.launch {
            val s = settings.value
            val colSpan = widgetType.defaultColSpan.coerceAtMost(s.gridColumns)
            val rowSpan = widgetType.defaultRowSpan.coerceAtMost(s.gridRows)
            val (page, pos) = if (targetCell != null && homeRepository.canPlaceAt(
                    targetPage, targetCell.first, targetCell.second, colSpan, rowSpan, s.gridColumns, s.gridRows, -1L
                )
            ) {
                targetPage to targetCell
            } else {
                homeRepository.findFirstOpenSlot(targetPage, colSpan, rowSpan, s.gridColumns, s.gridRows)
            }
            homeRepository.addItem(
                HomeGridItem(
                    page = page,
                    col = pos.first,
                    row = pos.second,
                    colSpan = colSpan,
                    rowSpan = rowSpan,
                    type = HomeItemType.BUILT_IN_WIDGET,
                    builtInWidgetType = widgetType
                )
            )
            _activeRoute.value = LauncherRoute.HOME
            _currentHomePage.value = page
            _statusToast.value = "Placed ${widgetType.title} on page ${page + 1}"
        }
    }

    fun placeBoundSystemWidget(
        appWidgetId: Int,
        info: AppWidgetProviderInfo,
        targetPage: Int = _currentHomePage.value
    ) {
        viewModelScope.launch {
            val s = settings.value
            val cellWidth = (info.minWidth / 72).coerceIn(1, s.gridColumns)
            val cellHeight = (info.minHeight / 72).coerceIn(1, s.gridRows)
            val (page, pos) = homeRepository.findFirstOpenSlot(
                targetPage, cellWidth, cellHeight, s.gridColumns, s.gridRows
            )
            homeRepository.addItem(
                HomeGridItem(
                    page = page,
                    col = pos.first,
                    row = pos.second,
                    colSpan = cellWidth,
                    rowSpan = cellHeight,
                    type = HomeItemType.SYSTEM_WIDGET,
                    packageName = info.provider.packageName,
                    appWidgetId = appWidgetId
                )
            )
            _activeRoute.value = LauncherRoute.HOME
            _currentHomePage.value = page
            _statusToast.value = "System widget added to page ${page + 1}"
        }
    }

    fun moveHomeItem(item: HomeGridItem, newPage: Int, newCol: Int, newRow: Int) {
        viewModelScope.launch {
            val s = settings.value
            val clampedSpanCol = item.colSpan.coerceAtMost(s.gridColumns)
            val clampedSpanRow = item.rowSpan.coerceAtMost(s.gridRows)
            val safeCol = newCol.coerceIn(0, (s.gridColumns - clampedSpanCol).coerceAtLeast(0))
            val safeRow = newRow.coerceIn(0, (s.gridRows - clampedSpanRow).coerceAtLeast(0))
            if (homeRepository.canPlaceAt(
                    newPage, safeCol, safeRow, clampedSpanCol, clampedSpanRow,
                    s.gridColumns, s.gridRows, item.id
                )
            ) {
                homeRepository.updateItem(
                    item.copy(
                        page = newPage,
                        col = safeCol,
                        row = safeRow,
                        colSpan = clampedSpanCol,
                        rowSpan = clampedSpanRow
                    )
                )
            } else {
                _statusToast.value = "Target cells are occupied"
            }
        }
    }

    fun resizeHomeItem(item: HomeGridItem, deltaCol: Int, deltaRow: Int) {
        viewModelScope.launch {
            val s = settings.value
            val targetColSpan = (item.colSpan + deltaCol).coerceIn(1, s.gridColumns - item.col)
            val targetRowSpan = (item.rowSpan + deltaRow).coerceIn(1, s.gridRows - item.row)
            if (homeRepository.canPlaceAt(
                    item.page, item.col, item.row, targetColSpan, targetRowSpan,
                    s.gridColumns, s.gridRows, item.id
                )
            ) {
                homeRepository.updateItem(
                    item.copy(colSpan = targetColSpan, rowSpan = targetRowSpan)
                )
            } else {
                _statusToast.value = "Not enough free space to resize"
            }
        }
    }

    fun updateItemGeometry(
        item: HomeGridItem,
        newPage: Int,
        newCol: Int,
        newRow: Int,
        newColSpan: Int,
        newRowSpan: Int
    ) {
        viewModelScope.launch {
            val s = settings.value
            val safeColSpan = newColSpan.coerceIn(1, s.gridColumns)
            val safeRowSpan = newRowSpan.coerceIn(1, s.gridRows)
            val safeCol = newCol.coerceIn(0, (s.gridColumns - safeColSpan).coerceAtLeast(0))
            val safeRow = newRow.coerceIn(0, (s.gridRows - safeRowSpan).coerceAtLeast(0))
            val safePage = newPage.coerceAtLeast(0)

            if (homeRepository.canPlaceAt(
                    safePage, safeCol, safeRow, safeColSpan, safeRowSpan,
                    s.gridColumns, s.gridRows, item.id
                )
            ) {
                homeRepository.updateItem(
                    item.copy(
                        page = safePage,
                        col = safeCol,
                        row = safeRow,
                        colSpan = safeColSpan,
                        rowSpan = safeRowSpan
                    )
                )
            } else {
                _statusToast.value = "Cells occupied — drop on free space"
            }
        }
    }

    fun updateFolder(item: HomeGridItem, newTitle: String, newAppKeys: List<String>) {
        viewModelScope.launch {
            homeRepository.updateItem(
                item.copy(
                    folderTitle = newTitle.ifBlank { "~/folder" },
                    folderAppKeys = newAppKeys
                )
            )
        }
    }

    fun removeHomeItem(item: HomeGridItem) {
        viewModelScope.launch {
            if (item.type == HomeItemType.SYSTEM_WIDGET && item.appWidgetId > 0) {
                widgetHostManager.deleteAppWidgetId(item.appWidgetId)
            }
            homeRepository.deleteItem(item.id)
        }
    }

    fun resetHomeGrid() {
        viewModelScope.launch {
            val s = settings.value
            homeRepository.resetHomeLayout(allApps.value, s.gridColumns, s.gridRows)
            _currentHomePage.value = 0
            _statusToast.value = "Home layout reset"
        }
    }

    fun saveAppCustomization(
        app: AppInfo,
        newLabel: String?,
        customIconUri: String?,
        customPackPackage: String?,
        customPackDrawable: String?,
        clearIcon: Boolean
    ) {
        viewModelScope.launch {
            val trimmedLabel = newLabel?.trim()?.takeIf { it.isNotEmpty() && it != app.label }
            if (trimmedLabel == null) {
                appRepository.resetAppLabel(app.componentKey)
            }
            appRepository.updateAppCustomization(
                componentKey = app.componentKey,
                customLabel = trimmedLabel,
                customIconUri = customIconUri,
                customPackPackage = customPackPackage,
                customPackDrawable = customPackDrawable,
                clearCustomIcon = clearIcon,
                replaceIconSource = true
            )
            _statusToast.value = "Updated ${trimmedLabel ?: app.label}"
        }
    }

    fun updatePhotoWidgetImage(itemId: Long, savedPath: String) {
        viewModelScope.launch {
            val current = homeRepository.getItemById(itemId) ?: return@launch
            val json = runCatching {
                JSONObject(current.widgetConfig ?: "{}")
            }.getOrElse { JSONObject() }
            json.put("photoPath", savedPath)
            if (!json.has("showBorder")) json.put("showBorder", true)
            homeRepository.updateWidgetConfig(itemId, json.toString())
            _statusToast.value = "Photo widget updated"
        }
    }

    fun togglePhotoWidgetBorder(item: HomeGridItem) {
        viewModelScope.launch {
            val json = runCatching {
                JSONObject(item.widgetConfig ?: "{}")
            }.getOrElse { JSONObject() }
            val currentBorder = json.optBoolean("showBorder", true)
            json.put("showBorder", !currentBorder)
            homeRepository.updateWidgetConfig(item.id, json.toString())
        }
    }

    fun setAppHidden(app: AppInfo, hidden: Boolean) {
        viewModelScope.launch {
            appRepository.updateAppCustomization(
                componentKey = app.componentKey,
                isHidden = hidden
            )
            _statusToast.value = if (hidden) "Hid ${app.displayLabel}" else "Unhid ${app.displayLabel}"
        }
    }

    fun addQuickNote(text: String) {
        viewModelScope.launch { homeRepository.addQuickNote(text) }
    }

    fun toggleQuickNote(note: QuickNoteEntity) {
        viewModelScope.launch { homeRepository.toggleQuickNote(note) }
    }

    fun deleteQuickNote(id: Long) {
        viewModelScope.launch { homeRepository.deleteQuickNote(id) }
    }

    fun isGifUri(uri: Uri): Boolean {
        val mime = runCatching { appContext.contentResolver.getType(uri) }.getOrNull()
        if (mime.equals("image/gif", ignoreCase = true)) return true
        return runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(6)
                val read = stream.read(header)
                read >= 6 && String(header, 0, 6, Charsets.US_ASCII).startsWith("GIF8")
            } ?: false
        }.getOrDefault(false)
    }

    fun savePickedGifToInternal(uri: Uri, prefix: String, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(appContext.filesDir, "user_media").apply { mkdirs() }
                    val file = File(dir, "${prefix}_${System.currentTimeMillis()}.gif")
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    file.absolutePath
                }.getOrNull()
            }
            if (path != null) {
                onSaved(path)
            } else {
                _statusToast.value = "Failed to load selected GIF"
            }
        }
    }

    fun savePickedImageToInternal(uri: Uri, prefix: String, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(appContext.filesDir, "user_media").apply { mkdirs() }
                    val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    file.absolutePath
                }.getOrNull()
            }
            if (path != null) {
                onSaved(path)
            } else {
                _statusToast.value = "Failed to load selected image"
            }
        }
    }

    fun applyCustomWallpaperToSystem() {
        val path = settings.value.customWallpaperPath
        if (path.isBlank()) {
            _statusToast.value = "Select a custom wallpaper image first"
            return
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val bmp = BitmapFactory.decodeFile(path) ?: return@runCatching false
                    WallpaperManager.getInstance(appContext).setBitmap(bmp)
                    true
                }.getOrDefault(false)
            }
            _statusToast.value = if (ok) "Applied to system wallpaper" else "System wallpaper update unavailable"
        }
    }

    fun openDefaultHomeSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val started = runCatching {
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
        if (!started) {
            runCatching {
                appContext.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
