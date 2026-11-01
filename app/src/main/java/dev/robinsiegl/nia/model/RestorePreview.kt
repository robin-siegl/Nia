package dev.robinsiegl.nia.model

data class RestoreChange(val section: String, val label: String, val before: String, val after: String)
data class UnavailableAssignment(val label: String, val target: AppTarget)

data class RestorePreview(
    val current: LauncherPreferences,
    val requested: LauncherPreferences,
    val preferences: LauncherPreferences,
    val changes: List<RestoreChange>,
    val unavailable: List<UnavailableAssignment>,
) {
    companion object {
        fun create(current: LauncherPreferences, requested: LauncherPreferences, apps: List<AppEntry>): RestorePreview {
            val available = apps.map { it.target }.toSet()
            val unavailable = buildList {
                requested.homeShortcuts.forEach { shortcut ->
                    shortcut.app?.takeIf { it !in available }?.let { add(UnavailableAssignment(shortcut.name, it)) }
                }
                requested.calendar?.takeIf { it !in available }?.let { add(UnavailableAssignment("Calendar", it)) }
                requested.clock?.takeIf { it !in available }?.let { add(UnavailableAssignment("Clock", it)) }
                CornerPosition.entries.forEach { position ->
                    requested.shortcut(position).app?.takeIf { it !in available }?.let {
                        add(UnavailableAssignment("${position.label} shortcut", it))
                    }
                }
            }
            fun sanitize(shortcut: CornerShortcut) = if (shortcut.app != null && shortcut.app !in available)
                CornerShortcut(icon = shortcut.icon) else shortcut
            val restored = requested.copy(homeShortcuts = requested.homeShortcuts.map { it.copy(app = it.app?.takeIf { target -> target in available }) },
                calendar = requested.calendar?.takeIf { it in available },
                clock = requested.clock?.takeIf { it in available },
                leftShortcut = sanitize(requested.leftShortcut), rightShortcut = sanitize(requested.rightShortcut))
            fun appName(target: AppTarget?, empty: String): String {
                if (target == null) return empty
                val entry = apps.find { it.target == target }
                if (entry == null) return "Unavailable · ${target.key}"
                return if (apps.count { it.label == entry.label } > 1) "${entry.label} · ${target.key}" else "${entry.label} · ${target.packageName}"
            }
            fun actionName(target: Pair<AppTarget?, ShortcutCommand?>, empty: String): String =
                target.second?.label ?: appName(target.first, empty)
            val changes = buildList {
                fun <T> change(section: String, label: String, before: T, after: T, display: (T) -> String) {
                    if (before != after) add(RestoreChange(section, label, display(before), display(after)))
                }
                fun toggle(value: Boolean) = if (value) "On" else "Off"
                fun enumLabel(value: Enum<*>) = value.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
                change("Home screen", "Layout", current.layout, restored.layout) { if (it == HomeLayout.GRID) "Icon grid" else "Text list" }
                change("Home screen", "Home text size", current.homeTextSize, restored.homeTextSize, ::enumLabel)
                change("Home screen", "Pixel-style clock", current.pixelClockLayout, restored.pixelClockLayout, ::toggle)
                change("Appearance", "Colors", current.colorStyle, restored.colorStyle) { if (it == ColorStyle.COLOR) "Color" else "Black & white" }
                change("Appearance", "Theme", current.themeMode, restored.themeMode, ::enumLabel)
                change("Appearance", "Theme color", current.seedColor, restored.seedColor) { it.uppercase() }
                change("Appearance", "Pure black background", current.pureBlackColorBackground, restored.pureBlackColorBackground, ::toggle)
                change("Appearance", "Animations", current.motionMode, restored.motionMode) { if (it == MotionMode.SYSTEM) "Standard" else "Reduced" }
                change("App list", "Keyboard on open", current.searchAutofocus, restored.searchAutofocus, ::toggle)
                change("App list", "A–Z navigation", current.alphabetScrubber, restored.alphabetScrubber, ::toggle)
                val beforeHome = current.homeShortcuts.associateBy { it.id }
                val afterHome = restored.homeShortcuts.associateBy { it.id }
                fun describe(shortcut: HomeShortcut?) = shortcut?.let {
                    "${it.name} · ${it.icon.label} icon · ${actionName(it.app to it.command, "Choose an action")}" } ?: "Not on Home"
                (beforeHome.keys + afterHome.keys).forEach { id ->
                    val before = beforeHome[id]
                    val after = afterHome[id]
                    if (before == null || after == null) {
                        change("Home shortcuts", (after ?: before)!!.name, before, after, ::describe)
                    } else {
                        change("Home shortcuts", "${before.name} · Name", before.name, after.name) { it }
                        change("Home shortcuts", "${after.name} · Icon", before.icon, after.icon) { it.label }
                        change("Home shortcuts", "${after.name} · Action", before.app to before.command, after.app to after.command) { actionName(it, "Choose an action") }
                    }
                }
                if (current.homeShortcuts.map { it.id } != restored.homeShortcuts.map { it.id }) {
                    add(RestoreChange("Home shortcuts", "Order", current.homeShortcuts.map { it.name }.joinToString(" → ").ifEmpty { "Empty" },
                        restored.homeShortcuts.map { it.name }.joinToString(" → ").ifEmpty { "Empty" }))
                }
                change("Clock & date", "Clock", current.clock, restored.clock) { appName(it, "System clock") }
                change("Clock & date", "Calendar", current.calendar, restored.calendar) { appName(it, "Choose an app") }
                CornerPosition.entries.forEach { position ->
                    val before = current.shortcut(position)
                    val after = restored.shortcut(position)
                    change("Quick corners", "${position.label} shortcut", before.app to before.command, after.app to after.command) { actionName(it, "Empty") }
                    change("Quick corners", "${position.label} icon", before.icon, after.icon) { it.label }
                }
            }
            return RestorePreview(current, requested, restored, changes, unavailable)
        }
    }
}
