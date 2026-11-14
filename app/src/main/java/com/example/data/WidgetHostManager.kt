package com.example.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.model.SystemWidgetGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetHostManager(private val context: Context) {

    companion object {
        const val APPWIDGET_HOST_ID = 1024
    }

    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(context.applicationContext, APPWIDGET_HOST_ID)

    fun startListening() {
        runCatching { appWidgetHost.startListening() }
    }

    fun stopListening() {
        runCatching { appWidgetHost.stopListening() }
    }

    fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun deleteAppWidgetId(appWidgetId: Int) {
        if (appWidgetId > 0) {
            runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
        }
    }

    fun tryBindWidget(appWidgetId: Int, info: AppWidgetProviderInfo): Boolean {
        return runCatching {
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
        }.getOrDefault(false)
    }

    fun createBindIntent(appWidgetId: Int, info: AppWidgetProviderInfo): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
        }
    }

    fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        if (appWidgetId <= 0) return null
        return runCatching { appWidgetManager.getAppWidgetInfo(appWidgetId) }.getOrNull()
    }

    fun createHostView(appWidgetId: Int): AppWidgetHostView? {
        val info = getAppWidgetInfo(appWidgetId) ?: return null
        return runCatching {
            appWidgetHost.createView(context, appWidgetId, info).apply {
                setAppWidget(appWidgetId, info)
            }
        }.getOrNull()
    }

    suspend fun getInstalledSystemWidgets(): List<SystemWidgetGroup> = withContext(Dispatchers.IO) {
        val pm: PackageManager = context.packageManager
        val providers = runCatching { appWidgetManager.installedProviders }.getOrDefault(emptyList())
        providers
            .groupBy { it.provider.packageName }
            .map { (pkg, widgets) ->
                val appLabel = runCatching {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                }.getOrDefault(pkg)
                SystemWidgetGroup(
                    appName = appLabel,
                    packageName = pkg,
                    widgets = widgets.sortedBy { it.loadLabel(pm).lowercase() }
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
}
