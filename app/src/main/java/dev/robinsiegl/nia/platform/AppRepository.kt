package dev.robinsiegl.nia.platform

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.AlarmClock
import android.provider.Settings
import dev.robinsiegl.nia.model.AppEntry
import dev.robinsiegl.nia.model.AppSearch
import dev.robinsiegl.nia.model.AppTarget
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

class AppRepository(private val context: Context) {
    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val user = Process.myUserHandle()

    fun listApps(): List<AppEntry> {
        // LauncherApps can synthesize App-info entries for background-only packages.
        // Keep actual declared launcher activities, including their distinct aliases.
        val declared = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL,
        ).map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }.toSet()
        return AppSearch.filter(
        launcher.getActivityList(null, user).filter { it.componentName.packageName != context.packageName && it.componentName in declared }
            .map { AppEntry(AppTarget(it.componentName.packageName, it.componentName.className), it.label.toString()) }
            .distinctBy { it.target.key }, "",
        )
    }

    val apps: Flow<List<AppEntry>> = callbackFlow {
        fun changed(profile: UserHandle) { if (profile == user) trySend(Unit) }
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) = changed(user)
            override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(user)
            override fun onPackageChanged(packageName: String, user: UserHandle) = changed(user)
            override fun onPackagesAvailable(names: Array<out String>, user: UserHandle, replacing: Boolean) = changed(user)
            override fun onPackagesUnavailable(names: Array<out String>, user: UserHandle, replacing: Boolean) = changed(user)
        }
        launcher.registerCallback(callback, Handler(Looper.getMainLooper()))
        trySend(Unit)
        awaitClose { launcher.unregisterCallback(callback) }
    }.conflate().map { listApps() }

    fun launch(target: AppTarget) {
        launcher.startMainActivity(ComponentName(target.packageName, target.activityName), user, null, null)
    }

    fun showAppInfo(target: AppTarget) {
        launcher.startAppDetailsActivity(ComponentName(target.packageName, target.activityName), user, null, null)
    }

    fun openClock() {
        context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun isDefaultHome(): Boolean {
        val role = context.getSystemService(RoleManager::class.java)
        return role.isRoleAvailable(RoleManager.ROLE_HOME) && role.isRoleHeld(RoleManager.ROLE_HOME)
    }

    fun homeSelectionIntent(): Intent {
        val role = context.getSystemService(RoleManager::class.java)
        return if (role.isRoleAvailable(RoleManager.ROLE_HOME)) role.createRequestRoleIntent(RoleManager.ROLE_HOME)
        else Intent(Settings.ACTION_HOME_SETTINGS)
    }
}
