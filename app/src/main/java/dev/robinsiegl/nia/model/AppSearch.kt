package dev.robinsiegl.nia.model

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

data class AlphabetSection(val label: String, val index: Int)

object AppSearch {
    private val marks = Regex("\\p{M}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")
    private val camelBoundary = Regex("(?<=[\\p{Ll}\\p{N}])(?=\\p{Lu})|(?<=\\p{Lu})(?=\\p{Lu}\\p{Ll})")
    private data class SearchEntry(val app: AppEntry, val name: String, val words: List<String>, val initials: String, val heading: Char)
    private fun normalized(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(marks, "").lowercase(Locale.ROOT).trim()
    private fun heading(label: String): Char = normalized(label).firstOrNull()?.uppercaseChar()
        ?.takeIf { it in 'A'..'Z' } ?: '#'

    fun filter(apps: List<AppEntry>, query: String): List<AppEntry> {
        val needle = normalized(query)
        val queryWords = needle.split(separators).filter { it.isNotEmpty() }
        val collator = Collator.getInstance(Locale.ENGLISH).apply { strength = Collator.PRIMARY }
        return apps.asSequence().map { app ->
            val name = normalized(app.label)
            val words = normalized(camelBoundary.replace(app.label, " ")).split(separators).filter { it.isNotEmpty() }
            SearchEntry(app, name, words, words.joinToString("") { it.take(1) }, heading(app.label))
        }.filter { entry ->
            needle.isEmpty() || entry.name.contains(needle) ||
                (queryWords.size > 1 && queryWords.all { word -> entry.words.any { it.startsWith(word) } }) ||
                (queryWords.size == 1 && needle.length >= 2 && entry.initials.startsWith(needle))
        }
            .sortedWith { a, b ->
                // Keep the English A–Z and # buckets contiguous for alphabet navigation.
                val bySection = a.heading.compareTo(b.heading)
                val byName = collator.compare(a.app.label, b.app.label)
                if (bySection != 0) bySection else if (byName != 0) byName else a.app.target.key.compareTo(b.app.target.key)
            }.map { it.app }.toList()
    }

    fun sections(sortedApps: List<AppEntry>): List<AlphabetSection> = sortedApps.mapIndexed { index, app ->
        AlphabetSection(heading(app.label).toString(), index)
    }.distinctBy { it.label }
}
