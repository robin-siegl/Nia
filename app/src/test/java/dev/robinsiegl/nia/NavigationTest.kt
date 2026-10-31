package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.*
import org.junit.Assert.*
import org.junit.Test

class NavigationTest {
    @Test fun `back from app assignment returns to settings then app list`() {
        val nav = Navigation().push(Screen.Apps).push(Screen.Settings).push(Screen.Picker(HeaderTarget.CLOCK))
        assertEquals(Screen.Settings, nav.back().screen)
        assertEquals(Screen.Apps, nav.back().back().screen)
    }
    @Test fun `Home clears the entire launcher navigation stack`() {
        val nav = Navigation().push(Screen.Apps).push(Screen.Settings).home()
        assertEquals(listOf(Screen.Home), nav.stack)
        assertEquals(Screen.Home, nav.back().screen)
    }
    @Test fun `opening the same destination twice does not duplicate it`() {
        assertEquals(2, Navigation().push(Screen.Apps).push(Screen.Apps).stack.size)
    }
    @Test fun `assignment picker returns to its settings page and then the overview`() {
        val detail = Screen.SettingsDetail(SettingsSection.ASSIGNMENTS)
        val nav = Navigation().push(Screen.Settings).push(detail).push(Screen.Picker(HeaderTarget.CALENDAR))
        assertEquals(detail, nav.back().screen)
        assertEquals(Screen.Settings, nav.back().back().screen)
        assertEquals(Screen.Home, nav.home().screen)
    }
}
