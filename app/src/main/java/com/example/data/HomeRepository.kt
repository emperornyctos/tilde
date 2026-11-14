package com.example.data

import com.example.model.AppInfo
import com.example.model.BuiltInWidgetType
import com.example.model.HomeGridItem
import com.example.model.HomeItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HomeRepository(
    private val homeItemDao: HomeItemDao,
    private val quickNoteDao: QuickNoteDao
) {
    val homeItemsFlow: Flow<List<HomeGridItem>> = homeItemDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    val quickNotesFlow: Flow<List<QuickNoteEntity>> = quickNoteDao.observeAll()

    suspend fun seedDefaultsIfEmpty(installedApps: List<AppInfo>, cols: Int, rows: Int) =
        withContext(Dispatchers.IO) {
            if (homeItemDao.count() > 0) return@withContext

            val safeCols = cols.coerceAtLeast(4)
            val safeRows = rows.coerceAtLeast(5)
            val defaults = mutableListOf<HomeItemEntity>()

            // Page 0: Signature TUI Home Layout inspired directly by the screenshot
            defaults += HomeItemEntity(
                page = 0,
                col = 0,
                row = 0,
                colSpan = safeCols,
                rowSpan = 3.coerceAtMost(safeRows - 2),
                type = HomeItemType.BUILT_IN_WIDGET.name,
                builtInWidgetType = BuiltInWidgetType.PROFILE_CARD.name
            )

            val quoteRow = 3.coerceAtMost(safeRows - 2)
            defaults += HomeItemEntity(
                page = 0,
                col = 0,
                row = quoteRow,
                colSpan = safeCols,
                rowSpan = 1,
                type = HomeItemType.BUILT_IN_WIDGET.name,
                builtInWidgetType = BuiltInWidgetType.WELCOME_QUOTE.name
            )

            val appsRow = (quoteRow + 1).coerceAtMost(safeRows - 1)
            val topApps = installedApps.take(safeCols)
            topApps.forEachIndexed { index, app ->
                defaults += HomeItemEntity(
                    page = 0,
                    col = index,
                    row = appsRow,
                    colSpan = 1,
                    rowSpan = 1,
                    type = HomeItemType.APP.name,
                    packageName = app.packageName,
                    activityName = app.activityName
                )
            }

            // Page 1: Productivity & Telemetry Workspace
            defaults += HomeItemEntity(
                page = 1,
                col = 0,
                row = 0,
                colSpan = safeCols,
                rowSpan = 1,
                type = HomeItemType.BUILT_IN_WIDGET.name,
                builtInWidgetType = BuiltInWidgetType.TERMINAL_CLOCK.name
            )
            defaults += HomeItemEntity(
                page = 1,
                col = 0,
                row = 1,
                colSpan = safeCols,
                rowSpan = 2.coerceAtMost(safeRows - 2),
                type = HomeItemType.BUILT_IN_WIDGET.name,
                builtInWidgetType = BuiltInWidgetType.MOST_USED_APPS.name
            )
            if (safeRows >= 5) {
                defaults += HomeItemEntity(
                    page = 1,
                    col = 0,
                    row = 3,
                    colSpan = safeCols,
                    rowSpan = 2,
                    type = HomeItemType.BUILT_IN_WIDGET.name,
                    builtInWidgetType = BuiltInWidgetType.SYSTEM_TELEMETRY.name
                )
            }

            homeItemDao.insertAll(defaults)

            // Seed initial quick notes
            quickNoteDao.insert(QuickNoteEntity(text = "Long-press any widget or cell to customize"))
            quickNoteDao.insert(QuickNoteEntity(text = "Open ~/settings or App Drawer > Settings to theme"))
        }

    suspend fun findFirstOpenSlot(
        page: Int,
        colSpan: Int,
        rowSpan: Int,
        gridCols: Int,
        gridRows: Int
    ): Pair<Int, Pair<Int, Int>> = withContext(Dispatchers.IO) {
        val items = homeItemDao.getAllOnce().map { it.toDomain() }
        val maxPage = (items.maxOfOrNull { it.page } ?: 0).coerceAtLeast(page)
        val clampedColSpan = colSpan.coerceIn(1, gridCols)
        val clampedRowSpan = rowSpan.coerceIn(1, gridRows)

        for (p in page..(maxPage + 1)) {
            val pageItems = items.filter { it.page == p }
            for (r in 0..(gridRows - clampedRowSpan)) {
                for (c in 0..(gridCols - clampedColSpan)) {
                    if (!collides(pageItems, c, r, clampedColSpan, clampedRowSpan, excludeId = -1L)) {
                        return@withContext p to (c to r)
                    }
                }
            }
        }
        (maxPage + 1) to (0 to 0)
    }

    suspend fun addItem(item: HomeGridItem): Long = withContext(Dispatchers.IO) {
        homeItemDao.insert(item.toEntity())
    }

    suspend fun updateItem(item: HomeGridItem) = withContext(Dispatchers.IO) {
        homeItemDao.update(item.toEntity())
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.IO) {
        homeItemDao.deleteById(id)
    }

    suspend fun canPlaceAt(
        page: Int,
        col: Int,
        row: Int,
        colSpan: Int,
        rowSpan: Int,
        gridCols: Int,
        gridRows: Int,
        excludeId: Long
    ): Boolean = withContext(Dispatchers.IO) {
        if (col < 0 || row < 0 || col + colSpan > gridCols || row + rowSpan > gridRows) {
            return@withContext false
        }
        val pageItems = homeItemDao.getAllOnce().map { it.toDomain() }.filter { it.page == page }
        !collides(pageItems, col, row, colSpan, rowSpan, excludeId)
    }

    private fun collides(
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

    suspend fun addQuickNote(text: String) = withContext(Dispatchers.IO) {
        if (text.isNotBlank()) {
            quickNoteDao.insert(QuickNoteEntity(text = text.trim()))
        }
    }

    suspend fun toggleQuickNote(note: QuickNoteEntity) = withContext(Dispatchers.IO) {
        quickNoteDao.update(note.copy(isDone = !note.isDone))
    }

    suspend fun deleteQuickNote(id: Long) = withContext(Dispatchers.IO) {
        quickNoteDao.deleteById(id)
    }

    suspend fun resetHomeLayout(installedApps: List<AppInfo>, cols: Int, rows: Int) =
        withContext(Dispatchers.IO) {
            homeItemDao.deleteAll()
            seedDefaultsIfEmpty(installedApps, cols, rows)
        }

    suspend fun getItemById(id: Long): HomeGridItem? = withContext(Dispatchers.IO) {
        homeItemDao.getAllOnce().firstOrNull { it.id == id }?.toDomain()
    }

    suspend fun updateWidgetConfig(itemId: Long, config: String?) = withContext(Dispatchers.IO) {
        val current = homeItemDao.getAllOnce().firstOrNull { it.id == itemId } ?: return@withContext
        homeItemDao.update(current.copy(widgetConfig = config))
    }

    private fun HomeItemEntity.toDomain(): HomeGridItem = HomeGridItem(
        id = id,
        page = page,
        col = col,
        row = row,
        colSpan = colSpan,
        rowSpan = rowSpan,
        type = runCatching { HomeItemType.valueOf(type) }.getOrDefault(HomeItemType.APP),
        packageName = packageName,
        activityName = activityName,
        folderTitle = folderTitle,
        folderAppKeys = folderAppsCsv.split("|").filter { it.isNotBlank() },
        builtInWidgetType = builtInWidgetType?.let {
            runCatching { BuiltInWidgetType.valueOf(it) }.getOrNull()
        },
        appWidgetId = appWidgetId,
        widgetConfig = widgetConfig
    )

    private fun HomeGridItem.toEntity(): HomeItemEntity = HomeItemEntity(
        id = id,
        page = page,
        col = col,
        row = row,
        colSpan = colSpan,
        rowSpan = rowSpan,
        type = type.name,
        packageName = packageName,
        activityName = activityName,
        folderTitle = folderTitle,
        folderAppsCsv = folderAppKeys.joinToString("|"),
        builtInWidgetType = builtInWidgetType?.name,
        appWidgetId = appWidgetId,
        widgetConfig = widgetConfig
    )
}
