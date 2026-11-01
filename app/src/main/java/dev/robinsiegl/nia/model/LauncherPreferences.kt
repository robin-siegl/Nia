package dev.robinsiegl.nia.model

import kotlinx.serialization.Serializable

@Serializable
enum class HomeCategory(val label: String) {
    PHONE("Phone"), MESSAGES("Messages"), MUSIC("Music"), CALENDAR("Calendar"), MAPS("Maps")
}

@Serializable enum class HomeLayout { GRID, LIST }
@Serializable enum class HomeTextSize(val scale: Float) { SMALL(0.85f), DEFAULT(1f), LARGE(1.15f) }
@Serializable enum class ColorStyle { MONOCHROME, COLOR }
@Serializable enum class ThemeMode { SYSTEM, LIGHT, DARK }
@Serializable enum class SetupStep { LAYOUT, APPS, APPEARANCE, DEFAULT }
@Serializable enum class MotionMode { SYSTEM, REDUCED }
@Serializable enum class ShortcutAction { EMPTY, APP, COMMAND }
enum class CornerPosition(val label: String) { LEFT("Left"), RIGHT("Right") }

@Serializable
data class CornerShortcut(
    val action: ShortcutAction = ShortcutAction.EMPTY,
    val icon: ShortcutIcon = ShortcutIcon.STAR,
    val app: AppTarget? = null,
    val command: ShortcutCommand? = null,
    val timerMinutes: Int? = null,
)

@Serializable
data class AppTarget(val packageName: String, val activityName: String) {
    val key: String get() = "$packageName/$activityName"
}

data class AppEntry(val target: AppTarget, val label: String)

const val MAX_HOME_SHORTCUTS = 9
const val MAX_HOME_NAME_LENGTH = 80

/** A Home entry's identity is independent of its editable label, icon and app/command target. */
@Serializable
data class HomeShortcut(
    val id: String,
    val name: String,
    val icon: ShortcutIcon = ShortcutIcon.STAR,
    val app: AppTarget? = null,
    val command: ShortcutCommand? = null,
    val timerMinutes: Int? = null,
)

enum class HeaderTarget(val label: String) { CLOCK("Clock"), CALENDAR("Calendar") }

data class HomeShortcutDraft(val shortcut: HomeShortcut, val isNew: Boolean) {
    val canSave: Boolean get() = PreferencesValidation.isHomeName(shortcut.name.trim()) &&
        !(shortcut.app != null && shortcut.command != null) && validTimerOptions(shortcut.command, shortcut.timerMinutes) &&
        (!isNew || shortcut.app != null || shortcut.command != null)
}

/** Only used when converting a pre-1.4 setup. New installations start with no Home shortcuts. */
fun legacyHomeShortcuts(assignments: Map<HomeCategory, AppTarget> = emptyMap()): List<HomeShortcut> =
    HomeCategory.entries.map { HomeShortcut("legacy-${it.name.lowercase()}", it.label, ShortcutIcon.valueOf(it.name), assignments[it]) }

@Serializable
data class LauncherPreferences(
    val layout: HomeLayout = HomeLayout.GRID,
    val homeTextSize: HomeTextSize = HomeTextSize.DEFAULT,
    val pixelClockLayout: Boolean = true,
    val colorStyle: ColorStyle = ColorStyle.MONOCHROME,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val seedColor: String = "#7C3AED",
    val pureBlackColorBackground: Boolean = false,
    val searchAutofocus: Boolean = false,
    val alphabetScrubber: Boolean = false,
    val motionMode: MotionMode = MotionMode.SYSTEM,
    val leftShortcut: CornerShortcut = CornerShortcut(),
    val rightShortcut: CornerShortcut = CornerShortcut(),
    val homeShortcuts: List<HomeShortcut> = emptyList(),
    val clock: AppTarget? = null,
    val calendar: AppTarget? = null,
) {
    fun shortcut(position: CornerPosition) = if (position == CornerPosition.LEFT) leftShortcut else rightShortcut
    fun withShortcut(position: CornerPosition, shortcut: CornerShortcut) =
        if (position == CornerPosition.LEFT) copy(leftShortcut = shortcut) else copy(rightShortcut = shortcut)
}

@Serializable
data class SavedState(
    val preferences: LauncherPreferences = LauncherPreferences(),
    val setupComplete: Boolean = false,
    val setupStep: SetupStep = SetupStep.LAYOUT,
)

object PreferencesValidation {
    private val colorPattern = Regex("#[0-9a-fA-F]{6}")
    private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*")
    private val activityPattern = Regex("[A-Za-z_.][A-Za-z0-9_.$]*")

    fun isColor(value: String): Boolean = colorPattern.matches(value)
    fun isHomeName(value: String): Boolean = value.isNotBlank() && value.length <= MAX_HOME_NAME_LENGTH &&
        value == value.trim() && value.none { it.isISOControl() }


    fun validate(preferences: LauncherPreferences) {
        require(isColor(preferences.seedColor)) { "Choose a color in #RRGGBB format." }
        require(preferences.homeShortcuts.size <= MAX_HOME_SHORTCUTS) { "Home can contain up to nine shortcuts." }
        require(preferences.homeShortcuts.map { it.id }.distinct().size == preferences.homeShortcuts.size) { "Home shortcut IDs must be unique." }
        preferences.homeShortcuts.forEach {
            require(it.id.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "The settings file contains an invalid shortcut ID." }
            require(isHomeName(it.name)) { "Shortcut names must contain 1–80 characters without line breaks." }
            require(it.app == null || it.command == null) { "Choose an app or a command for each shortcut." }
            require(validTimerOptions(it.command, it.timerMinutes)) { "Only preset timers can have a duration, from 1 to 1,440 minutes." }
        }
        val shortcuts = listOf(preferences.leftShortcut, preferences.rightShortcut)
        shortcuts.forEach {
            require(when (it.action) {
                ShortcutAction.EMPTY -> it.app == null && it.command == null
                ShortcutAction.APP -> it.app != null && it.command == null
                ShortcutAction.COMMAND -> it.app == null && it.command != null
            }) { "A corner must be empty, open an app, or run a command." }
            require(validTimerOptions(it.command, it.timerMinutes)) { "Only preset timers can have a duration, from 1 to 1,440 minutes." }
        }
        (preferences.homeShortcuts.mapNotNull { it.app } + listOfNotNull(preferences.clock, preferences.calendar) + shortcuts.mapNotNull { it.app }).forEach { target ->
            require(target.packageName.length <= 255 && packagePattern.matches(target.packageName)) {
                "The settings file contains an invalid app package."
            }
            require(target.activityName.length <= 512 && activityPattern.matches(target.activityName)) {
                "The settings file contains an invalid app activity."
            }
        }
    }
}
