package dev.robinsiegl.nia.model

import kotlinx.serialization.Serializable

/** Explicit, local actions. Never deserialize or execute arbitrary intent/command strings. */
@Serializable
enum class ShortcutCommand(val label: String, val description: String, val icon: ShortcutIcon, val defaultName: String) {
    OPEN_APPS("Open Apps", "Browse all installed apps.", ShortcutIcon.APP_GRID, "Apps"),
    SEARCH_APPS("Search apps", "Open Apps with search and the keyboard ready.", ShortcutIcon.SEARCH, "Search"),
    FLASHLIGHT("Toggle flashlight", "Turn the phone's flashlight on or off.", ShortcutIcon.FLASHLIGHT, "Flashlight"),
    START_TIMER("Start a timer", "Choose a duration in your clock app.", ShortcutIcon.TIMER, "Timer"),
    SET_ALARM("Set an alarm", "Choose an alarm time in your clock app.", ShortcutIcon.ALARM, "Alarm"),
    PRESET_TIMER("Preset timer", "Start a timer for a saved duration in your clock app.", ShortcutIcon.TIMER, "Timer"),
    TOGGLE_THEME("Toggle theme", "Switch nia between light and dark. Overrides System.", ShortcutIcon.PALETTE, "Theme"),
    SOUND_CONTROLS("Sound controls", "Open Android's volume controls.", ShortcutIcon.VOLUME, "Sound"),
}

const val DEFAULT_TIMER_MINUTES = 5
const val MAX_TIMER_MINUTES = 1440

fun validTimerOptions(command: ShortcutCommand?, minutes: Int?): Boolean =
    if (command == ShortcutCommand.PRESET_TIMER) minutes != null && minutes in 1..MAX_TIMER_MINUTES else minutes == null

fun ShortcutCommand.displayName(minutes: Int?): String =
    if (this == ShortcutCommand.PRESET_TIMER && minutes != null) "$label · $minutes min" else label

fun presetTimerName(minutes: Int) = "$minutes min timer"
fun timerDurationDescription(minutes: Int) = if (minutes == 1) "1 minute" else "$minutes minutes"

/** Keep custom presentation, and retain the duration when reselecting the same preset command. */
fun HomeShortcut.withCommand(next: ShortcutCommand): HomeShortcut {
    val minutes = if (next == ShortcutCommand.PRESET_TIMER) timerMinutes ?: DEFAULT_TIMER_MINUTES else null
    return copy(app = null, command = next, timerMinutes = minutes,
        name = name.ifBlank { minutes?.let(::presetTimerName) ?: next.defaultName },
        icon = if (name.isBlank()) next.icon else icon)
}

fun HomeShortcut.withTimerDuration(minutes: Int): HomeShortcut {
    require(command == ShortcutCommand.PRESET_TIMER && minutes in 1..MAX_TIMER_MINUTES)
    return copy(timerMinutes = minutes, name = if (timerMinutes?.let(::presetTimerName) == name) presetTimerName(minutes) else name)
}

fun CornerShortcut.withCommand(next: ShortcutCommand): CornerShortcut = copy(
    action = ShortcutAction.COMMAND, app = null, command = next,
    timerMinutes = if (next == ShortcutCommand.PRESET_TIMER) timerMinutes ?: DEFAULT_TIMER_MINUTES else null,
    icon = if (action == ShortcutAction.EMPTY) next.icon else icon,
)

fun ThemeMode.toggled(systemDark: Boolean): ThemeMode = when (this) {
    ThemeMode.SYSTEM -> if (systemDark) ThemeMode.LIGHT else ThemeMode.DARK
    ThemeMode.LIGHT -> ThemeMode.DARK
    ThemeMode.DARK -> ThemeMode.LIGHT
}

/** Ignore stale drag results rather than dropping a shortcut added or removed during the gesture. */
internal fun LauncherPreferences.reorderHome(ids: List<String>): LauncherPreferences {
    val current = homeShortcuts.associateBy { it.id }
    if (ids.size != current.size || ids.toSet() != current.keys) return this
    return copy(homeShortcuts = ids.map(current::getValue))
}
