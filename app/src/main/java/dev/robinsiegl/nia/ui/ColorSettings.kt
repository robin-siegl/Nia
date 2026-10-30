package dev.robinsiegl.nia.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.theme.Palette
import dev.robinsiegl.nia.theme.SeedColor
import kotlin.math.*

@Composable
internal fun ColorSettings(preferences: LauncherPreferences, onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit) {
    val initial = remember(preferences.seedColor) { SeedColor.fromHex(preferences.seedColor) }
    var hue by rememberSaveable(preferences.seedColor) { mutableFloatStateOf(initial.hue) }
    var saturation by rememberSaveable(preferences.seedColor) { mutableFloatStateOf(initial.saturation) }
    var brightness by rememberSaveable(preferences.seedColor) { mutableFloatStateOf(initial.brightness) }
    var input by rememberSaveable(preferences.seedColor) { mutableStateOf(preferences.seedColor) }
    fun currentSeed() = SeedColor(hue, saturation, brightness)
    fun select(seed: SeedColor) {
        hue = seed.hue; saturation = seed.saturation; brightness = seed.brightness
        input = seed.toHex()
    }
    val commit: () -> Unit = {
        val hex = currentSeed().toHex()
        onChange { it.copy(seedColor = hex) }
    }
    val seed = currentSeed()
    val valid = PreferencesValidation.isColor(input)
    Text("Pick one color. nia finds the matching background and text.", fontSize = 14.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
    val presets = listOf("Violet" to "#7C3AED", "Rose" to "#D54778", "Blue" to "#2563EB",
        "Mint" to "#008B75", "Amber" to "#BD7900", "Coral" to "#D8553D")
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 288.dp) 3 else 6
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { (name, hex) ->
                        val active = seed.toHex() == hex
                        Box(Modifier.size(48.dp).clickable(role = Role.Button) { select(SeedColor.fromHex(hex)); commit() }
                            .semantics { contentDescription = "$name color"; selected = active }, contentAlignment = Alignment.Center) {
                            Box(Modifier.size(30.dp).border(if (active) 2.dp else 0.dp,
                                if (active) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape).padding(4.dp)
                                .background(Color(SeedColor.fromHex(hex).toArgb()), CircleShape))
                        }
                    }
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
        ColorWheel(seed, ::select, commit)
    }
    Text("Brightness", fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
    Slider(brightness, onValueChange = { select(currentSeed().copy(brightness = it)) },
        onValueChangeFinished = commit,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Color brightness" })
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(input, {
            input = it.trim().take(7).uppercase()
            if (PreferencesValidation.isColor(input)) {
                val parsed = SeedColor.fromHex(input)
                hue = parsed.hue; saturation = parsed.saturation; brightness = parsed.brightness
            }
        }, label = { Text("Hex color") }, singleLine = true,
            isError = input.isNotEmpty() && !valid, modifier = Modifier.weight(1f),
            supportingText = { if (!valid) Text("Use #RRGGBB") })
        TextButton(onClick = commit, enabled = valid, modifier = Modifier.padding(start = 8.dp)) { Text("Apply") }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark").forEach { (mode, label) ->
            val colors = Palette.generate(preferences.copy(seedColor = seed.toHex(), themeMode = mode), false)
            Column(Modifier.weight(1f).testTag("color-preview-$label")
                .background(Color(colors.background), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f), RoundedCornerShape(8.dp)).padding(16.dp)) {
                Text("Aa", fontSize = 28.sp, fontWeight = FontWeight.Light, color = Color(colors.foreground))
                Text(label, fontSize = 12.sp, color = Color(colors.foreground))
            }
        }
    }
    SettingSwitch("Pure black in dark mode", "Keeps your colored text on an OLED-black background.", preferences.pureBlackColorBackground) {
        value -> onChange { it.copy(pureBlackColorBackground = value) }
    }
}

@Composable
private fun ColorWheel(seed: SeedColor, onSelect: (SeedColor) -> Unit, onFinished: () -> Unit) {
    val padding = with(LocalDensity.current) { 10.dp.toPx() }
    val rainbow = remember { (0..6).map { Color(SeedColor(it * 60f, 1f, 1f).toArgb()) } }
    fun adjust(hue: Float = seed.hue, saturation: Float = seed.saturation): Boolean {
        onSelect(seed.copy(hue = (hue + 360f) % 360f, saturation = saturation.coerceIn(0f, 1f)))
        onFinished()
        return true
    }
    Canvas(Modifier.size(184.dp).testTag("color-wheel")
        .semantics {
            contentDescription = "Color wheel"
            stateDescription = "Hue ${seed.hue.roundToInt()} degrees, saturation ${(seed.saturation * 100).roundToInt()} percent"
            customActions = listOf(
                CustomAccessibilityAction("Next hue") { adjust(hue = seed.hue + 15f) },
                CustomAccessibilityAction("Previous hue") { adjust(hue = seed.hue - 15f) },
                CustomAccessibilityAction("Increase saturation") { adjust(saturation = seed.saturation + 0.1f) },
                CustomAccessibilityAction("Decrease saturation") { adjust(saturation = seed.saturation - 0.1f) },
            )
        }.onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                Key.DirectionRight -> adjust(hue = seed.hue + 5f)
                Key.DirectionLeft -> adjust(hue = seed.hue - 5f)
                Key.DirectionUp -> adjust(saturation = seed.saturation + 0.05f)
                Key.DirectionDown -> adjust(saturation = seed.saturation - 0.05f)
                else -> false
            }
        }.focusable().dragToSelect({ position, size ->
            val x = position.x - size.width / 2f
            val y = position.y - size.height / 2f
            val radius = min(size.width, size.height) / 2f - padding
            val hue = ((atan2(y, x) * 180f / PI.toFloat()) + 360f) % 360f
            onSelect(seed.copy(hue = hue, saturation = (hypot(x, y) / radius).coerceIn(0f, 1f)))
        }, onFinished)) {
        val radius = size.minDimension / 2f - padding
        drawCircle(Brush.sweepGradient(rainbow, center), radius)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), center, radius), radius)
        drawCircle(Color.Black.copy(alpha = 1f - seed.brightness), radius)
        val angle = seed.hue * PI.toFloat() / 180f
        val marker = center + Offset(cos(angle), sin(angle)) * (radius * seed.saturation)
        drawCircle(Color(seed.toArgb()), 7.dp.toPx(), marker)
        drawCircle(Color.White, 7.dp.toPx(), marker, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black, 9.dp.toPx(), marker, style = Stroke(1.dp.toPx()))
    }
}
