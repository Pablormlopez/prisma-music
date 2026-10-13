package com.prisma.music

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistOrderingTest {
    @Test fun movesDownToLastPosition() {
        assertEquals(listOf("b", "c", "a"), movePlaylistSong(listOf("a", "b", "c"), "a", "c"))
    }
    @Test fun movesUpToFirstPosition() {
        assertEquals(listOf("c", "a", "b"), movePlaylistSong(listOf("a", "b", "c"), "c", "a"))
    }
    @Test fun preservesFilteredSongsAndMembership() {
        assertEquals(listOf("hidden", "b", "a", "c"), movePlaylistSong(listOf("a", "hidden", "b", "c"), "a", "b"))
    }
    @Test fun ignoresMissingSongsAndNoOpMoves() {
        val ids = listOf("a", "b")
        assertEquals(ids, movePlaylistSong(ids, "missing", "a"))
        assertEquals(ids, movePlaylistSong(ids, "a", "missing"))
        assertEquals(ids, movePlaylistSong(ids, "a", "a"))
        assertEquals(emptyList<String>(), movePlaylistSong(emptyList(), "a", "b"))
    }
}
