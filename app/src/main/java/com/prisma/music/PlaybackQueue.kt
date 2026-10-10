@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.prisma.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController

// Use playlist positions: remote timeline UIDs are not stable across session updates.
internal data class QueueEntry(val index:Int,val mediaId:String,val title:String,val artist:String,val current:Boolean)
internal fun queueEntries(player:Player):List<QueueEntry> {
    val timeline=player.currentTimeline
    val window=Timeline.Window()
    val result=mutableListOf<QueueEntry>()
    var index=timeline.getFirstWindowIndex(player.shuffleModeEnabled)
    while(index!=androidx.media3.common.C.INDEX_UNSET && result.size<timeline.windowCount) {
        timeline.getWindow(index,window)
        val metadata=window.mediaItem.mediaMetadata
        result+=QueueEntry(index,window.mediaItem.mediaId,metadata.title?.toString() ?: "Canción",metadata.artist?.toString().orEmpty(),index==player.currentMediaItemIndex)
        index=timeline.getNextWindowIndex(index,Player.REPEAT_MODE_OFF,player.shuffleModeEnabled)
    }
    return result
}
internal fun moveQueueEntry(player:Player,index:Int,direction:Int) {
    val ordered=queueEntries(player).map {it.index}.toMutableList()
    val from=ordered.indexOf(index)
    val to=from+direction
    if(from<0 || to !in ordered.indices) return
    ordered.add(to,ordered.removeAt(from))
    // Materialize the heard shuffle order and its edit using a local permutation.
    // Native moveMediaItem preserves the playing item, position and play/pause state.
    val positions=(0 until player.mediaItemCount).toMutableList()
    player.shuffleModeEnabled=false
    ordered.forEachIndexed {destination,originalIndex ->
        val source=positions.indexOf(originalIndex)
        if(source!=destination) {
            player.moveMediaItem(source,destination)
            positions.add(destination,positions.removeAt(source))
        }
    }
}

@Composable fun PlaybackQueue(controller:MediaController,onDismiss:()->Unit) {
    var snapshot by remember(controller) {mutableStateOf(controller.currentTimeline,referentialEqualityPolicy())}
    var entries by remember(controller) {mutableStateOf(queueEntries(controller))}
    var shuffled by remember(controller) {mutableStateOf(controller.shuffleModeEnabled)}
    var repeat by remember(controller) {mutableIntStateOf(controller.repeatMode)}
    DisposableEffect(controller) {
        val listener=object:Player.Listener {
            override fun onEvents(player:Player,events:Player.Events) {
                snapshot=player.currentTimeline;entries=queueEntries(player);shuffled=player.shuffleModeEnabled;repeat=player.repeatMode
            }
        }
        controller.addListener(listener)
        onDispose {controller.removeListener(listener)}
    }
    val expectedTimeline=snapshot
    val state=rememberLazyListState()
    LaunchedEffect(controller) {val current=entries.indexOfFirst {it.current};if(current>=0) state.scrollToItem(current)}
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal=16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cola de reproducción",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text("${entries.size} canciones",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick=onDismiss) {Icon(Icons.Rounded.Close,"Cerrar cola")}
            }
            if(shuffled) Text("Al mover canciones se usará tu orden manual.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=8.dp))
            if(repeat==Player.REPEAT_MODE_ONE) Text("La canción actual se repite hasta que desactives la repetición.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=8.dp))
            if(entries.isEmpty()) Text("La cola está vacía",modifier=Modifier.padding(24.dp))
            val current=entries.indexOfFirst {it.current}
            val next=if(repeat==Player.REPEAT_MODE_ONE) current else if(current+1<entries.size) current+1 else if(repeat==Player.REPEAT_MODE_ALL && entries.isNotEmpty()) 0 else -1
            LazyColumn(state=state,modifier=Modifier.fillMaxWidth().heightIn(max=520.dp),contentPadding=PaddingValues(bottom=24.dp)) {
                itemsIndexed(entries,key={_,entry->entry.index}) {index,entry ->
                    Column(Modifier.fillMaxWidth().background(if(entry.current) MaterialTheme.colorScheme.primaryContainer.copy(alpha=.5f) else MaterialTheme.colorScheme.surface,MaterialTheme.shapes.medium).padding(start=12.dp,top=8.dp,end=4.dp,bottom=4.dp)) {
                        Text(if(entry.current) "Sonando ahora" else if(index==next) "A continuación" else "${index+1}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                        Text(entry.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold)
                        Text(entry.artist,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                            IconButton(enabled=index>0,onClick={if(controller.currentTimeline===expectedTimeline) moveQueueEntry(controller,entry.index,-1)}) {Icon(Icons.Rounded.ArrowUpward,"Subir canción ${index+1}")}
                            IconButton(enabled=index<entries.lastIndex,onClick={if(controller.currentTimeline===expectedTimeline) moveQueueEntry(controller,entry.index,1)}) {Icon(Icons.Rounded.ArrowDownward,"Bajar canción ${index+1}")}
                            IconButton(onClick={if(controller.currentTimeline===expectedTimeline && entry.index<controller.mediaItemCount) controller.removeMediaItem(entry.index)}) {Icon(Icons.Rounded.RemoveCircleOutline,"Quitar canción ${index+1} de la cola")}
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
