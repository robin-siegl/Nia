package dev.robinsiegl.nia.model

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

data class AlphabetSection(val label: String, val index: Int)

object AppSearch {
    private val marks = Regex("\\p{M}+")
    private fun normalized(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(marks, "").lowercase(Locale.ROOT).trim()
    private fun heading(label: String): Char = normalized(label).firstOrNull()?.uppercaseChar()
        ?.takeIf { it in 'A'..'Z' } ?: '#'

    fun filter(apps: List<AppEntry>, query: String): List<AppEntry> {
        val needle = normalized(query)
        val collator = Collator.getInstance(Locale.ENGLISH).apply { strength = Collator.PRIMARY }
        return apps.asSequence().filter { normalized(it.label).contains(needle) }
            .sortedWith { a, b ->
                // Keep the English A–Z and # buckets contiguous for alphabet navigation.
                val bySection = heading(a.label).compareTo(heading(b.label))
                val byName = collator.compare(a.label, b.label)
                if (bySection != 0) bySection else if (byName != 0) byName else a.target.key.compareTo(b.target.key)
            }.toList()
    }

    fun sections(sortedApps: List<AppEntry>): List<AlphabetSection> = sortedApps.mapIndexed { index, app ->
        AlphabetSection(heading(app.label).toString(), index)
    }.distinctBy { it.label }
}
