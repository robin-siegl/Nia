package dev.robinsiegl.nia.model

enum class SettingsSection(val title: String, val description: String) {
    HOME("Home", "Layout and text size"),
    APPEARANCE("Appearance", "Themes, colors and OLED black"),
    ASSIGNMENTS("App assignments", "Your everyday apps and Clock"),
    APP_LIST("App list", "Search and alphabet navigation"),
    BACKUP("Backup & reset", "Export, restore and reset settings"),
    ABOUT("About nia", "Version, privacy and project information"),
}

sealed interface Screen {
    data object Home : Screen
    data object Apps : Screen
    data object Settings : Screen
    data class SettingsDetail(val section: SettingsSection) : Screen
    data class Picker(val category: HomeCategory?) : Screen
}

data class Navigation(val stack: List<Screen> = listOf(Screen.Home)) {
    val screen: Screen get() = stack.last()
    fun push(screen: Screen): Navigation = if (this.screen == screen) this else copy(stack = stack + screen)
    fun back(): Navigation = if (stack.size > 1) copy(stack = stack.dropLast(1)) else this
    fun home(): Navigation = Navigation()
}
