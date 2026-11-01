package dev.robinsiegl.nia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import dev.robinsiegl.nia.R
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.robinsiegl.nia.model.MAX_TIMER_MINUTES

/** Shared by Home and Quick corners. Editing and cancellation never write preferences. */
@Composable
internal fun TimerDurationPicker(minutes: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    var text by rememberSaveable(minutes) { mutableStateOf(minutes.toString()) }
    val chosen = text.toIntOrNull()?.takeIf { it in 1..MAX_TIMER_MINUTES }
    val parentFocus = LocalFocusManager.current
    LaunchedEffect(parentFocus) { parentFocus.clearFocus() }
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        val largeText = LocalDensity.current.fontScale > 1.5f
        val window = (view.parent as? DialogWindowProvider)?.window
        val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
        SideEffect {
            window?.let {
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
                it.isNavigationBarContrastEnforced = false
            }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 600.dp).fillMaxSize().testTag("timer-duration-picker")) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (largeText) "Duration" else "Timer duration", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f).semantics { heading() })
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(painterResource(R.drawable.ic_material_close), "Close timer duration")
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
                        SettingsDescription("One tap on this shortcut starts the saved timer in Clock.")
                        ChoiceRow(listOf(5, 15, 25).map { it to "$it min" }, text.toIntOrNull() ?: 0,
                            { text = it.toString() }, Modifier.padding(vertical = 24.dp))
                        OutlinedTextField(text, onValueChange = { value ->
                            if (value.length <= 4 && value.all { it in '0'..'9' }) text = value
                        }, label = { Text("Minutes") }, singleLine = true,
                            isError = text.isNotEmpty() && chosen == null,
                            supportingText = { Text(if (text.isNotEmpty() && chosen == null) "Enter 1–1,440 minutes." else "1 minute to 24 hours.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { chosen?.let(onSelect) }),
                            modifier = Modifier.fillMaxWidth().testTag("timer-minutes"))
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Button(onClick = { chosen?.let(onSelect) }, enabled = chosen != null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Use duration") }
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel") }
                    }
                }
            }
        }
    }
}
