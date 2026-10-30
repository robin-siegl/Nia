package dev.robinsiegl.nia.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import dev.robinsiegl.nia.model.HomeCategory
import dev.robinsiegl.nia.model.ShortcutIcon

private val shortcutVectors = mapOf(
    ShortcutIcon.STAR to outlineIcon("Star", "M12,3L14.8,8.7L21,9.6L16.5,14L17.6,20.2L12,17.3L6.4,20.2L7.5,14L3,9.6L9.2,8.7Z"),
    ShortcutIcon.CAMERA to outlineIcon("Camera", "M3,7H7L9,4H15L17,7H21V20H3Z M16,13A4,4 0,1 1,8,13A4,4 0,1 1,16,13"),
    ShortcutIcon.BROWSER to outlineIcon("Browser", "M21,12A9,9 0,1 1,3,12A9,9 0,1 1,21,12 M3,12H21 M12,3C7,8 7,16 12,21C17,16 17,8 12,3"),
    ShortcutIcon.NOTES to outlineIcon("Notes", "M5,3H15L20,8V21H5Z M15,3V8H20 M9,12H16 M9,16H16"),
    ShortcutIcon.WALLET to outlineIcon("Wallet", "M20,7H4V4H18V7 M4,7V20H21V7Z M21,11H15V16H21 M17,13.5H17.1"),
    ShortcutIcon.HEART to outlineIcon("Heart", "M12,21L3.8,13C-1,8 6,0 12,7C18,0 25,8 20.2,13Z"),
    ShortcutIcon.SETTINGS to settingsVector,
)

@Composable
internal fun ShortcutGlyph(icon: ShortcutIcon, modifier: Modifier = Modifier) {
    val category = HomeCategory.entries.find { it.name == icon.name }
    if (category != null) CategoryIcon(category, modifier)
    else Icon(shortcutVectors.getValue(icon), contentDescription = null, modifier = modifier)
}

@Composable
internal fun Modifier.shortcutButton(enabled: Boolean, reducedMotion: Boolean, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val view = LocalView.current
    val scale by animateFloatAsState(if (pressed && enabled && !reducedMotion) 0.96f else 1f, tween(90), label = "Shortcut press")
    return graphicsLayer { scaleX = scale; scaleY = scale }.clickable(
        enabled = enabled, role = Role.Button, interactionSource = source, indication = LocalIndication.current,
    ) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        onClick()
    }
}
