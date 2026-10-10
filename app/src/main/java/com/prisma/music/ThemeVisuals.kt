package com.prisma.music

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    if(details?.art!=null) Image(details.art.asImageBitmap(),"Carátula de ${song.album}",contentScale=ContentScale.Crop,modifier=modifier) else Cover(song,modifier)
}
@Composable fun ThemeArtwork(song:Song,details:Library.Details?,theme:VisualTheme) {
    val c=MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().semantics {contentDescription="Diseño ${theme.title}"},contentAlignment=Alignment.Center) {
        when(theme) {
            VisualTheme.CHROMATIC->Box(Modifier.fillMaxSize().clip(RoundedCornerShape(36.dp)).background(Brush.linearGradient(listOf(c.primary.copy(alpha=.4f),c.tertiary.copy(alpha=.2f),c.background)))) {
                ActualArtwork(song,details,Modifier.align(Alignment.Center).padding(22.dp).fillMaxHeight(.84f).aspectRatio(1f).clip(RoundedCornerShape(28.dp)))
                Text("CHROMATIC / FLOW",fontSize=10.sp,letterSpacing=3.sp,fontWeight=FontWeight.Bold,color=c.onBackground,modifier=Modifier.align(Alignment.BottomCenter).padding(12.dp))
            }
            VisualTheme.GLASS->Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center,modifier=Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth(.9f).aspectRatio(1f).crystal().background(c.surface.copy(alpha=.22f)).padding(18.dp)) {
                    ActualArtwork(song,details,Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)))
                }
                Spacer(Modifier.height(18.dp))
                Box(Modifier.crystal().background(c.surface.copy(alpha=.35f))) {Text("LIQUID GLASS",fontSize=10.sp,letterSpacing=3.sp,color=c.onSurface,modifier=Modifier.padding(horizontal=24.dp,vertical=12.dp))}
            }
            VisualTheme.MINIMAL->Column(Modifier.fillMaxSize().padding(top=18.dp),verticalArrangement=Arrangement.Center) {
                Text("${song.artist.uppercase()}",fontSize=11.sp,letterSpacing=2.sp,color=c.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                Spacer(Modifier.height(16.dp));Row(verticalAlignment=Alignment.CenterVertically) {ActualArtwork(song,details,Modifier.size(110.dp).clip(RoundedCornerShape(4.dp)));Text("MUSIC\nIN ITS\nPURE FORM.",fontSize=26.sp,lineHeight=28.sp,fontWeight=FontWeight.Light,color=c.onBackground,modifier=Modifier.padding(start=20.dp))};Spacer(Modifier.height(25.dp));HorizontalDivider(color=c.outlineVariant);Text("PURE MINIMAL",fontSize=10.sp,letterSpacing=3.sp,color=c.onSurfaceVariant,modifier=Modifier.padding(top=15.dp))
            }
            VisualTheme.NIGHT->Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
                ActualArtwork(song,details,Modifier.fillMaxSize());Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,c.background.copy(alpha=.2f),c.background))));Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {Text("BLACK NIGHT",fontSize=11.sp,letterSpacing=3.sp,fontWeight=FontWeight.Bold,color=c.primary);Text(song.album,fontSize=24.sp,fontWeight=FontWeight.Bold,color=c.onBackground,maxLines=2,overflow=TextOverflow.Ellipsis)}
            }
        }
    }
}
@Composable fun ThemeSongTile(song:Song,onPlay:()->Unit,onAdd:()->Unit) {
    val theme=LocalVisualTheme.current;val c=MaterialTheme.colorScheme
    Surface(onClick=onPlay,shape=RoundedCornerShape(if(theme==VisualTheme.GLASS) 30.dp else 20.dp),color=if(theme==VisualTheme.GLASS)c.surface.copy(alpha=.4f) else c.surface,border=if(theme==VisualTheme.GLASS) BorderStroke(1.dp,c.primary.copy(alpha=.3f)) else null,modifier=Modifier.padding(4.dp).then(if(theme==VisualTheme.GLASS) Modifier.crystal() else Modifier)) {
        Column(Modifier.padding(if(theme==VisualTheme.GLASS)16.dp else 10.dp)) {
            Cover(song,Modifier.fillMaxWidth().aspectRatio(1f).clip(if(theme==VisualTheme.GLASS) CircleShape else RoundedCornerShape(14.dp)))
            Text(song.title,color=c.onSurface,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=10.dp))
            Text(song.artist,color=c.onSurfaceVariant,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {Text(time(song.duration),fontSize=10.sp,color=c.onSurfaceVariant);IconButton(onClick=onAdd,modifier=Modifier.size(32.dp)) {Icon(Icons.Rounded.PlaylistAdd,"Añadir ${song.title} a una lista",Modifier.size(20.dp))}}
        }
    }
}
