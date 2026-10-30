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
    ShortcutIcon.SEARCH to outlineIcon("Search", "M17,10A7,7 0,1 1,3,10A7,7 0,1 1,17,10 M15,15L21,21"),
    ShortcutIcon.MAIL to outlineIcon("Mail", "M3,5H21V19H3Z M3,5L12,12L21,5"),
    ShortcutIcon.PHOTOS to outlineIcon("Photos", "M3,3H21V21H3Z M3,17L9,11L14,16L17,13L21,17 M17,7A1,1 0,1 1,15,7A1,1 0,1 1,17,7"),
    ShortcutIcon.FILES to outlineIcon("Files", "M3,6H10L12,8H21V20H3Z M3,6V4H9L11,6"),
    ShortcutIcon.BOOK to outlineIcon("Book", "M3,4H8C10,4 12,5 12,7V21C12,19 10,18 8,18H3Z M12,7C12,5 14,4 16,4H21V18H16C14,18 12,19 12,21"),
    ShortcutIcon.HEADPHONES to outlineIcon("Headphones", "M3,14V11A9,9 0,0 1,21,11V14 M3,13H7V21H3Z M17,13H21V21H17Z"),
    ShortcutIcon.PODCAST to outlineIcon("Podcast", "M5,17A9,9 0,1 1,19,17 M8,14A5,5 0,1 1,16,14 M14,10A2,2 0,1 1,10,10A2,2 0,1 1,14,10 M9,16Q12,14 15,16L14,22H10Z"),
    ShortcutIcon.VIDEO to outlineIcon("Video", "M3,5H21V19H3Z M10,9L16,12L10,15Z"),
    ShortcutIcon.SHOPPING to outlineIcon("Shopping", "M5,7H19L21,21H3Z M8,8V6A4,4 0,0 1,16,6V8"),
    ShortcutIcon.TRAIN to outlineIcon("Train", "M6,3H18V18H6Z M6,7H18 M12,3V12 M6,12H18 M9,16H9.1 M15,16H15.1 M8,18L5,22 M16,18L19,22 M7,21H17"),
    ShortcutIcon.BIKE to outlineIcon("Bike", "M9,16A4,4 0,1 1,1,16A4,4 0,1 1,9,16 M23,16A4,4 0,1 1,15,16A4,4 0,1 1,23,16 M5,16L9,8L14,16H5 M9,8H16L19,16 M7,5H11 M16,8V4H19"),
    ShortcutIcon.FITNESS to outlineIcon("Fitness", "M6,10L10,6 M14,18L18,14 M8,8L16,16 M3,8L8,3 M2,5L5,2 M16,21L21,16 M19,22L22,19"),
    ShortcutIcon.COFFEE to outlineIcon("Coffee", "M4,8H17V14A6.5,6.5 0,0 1,4,14Z M17,8H19A3,3 0,0 1,19,14H17 M3,22H19 M7,2V4 M11,2V4 M15,2V4"),
    ShortcutIcon.CALCULATOR to outlineIcon("Calculator", "M5,2H19V22H5Z M8,5H16V9H8Z M8,13H9 M15,13H16 M8,16H9 M15,16H16 M8,19H9 M15,19H16"),
    ShortcutIcon.CLOCK to outlineIcon("Clock", "M21,12A9,9 0,1 1,3,12A9,9 0,1 1,21,12 M12,6V12L16,14"),
    ShortcutIcon.CLOUD to outlineIcon("Cloud", "M7,19A5,5 0,1 1,7,9A6,6 0,0 1,18,7A6,6 0,0 1,18,19Z"),
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
