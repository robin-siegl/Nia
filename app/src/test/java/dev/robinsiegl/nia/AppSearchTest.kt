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
    @Test fun `initials match words and camel case labels`() {
        val apps = listOf(app("Google Maps"), app("WhatsApp"), app("Google Play Music"), app("Music"))
        assertEquals(listOf("Google Maps"), AppSearch.filter(apps, "gm").map { it.label })
        assertEquals(listOf("WhatsApp"), AppSearch.filter(apps, "wa").map { it.label })
        assertEquals(listOf("Google Play Music"), AppSearch.filter(apps, "gpm").map { it.label })
    }
    @Test fun `multiple words match prefixes in any order`() {
        val apps = listOf(app("Google Maps"), app("Google Calendar"), app("Maps"))
        for (query in listOf("maps google", "ma goo", " GOO   MA ", "g.m")) {
            assertEquals(query, listOf("Google Maps"), AppSearch.filter(apps, query).map { it.label })
        }
        assertTrue(AppSearch.filter(apps, "maps calendar").isEmpty())
    }
    @Test fun `smart search normalizes accented initials and punctuation`() {
        val apps = listOf(app("École Notes"), app("Photo-Editor"), app("Other"))
        assertEquals(listOf("École Notes"), AppSearch.filter(apps, "en").map { it.label })
        assertEquals(listOf("Photo-Editor"), AppSearch.filter(apps, "pe").map { it.label })
        assertTrue(AppSearch.filter(apps, "!!!").isEmpty())
    }
    @Test fun `smart matches retain alphabetical ordering`() {
        val apps = listOf(app("Map Garden"), app("Music Gallery"), app("More Games"))
        assertEquals(listOf("Map Garden", "More Games", "Music Gallery"), AppSearch.filter(apps, "mg").map { it.label })
    }
    @Test fun `alphabet sections point at the first app for each actual letter`() {
        val apps = AppSearch.filter(listOf(app("Zebra"), app("École"), app("Email"), app("Calendar"),
            app("Camera"), app("3D Viewer")), "")
        assertEquals(listOf(AlphabetSection("#", 0), AlphabetSection("C", 1), AlphabetSection("E", 3),
            AlphabetSection("Z", 5)), AppSearch.sections(apps))
    }
    @Test fun `alphabet sections handle empty names and non Latin labels without losing apps`() {
        val apps = listOf(app(""), app("日本語"), app(" alpha"), app("Ärger"))
        assertEquals(listOf(AlphabetSection("#", 0), AlphabetSection("A", 2)), AppSearch.sections(apps))
        assertTrue(AppSearch.sections(emptyList()).isEmpty())
    }
    @Test fun `names outside the English alphabet share one contiguous bucket`() {
        val apps = listOf(app("日本語"), app("Zebra"), app("3D Viewer"), app("Alphabet"), app("Календарь"))
        val sorted = AppSearch.filter(apps, "")
        assertEquals(apps.size, sorted.size)
        assertEquals(listOf(AlphabetSection("#", 0), AlphabetSection("A", 3), AlphabetSection("Z", 4)), AppSearch.sections(sorted))
    }
}
