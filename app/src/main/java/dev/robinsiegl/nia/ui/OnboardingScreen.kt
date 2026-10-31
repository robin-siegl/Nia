package dev.robinsiegl.nia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.*

@Composable
fun OnboardingScreen(
    preferences: LauncherPreferences, apps: List<AppEntry>, step: SetupStep, isDefault: Boolean,
    onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit, onStep: (SetupStep) -> Unit,
    onEditHome: (String?) -> Unit, onMoveHome: (String, Int) -> Unit,
    onDefault: () -> Unit, onStart: () -> Unit, onApps: () -> Unit, onImport: () -> Unit,
    storageError: Boolean = false, onRecovery: () -> Unit = {},
) {
    val previous = SetupStep.entries.getOrNull(step.ordinal - 1)
    val next = SetupStep.entries.getOrNull(step.ordinal + 1)
    BackHandler(enabled = previous != null && !storageError) { previous?.let(onStep) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("nia", fontSize = 32.sp, fontWeight = FontWeight.Light)
            Text("${step.ordinal + 1} of ${SetupStep.entries.size}", fontSize = 13.sp)
        }
        key(step) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 24.dp)) {
                if (storageError) {
                    Text("Restore your Home", fontSize = 28.sp, fontWeight = FontWeight.Light)
                    Text("Saved settings could not be read. Restore a backup or reset nia to recover.",
                        fontSize = 16.sp, modifier = Modifier.padding(vertical = 20.dp))
                    Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Restore a settings backup") }
                    TextButton(onClick = onRecovery, modifier = Modifier.fillMaxWidth()) { Text("Open recovery settings") }
                } else when (step) {
                    SetupStep.LAYOUT -> {
                        SetupTitle("Your Home", "Choose a layout and text size. Add just the apps you want to keep close.")
                        TextButton(onClick = onImport, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Text("Restore a settings backup")
                        }
                        HomeSettings(preferences, onChange)
                    }
                    SetupStep.APPS -> {
                        SetupTitle("Add your apps", "Keep Home as simple as you like. Add a few apps now, or skip this step and use Apps.")
                        HomeAppsSettings(preferences, apps, onEditHome, onMoveHome)
                    }
                    SetupStep.APPEARANCE -> {
                        SetupTitle("Your colors", "Choose black and white or pick one color for matching light and dark themes.")
                        AppearanceSettings(preferences, onChange)
                    }
                    SetupStep.DEFAULT -> {
                        SetupTitle("A quieter way home", "Choose nia as your default launcher to see it whenever you press Home.")
                        if (isDefault) {
                            Text("nia is your default launcher.", fontSize = 16.sp)
                        } else Button(onClick = onDefault, modifier = Modifier.fillMaxWidth()) { Text("Set as default launcher") }
                        HomePreview(preferences, Modifier.padding(top = 20.dp))
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (previous != null && !storageError) TextButton(onClick = { onStep(previous) }) { Text("Back") }
                Button(onClick = { if (next != null) onStep(next) else onStart() }, enabled = !storageError,
                    modifier = Modifier.weight(1f)) { Text(if (next == null) "Start using nia" else if (step == SetupStep.APPS && preferences.homeShortcuts.isEmpty()) "Skip for now" else "Continue") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onApps) { Text("All apps") }
                if (step != SetupStep.DEFAULT && !storageError) TextButton(onClick = onStart) { Text("Skip setup") }
            }
        }
    }
}

@Composable
private fun SetupTitle(title: String, description: String) {
    Text(title, fontSize = 28.sp, fontWeight = FontWeight.Light)
    Text(description, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
}
