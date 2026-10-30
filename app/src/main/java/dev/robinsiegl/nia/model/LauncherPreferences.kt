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
    val assignments: Map<HomeCategory, AppTarget> = emptyMap(),
    val clock: AppTarget? = null,
)

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
        (preferences.assignments.values + listOfNotNull(preferences.clock)).forEach { target ->
            require(target.packageName.length <= 255 && packagePattern.matches(target.packageName)) {
                "The settings file contains an invalid app package."
            }
            require(target.activityName.length <= 512 && activityPattern.matches(target.activityName)) {
                "The settings file contains an invalid app activity."
            }
        }
    }
}
