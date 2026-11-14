package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.IconPackManager
import com.example.data.MediaPlaybackState
import com.example.data.QuickNoteEntity
import com.example.data.WidgetHostManager
import com.example.model.AppInfo
import com.example.model.BuiltInWidgetType
import com.example.model.HomeGridItem
import com.example.model.HomeItemType
import com.example.model.LauncherSettings
import com.example.model.PageTransitionStyle
import com.example.ui.components.AppIconView
import com.example.ui.components.BuiltInWidgetRenderer
import com.example.ui.components.SystemAppWidgetView
import com.example.ui.components.TuiCliLink
import com.example.ui.theme.LocalLauncherTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class ResizeEdge {
    TOP, BOTTOM, LEFT, RIGHT
}

private data class ResizePreview(
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeGridScreen(
    settings: LauncherSettings,
    homeItems: List<HomeGridItem>,
    allApps: List<AppInfo>,
    quickNotes: List<QuickNoteEntity>,
    mediaState: MediaPlaybackState,
    currentPage: Int,
    isEditMode: Boolean,
    iconPackManager: IconPackManager,
    widgetHostManager: WidgetHostManager,
    onPageChanged: (Int, Float) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenFolder: (HomeGridItem) -> Unit,
    onLongPressItem: (HomeGridItem) -> Unit,
    onEditAppItem: (HomeGridItem) -> Unit,
    onPickWidgetPhoto: (HomeGridItem) -> Unit,
    onUpdateItemGeometry: (item: HomeGridItem, newPage: Int, newCol: Int, newRow: Int, newColSpan: Int, newRowSpan: Int) -> Unit,
    onRemoveItem: (HomeGridItem) -> Unit,
    onEmptyCellAction: (page: Int, col: Int, row: Int) -> Unit,
    onSwipeUpDrawer: () -> Unit,
    onDoubleTapEmpty: () -> Unit,
    onToggleEditMode: () -> Unit,
    onEditProfile: (String) -> Unit,
    onEditWelcomeQuote: () -> Unit,
    onMediaTogglePlayPause: () -> Unit,
    onMediaSkipNext: () -> Unit,
    onMediaSkipPrevious: () -> Unit,
    onOpenMediaPlayer: () -> Unit,
    onGrantMediaAccess: () -> Unit,
    onAddQuickNote: (String) -> Unit,
    onToggleQuickNote: (QuickNoteEntity) -> Unit,
    onDeleteQuickNote: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var selectedItemId by remember { mutableStateOf<Long?>(null) }
    var draggingItem by remember { mutableStateOf<HomeGridItem?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var dragTotalDistance by remember { mutableFloatStateOf(0f) }
    var activeResizeEdge by remember { mutableStateOf<ResizeEdge?>(null) }
    var resizePreview by remember { mutableStateOf<ResizePreview?>(null) }

    val maxDataPage = homeItems.maxOfOrNull { it.page } ?: 0
    val totalPages = (maxDataPage + if (isEditMode || draggingItem != null) 2 else 1).coerceAtLeast(2)

    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, totalPages - 1),
        pageCount = { totalPages }
    )

    // Clear selection if selected item was deleted
    LaunchedEffect(homeItems) {
        if (selectedItemId != null && homeItems.none { it.id == selectedItemId }) {
            selectedItemId = null
        }
    }

    LaunchedEffect(currentPage, totalPages) {
        val target = currentPage.coerceIn(0, totalPages - 1)
        if (pagerState.currentPage != target && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        onPageChanged(pagerState.currentPage, pagerState.currentPageOffsetFraction)
    }

    val appLookup = remember(allApps) { allApps.associateBy { it.componentKey } }
    val appByPackage = remember(allApps) { allApps.associateBy { it.packageName } }

    var pagerPixelSize by remember { mutableStateOf(IntSize.Zero) }
    val removeZoneHeightPx = with(density) { 64.dp.toPx() }
    val currentDragItem by rememberUpdatedState(draggingItem)

    // Check if dragged item's vertical displacement reaches top Remove Zone
    var dragStartTopY by remember { mutableFloatStateOf(0f) }
    val isOverRemoveZone = draggingItem != null && (dragStartTopY + dragDelta.y) < removeZoneHeightPx * 0.85f

    // Edge-hover page flip while dragging an item
    var edgeHoverDir by remember { mutableIntStateOf(0) } // -1 left, 1 right, 0 none
    LaunchedEffect(edgeHoverDir, draggingItem?.id) {
        if (draggingItem != null && edgeHoverDir != 0) {
            delay(500L)
            val target = (pagerState.currentPage + edgeHoverDir).coerceIn(0, totalPages - 1)
            if (target != pagerState.currentPage) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pagerState.animateScrollToPage(target)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            // Swipe-up anywhere (unconsumed by inner LazyColumns) to open App Drawer
            .pointerInput(settings.swipeUpForDrawer, draggingItem != null, activeResizeEdge != null) {
                if (!settings.swipeUpForDrawer || draggingItem != null || activeResizeEdge != null) {
                    return@pointerInput
                }
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                        val startPos = down.position
                        var triggered = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            if (!change.isConsumed && !triggered) {
                                val dx = change.position.x - startPos.x
                                val dy = change.position.y - startPos.y
                                if (dy < -110f && abs(dy) > abs(dx) * 1.35f) {
                                    triggered = true
                                    change.consume()
                                    onSwipeUpDrawer()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // Top Bar Area: Either Remove Drop Zone (when dragging) OR subtle Edit/Status strip
        Box(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.animation.AnimatedVisibility(
                visible = draggingItem != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.zIndex(10f)
            ) {
                val danger = MaterialTheme.colorScheme.error
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                        .background(
                            if (isOverRemoveZone) danger.copy(alpha = 0.9f)
                            else tokens.surface.copy(alpha = 0.95f)
                        )
                        .border(
                            width = if (isOverRemoveZone) 2.dp else 1.dp,
                            color = if (isOverRemoveZone) danger else tokens.border,
                            shape = RoundedCornerShape(tokens.cardCornerRadius)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove item",
                        tint = if (isOverRemoveZone) tokens.background else danger,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isOverRemoveZone) "Release to remove" else "Drag here to remove",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isOverRemoveZone) tokens.background else tokens.textPrimary
                    )
                }
            }

            if (draggingItem == null && (isEditMode || selectedItemId != null || settings.showTopStatusPill)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = settings.gridHorizontalMarginDp.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            selectedItemId != null -> "Drag handles to resize · Hold to move"
                            isEditMode -> "Edit grid · tap cell to add · hold item to move"
                            else -> "~/workspace"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedItemId != null || isEditMode) tokens.accent else tokens.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedItemId != null || isEditMode) {
                        TuiCliLink(
                            label = "done",
                            onClick = {
                                selectedItemId = null
                                if (isEditMode) onToggleEditMode()
                            }
                        )
                    }
                }
            }
        }

        // Main Swipeable Multi-Page Launcher Grid
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = draggingItem == null && activeResizeEdge == null,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { pagerPixelSize = it }
                .testTag("home_horizontal_pager")
        ) { pageIndex ->
            val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
            val transitionModifier = when (settings.pageTransitionStyle) {
                PageTransitionStyle.SLIDE -> Modifier
                PageTransitionStyle.FADE -> Modifier.alpha((1f - pageOffset * 0.85f).coerceIn(0.15f, 1f))
                PageTransitionStyle.SCALE -> Modifier.graphicsLayer {
                    val s = (1f - pageOffset * 0.12f).coerceIn(0.85f, 1f)
                    scaleX = s
                    scaleY = s
                    alpha = (1f - pageOffset * 0.5f).coerceIn(0.3f, 1f)
                }
                PageTransitionStyle.TERMINAL_SNAP -> Modifier.alpha(if (pageOffset < 0.35f) 1f else 0.25f)
            }

            val pageItems = remember(homeItems, pageIndex) {
                homeItems.filter { it.page == pageIndex }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .then(transitionModifier)
                    .padding(horizontal = settings.gridHorizontalMarginDp.dp, vertical = 6.dp)
                    .pointerInput(settings.doubleTapAction, selectedItemId) {
                        detectTapGestures(
                            onTap = {
                                if (selectedItemId != null) selectedItemId = null
                            },
                            onDoubleTap = {
                                if (selectedItemId == null) onDoubleTapEmpty()
                            }
                        )
                    }
            ) {
                val cols = settings.gridColumns.coerceIn(3, 7)
                val rows = settings.gridRows.coerceIn(4, 8)
                val gap = settings.gridCellGapDp.dp
                val cellWidth: Dp = (maxWidth - gap * (cols - 1)) / cols
                val cellHeight: Dp = (maxHeight - gap * (rows - 1)) / rows
                val cellWidthPx = with(density) { cellWidth.toPx() }
                val cellHeightPx = with(density) { cellHeight.toPx() }
                val gapPx = with(density) { gap.toPx() }
                val stepXPx = (cellWidthPx + gapPx).coerceAtLeast(1f)
                val stepYPx = (cellHeightPx + gapPx).coerceAtLeast(1f)

                // Empty cell grid targets
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val occupied = pageItems.any { item ->
                            c in item.col until (item.col + item.colSpan) &&
                                r in item.row until (item.row + item.rowSpan)
                        }
                        if (!occupied) {
                            val xOffset = (cellWidth + gap) * c
                            val yOffset = (cellHeight + gap) * r
                            val showOutline = isEditMode || settings.showGridCellOutlines || draggingItem != null
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .offset(x = xOffset, y = yOffset)
                                    .size(width = cellWidth, height = cellHeight)
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .then(
                                        if (showOutline) {
                                            Modifier
                                                .border(
                                                    width = 0.75.dp,
                                                    color = tokens.border.copy(
                                                        alpha = if (isEditMode || draggingItem != null) 0.75f else 0.35f
                                                    ),
                                                    shape = RoundedCornerShape(tokens.cardCornerRadius)
                                                )
                                                .background(
                                                    tokens.surface.copy(alpha = if (isEditMode) 0.22f else 0.08f)
                                                )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedItemId != null) {
                                                selectedItemId = null
                                            } else if (isEditMode) {
                                                onEmptyCellAction(pageIndex, c, r)
                                            }
                                        },
                                        onLongClick = {
                                            if (!settings.lockHomeLayout) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onEmptyCellAction(pageIndex, c, r)
                                            }
                                        }
                                    )
                            ) {
                                if (isEditMode) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add item to cell",
                                        tint = tokens.textMuted.copy(alpha = 0.65f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Live Drag Target Ghost Outline
                val activeDrag = draggingItem
                if (activeDrag != null && !isOverRemoveZone && pagerState.currentPage == pageIndex) {
                    val spanCol = activeDrag.colSpan.coerceIn(1, cols)
                    val spanRow = activeDrag.rowSpan.coerceIn(1, rows)
                    val origXPx = activeDrag.col * stepXPx
                    val origYPx = activeDrag.row * stepYPx
                    val targetCol = ((origXPx + dragDelta.x) / stepXPx).roundToInt()
                        .coerceIn(0, (cols - spanCol).coerceAtLeast(0))
                    val targetRow = ((origYPx + dragDelta.y) / stepYPx).roundToInt()
                        .coerceIn(0, (rows - spanRow).coerceAtLeast(0))

                    val isFree = !collidesWithOthers(
                        pageItems, targetCol, targetRow, spanCol, spanRow, activeDrag.id
                    )
                    val ghostColor = if (isFree) tokens.accent else MaterialTheme.colorScheme.error
                    Box(
                        modifier = Modifier
                            .offset(x = (cellWidth + gap) * targetCol, y = (cellHeight + gap) * targetRow)
                            .size(
                                width = cellWidth * spanCol + gap * (spanCol - 1),
                                height = cellHeight * spanRow + gap * (spanRow - 1)
                            )
                            .zIndex(5f)
                            .background(
                                ghostColor.copy(alpha = 0.18f),
                                RoundedCornerShape(tokens.cardCornerRadius)
                            )
                            .border(
                                2.dp,
                                ghostColor,
                                RoundedCornerShape(tokens.cardCornerRadius)
                            )
                    )
                }

                // Live Resize Target Ghost Outline
                val currentResize = resizePreview
                val resizedOwner = pageItems.firstOrNull { it.id == selectedItemId }
                if (currentResize != null && resizedOwner != null) {
                    val isFree = !collidesWithOthers(
                        pageItems,
                        currentResize.col,
                        currentResize.row,
                        currentResize.colSpan,
                        currentResize.rowSpan,
                        resizedOwner.id
                    )
                    val ghostColor = if (isFree) tokens.accent else MaterialTheme.colorScheme.error
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (cellWidth + gap) * currentResize.col,
                                y = (cellHeight + gap) * currentResize.row
                            )
                            .size(
                                width = cellWidth * currentResize.colSpan + gap * (currentResize.colSpan - 1),
                                height = cellHeight * currentResize.rowSpan + gap * (currentResize.rowSpan - 1)
                            )
                            .zIndex(8f)
                            .background(
                                ghostColor.copy(alpha = 0.16f),
                                RoundedCornerShape(tokens.cardCornerRadius)
                            )
                            .border(2.dp, ghostColor, RoundedCornerShape(tokens.cardCornerRadius))
                    )
                }

                // Placed Items (Apps, Folders, Built-in TUI Widgets, Android System Widgets)
                pageItems.forEach { item ->
                    val isBeingDragged = draggingItem?.id == item.id
                    val isSelected = selectedItemId == item.id
                    val safeCol = item.col.coerceIn(0, cols - 1)
                    val safeRow = item.row.coerceIn(0, rows - 1)
                    val safeColSpan = item.colSpan.coerceIn(1, cols - safeCol)
                    val safeRowSpan = item.rowSpan.coerceIn(1, rows - safeRow)

                    val xOffset = (cellWidth + gap) * safeCol
                    val yOffset = (cellHeight + gap) * safeRow
                    val itemWidth = cellWidth * safeColSpan + gap * (safeColSpan - 1)
                    val itemHeight = cellHeight * safeRowSpan + gap * (safeRowSpan - 1)

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .then(
                                if (isBeingDragged) {
                                    Modifier.offset {
                                        IntOffset(dragDelta.x.roundToInt(), dragDelta.y.roundToInt())
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .size(width = itemWidth, height = itemHeight)
                            .zIndex(if (isBeingDragged) 20f else if (isSelected) 12f else 1f)
                            .graphicsLayer {
                                if (isBeingDragged) {
                                    scaleX = 0.96f
                                    scaleY = 0.96f
                                    alpha = if (isOverRemoveZone) 0.55f else 0.92f
                                }
                            }
                            // Catch long-press anywhere over the widget/app/folder BEFORE child clickables!
                            .pointerInput(item.id, settings.lockHomeLayout, isSelected, cols, rows) {
                                if (settings.lockHomeLayout) return@pointerInput
                                val longPressMs = viewConfiguration.longPressTimeoutMillis
                                val touchSlop = viewConfiguration.touchSlop

                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                        val startPos = down.position
                                        var longPressed = false

                                        val timerJob = scope.launch {
                                            delay(longPressMs)
                                            longPressed = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggingItem = item
                                            dragStartTopY = with(density) { yOffset.toPx() }
                                            dragDelta = Offset.Zero
                                            dragTotalDistance = 0f
                                            selectedItemId = item.id
                                        }

                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val change = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null || !change.pressed) {
                                                timerJob.cancel()
                                                if (longPressed && currentDragItem?.id == item.id) {
                                                    val dist = dragTotalDistance
                                                    val dropDelta = dragDelta
                                                    val removeNow = (dragStartTopY + dropDelta.y) < removeZoneHeightPx * 0.85f
                                                    val targetPage = pagerState.currentPage
                                                    draggingItem = null
                                                    dragDelta = Offset.Zero
                                                    edgeHoverDir = 0

                                                    if (removeNow) {
                                                        selectedItemId = null
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onRemoveItem(item)
                                                    } else if (dist > with(density) { 24.dp.toPx() } || targetPage != item.page) {
                                                        val targetCol = ((safeCol * stepXPx + dropDelta.x) / stepXPx)
                                                            .roundToInt()
                                                            .coerceIn(0, (cols - safeColSpan).coerceAtLeast(0))
                                                        val targetRow = ((safeRow * stepYPx + dropDelta.y) / stepYPx)
                                                            .roundToInt()
                                                            .coerceIn(0, (rows - safeRowSpan).coerceAtLeast(0))
                                                        onUpdateItemGeometry(
                                                            item,
                                                            targetPage,
                                                            targetCol,
                                                            targetRow,
                                                            safeColSpan,
                                                            safeRowSpan
                                                        )
                                                    }
                                                }
                                                break
                                            }

                                            if (!longPressed) {
                                                val moved = (change.position - startPos).getDistance()
                                                if (moved > touchSlop) {
                                                    timerJob.cancel()
                                                    break
                                                }
                                            } else {
                                                val delta = change.position - change.previousPosition
                                                change.consume()
                                                dragDelta += delta
                                                dragTotalDistance += delta.getDistance()

                                                // Edge page flip detection
                                                val approxCenterX = with(density) { (xOffset + itemWidth / 2).toPx() } + dragDelta.x
                                                val pw = pagerPixelSize.width.toFloat()
                                                edgeHoverDir = when {
                                                    pw > 0f && approxCenterX < pw * 0.1f && pagerState.currentPage > 0 -> -1
                                                    pw > 0f && approxCenterX > pw * 0.9f && pagerState.currentPage < totalPages - 1 -> 1
                                                    else -> 0
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        2.dp,
                                        tokens.accent,
                                        RoundedCornerShape(tokens.cardCornerRadius)
                                    )
                                } else if (isEditMode) {
                                    Modifier.border(
                                        1.dp,
                                        tokens.accent.copy(alpha = 0.6f),
                                        RoundedCornerShape(tokens.cardCornerRadius)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        when (item.type) {
                            HomeItemType.APP -> {
                                val app = appLookup["${item.packageName}/${item.activityName}"]
                                    ?: appByPackage[item.packageName]
                                if (app != null) {
                                    HomeAppCell(
                                        app = app,
                                        settings = settings,
                                        iconPackManager = iconPackManager,
                                        onClick = {
                                            if (isEditMode || isSelected) onLongPressItem(item)
                                            else onLaunchApp(app)
                                        }
                                    )
                                }
                            }

                            HomeItemType.FOLDER -> {
                                HomeFolderCell(
                                    item = item,
                                    allApps = allApps,
                                    settings = settings,
                                    iconPackManager = iconPackManager,
                                    onClick = {
                                        if (isEditMode || isSelected) onLongPressItem(item)
                                        else onOpenFolder(item)
                                    }
                                )
                            }

                            HomeItemType.BUILT_IN_WIDGET -> {
                                val wType = item.builtInWidgetType
                                if (wType != null) {
                                    BuiltInWidgetRenderer(
                                        widgetType = wType,
                                        widgetConfig = item.widgetConfig,
                                        settings = settings,
                                        allApps = allApps,
                                        quickNotes = quickNotes,
                                        mediaState = mediaState,
                                        iconPackManager = iconPackManager,
                                        onLaunchApp = onLaunchApp,
                                        onEditProfile = onEditProfile,
                                        onEditWelcomeQuote = onEditWelcomeQuote,
                                        onPickWidgetPhoto = { onPickWidgetPhoto(item) },
                                        onMediaTogglePlayPause = onMediaTogglePlayPause,
                                        onMediaSkipNext = onMediaSkipNext,
                                        onMediaSkipPrevious = onMediaSkipPrevious,
                                        onOpenMediaPlayer = onOpenMediaPlayer,
                                        onGrantMediaAccess = onGrantMediaAccess,
                                        onAddQuickNote = onAddQuickNote,
                                        onToggleQuickNote = onToggleQuickNote,
                                        onDeleteQuickNote = onDeleteQuickNote,
                                        isInteractive = !isEditMode && !isSelected && !isBeingDragged,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            HomeItemType.SYSTEM_WIDGET -> {
                                SystemAppWidgetView(
                                    appWidgetId = item.appWidgetId,
                                    widgetHostManager = widgetHostManager,
                                    widthDp = itemWidth,
                                    heightDp = itemHeight,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Resize Handles & Floating Quick Actions when item is selected
                        if (isSelected && draggingItem == null) {
                            val canResize = item.type == HomeItemType.BUILT_IN_WIDGET ||
                                item.type == HomeItemType.SYSTEM_WIDGET

                            if (canResize) {
                                // Top Edge Resize Handle
                                ResizeEdgeHandle(
                                    edge = ResizeEdge.TOP,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    onDrag = { totalDelta ->
                                        val dRows = (totalDelta.y / stepYPx).roundToInt()
                                        val newRow = (safeRow + dRows).coerceIn(0, safeRow + safeRowSpan - 1)
                                        val newSpan = (safeRowSpan - (newRow - safeRow)).coerceIn(1, rows - newRow)
                                        activeResizeEdge = ResizeEdge.TOP
                                        resizePreview = ResizePreview(safeCol, newRow, safeColSpan, newSpan)
                                    },
                                    onDragEnd = {
                                        resizePreview?.let { p ->
                                            onUpdateItemGeometry(item, item.page, p.col, p.row, p.colSpan, p.rowSpan)
                                        }
                                        activeResizeEdge = null
                                        resizePreview = null
                                    }
                                )
                                // Bottom Edge Resize Handle
                                ResizeEdgeHandle(
                                    edge = ResizeEdge.BOTTOM,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    onDrag = { totalDelta ->
                                        val dRows = (totalDelta.y / stepYPx).roundToInt()
                                        val newSpan = (safeRowSpan + dRows).coerceIn(1, rows - safeRow)
                                        activeResizeEdge = ResizeEdge.BOTTOM
                                        resizePreview = ResizePreview(safeCol, safeRow, safeColSpan, newSpan)
                                    },
                                    onDragEnd = {
                                        resizePreview?.let { p ->
                                            onUpdateItemGeometry(item, item.page, p.col, p.row, p.colSpan, p.rowSpan)
                                        }
                                        activeResizeEdge = null
                                        resizePreview = null
                                    }
                                )
                                // Left Edge Resize Handle
                                ResizeEdgeHandle(
                                    edge = ResizeEdge.LEFT,
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    onDrag = { totalDelta ->
                                        val dCols = (totalDelta.x / stepXPx).roundToInt()
                                        val newCol = (safeCol + dCols).coerceIn(0, safeCol + safeColSpan - 1)
                                        val newSpan = (safeColSpan - (newCol - safeCol)).coerceIn(1, cols - newCol)
                                        activeResizeEdge = ResizeEdge.LEFT
                                        resizePreview = ResizePreview(newCol, safeRow, newSpan, safeRowSpan)
                                    },
                                    onDragEnd = {
                                        resizePreview?.let { p ->
                                            onUpdateItemGeometry(item, item.page, p.col, p.row, p.colSpan, p.rowSpan)
                                        }
                                        activeResizeEdge = null
                                        resizePreview = null
                                    }
                                )
                                // Right Edge Resize Handle
                                ResizeEdgeHandle(
                                    edge = ResizeEdge.RIGHT,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    onDrag = { totalDelta ->
                                        val dCols = (totalDelta.x / stepXPx).roundToInt()
                                        val newSpan = (safeColSpan + dCols).coerceIn(1, cols - safeCol)
                                        activeResizeEdge = ResizeEdge.RIGHT
                                        resizePreview = ResizePreview(safeCol, safeRow, newSpan, safeRowSpan)
                                    },
                                    onDragEnd = {
                                        resizePreview?.let { p ->
                                            onUpdateItemGeometry(item, item.page, p.col, p.row, p.colSpan, p.rowSpan)
                                        }
                                        activeResizeEdge = null
                                        resizePreview = null
                                    }
                                )
                            }

                            // Floating Action Pill ([edit/photo] [remove] [more] [ok])
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(if (safeRow > 0) Alignment.TopEnd else Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .background(tokens.surface.copy(alpha = 0.96f))
                                    .border(1.dp, tokens.accent, RoundedCornerShape(tokens.cardCornerRadius))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                if (item.type == HomeItemType.APP) {
                                    TuiCliLink(
                                        label = "edit",
                                        onClick = {
                                            selectedItemId = null
                                            onEditAppItem(item)
                                        }
                                    )
                                } else if (item.builtInWidgetType == BuiltInWidgetType.PHOTO_FRAME ||
                                    item.builtInWidgetType == BuiltInWidgetType.PHOTO_FRAME_WIDE
                                ) {
                                    TuiCliLink(
                                        label = "photo",
                                        onClick = {
                                            selectedItemId = null
                                            onPickWidgetPhoto(item)
                                        }
                                    )
                                }
                                TuiCliLink(
                                    label = "remove",
                                    color = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        selectedItemId = null
                                        onRemoveItem(item)
                                    }
                                )
                                TuiCliLink(
                                    label = "more",
                                    onClick = {
                                        selectedItemId = null
                                        onLongPressItem(item)
                                    }
                                )
                                TuiCliLink(
                                    label = "ok",
                                    onClick = { selectedItemId = null }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Minimalist Page Indicator Bar
        if (settings.showPageIndicator) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "~/page ${pagerState.currentPage + 1}/$totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted
                )
                Spacer(Modifier.width(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(totalPages) { index ->
                        val active = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(width = if (active) 16.dp else 6.dp, height = 4.dp)
                                .background(
                                    if (active) tokens.accent else tokens.border,
                                    RoundedCornerShape(tokens.cardCornerRadius)
                                )
                        )
                    }
                }
                if (!settings.lockHomeLayout) {
                    Spacer(Modifier.width(10.dp))
                    TuiCliLink(
                        label = if (isEditMode) "done" else "edit grid",
                        onClick = {
                            selectedItemId = null
                            onToggleEditMode()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResizeEdgeHandle(
    edge: ResizeEdge,
    onDrag: (totalDelta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    val haptic = LocalHapticFeedback.current
    var totalDelta by remember { mutableStateOf(Offset.Zero) }
    val isHorizontal = edge == ResizeEdge.TOP || edge == ResizeEdge.BOTTOM

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .pointerInput(edge) {
                detectDragGestures(
                    onDragStart = {
                        totalDelta = Offset.Zero
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDelta += dragAmount
                        onDrag(totalDelta)
                    },
                    onDragEnd = {
                        totalDelta = Offset.Zero
                        onDragEnd()
                    },
                    onDragCancel = {
                        totalDelta = Offset.Zero
                        onDragEnd()
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = if (isHorizontal) 34.dp else 10.dp,
                    height = if (isHorizontal) 10.dp else 34.dp
                )
                .clip(CircleShape)
                .background(tokens.accent)
                .border(1.5.dp, tokens.background, CircleShape)
        )
    }
}

private fun collidesWithOthers(
    pageItems: List<HomeGridItem>,
    col: Int,
    row: Int,
    colSpan: Int,
    rowSpan: Int,
    excludeId: Long
): Boolean {
    val r1Right = col + colSpan
    val r1Bottom = row + rowSpan
    return pageItems.any { other ->
        if (other.id == excludeId) return@any false
        val r2Right = other.col + other.colSpan
        val r2Bottom = other.row + other.rowSpan
        col < r2Right && r1Right > other.col && row < r2Bottom && r1Bottom > other.row
    }
}

@Composable
private fun HomeAppCell(
    app: AppInfo,
    settings: LauncherSettings,
    iconPackManager: IconPackManager,
    onClick: () -> Unit
) {
    val tokens = LocalLauncherTokens.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(tokens.cardCornerRadius))
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        AppIconView(
            app = app,
            settings = settings,
            iconPackManager = iconPackManager
        )
        if (settings.showIconLabels) {
            Spacer(Modifier.height(4.dp))
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

@Composable
private fun HomeFolderCell(
    item: HomeGridItem,
    allApps: List<AppInfo>,
    settings: LauncherSettings,
    iconPackManager: IconPackManager,
    onClick: () -> Unit
) {
    val tokens = LocalLauncherTokens.current
    val appMap = remember(allApps) { allApps.associateBy { it.componentKey } }
    val previewApps = remember(item.folderAppKeys, appMap) {
        item.folderAppKeys.mapNotNull { appMap[it] }.take(4)
    }
    val iconSize = settings.iconSizeDp.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(tokens.cardCornerRadius))
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                .background(tokens.surface)
                .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                .padding(4.dp)
        ) {
            if (previewApps.isEmpty()) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = item.folderTitle ?: "Folder",
                    tint = tokens.accent
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        previewApps.getOrNull(0)?.let {
                            AppIconView(it, settings, iconPackManager, sizeOverride = (iconSize - 12.dp) / 2)
                        }
                        previewApps.getOrNull(1)?.let {
                            AppIconView(it, settings, iconPackManager, sizeOverride = (iconSize - 12.dp) / 2)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        previewApps.getOrNull(2)?.let {
                            AppIconView(it, settings, iconPackManager, sizeOverride = (iconSize - 12.dp) / 2)
                        }
                        previewApps.getOrNull(3)?.let {
                            AppIconView(it, settings, iconPackManager, sizeOverride = (iconSize - 12.dp) / 2)
                        }
                    }
                }
            }
        }
        if (settings.showIconLabels) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.folderTitle ?: "~/folder",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
