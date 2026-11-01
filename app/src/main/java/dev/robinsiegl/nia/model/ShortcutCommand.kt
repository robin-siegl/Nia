package dev.robinsiegl.nia.model

import kotlinx.serialization.Serializable

/** Explicit, local actions. Never deserialize or execute arbitrary intent/command strings. */
@Serializable
enum class ShortcutCommand(val label: String, val description: String, val icon: ShortcutIcon, val defaultName: String) {
    OPEN_APPS("Open Apps", "Browse all installed apps.", ShortcutIcon.APP_GRID, "Apps"),
    SEARCH_APPS("Search apps", "Open Apps with search and the keyboard ready.", ShortcutIcon.SEARCH, "Search"),
    FLASHLIGHT("Toggle flashlight", "Turn the phone's flashlight on or off.", ShortcutIcon.FLASHLIGHT, "Flashlight"),
    START_TIMER("Start a timer", "Choose a duration in your clock app.", ShortcutIcon.TIMER, "Timer"),
}

/** Ignore stale drag results rather than dropping a shortcut added or removed during the gesture. */
internal fun LauncherPreferences.reorderHome(ids: List<String>): LauncherPreferences {
    val current = homeShortcuts.associateBy { it.id }
    if (ids.size != current.size || ids.toSet() != current.keys) return this
    return copy(homeShortcuts = ids.map(current::getValue))
}
