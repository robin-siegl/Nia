package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.*
import org.junit.Assert.*
import org.junit.Test

class AppSearchTest {
    private fun app(label: String, id: String = label) = AppEntry(AppTarget("test.$id", "Main"), label)
    @Test fun `list and search results remain alphabetical`() {
        val apps = listOf(app("Zebra"), app("Music"), app("Calendar"), app("Camera"))
        assertEquals(listOf("Calendar", "Camera", "Music", "Zebra"), AppSearch.filter(apps, "").map { it.label })
        assertEquals(listOf("Calendar", "Camera"), AppSearch.filter(apps, "ca").map { it.label })
    }
    @Test fun `search normalizes accents case and surrounding whitespace`() {
        assertEquals(listOf("École"), AppSearch.filter(listOf(app("École")), "  ECO  ").map { it.label })
    }
    @Test fun `same label retains distinct launch targets with stable ordering`() {
        val apps = listOf(app("Maps", "second"), app("Maps", "first"))
        assertEquals(listOf("test.first", "test.second"), AppSearch.filter(apps, "maps").map { it.target.packageName })
    }
    @Test fun `no matches returns an empty result`() { assertTrue(AppSearch.filter(listOf(app("Music")), "xyz").isEmpty()) }
}
