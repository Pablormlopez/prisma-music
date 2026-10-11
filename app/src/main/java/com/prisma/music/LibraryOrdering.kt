package com.prisma.music

import java.text.Collator
import java.util.Locale

enum class SongSort(val title: String) {
    TITLE_ASC("A a Z"), TITLE_DESC("Z a A"),
    NEWEST("Más nuevas primero"), OLDEST("Más antiguas primero")
}

fun sortSongs(songs: List<Song>, order: SongSort): List<Song> {
    val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
    val titleOrder = Comparator<Song> { a, b -> collator.compare(a.title, b.title) }.thenBy { it.id }
    val dates = compareBy<Song> { it.dateAdded }.thenBy { it.id.toLongOrNull() ?: 0L }.then(titleOrder)
    return songs.sortedWith(when (order) {
        SongSort.TITLE_ASC -> titleOrder
        SongSort.TITLE_DESC -> titleOrder.reversed()
        SongSort.NEWEST -> dates.reversed()
        SongSort.OLDEST -> dates
    })
}

internal val mainPages = listOf("Inicio", "Biblioteca", "Listas", "Sonido", "Ajustes")

internal fun adjacentPage(page: String, distance: Float, threshold: Float): String? {
    val index = mainPages.indexOf(page)
    if (index < 0 || kotlin.math.abs(distance) < threshold) return null
    return mainPages.getOrNull(index + if (distance < 0) 1 else -1)
}
