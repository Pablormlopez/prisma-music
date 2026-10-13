@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.prisma.music

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalVisualTheme=staticCompositionLocalOf { VisualTheme.CHROMATIC }
@Composable fun ActualArtwork(song:Song,details:Library.Details?,modifier:Modifier) {
    val uri=LocalAnimatedArtwork.current
    val movement=coverMovement()
    if(uri!=null) ImportedArtwork(uri,modifier.then(movement))
    else if(details?.art!=null) Image(details.art.asImageBitmap(),"Carátula de ${song.album}",contentScale=ContentScale.Crop,modifier=modifier.then(movement)) else Cover(song,modifier.then(movement))
}
@Composable fun ThemeArtwork(song:Song,details:Library.Details?,theme:VisualTheme) {
    if(theme.hasAlbumAtmosphere() && LocalAdvanced.current.fullCover) {
        IntegratedArtwork(song,details,Modifier.fillMaxSize().semantics {contentDescription="Diseño ${theme.title}"})
        return
    }
    val c=MaterialTheme.colorScheme
    val corner=when(theme) {VisualTheme.MINIMAL->4.dp;VisualTheme.MONOCHROME->16.dp;VisualTheme.NIGHT->12.dp;VisualTheme.STUDIO->32.dp;VisualTheme.SUNSET->40.dp;else->28.dp}
    BoxWithConstraints(Modifier.fillMaxSize().semantics {contentDescription="Diseño ${theme.title}"},contentAlignment=Alignment.Center) {
        // Both dimensions constrain the cover: controls remain usable on short displays.
        val edge=minOf(maxWidth,maxHeight)-if(theme==VisualTheme.GLASS) 12.dp else 4.dp
        Box(Modifier.size(edge.coerceAtLeast(1.dp)).shadow(if(theme==VisualTheme.GLASS || theme==VisualTheme.MONOCHROME) 4.dp else 8.dp,RoundedCornerShape(corner)).clip(RoundedCornerShape(corner))) {
            ActualArtwork(song,details,Modifier.fillMaxSize())
            if(theme==VisualTheme.CHROMATIC || theme==VisualTheme.SUNSET) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,c.primary.copy(alpha=.16f)))))

            if(theme==VisualTheme.STUDIO) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.7f))))) {
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text("EN TU COLECCIÓN",color=Color.White.copy(alpha=.8f),fontSize=10.sp,letterSpacing=2.sp)
                    Text(song.album,color=Color.White,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                }
            }
            if(theme==VisualTheme.NIGHT) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.22f)))))
        }
    }
}
@Composable fun ThemeSongTile(song:Song,onPlay:()->Unit,onAdd:()->Unit) {
    val theme=LocalVisualTheme.current;val c=MaterialTheme.colorScheme
    Surface(shape=RoundedCornerShape(if(theme==VisualTheme.GLASS) 30.dp else 20.dp),color=if(theme==VisualTheme.GLASS)c.surface.copy(alpha=.4f) else c.surface,border=if(theme==VisualTheme.GLASS) BorderStroke(1.dp,c.primary.copy(alpha=.3f)) else null,modifier=Modifier.padding(4.dp).combinedClickable(onClick=onPlay,onLongClick=onAdd,onLongClickLabel="Opciones de canción").then(if(theme==VisualTheme.GLASS) Modifier.crystal() else Modifier)) {
        Column(Modifier.padding(if(theme==VisualTheme.GLASS)12.dp else 10.dp)) {
            Cover(song,Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(if(theme==VisualTheme.GLASS)22.dp else 14.dp)))
            Text(song.title,color=c.onSurface,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=10.dp))
            Text(song.subtitle(LocalPersonalization.current.info),color=c.onSurfaceVariant,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {Text(time(song.duration),fontSize=10.sp,color=c.onSurfaceVariant);PrismaIconButton(onClick=onAdd,modifier=Modifier.size(32.dp)) {Icon(Icons.Rounded.MoreVert,"Opciones de ${song.title}",Modifier.size(20.dp))}}
        }
    }
}
