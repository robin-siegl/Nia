package dev.robinsiegl.nia

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.app.KeyguardManager
import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import dev.robinsiegl.nia.model.UnlockEnvironment
import dev.robinsiegl.nia.model.Screen
import dev.robinsiegl.nia.ui.NiaApp

class MainActivity : ComponentActivity() {
    private val model: LauncherViewModel by viewModels()
    private var homeRequest by mutableIntStateOf(0)
    private lateinit var keyguard: KeyguardManager
    private lateinit var power: PowerManager
    private val unlock get() = model.unlockSession
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> unlock.screenOff(environment(), SystemClock.uptimeMillis())
                Intent.ACTION_USER_PRESENT -> unlock.userPresent(environment(), SystemClock.uptimeMillis())
                Intent.ACTION_SCREEN_ON -> unlock.refresh(environment(), SystemClock.uptimeMillis())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keyguard = getSystemService(KeyguardManager::class.java)
        power = getSystemService(PowerManager::class.java)
        unlock.initialize(environment())
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            NiaApp(model, homeRequest, unlock.request, unlock.deviceLocked, revealAllowed = unlock.active) { dark ->
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.refreshDefault()
        unlock.resume(environment(), SystemClock.uptimeMillis())
    }

    override fun onPause() {
        unlock.pause(environment())
        super.onPause()
    }

    override fun onStop() {
        unlock.stop(environment(), isChangingConfigurations)
        super.onStop()
    }

    override fun onUserLeaveHint() {
        unlock.userLeaving(environment())
        super.onUserLeaveHint()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (::keyguard.isInitialized && ::power.isInitialized) {
            unlock.focusChanged(environment(hasFocus), SystemClock.uptimeMillis())
        }
    }

    private fun environment(focused: Boolean = hasWindowFocus()) = UnlockEnvironment(
        locked = keyguard.isKeyguardLocked, interactive = power.isInteractive, focused = focused,
        homeVisible = model.navigation.value.screen == Screen.Home && model.saved.value?.setupComplete != false,
    )

    override fun onDestroy() { unregisterReceiver(screenReceiver); super.onDestroy() }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            model.home()
            homeRequest++
            unlock.refresh(environment(), SystemClock.uptimeMillis())
        }
    }
}
