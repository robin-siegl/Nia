package dev.robinsiegl.nia.model

import java.text.Normalizer
import java.util.Locale

/** One searchable catalog shared by every shortcut editor. Labels never become stored identities. */
object IconCatalog {
    val icons: List<ShortcutIcon> = ShortcutIcon.entries.sortedBy { it.label.lowercase(Locale.ROOT) }
    private val combiningMarks = Regex("\\p{M}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")
    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(combiningMarks, "").lowercase(Locale.ROOT).replace(separators, " ").trim()
    private val searchText by lazy {
        icons.associateWith { normalize("${it.label} ${it.label.filter(Char::isLetterOrDigit)} ${it.category.label} ${it.keywords}") }
    }

    fun search(query: String, category: IconCategory? = null): List<ShortcutIcon> {
        val terms = normalize(query).split(' ').filter { it.isNotEmpty() }
        return icons.filter { icon -> (category == null || icon.category == category) && terms.all { it in searchText.getValue(icon) } }
    }
}
