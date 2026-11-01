package dev.robinsiegl.nia

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import dev.robinsiegl.nia.model.*
import org.junit.Assert.assertTrue

/** Configure a real optional command when a test specifically needs a tappable Apps shortcut. */
internal fun appsCommand() = HomeShortcut("apps-command", "Apps", ShortcutIcon.APP_GRID, command = ShortcutCommand.OPEN_APPS)

internal fun ComposeContentTestRule.openAppsFromHome() {
    val action = onNodeWithTag("home-apps-surface").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        .first { it.label == "Open Apps" }
    runOnIdle { assertTrue(action.action()) }
}
