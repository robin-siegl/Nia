package dev.robinsiegl.nia

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.app.KeyguardManager
import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import dev.robinsiegl.nia.model.UnlockTracker
import dev.robinsiegl.nia.ui.NiaApp

class MainActivity : ComponentActivity() {
    private val model: LauncherViewModel by viewModels()
    private var homeRequest by mutableIntStateOf(0)
    private var unlockRequest by mutableIntStateOf(0)
    private var deviceLocked by mutableStateOf(false)
    private var resumed = false
    private lateinit var keyguard: KeyguardManager
    private lateinit var power: PowerManager
    private val unlocks = UnlockTracker()
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> { unlocks.screenOff(); deviceLocked = true }
                Intent.ACTION_USER_PRESENT -> {
                    unlocks.userPresent(resumed)
                    if (resumed) checkUnlock() else deviceLocked = false
                }
                Intent.ACTION_SCREEN_ON -> if (resumed) checkUnlock()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keyguard = getSystemService(KeyguardManager::class.java)
        power = getSystemService(PowerManager::class.java)
        deviceLocked = keyguard.isKeyguardLocked || !power.isInteractive
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            NiaApp(model, homeRequest, unlockRequest, deviceLocked) { dark ->
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
        }
    }

    override fun onResume() { super.onResume(); resumed = true; model.refreshDefault(); checkUnlock() }

    override fun onPause() {
        unlocks.leaveForeground(keyguard.isKeyguardLocked, power.isInteractive)
        if (keyguard.isKeyguardLocked || !power.isInteractive) deviceLocked = true
        resumed = false
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && resumed) checkUnlock()
    }

    private fun checkUnlock() {
        val locked = keyguard.isKeyguardLocked
        val interactive = power.isInteractive
        val focused = hasWindowFocus()
        if (unlocks.foreground(locked, interactive, focused)) unlockRequest++
        // Keep shortcuts hidden between keyguard dismissal and this window receiving focus.
        deviceLocked = locked || !interactive || (unlocks.awaitingUnlock && !focused)
    }

    override fun onDestroy() { unregisterReceiver(screenReceiver); super.onDestroy() }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homeRequest++
    }
}
