package com.argus.agi.platform.android.capture

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val isExcluded: Boolean
)

class ExcludedAppsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("argus_excluded_apps", Context.MODE_PRIVATE)
    private val _excludedPackages = MutableStateFlow<Set<String>>(emptySet())
    val excludedPackages: StateFlow<Set<String>> = _excludedPackages.asStateFlow()

    init {
        val saved = prefs.getStringSet("excluded_set", emptySet()) ?: emptySet()
        // By default, add known sensitive app signatures if not set
        if (saved.isEmpty()) {
            val defaults = setOf(
                "com.google.android.apps.authenticator2",
                "com.whatsapp",
                "org.telegram.messenger",
                "com.bca",
                "id.co.bri.brimo",
                "com.bankmandiri.livin"
            )
            prefs.edit().putStringSet("excluded_set", defaults).apply()
            _excludedPackages.value = defaults
        } else {
            _excludedPackages.value = saved
        }
    }

    fun isAppExcluded(packageName: String): Boolean {
        return _excludedPackages.value.contains(packageName)
    }

    fun toggleAppExclusion(packageName: String) {
        val current = _excludedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.edit().putStringSet("excluded_set", current).apply()
        _excludedPackages.value = current
    }

    suspend fun getInstalledApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        val list = mutableListOf<InstalledAppItem>()

        val currentExcluded = _excludedPackages.value

        for (appInfo in packages) {
            // Include non-system or launchable applications
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
            if (!isSystem || launchIntent != null) {
                val name = pm.getApplicationLabel(appInfo).toString()
                val icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                val isExcluded = currentExcluded.contains(appInfo.packageName)
                list.add(
                    InstalledAppItem(
                        appName = name,
                        packageName = appInfo.packageName,
                        icon = icon,
                        isExcluded = isExcluded
                    )
                )
            }
        }
        list.sortedBy { it.appName.lowercase() }
    }
}
