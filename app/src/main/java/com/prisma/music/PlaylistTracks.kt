package com.prisma.music

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

@Composable
fun PlaylistTracks(
    songs: List<Song>, storedIds: List<String>, currentId: String,
    onPlay: (List<Song>, Song) -> Unit, onOptions: (Song) -> Unit,
    onRemove: (String) -> Unit, onMove: (List<String>, String, String) -> Unit
) {
    val state = rememberLazyListState()
    var pinned by remember { mutableStateOf<PinnableContainer.PinnedHandle?>(null) }
    DisposableEffect(Unit) { onDispose { pinned?.release() } }
    var dragging by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var center by remember { mutableFloatStateOf(0f) }
    var target by remember { mutableIntStateOf(-1) }
    var snapshot by remember { mutableStateOf(emptyList<String>()) }
    val latestIds by rememberUpdatedState(storedIds)
    val latestSongs by rememberUpdatedState(songs)
    val latestMove by rememberUpdatedState(onMove)
    val edge = with(LocalDensity.current) { 64.dp.toPx() }
    val scrollStep = with(LocalDensity.current) { 8.dp.toPx() }
    fun resolveTarget() {
        state.layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }
            ?.let { target = it.index }
    }
    fun reset() { pinned?.release(); pinned = null; dragging = null; offset = 0f; target = -1 }
    // Cancel if the collection or library filters change during a gesture.
    LaunchedEffect(storedIds, songs.map { it.id }) { reset() }
    LaunchedEffect(dragging) {
        while (dragging != null && isActive) {
            val layout = state.layoutInfo
            val scroll = when {
                center < layout.viewportStartOffset + edge -> -scrollStep
                center > layout.viewportEndOffset - edge -> scrollStep
                else -> 0f
            }
            if (scroll != 0f) offset += state.scrollBy(scroll)
            resolveTarget()
            delay(16)
        }
    }
    LazyColumn(state = state, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            val active = dragging == song.id
            val pinnable = LocalPinnableContainer.current
            Row(
                Modifier.fillMaxWidth().zIndex(if (active) 1f else 0f)
                    .graphicsLayer { translationY = if (active) offset else 0f }
                    .background(
                        if (active || (dragging != null && index == target)) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0f), MaterialTheme.shapes.medium
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).semantics {
                        customActions = buildList {
                            if (index > 0) add(CustomAccessibilityAction("Mover hacia arriba") {
                                latestMove(latestIds, song.id, latestSongs[index - 1].id); true
                            })
                            if (index < songs.lastIndex) add(CustomAccessibilityAction("Mover hacia abajo") {
                                latestMove(latestIds, song.id, latestSongs[index + 1].id); true
                            })
                        }
                    }.pointerInput(song.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                val item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == song.id }
                                if (item != null) {
                                    pinned = pinnable?.pin()
                                    snapshot = latestIds.toList(); dragging = song.id; offset = 0f
                                    center = item.offset + item.size / 2f; target = item.index
                                }
                            },
                            onDragCancel = { reset() },
                            onDragEnd = {
                                val destination = latestSongs.getOrNull(target)
                                if (dragging == song.id && snapshot == latestIds && destination != null)
                                    latestMove(snapshot, song.id, destination.id)
                                reset()
                            }
                        ) { change, amount ->
                            change.consume()
                            if (dragging == song.id) {
                                offset += amount.y; center += amount.y; resolveTarget()
                            }
                        }
                    }, contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.DragHandle, "Mantén pulsado y arrastra ${song.title} para ordenar", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.weight(1f)) { SongRow(song, currentId, { onPlay(songs, song) }, onOptions = { onOptions(song) }) }
                PrismaIconButton(onClick = { onRemove(song.id) }) {
                    Icon(Icons.Rounded.RemoveCircleOutline, "Quitar ${song.title} de la lista")
                }
            }
        }
    }
}
