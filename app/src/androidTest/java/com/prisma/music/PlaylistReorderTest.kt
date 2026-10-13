package com.prisma.music

import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaylistReorderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dragSavesOrderReopeningRetainsItAndCancelDoesNotSave() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = Preferences(context)
        val previous = prefs.playlists()
        val name = "Reorder instrumentation"
        fun song(id: String) = Song(id, Uri.EMPTY, id, "Artist", "Album", "", "", "", 60000, 0, "", Uri.EMPTY, 0)
        val songs = listOf(song("Alpha"), song("Beta"), song("Gamma"))
        var lists by mutableStateOf(mapOf(name to songs.map { it.id }))
        var epoch by mutableIntStateOf(0)
        prefs.savePlaylists(lists)
        try {
            compose.setContent {
                MaterialTheme {
                    key(epoch) {
                        PlaylistTracks(lists.getValue(name).map { id -> songs.first { it.id == id } }, lists.getValue(name), "",
                            onPlay = { _, _ -> }, onOptions = {}, onRemove = {},
                            onMove = { expected, id, target ->
                                if (lists[name] == expected) {
                                    lists = lists + (name to movePlaylistSong(expected, id, target))
                                    prefs.savePlaylists(lists)
                                }
                            })
                    }
                }
            }
            fun drag(from: String, to: String, cancelGesture: Boolean = false) {
                val source = compose.onNodeWithContentDescription("Mantén pulsado y arrastra $from para ordenar")
                val destination = compose.onNodeWithContentDescription("Mantén pulsado y arrastra $to para ordenar")
                val delta = destination.fetchSemanticsNode().boundsInRoot.center.y - source.fetchSemanticsNode().boundsInRoot.center.y
                source.performTouchInput {
                    down(center); advanceEventTime(700)
                    moveBy(Offset(0f, delta), 300)
                    if (cancelGesture) cancel() else up()
                }
                compose.waitForIdle()
            }
            drag("Alpha", "Gamma")
            compose.runOnIdle {
                assertEquals(listOf("Beta", "Gamma", "Alpha"), Preferences(context).playlists()[name])
                lists = Preferences(context).playlists(); epoch++
            }
            compose.waitForIdle()
            val beta = compose.onNodeWithContentDescription("Mantén pulsado y arrastra Beta para ordenar").fetchSemanticsNode().boundsInRoot
            val alpha = compose.onNodeWithContentDescription("Mantén pulsado y arrastra Alpha para ordenar").fetchSemanticsNode().boundsInRoot
            assert(beta.top < alpha.top)
            drag("Alpha", "Beta", cancelGesture = true)
            compose.runOnIdle { assertEquals(listOf("Beta", "Gamma", "Alpha"), prefs.playlists()[name]) }
            drag("Alpha", "Beta")
            compose.runOnIdle { assertEquals(listOf("Alpha", "Beta", "Gamma"), prefs.playlists()[name]) }
        } finally { prefs.savePlaylists(previous) }
    }
}
