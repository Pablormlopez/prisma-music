package com.prisma.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.session.MediaController

/** Fullscreen local listening view; follows device rotation without forcing orientation. */
@Composable internal fun AmbientPlayer(song:Song,details:Library.Details?,prefs:Preferences,position:Long,playing:Boolean,controller:MediaController?,onDismiss:()->Unit) {
    val options=LocalAdvanced.current
    val lines=remember(song.id,details,prefs.lyrics(song.id)) {Lyrics.parse(details?.embeddedLyrics?.takeIf {it.isNotBlank()} ?: prefs.lyrics(song.id))}
    val active=if(options.syncLyrics) lines.indexOfLast {it.time!=null && it.time<=position} else -1
    val state=rememberLazyListState()
    LaunchedEffect(song.id,active) {if(active>=0) state.animateScrollToItem(active)}
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        val view=androidx.compose.ui.platform.LocalView.current
        DisposableEffect(view,prefs.keepScreenOn) {
            val previous=view.keepScreenOn
            view.keepScreenOn=prefs.keepScreenOn
            onDispose {view.keepScreenOn=previous}
        }
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            AlbumAtmosphere(details)
            val landscape=maxWidth>maxHeight
            val cover:@Composable ()->Unit={
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    ActualArtwork(song,details,Modifier.size(if(landscape) (maxHeight*.35f).coerceIn(64.dp,210.dp) else (maxHeight*.25f).coerceIn(64.dp,190.dp)).clip(RoundedCornerShape(24.dp)))
                    Text(song.title,color=Color.White,fontWeight=FontWeight.Bold,maxLines=2,modifier=Modifier.padding(top=12.dp))
                    Text(song.artist,color=Color.White.copy(alpha=.7f),maxLines=1)
                }
            }
            val lyrics:@Composable ()->Unit={
                LazyColumn(state=state,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(vertical=32.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                    if(lines.isEmpty()) item {Text("Importa una letra LRC o TXT desde el reproductor para verla aquí.",color=Color.White.copy(alpha=.7f))}
                    itemsIndexed(lines) {index,line->Text(line.text.ifBlank {"♪"},fontSize=options.lyricSize.sp,fontWeight=if(index==active) FontWeight.Bold else FontWeight.Medium,color=Color.White.copy(alpha=if(active<0 || index==active) 1f else .45f),textAlign=if(options.centerLyrics) TextAlign.Center else TextAlign.Start,modifier=Modifier.fillMaxWidth())}
                }
            }
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Modo ambiente",color=Color.White,modifier=Modifier.weight(1f))
                    IconButton(onClick=onDismiss) {Icon(Icons.Rounded.Close,"Cerrar modo ambiente",tint=Color.White)}
                }
                if(landscape) Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(32.dp)) {Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {cover()};Box(Modifier.weight(1.5f)) {lyrics()}}
                else {cover();Box(Modifier.weight(1f)) {lyrics()}}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {
                    IconButton(enabled=controller!=null,onClick={controller?.seekToPrevious()}) {Icon(Icons.Rounded.SkipPrevious,"Anterior",tint=Color.White)}
                    IconButton(enabled=controller!=null,onClick={controller?.let {if(it.isPlaying) it.pause() else it.play()}}) {Icon(if(playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,if(playing) "Pausar" else "Reproducir",tint=Color.White)}
                    IconButton(enabled=controller!=null,onClick={controller?.seekToNextMediaItem()}) {Icon(Icons.Rounded.SkipNext,"Siguiente",tint=Color.White)}
                }
            }
        }
    }
}
