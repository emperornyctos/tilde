package com.example.data

import com.example.model.AppInfo

enum class SearchMatchKind(val badge: String) {
    EXACT("exact"),
    PREFIX("prefix"),
    WORD("word"),
    ACRONYM("abbr"),
    SUBSTRING("match"),
    PACKAGE("pkg"),
    FUZZY("fuzzy"),
    FREQUENT("frequent"),
    INDEXED("indexed")
}

data class IndexedSearchMatch(
    val app: AppInfo,
    val score: Int,
    val kind: SearchMatchKind
)

data class TerminalCommand(
    val command: String,
    val description: String
)

private data class IndexedAppEntry(
    val app: AppInfo,
    val labelLower: String,
    val originalLower: String,
    val words: List<String>,
    val acronym: String,
    val packageTokens: List<String>
)

class AppSearchIndex private constructor(
    private val entries: List<IndexedAppEntry>
) {
    val indexedCount: Int get() = entries.size

    fun search(query: String, limit: Int = 18): List<IndexedSearchMatch> {
        val raw = query.trim().lowercase()
        if (raw.isEmpty()) {
            return entries
                .sortedWith(
                    compareByDescending<IndexedAppEntry> { it.app.launchCount }
                        .thenBy { it.labelLower }
                )
                .take(limit.coerceAtMost(8))
                .map { entry ->
                    IndexedSearchMatch(
                        app = entry.app,
                        score = entry.app.launchCount,
                        kind = if (entry.app.launchCount > 0) SearchMatchKind.FREQUENT else SearchMatchKind.INDEXED
                    )
                }
        }

        val results = ArrayList<IndexedSearchMatch>(entries.size.coerceAtMost(32))
        for (entry in entries) {
            val match = scoreEntry(entry, raw)
            if (match != null) {
                results.add(match)
            }
        }

        results.sortWith(
            compareByDescending<IndexedSearchMatch> { it.score }
                .thenByDescending { it.app.launchCount }
                .thenBy { it.app.displayLabel.length }
                .thenBy { it.app.displayLabel.lowercase() }
        )
        return results.take(limit)
    }

    private fun scoreEntry(entry: IndexedAppEntry, q: String): IndexedSearchMatch? {
        val usageBonus = (entry.app.launchCount * 4).coerceAtMost(80)

        if (entry.labelLower == q || entry.originalLower == q) {
            return IndexedSearchMatch(entry.app, 1000 + usageBonus, SearchMatchKind.EXACT)
        }
        if (entry.labelLower.startsWith(q) || entry.originalLower.startsWith(q)) {
            return IndexedSearchMatch(entry.app, 860 + usageBonus, SearchMatchKind.PREFIX)
        }
        if (entry.words.any { it.startsWith(q) }) {
            return IndexedSearchMatch(entry.app, 720 + usageBonus, SearchMatchKind.WORD)
        }
        if (q.length >= 2 && entry.acronym.startsWith(q)) {
            return IndexedSearchMatch(entry.app, 620 + usageBonus, SearchMatchKind.ACRONYM)
        }
        if (entry.labelLower.contains(q) || entry.originalLower.contains(q)) {
            return IndexedSearchMatch(entry.app, 520 + usageBonus, SearchMatchKind.SUBSTRING)
        }
        if (entry.packageTokens.any { it.startsWith(q) || it.contains(q) } ||
            entry.app.packageName.lowercase().contains(q)
        ) {
            return IndexedSearchMatch(entry.app, 360 + usageBonus, SearchMatchKind.PACKAGE)
        }
        if (q.length >= 2 && isSubsequence(q, entry.labelLower)) {
            return IndexedSearchMatch(entry.app, 220 + usageBonus, SearchMatchKind.FUZZY)
        }
        return null
    }

    companion object {
        val TERMINAL_COMMANDS = listOf(
            TerminalCommand(">home", "Jump to home workspace"),
            TerminalCommand(">apps", "Open app drawer"),
            TerminalCommand(">widgets", "Browse built-in & system widgets"),
            TerminalCommand(">settings", "Open customization studio"),
            TerminalCommand(">edit", "Toggle home grid edit mode"),
            TerminalCommand(">zen", "Toggle distraction-free zen mode")
        )

        private val IGNORED_PKG_TOKENS = setOf("com", "org", "net", "io", "app", "android", "google")

        fun build(apps: List<AppInfo>): AppSearchIndex {
            val entries = apps
                .asSequence()
                .filter { !it.isHidden }
                .map { app ->
                    val label = app.displayLabel
                    val labelLower = label.lowercase()
                    val originalLower = app.label.lowercase()
                    val words = tokenizeWords(label)
                    val acronym = buildString {
                        words.forEach { w ->
                            val c = w.firstOrNull()
                            if (c != null && c.isLetterOrDigit()) append(c)
                        }
                    }
                    val pkgTokens = app.packageName
                        .lowercase()
                        .split('.', '_', '-')
                        .filter { it.isNotBlank() && it !in IGNORED_PKG_TOKENS }

                    IndexedAppEntry(
                        app = app,
                        labelLower = labelLower,
                        originalLower = originalLower,
                        words = words,
                        acronym = acronym,
                        packageTokens = pkgTokens
                    )
                }
                .toList()
            return AppSearchIndex(entries)
        }

        private fun tokenizeWords(label: String): List<String> {
            val tokens = mutableListOf<String>()
            val current = StringBuilder()
            for (i in label.indices) {
                val ch = label[i]
                if (!ch.isLetterOrDigit()) {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString().lowercase())
                        current.clear()
                    }
                } else {
                    if (current.isNotEmpty() && ch.isUpperCase() && !label[i - 1].isUpperCase()) {
                        tokens.add(current.toString().lowercase())
                        current.clear()
                    }
                    current.append(ch)
                }
            }
            if (current.isNotEmpty()) {
                tokens.add(current.toString().lowercase())
            }
            return tokens
        }

        private fun isSubsequence(needle: String, haystack: String): Boolean {
            var ni = 0
            var hi = 0
            while (ni < needle.length && hi < haystack.length) {
                if (needle[ni] == haystack[hi]) ni++
                hi++
            }
            return ni == needle.length
        }
    }
}
