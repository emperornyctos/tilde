package com.example.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val appCustomizationDao: AppCustomizationDao
) {
    private val packageManager: PackageManager = context.packageManager
    private val rawInstalledApps = MutableStateFlow<List<AppInfo>>(emptyList())

    val allAppsFlow: Flow<List<AppInfo>> = combine(
        rawInstalledApps,
        appCustomizationDao.observeAll()
    ) { installed, customizations ->
        val customMap = customizations.associateBy { it.componentKey }
        installed.map { app ->
            val custom = customMap[app.componentKey]
            if (custom != null) {
                app.copy(
                    customLabel = custom.customLabel,
                    customIconUri = custom.customIconUri,
                    customPackPackage = custom.customPackPackage,
                    customPackDrawable = custom.customPackDrawable,
                    isHidden = custom.isHidden,
                    launchCount = custom.launchCount
                )
            } else {
                app
            }
        }
    }

    suspend fun refreshInstalledApps() = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        val selfPackage = context.packageName
        val apps = resolveInfos
            .filter { it.activityInfo.packageName != selfPackage }
            .map { info ->
                val pkg = info.activityInfo.packageName
                val cls = info.activityInfo.name
                val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                    .ifEmpty { pkg }
                val icon = runCatching { info.loadIcon(packageManager) }.getOrNull()
                val installTime = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getPackageInfo(
                            pkg,
                            PackageManager.PackageInfoFlags.of(0)
                        ).firstInstallTime
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(pkg, 0).firstInstallTime
                    }
                }.getOrDefault(0L)

                AppInfo(
                    packageName = pkg,
                    activityName = cls,
                    label = label,
                    installTime = installTime,
                    systemIcon = icon
                )
            }
            .distinctBy { it.componentKey }
            .sortedBy { it.label.lowercase() }

        rawInstalledApps.value = apps
    }

    suspend fun launchApp(app: AppInfo): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        val launched = runCatching {
            context.startActivity(intent)
            true
        }.getOrElse {
            val fallback = packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (fallback != null) {
                runCatching {
                    context.startActivity(fallback)
                    true
                }.getOrDefault(false)
            } else {
                false
            }
        }
        if (launched) {
            withContext(Dispatchers.IO) {
                val existing = appCustomizationDao.getByKey(app.componentKey)
                if (existing == null) {
                    appCustomizationDao.upsert(
                        AppCustomizationEntity(
                            componentKey = app.componentKey,
                            launchCount = 1
                        )
                    )
                } else {
                    appCustomizationDao.incrementLaunchCount(app.componentKey)
                }
            }
        }
        return launched
    }

    fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    suspend fun updateAppCustomization(
        componentKey: String,
        customLabel: String? = null,
        customIconUri: String? = null,
        customPackPackage: String? = null,
        customPackDrawable: String? = null,
        isHidden: Boolean? = null,
        clearCustomIcon: Boolean = false,
        replaceIconSource: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val current = appCustomizationDao.getByKey(componentKey) ?: AppCustomizationEntity(componentKey)
        val updated = current.copy(
            customLabel = customLabel ?: current.customLabel,
            customIconUri = when {
                clearCustomIcon -> null
                replaceIconSource -> customIconUri
                else -> customIconUri ?: current.customIconUri
            },
            customPackPackage = when {
                clearCustomIcon -> null
                replaceIconSource -> customPackPackage
                else -> customPackPackage ?: current.customPackPackage
            },
            customPackDrawable = when {
                clearCustomIcon -> null
                replaceIconSource -> customPackDrawable
                else -> customPackDrawable ?: current.customPackDrawable
            },
            isHidden = isHidden ?: current.isHidden
        )
        appCustomizationDao.upsert(updated)
    }

    suspend fun resetAppLabel(componentKey: String) = withContext(Dispatchers.IO) {
        val current = appCustomizationDao.getByKey(componentKey) ?: return@withContext
        appCustomizationDao.upsert(current.copy(customLabel = null))
    }

    fun registerPackageReceiver(onChanged: () -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                onChanged()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        return receiver
    }
}
