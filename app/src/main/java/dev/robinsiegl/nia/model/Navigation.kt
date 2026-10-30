package dev.robinsiegl.nia.model

sealed interface Screen {
    data object Home : Screen
    data object Apps : Screen
    data object Settings : Screen
    data class Picker(val category: HomeCategory?) : Screen
}

data class Navigation(val stack: List<Screen> = listOf(Screen.Home)) {
    val screen: Screen get() = stack.last()
    fun push(screen: Screen): Navigation = if (this.screen == screen) this else copy(stack = stack + screen)
    fun back(): Navigation = if (stack.size > 1) copy(stack = stack.dropLast(1)) else this
    fun home(): Navigation = Navigation()
}
