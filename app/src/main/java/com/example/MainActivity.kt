package com.example

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppInfo
import com.example.model.DoubleTapAction
import com.example.model.HomeGridItem
import com.example.model.LauncherRoute
import com.example.model.WallpaperType
import com.example.ui.components.BottomCommandPanel
import com.example.ui.components.TuiCliLink
import com.example.ui.components.WallpaperBackground
import com.example.ui.screens.AddToCellDialog
import com.example.ui.screens.AppDrawerSheet
import com.example.ui.screens.AppEditDialog
import com.example.ui.screens.FolderModalDialog
import com.example.ui.screens.HomeGridScreen
import com.example.ui.screens.ImageCropDialog
import com.example.ui.screens.ItemContextDialog
import com.example.ui.screens.ProfileEditDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WelcomeQuoteDialog
import com.example.ui.screens.WidgetHubScreen
import com.example.ui.theme.LocalLauncherTokens
import com.example.ui.theme.TildeTheme
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TildeLauncherRoot(viewModel = viewModel)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.widgetHostManager.startListening()
        viewModel.mediaMonitor.refresh()
    }

    override fun onResume() {
        super.onResume()
        viewModel.mediaMonitor.refresh()
    }

    override fun onStop() {
        super.onStop()
        viewModel.widgetHostManager.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_HOME)
        ) {
            viewModel.closeDrawer()
            viewModel.navigateTo(LauncherRoute.HOME)
            viewModel.setCurrentHomePage(0)
        }
    }
}

private enum class ImagePickTarget {
    WALLPAPER,
    PROFILE_AVATAR,
    PROFILE_BANNER,
    APP_ICON,
    WIDGET_PHOTO
}

private data class PendingCropRequest(
    val uri: Uri,
    val target: ImagePickTarget,
    val app: AppInfo? = null,
    val widgetItemId: Long = -1L,
    val isWideWidget: Boolean = false
)

