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
@Serializable enum class ShortcutAction { EMPTY, APP }
@Serializable enum class ShortcutIcon(val label: String) {
    STAR("Star"), CAMERA("Camera"), BROWSER("Browser"), NOTES("Notes"), WALLET("Wallet"), HEART("Heart"),
    PHONE("Phone"), MESSAGES("Messages"), MUSIC("Music"), CALENDAR("Calendar"), MAPS("Maps"),
    SEARCH("Search"), MAIL("Mail"), PHOTOS("Photos"), FILES("Files"), BOOK("Book"), HEADPHONES("Headphones"),
    PODCAST("Podcast"), VIDEO("Video"), SHOPPING("Shopping"), TRAIN("Train"), BIKE("Bike"), FITNESS("Fitness"),
    COFFEE("Coffee"), CALCULATOR("Calculator"), CLOCK("Clock"), CLOUD("Cloud")
}
enum class CornerPosition(val label: String) { LEFT("Left"), RIGHT("Right") }

@Serializable
data class CornerShortcut(
    val action: ShortcutAction = ShortcutAction.EMPTY,
    val icon: ShortcutIcon = ShortcutIcon.STAR,
    val app: AppTarget? = null,
)

@Serializable
data class AppTarget(val packageName: String, val activityName: String) {
    val key: String get() = "$packageName/$activityName"
}

data class AppEntry(val target: AppTarget, val label: String)

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
    val assignments: Map<HomeCategory, AppTarget> = emptyMap(),
    val clock: AppTarget? = null,
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

    fun validate(preferences: LauncherPreferences) {
        require(isColor(preferences.seedColor)) { "Choose a color in #RRGGBB format." }
        val shortcuts = listOf(preferences.leftShortcut, preferences.rightShortcut)
        shortcuts.forEach {
            require((it.action == ShortcutAction.APP) == (it.app != null)) {
                "An app shortcut must have an assigned app. Empty shortcuts cannot contain an app."
            }
        }
        (preferences.assignments.values + listOfNotNull(preferences.clock) + shortcuts.mapNotNull { it.app }).forEach { target ->
            require(target.packageName.length <= 255 && packagePattern.matches(target.packageName)) {
                "The settings file contains an invalid app package."
            }
            require(target.activityName.length <= 512 && activityPattern.matches(target.activityName)) {
                "The settings file contains an invalid app activity."
            }
        }
    }
}
