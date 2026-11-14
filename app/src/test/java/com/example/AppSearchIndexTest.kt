package com.example

import com.example.data.AppSearchIndex
import com.example.data.SearchMatchKind
import com.example.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchIndexTest {

    private val sampleApps = listOf(
        AppInfo("com.google.android.youtube", "Main", "YouTube", launchCount = 5),
        AppInfo("com.android.settings", "Settings", "Settings", launchCount = 2),
        AppInfo("com.whatsapp", "Main", "WhatsApp", launchCount = 10),
        AppInfo("com.android.vending", "AssetBrowser", "Play Store", launchCount = 1),
        AppInfo("org.mozilla.firefox", "App", "Firefox Browser", launchCount = 0),
        AppInfo("com.secret.vault", "Main", "Hidden Vault", isHidden = true)
    )

    @Test
    fun indexExcludesHiddenAppsAndCountsVisible() {
        val index = AppSearchIndex.build(sampleApps)
        assertEquals(5, index.indexedCount)
        assertTrue(index.search("vault").isEmpty())
    }

    @Test
    fun prefixAndExactMatchesRankFirst() {
        val index = AppSearchIndex.build(sampleApps)
        val matches = index.search("set")
        assertEquals("Settings", matches.first().app.displayLabel)
        assertEquals(SearchMatchKind.PREFIX, matches.first().kind)
    }

    @Test
    fun acronymMatchesCamelCaseAndMultiWordApps() {
        val index = AppSearchIndex.build(sampleApps)
        val ytMatches = index.search("yt")
        assertEquals("YouTube", ytMatches.first().app.displayLabel)
        assertEquals(SearchMatchKind.ACRONYM, ytMatches.first().kind)

        val psMatches = index.search("ps")
        assertEquals("Play Store", psMatches.first().app.displayLabel)
    }

    @Test
    fun packageAndFuzzyMatchesWork() {
        val index = AppSearchIndex.build(sampleApps)
        val pkgMatches = index.search("mozilla")
        assertEquals("Firefox Browser", pkgMatches.first().app.displayLabel)
        assertEquals(SearchMatchKind.PACKAGE, pkgMatches.first().kind)

        val fuzzyMatches = index.search("sttg")
        assertFalse(fuzzyMatches.isEmpty())
        assertEquals("Settings", fuzzyMatches.first().app.displayLabel)
    }

    @Test
    fun blankQueryPrioritizesFrequentApps() {
        val index = AppSearchIndex.build(sampleApps)
        val recent = index.search("")
        assertEquals("WhatsApp", recent.first().app.displayLabel)
        assertEquals(SearchMatchKind.FREQUENT, recent.first().kind)
    }
}