@Composable
fun TildeLauncherRoot(viewModel: LauncherViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val homeItems by viewModel.homeItems.collectAsStateWithLifecycle()
    val quickNotes by viewModel.quickNotes.collectAsStateWithLifecycle()
    val iconPacks by viewModel.iconPacks.collectAsStateWithLifecycle()
    val systemWidgetGroups by viewModel.systemWidgetGroups.collectAsStateWithLifecycle()
    val activeRoute by viewModel.activeRoute.collectAsStateWithLifecycle()
    val settingsFocusSection by viewModel.settingsFocusSection.collectAsStateWithLifecycle()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val isZenMode by viewModel.isZenMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val appSearchIndex by viewModel.appSearchIndex.collectAsStateWithLifecycle()
    val currentHomePage by viewModel.currentHomePage.collectAsStateWithLifecycle()
    val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()

    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()

    var pageOffsetFraction by remember { mutableFloatStateOf(0f) }

    // Dialog states
    var showProfileEdit by remember { mutableStateOf(false) }
    var showWelcomeQuoteEdit by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<AppInfo?>(null) }
    var openedFolder by remember { mutableStateOf<HomeGridItem?>(null) }
    var contextMenuItem by remember { mutableStateOf<HomeGridItem?>(null) }
    var addTargetCell by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var pendingCropRequest by remember { mutableStateOf<PendingCropRequest?>(null) }

    // Photo Picker state
    var pendingPickTarget by remember { mutableStateOf<ImagePickTarget?>(null) }
    var pendingPickApp by remember { mutableStateOf<AppInfo?>(null) }
    var pendingPickWidgetId by remember { mutableStateOf(-1L) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingPickTarget
        val targetApp = pendingPickApp
        val targetWidgetId = pendingPickWidgetId
        if (uri != null && target != null) {
            when (target) {
                ImagePickTarget.APP_ICON -> {
                    pendingCropRequest = PendingCropRequest(
                        uri = uri,
                        target = ImagePickTarget.APP_ICON,
                        app = targetApp
                    )
                }
                ImagePickTarget.WIDGET_PHOTO -> {
                    if (viewModel.isGifUri(uri)) {
                        viewModel.savePickedGifToInternal(uri, "widget_gif") { savedPath ->
                            if (targetWidgetId > 0L) {
                                viewModel.updatePhotoWidgetImage(targetWidgetId, savedPath)
                                viewModel.showMessage("Animated GIF added to photo widget")
                            }
                        }
                    } else {
                        val isWide = homeItems.firstOrNull { it.id == targetWidgetId }?.let {
                            it.colSpan > it.rowSpan
                        } == true
                        pendingCropRequest = PendingCropRequest(
                            uri = uri,
                            target = ImagePickTarget.WIDGET_PHOTO,
                            widgetItemId = targetWidgetId,
                            isWideWidget = isWide
                        )
                    }
                }
                else -> {
                    val prefix = when (target) {
                        ImagePickTarget.WALLPAPER -> "wallpaper"
                        ImagePickTarget.PROFILE_AVATAR -> "avatar"
                        ImagePickTarget.PROFILE_BANNER -> "banner"
                        else -> "media"
                    }
                    viewModel.savePickedImageToInternal(uri, prefix) { savedPath ->
                        when (target) {
                            ImagePickTarget.WALLPAPER -> {
                                viewModel.updateSettings {
                                    it.copy(
                                        wallpaperType = WallpaperType.CUSTOM_IMAGE,
                                        customWallpaperPath = savedPath
                                    )
                                }
                                viewModel.showMessage("Custom wallpaper updated")
                            }
                            ImagePickTarget.PROFILE_AVATAR -> {
                                viewModel.updateSettings { it.copy(profileAvatarPath = savedPath) }
                                viewModel.showMessage("Profile avatar updated")
                            }
                            ImagePickTarget.PROFILE_BANNER -> {
                                viewModel.updateSettings { it.copy(profileBannerPath = savedPath) }
                                viewModel.showMessage("Profile banner updated")
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
        pendingPickTarget = null
        pendingPickApp = null
        pendingPickWidgetId = -1L
    }

    fun launchPhotoPicker(target: ImagePickTarget, app: AppInfo? = null, widgetItemId: Long = -1L) {
        pendingPickTarget = target
        pendingPickApp = app
        pendingPickWidgetId = widgetItemId
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Android System AppWidget Binding & Configuration Launchers
    var pendingWidgetId by remember { mutableStateOf(-1) }
    var pendingWidgetInfo by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }

    val configureWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingWidgetId
        val info = pendingWidgetInfo
        if (result.resultCode == Activity.RESULT_OK && id > 0 && info != null) {
            viewModel.placeBoundSystemWidget(id, info)
        } else if (id > 0) {
            viewModel.widgetHostManager.deleteAppWidgetId(id)
        }
        pendingWidgetId = -1
        pendingWidgetInfo = null
    }

    fun proceedAfterBind(widgetId: Int, info: AppWidgetProviderInfo) {
        val configureComponent = info.configure
        if (configureComponent != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configureComponent
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val launched = runCatching {
                configureWidgetLauncher.launch(intent)
                true
            }.getOrDefault(false)
            if (!launched) {
                viewModel.placeBoundSystemWidget(widgetId, info)
                pendingWidgetId = -1
                pendingWidgetInfo = null
            }
        } else {
            viewModel.placeBoundSystemWidget(widgetId, info)
            pendingWidgetId = -1
            pendingWidgetInfo = null
        }
    }

    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingWidgetId
        val info = pendingWidgetInfo
        if (result.resultCode == Activity.RESULT_OK && id > 0 && info != null) {
            proceedAfterBind(id, info)
        } else {
            if (id > 0) viewModel.widgetHostManager.deleteAppWidgetId(id)
            pendingWidgetId = -1
            pendingWidgetInfo = null
            viewModel.showMessage("Widget permission cancelled")
        }
    }

    fun startAddingSystemWidget(info: AppWidgetProviderInfo) {
        val newId = viewModel.widgetHostManager.allocateAppWidgetId()
        pendingWidgetId = newId
        pendingWidgetInfo = info
        if (viewModel.widgetHostManager.tryBindWidget(newId, info)) {
            proceedAfterBind(newId, info)
        } else {
            val bindIntent = viewModel.widgetHostManager.createBindIntent(newId, info)
            val ok = runCatching {
                bindWidgetLauncher.launch(bindIntent)
                true
            }.getOrDefault(false)
            if (!ok) {
                viewModel.widgetHostManager.deleteAppWidgetId(newId)
                pendingWidgetId = -1
                pendingWidgetInfo = null
                viewModel.showMessage("System widget binding unavailable")
            }
        }
    }

    val searchResults = remember(appSearchIndex, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else appSearchIndex.search(searchQuery, limit = 12).map { it.app }
    }

    val pinnedApps = remember(allApps, settings.pinnedDockPackages) {
        val byPkg = allApps.associateBy { it.packageName }
        settings.pinnedDockPackages.mapNotNull { byPkg[it] }
    }

    fun runTerminalCommand(rawCmd: String): Boolean {
        val cmd = rawCmd.trim().lowercase()
        return when (cmd) {
            ">home", "home" -> {
                viewModel.navigateTo(LauncherRoute.HOME)
                viewModel.setCurrentHomePage(0)
                true
            }
            ">settings", "settings" -> {
                viewModel.navigateTo(LauncherRoute.SETTINGS)
                true
            }
            ">widgets", "widgets" -> {
                viewModel.navigateTo(LauncherRoute.WIDGETS)
                true
            }
            ">apps", "drawer" -> {
                viewModel.openDrawer()
                true
            }
            ">edit" -> {
                viewModel.navigateTo(LauncherRoute.HOME)
                viewModel.setEditMode(!isEditMode)
                true
            }
            ">zen" -> {
                viewModel.toggleZenMode()
                true
            }
            ">fonts", "fonts" -> {
                viewModel.openSettingsSection("fonts")
                true
            }
            ">theme", "theme" -> {
                viewModel.openSettingsSection("theme")
                true
            }
            else -> false
        }
    }

    LaunchedEffect(statusToast) {
        if (statusToast != null) {
            delay(2600L)
            viewModel.clearToast()
        }
    }

    if (isEditMode) {
        BackHandler { viewModel.setEditMode(false) }
    } else if (isZenMode) {
        BackHandler { viewModel.toggleZenMode() }
    }

    TildeTheme(settings = settings) {
        val tokens = LocalLauncherTokens.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            WallpaperBackground(
                settings = settings,
                pageOffsetFraction = pageOffsetFraction
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Main Content Area (No Top Bar! Clean full-bleed workspace)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeRoute) {
                        LauncherRoute.HOME -> {
                            HomeGridScreen(
                                settings = settings,
                                homeItems = homeItems,
                                allApps = allApps,
                                quickNotes = quickNotes,
                                mediaState = mediaState,
                                currentPage = currentHomePage,
                                isEditMode = isEditMode,
                                iconPackManager = viewModel.iconPackManager,
                                widgetHostManager = viewModel.widgetHostManager,
                                onPageChanged = { page, offset ->
                                    viewModel.setCurrentHomePage(page)
                                    pageOffsetFraction = offset
                                },
                                onLaunchApp = { viewModel.launchApp(it) },
                                onOpenFolder = { openedFolder = it },
                                onLongPressItem = { contextMenuItem = it },
                                onEditAppItem = { item ->
                                    val matched = allApps.firstOrNull {
                                        it.packageName == item.packageName && it.activityName == item.activityName
                                    } ?: allApps.firstOrNull { it.packageName == item.packageName }
                                    if (matched != null) editingApp = matched
                                },
                                onPickWidgetPhoto = { item ->
                                    launchPhotoPicker(ImagePickTarget.WIDGET_PHOTO, widgetItemId = item.id)
                                },
                                onUpdateItemGeometry = { item, newPage, newCol, newRow, newColSpan, newRowSpan ->
                                    viewModel.updateItemGeometry(item, newPage, newCol, newRow, newColSpan, newRowSpan)
                                },
                                onRemoveItem = { viewModel.removeHomeItem(it) },
                                onEmptyCellAction = { page, col, row ->
                                    addTargetCell = Triple(page, col, row)
                                },
                                onSwipeUpDrawer = { viewModel.openDrawer() },
                                onDoubleTapEmpty = {
                                    when (settings.doubleTapAction) {
                                        DoubleTapAction.NONE -> Unit
                                        DoubleTapAction.SEARCH -> viewModel.openDrawer()
                                        DoubleTapAction.APP_DRAWER -> viewModel.openDrawer()
                                        DoubleTapAction.ZEN_MODE -> viewModel.toggleZenMode()
                                        DoubleTapAction.SETTINGS -> viewModel.navigateTo(LauncherRoute.SETTINGS)
                                    }
                                },
                                onToggleEditMode = { viewModel.setEditMode(!isEditMode) },
                                onEditProfile = { field ->
                                    when (field) {
                                        "pfp" -> launchPhotoPicker(ImagePickTarget.PROFILE_AVATAR)
                                        "banner" -> launchPhotoPicker(ImagePickTarget.PROFILE_BANNER)
                                        else -> showProfileEdit = true
                                    }
                                },
                                onEditWelcomeQuote = { showWelcomeQuoteEdit = true },
                                onMediaTogglePlayPause = { viewModel.mediaMonitor.togglePlayPause() },
                                onMediaSkipNext = { viewModel.mediaMonitor.skipToNext() },
                                onMediaSkipPrevious = { viewModel.mediaMonitor.skipToPrevious() },
                                onOpenMediaPlayer = { viewModel.mediaMonitor.openPlayerApp() },
                                onGrantMediaAccess = { viewModel.mediaMonitor.openNotificationAccessSettings() },
                                onAddQuickNote = { viewModel.addQuickNote(it) },
                                onToggleQuickNote = { viewModel.toggleQuickNote(it) },
                                onDeleteQuickNote = { viewModel.deleteQuickNote(it) }
                            )
                        }

                        LauncherRoute.WIDGETS -> {
                            WidgetHubScreen(
                                systemWidgetGroups = systemWidgetGroups,
                                searchQuery = searchQuery,
                                onAddBuiltInWidget = { viewModel.addBuiltInWidgetToHome(it) },
                                onSelectSystemWidget = { startAddingSystemWidget(it) },
                                onBackHome = { viewModel.navigateTo(LauncherRoute.HOME) }
                            )
                        }

                        LauncherRoute.SETTINGS -> {
                            SettingsScreen(
                                settings = settings,
                                allApps = allApps,
                                iconPacks = iconPacks,
                                iconPackManager = viewModel.iconPackManager,
                                initialSection = settingsFocusSection,
                                onUpdateSettings = { viewModel.updateSettings(it) },
                                onApplyPreset = { viewModel.applyThemePreset(it) },
                                onPickCustomWallpaper = { launchPhotoPicker(ImagePickTarget.WALLPAPER) },
                                onApplySystemWallpaper = { viewModel.applyCustomWallpaperToSystem() },
                                onEditProfile = { showProfileEdit = true },
                                onTogglePinDock = { viewModel.togglePinnedDockApp(it) },
                                onUnhideApp = { viewModel.setAppHidden(it, false) },
                                onResetHomeGrid = { viewModel.resetHomeGrid() },
                                onResetAllSettings = { viewModel.resetAllSettings() },
                                onOpenDefaultLauncherSettings = { viewModel.openDefaultHomeSettings() },
                                onExportJson = { viewModel.exportSettingsJson() },
                                onImportJson = { viewModel.importSettingsJson(it) },
                                onShowToast = { viewModel.showMessage(it) },
                                onBackHome = { viewModel.navigateTo(LauncherRoute.HOME) }
                            )
                        }
                    }

                    // Slide-up App Drawer Overlay (includes Settings menu!)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isDrawerOpen,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                    ) {
                        AppDrawerSheet(
                            settings = settings,
                            allApps = allApps,
                            searchQuery = searchQuery,
                            iconPackManager = viewModel.iconPackManager,
                            onLaunchApp = { viewModel.launchApp(it) },
                            onAddAppToHome = {
                                viewModel.addAppToHome(it)
                                viewModel.closeDrawer()
                                viewModel.navigateTo(LauncherRoute.HOME)
                            },
                            onTogglePinDock = { viewModel.togglePinnedDockApp(it.packageName) },
                            onEditApp = { editingApp = it },
                            onHideApp = { viewModel.setAppHidden(it, true) },
                            onAppInfo = { viewModel.openAppInfo(it) },
                            onUninstallApp = { viewModel.uninstallApp(it) },
                            onOpenSettings = { viewModel.openSettingsSection("theme") },
                            onOpenSettingsSection = { section -> viewModel.openSettingsSection(section) },
                            onOpenWidgets = { viewModel.navigateTo(LauncherRoute.WIDGETS) },
                            onCycleLayoutMode = {
                                val entries = com.example.model.DrawerLayoutMode.entries
                                val next = entries[(entries.indexOf(settings.drawerLayoutMode) + 1) % entries.size]
                                viewModel.updateSettings { it.copy(drawerLayoutMode = next) }
                            },
                            onCycleSortOrder = {
                                val entries = com.example.model.DrawerSortOrder.entries
                                val next = entries[(entries.indexOf(settings.drawerSortOrder) + 1) % entries.size]
                                viewModel.updateSettings { it.copy(drawerSortOrder = next) }
                            },
                            onCloseDrawer = { viewModel.closeDrawer() }
                        )
                    }

                    // Toast / Status Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = statusToast != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                    ) {
                        statusToast?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .background(tokens.pillBg)
                                    .border(1.dp, tokens.accent, RoundedCornerShape(tokens.cardCornerRadius))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "> $msg",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.pillText
                                )
                            }
                        }
                    }
                }

                // Bottom Panel (replaces Top Bar; houses Search Bar, App Drawer Icon & Primary Navigation Shortcuts)
                if (!isZenMode) {
                    BottomCommandPanel(
                        settings = settings,
                        activeRoute = activeRoute,
                        isDrawerOpen = isDrawerOpen,
                        searchQuery = searchQuery,
                        pinnedApps = pinnedApps,
                        searchResults = searchResults,
                        iconPackManager = viewModel.iconPackManager,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onSubmitSearch = {
                            if (runTerminalCommand(searchQuery)) {
                                viewModel.updateSearchQuery("")
                            } else if (searchResults.isNotEmpty()) {
                                viewModel.launchApp(searchResults.first())
                            }
                        },
                        onNavigate = { viewModel.navigateTo(it) },
                        onToggleDrawer = { viewModel.toggleDrawer() },
                        onLaunchApp = { viewModel.launchApp(it) }
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        TuiCliLink(label = "exit zen mode", onClick = { viewModel.toggleZenMode() })
                    }
                }
            }
        }

        // Modals & Dialogs
        if (showProfileEdit) {
            ProfileEditDialog(
                settings = settings,
                onDismiss = { showProfileEdit = false },
                onPickAvatar = { launchPhotoPicker(ImagePickTarget.PROFILE_AVATAR) },
                onPickBanner = { launchPhotoPicker(ImagePickTarget.PROFILE_BANNER) },
                onSave = { name, pronouns, subtitle, bio, quote ->
                    viewModel.updateSettings {
                        it.copy(
                            profileDisplayName = name.ifBlank { it.profileDisplayName },
                            profilePronouns = pronouns.ifBlank { it.profilePronouns },
                            profileSubtitle = subtitle.ifBlank { it.profileSubtitle },
                            profileBio = bio.ifBlank { it.profileBio },
                            profileWelcomeQuote = quote.ifBlank { it.profileWelcomeQuote }
                        )
                    }
                }
            )
        }

        if (showWelcomeQuoteEdit) {
            WelcomeQuoteDialog(
                initialQuote = settings.profileWelcomeQuote,
                onDismiss = { showWelcomeQuoteEdit = false },
                onSave = { quote ->
                    viewModel.updateSettings {
                        it.copy(profileWelcomeQuote = quote.ifBlank { it.profileWelcomeQuote })
                    }
                }
            )
        }

        editingApp?.let { app ->
            AppEditDialog(
                app = app,
                settings = settings,
                iconPacks = iconPacks,
                iconPackManager = viewModel.iconPackManager,
                onPickCustomImage = {
                    launchPhotoPicker(ImagePickTarget.APP_ICON, app)
                },
                onSave = { newLabel, customIconUri, packPkg, packDrawable, clearIcon ->
                    viewModel.saveAppCustomization(
                        app = app,
                        newLabel = newLabel,
                        customIconUri = customIconUri,
                        customPackPackage = packPkg,
                        customPackDrawable = packDrawable,
                        clearIcon = clearIcon
                    )
                },
                onDismiss = { editingApp = null }
            )
        }

        pendingCropRequest?.let { cropReq ->
            ImageCropDialog(
                imageUri = cropReq.uri,
                title = if (cropReq.target == ImagePickTarget.APP_ICON) {
                    "Crop shortcut icon"
                } else {
                    "Crop photo widget"
                },
                isIconCrop = cropReq.target == ImagePickTarget.APP_ICON,
                outputPrefix = if (cropReq.target == ImagePickTarget.APP_ICON) "appicon" else "widget_photo",
                initialAspect = if (cropReq.isWideWidget) {
                    com.example.ui.screens.CropAspectPreset.WIDE_2_1
                } else {
                    com.example.ui.screens.CropAspectPreset.SQUARE
                },
                onCropped = { savedPath ->
                    when (cropReq.target) {
                        ImagePickTarget.APP_ICON -> {
                            val targetApp = cropReq.app
                            if (targetApp != null) {
                                viewModel.saveAppCustomization(
                                    app = targetApp,
                                    newLabel = targetApp.customLabel,
                                    customIconUri = savedPath,
                                    customPackPackage = null,
                                    customPackDrawable = null,
                                    clearIcon = false
                                )
                                editingApp = targetApp.copy(
                                    customIconUri = savedPath,
                                    customPackPackage = null,
                                    customPackDrawable = null
                                )
                            }
                        }
                        ImagePickTarget.WIDGET_PHOTO -> {
                            if (cropReq.widgetItemId > 0L) {
                                viewModel.updatePhotoWidgetImage(cropReq.widgetItemId, savedPath)
                            }
                        }
                        else -> Unit
                    }
                    pendingCropRequest = null
                },
                onDismiss = { pendingCropRequest = null }
            )
        }

        openedFolder?.let { folder ->
            val latestFolder = homeItems.firstOrNull { it.id == folder.id } ?: folder
            FolderModalDialog(
                folderItem = latestFolder,
                allApps = allApps,
                settings = settings,
                iconPackManager = viewModel.iconPackManager,
                onLaunchApp = { viewModel.launchApp(it) },
                onSaveFolder = { title, keys -> viewModel.updateFolder(latestFolder, title, keys) },
                onDismiss = { openedFolder = null }
            )
        }

        addTargetCell?.let { (page, col, row) ->
            AddToCellDialog(
                page = page,
                col = col,
                row = row,
                allApps = allApps,
                settings = settings,
                iconPackManager = viewModel.iconPackManager,
                onAddApp = { app -> viewModel.addAppToHome(app, page, col to row) },
                onAddFolder = { title, apps -> viewModel.addFolderToHome(title, apps, page, col to row) },
                onAddBuiltInWidget = { wType -> viewModel.addBuiltInWidgetToHome(wType, page, col to row) },
                onOpenSystemWidgets = { viewModel.navigateTo(LauncherRoute.WIDGETS) },
                onDismiss = { addTargetCell = null }
            )
        }

        contextMenuItem?.let { item ->
            val liveItem = homeItems.firstOrNull { it.id == item.id } ?: item
            val matchedApp = allApps.firstOrNull {
                it.packageName == liveItem.packageName && it.activityName == liveItem.activityName
            } ?: allApps.firstOrNull { it.packageName == liveItem.packageName }

            ItemContextDialog(
                item = liveItem,
                app = matchedApp,
                isPinnedToDock = matchedApp?.let { settings.pinnedDockPackages.contains(it.packageName) } == true,
                gridCols = settings.gridColumns,
                gridRows = settings.gridRows,
                onMove = { np, nc, nr -> viewModel.moveHomeItem(liveItem, np, nc, nr) },
                onResize = { dc, dr -> viewModel.resizeHomeItem(liveItem, dc, dr) },
                onEditApp = { if (matchedApp != null) editingApp = matchedApp },
                onPickWidgetPhoto = {
                    launchPhotoPicker(ImagePickTarget.WIDGET_PHOTO, widgetItemId = liveItem.id)
                },
                onTogglePhotoWidgetBorder = { viewModel.togglePhotoWidgetBorder(liveItem) },
                onTogglePinDock = { if (matchedApp != null) viewModel.togglePinnedDockApp(matchedApp.packageName) },
                onAppInfo = { if (matchedApp != null) viewModel.openAppInfo(matchedApp) },
                onUninstall = { if (matchedApp != null) viewModel.uninstallApp(matchedApp) },
                onRemove = { viewModel.removeHomeItem(liveItem) },
                onDismiss = { contextMenuItem = null }
            )
        }
    }
}
