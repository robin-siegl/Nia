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
import androidx.compose.ui.res.painterResource
import dev.robinsiegl.nia.model.ShortcutIcon

@Composable
internal fun ShortcutGlyph(icon: ShortcutIcon, modifier: Modifier = Modifier) {
    Icon(painterResource(icon.drawable), contentDescription = null, modifier = modifier)
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
