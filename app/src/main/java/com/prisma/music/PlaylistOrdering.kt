package com.prisma.music

/** Move within the stored order, retaining songs hidden by library filters. */
fun movePlaylistSong(ids: List<String>, songId: String, targetId: String): List<String> {
    val from = ids.indexOf(songId)
    val to = ids.indexOf(targetId)
    if (from < 0 || to < 0 || from == to) return ids
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}
