package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.model.BuiltInIconStyle
import com.example.model.IconPackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class IconPackManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val packFilterCache = mutableMapOf<String, Map<String, String>>()
    private val packResourcesCache = mutableMapOf<String, Resources>()

    private val themeActions = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME"
    )

    suspend fun getAvailableIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val builtIn = BuiltInIconStyle.entries.map { style ->
            IconPackInfo(
                id = style.id,
                packageName = null,
                label = style.label,
                description = style.description,
                isBuiltIn = true,
                builtInStyle = style
            )
        }

        val discovered = mutableMapOf<String, IconPackInfo>()
        for (action in themeActions) {
            val intent = Intent(action)
            val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }
            for (info in infos) {
                val pkg = info.activityInfo.packageName
                if (!discovered.containsKey(pkg) && pkg != context.packageName) {
                    val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                        .ifEmpty { pkg }
                    discovered[pkg] = IconPackInfo(
                        id = "pack:$pkg",
                        packageName = pkg,
                        label = label,
                        description = "Installed icon pack ($pkg)",
                        isBuiltIn = false
                    )
                }
            }
        }
        builtIn + discovered.values.sortedBy { it.label.lowercase() }
    }

    suspend fun loadPackMapping(packPackage: String): Map<String, String> = withContext(Dispatchers.IO) {
        packFilterCache[packPackage]?.let { return@withContext it }
        val mapping = mutableMapOf<String, String>()
        val res = getPackResources(packPackage) ?: return@withContext emptyMap()

        runCatching {
            @SuppressLint("DiscouragedApi")
            val xmlId = res.getIdentifier("appfilter", "xml", packPackage)
            if (xmlId != 0) {
                parseAppFilter(res.getXml(xmlId), mapping)
            } else {
                val stream: InputStream? = runCatching { res.assets.open("appfilter.xml") }.getOrNull()
                stream?.use { input ->
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(input, "UTF-8")
                    parseAppFilter(parser, mapping)
                }
            }
        }
        packFilterCache[packPackage] = mapping
        mapping
    }

    private fun parseAppFilter(parser: XmlPullParser, out: MutableMap<String, String>) {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    // Format: ComponentInfo{pkg/cls}
                    val cleaned = component
                        .removePrefix("ComponentInfo{")
                        .removeSuffix("}")
                    out[cleaned] = drawable
                }
            }
            eventType = parser.next()
        }
    }

    @SuppressLint("DiscouragedApi", "UseCompatLoadingForDrawables")
    suspend fun loadDrawableFromPack(
        packPackage: String,
        componentKey: String
    ): Drawable? = withContext(Dispatchers.IO) {
        val mapping = loadPackMapping(packPackage)
        val drawableName = mapping[componentKey] ?: return@withContext null
        loadNamedDrawableFromPack(packPackage, drawableName)
    }

    @SuppressLint("DiscouragedApi", "UseCompatLoadingForDrawables")
    suspend fun loadNamedDrawableFromPack(
        packPackage: String,
        drawableName: String
    ): Drawable? = withContext(Dispatchers.IO) {
        val res = getPackResources(packPackage) ?: return@withContext null
        val id = res.getIdentifier(drawableName, "drawable", packPackage)
        if (id != 0) {
            runCatching { res.getDrawable(id, null) }.getOrNull()
        } else {
            null
        }
    }

    suspend fun getSampleDrawableNames(packPackage: String, limit: Int = 500): List<String> =
        withContext(Dispatchers.IO) {
            val mapping = loadPackMapping(packPackage)
            val fromFilter = mapping.values.toMutableSet()
            val res = getPackResources(packPackage)
            if (res != null) {
                runCatching {
                    @SuppressLint("DiscouragedApi")
                    val drawableXmlId = res.getIdentifier("drawable", "xml", packPackage)
                    if (drawableXmlId != 0) {
                        val parser = res.getXml(drawableXmlId)
                        var eventType = parser.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                                val d = parser.getAttributeValue(null, "drawable")
                                if (!d.isNullOrBlank()) fromFilter.add(d)
                            }
                            eventType = parser.next()
                        }
                    }
                }
            }
            fromFilter.sorted().take(limit)
        }

    private fun getPackResources(packPackage: String): Resources? {
        packResourcesCache[packPackage]?.let { return it }
        return runCatching {
            packageManager.getResourcesForApplication(packPackage).also {
                packResourcesCache[packPackage] = it
            }
        }.getOrNull()
    }
}
