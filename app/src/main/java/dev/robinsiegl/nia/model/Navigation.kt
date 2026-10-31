package dev.robinsiegl.nia.model

enum class SettingsSection(val title: String, val description: String) {
    HOME("Home screen", "Layout, clock and text size"),
    SHORTCUTS("Corner shortcuts", "Optional shortcuts at the bottom of Home"),
    APPEARANCE("Appearance", "Colors, theme and animations"),
    ASSIGNMENTS("Home apps", "Add, edit and arrange your shortcuts"),
    APP_LIST("App list", "Search and A–Z navigation"),
    BACKUP("Backup & reset", "Save, restore or reset your settings"),
    ABOUT("About nia", "Updates, privacy and project information"),
}

sealed interface Screen {
    data object Home : Screen
    data object Apps : Screen
    data object Settings : Screen
    data class SettingsDetail(val section: SettingsSection) : Screen
    data class Picker(val header: HeaderTarget) : Screen
    data object HomeShortcutEditor : Screen
    data object HomeShortcutPicker : Screen
    data class CornerPicker(val position: CornerPosition) : Screen
}

data class Navigation(val stack: List<Screen> = listOf(Screen.Home)) {
    val screen: Screen get() = stack.last()
    fun push(screen: Screen): Navigation = if (this.screen == screen) this else copy(stack = stack + screen)
    fun back(): Navigation = if (stack.size > 1) copy(stack = stack.dropLast(1)) else this
    fun home(): Navigation = Navigation()
}
