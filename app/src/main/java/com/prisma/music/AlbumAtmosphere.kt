package com.prisma.music

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

internal fun VisualTheme.hasAlbumAtmosphere() = this == VisualTheme.GLASS || this == VisualTheme.CHROMATIC

/** A single opaque artwork atmosphere; white foreground stays readable in both app modes. */
@Composable internal fun AlbumAtmosphere(details:Library.Details?) {
    val motion=LocalPersonalization.current
    Crossfade(details,animationSpec=if(motion.motion==MotionStyle.NONE) snap() else tween(motion.motionDuration),label="albumAtmosphere",modifier=Modifier.fillMaxSize()) { album ->
        Box(Modifier.fillMaxSize().background(Color(0xff38353a))) {
            album?.glassBackdrop?.let { Image(it.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop) }
            // Black is composited after the art, limiting peak background luminance under text.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.58f)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.12f),Color.Black.copy(alpha=.30f)))))
        }
    }
}

/** Fade the existing cover into the same atmosphere instead of adding a tinted card. */
@Composable internal fun IntegratedArtwork(song:Song,details:Library.Details?,modifier:Modifier) {
    Box(modifier.graphicsLayer {compositingStrategy=CompositingStrategy.Offscreen}.drawWithContent {
        drawContent()
        drawRect(Brush.verticalGradient(0f to Color.White,.62f to Color.White,1f to Color.Transparent),blendMode=BlendMode.DstIn)
    }) { ActualArtwork(song,details,Modifier.fillMaxSize()) }
}

/** System music volume, independent of the service's per-song fade envelope. */
@Composable internal fun MusicVolume() {
    val context=LocalContext.current
    val audio=remember(context) {context.getSystemService(Context.AUDIO_SERVICE) as AudioManager}
    val maximum=remember(audio) {audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)}
    var volume by remember {mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())}
    var changing by remember {mutableStateOf(false)}
    var blocked by remember {mutableStateOf(false)}
    LaunchedEffect(audio) {while(true) {if(!changing) volume=audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat();delay(500)}}
    Column {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Rounded.VolumeDown,null,Modifier.size(18.dp),tint=Color.White.copy(alpha=.65f))
            Slider(volume.coerceIn(0f,maximum.toFloat()),{value->
                changing=true
                runCatching {audio.setStreamVolume(AudioManager.STREAM_MUSIC,value.toInt(),0)}.onSuccess {volume=value;blocked=false}.onFailure {blocked=true}
            },onValueChangeFinished={changing=false},valueRange=0f..maximum.toFloat(),enabled=!audio.isVolumeFixed,
                modifier=Modifier.weight(1f).semantics {contentDescription="Volumen multimedia"},
                colors=SliderDefaults.colors(activeTrackColor=Color.White,inactiveTrackColor=Color.White.copy(alpha=.24f),thumbColor=Color.White))
            Icon(Icons.Rounded.VolumeUp,null,Modifier.size(18.dp),tint=Color.White.copy(alpha=.65f))
        }
        if(blocked) Text("Usa los botones de volumen del teléfono.",style=MaterialTheme.typography.labelSmall)
    }
}
