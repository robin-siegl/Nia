package dev.robinsiegl.nia.model

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

object AppSearch {
    private val marks = Regex("\\p{M}+")
    private fun normalized(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(marks, "").lowercase(Locale.ROOT).trim()

    fun filter(apps: List<AppEntry>, query: String): List<AppEntry> {
        val needle = normalized(query)
        val collator = Collator.getInstance(Locale.ENGLISH).apply { strength = Collator.PRIMARY }
        return apps.asSequence().filter { normalized(it.label).contains(needle) }
            .sortedWith { a, b ->
                val byName = collator.compare(a.label, b.label)
                if (byName != 0) byName else a.target.key.compareTo(b.target.key)
            }.toList()
    }
}
