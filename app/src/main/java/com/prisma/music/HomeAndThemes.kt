@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.prisma.music

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun FloatingNavigation(page:String,onPage:(String)->Unit) {
    if(LocalVisualTheme.current==VisualTheme.GLASS) {LiquidNavigation(page,onPage);return}
    val c=MaterialTheme.colorScheme
    val glass=false
    Surface(shape=RoundedCornerShape(38.dp),color=c.surface.copy(alpha=if(glass) .78f else .97f),border=BorderStroke(1.dp,c.outlineVariant.copy(alpha=.4f)),shadowElevation=if(glass) 12.dp else 5.dp,modifier=Modifier.navigationBarsPadding().padding(horizontal=14.dp,vertical=8.dp).fillMaxWidth().then(if(glass) Modifier.crystal() else Modifier)) {
        Row(Modifier.padding(6.dp),verticalAlignment=Alignment.CenterVertically) {
            listOf("Inicio" to Icons.Rounded.Home,"Biblioteca" to Icons.Rounded.LibraryMusic,"Listas" to Icons.Rounded.QueueMusic,"Sonido" to Icons.Rounded.Equalizer,"Ajustes" to Icons.Rounded.Tune).forEach { (route,icon)->
                val active=page==route || page=="Artista" && route=="Biblioteca" || (page=="Temas" || page=="Personalización") && route=="Ajustes"
                val tint=if(active)c.primary else c.onSurfaceVariant
                Column(Modifier.weight(1f).clip(RoundedCornerShape(30.dp)).background(if(active)c.primary.copy(alpha=.13f) else Color.Transparent).semantics {selected=active}.clickable(role=Role.Tab) {onPage(route)}.padding(vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(icon,null,Modifier.size(25.dp),tint=tint)
                    Text(route,fontSize=10.sp,fontWeight=if(active)FontWeight.Bold else FontWeight.Medium,color=tint,maxLines=1)
                }
            }
        }
    }
}

@Composable fun HomeScreen(songs:List<Song>,busy:Boolean,current:Song?,recentIds:List<String>,onPlay:(List<Song>,Song)->Unit,onLibrary:()->Unit,onRecent:()->Unit,onOptions:(Song)->Unit) {
    val c=MaterialTheme.colorScheme
    if(songs.isEmpty()) {EmptyCard(if(busy) "Buscando tu música…" else "Tu música merece un lugar aquí","Añade canciones al teléfono y actualiza tu colección.",Icons.Rounded.LibraryMusic);return}
    val recent=recentIds.mapNotNull {id->songs.find {it.id==id}}
    val featured=(listOfNotNull(current?.takeIf {it in songs})+recent+songs).distinctBy {it.id}.take(8)
    val added=recentlyAdded(songs)
    val albums=songs.distinctBy {it.album to it.artist}.take(12)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=LocalCollectionBottomPadding.current),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        item {
            Row(Modifier.padding(horizontal=24.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {Text("Tu colección, contigo",fontSize=22.sp,fontWeight=FontWeight.Bold);Text("${songs.size} canciones · sin conexión",fontSize=13.sp,color=c.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))}
                PrismaIconButton(onClick=onLibrary) {Icon(Icons.Rounded.ArrowForward,"Explorar biblioteca")}
            }
        }
        item {
            Text(if(recent.isEmpty()) "Descubre tu colección" else "Vuelve a escuchar",fontSize=21.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=24.dp,end=24.dp,bottom=14.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val width=(maxWidth-64.dp).coerceAtMost(390.dp)
                LazyRow(contentPadding=PaddingValues(horizontal=24.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    items(featured,key={it.id}) {song->
                        Column(Modifier.width(width).combinedClickable(onClick={onPlay(songs,song)},onLongClick={onOptions(song)},onLongClickLabel="Opciones de canción")) {
                            Text(song.album.ifBlank {"Tu música"},fontSize=12.sp,fontWeight=FontWeight.Bold,color=c.primary,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Text(song.title,fontSize=24.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=5.dp))
                            Text(song.artist,fontSize=15.sp,color=c.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=2.dp,bottom=12.dp))
                            Box(Modifier.fillMaxWidth().aspectRatio(1.35f).shadow(6.dp,RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))) {
                                Cover(song,Modifier.fillMaxSize())
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.38f)))))
                                Surface(color=Color.White.copy(alpha=.88f),shape=RoundedCornerShape(28.dp),modifier=Modifier.align(Alignment.BottomEnd).padding(14.dp)) {Icon(Icons.Rounded.PlayArrow,null,tint=Color(0xff20202a),modifier=Modifier.padding(11.dp).size(26.dp))}
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal=24.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("Recién añadidas",fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                PrismaTextButton(onClick=onRecent) {Text("Ver todas")}
            }
            LazyRow(contentPadding=PaddingValues(horizontal=24.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                items(added.take(16),key={it.id}) {song->HomeSongCard(song,{onPlay(added,song)},{onOptions(song)})}
            }
        }
        item {
            Text("Tus álbumes",fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=24.dp,end=24.dp,bottom=14.dp))
            LazyRow(contentPadding=PaddingValues(horizontal=24.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                items(albums,key={"${it.album}\u0000${it.artist}"}) {song->HomeSongCard(song,{onPlay(songs.filter {it.album==song.album && it.artist==song.artist},song)},{onOptions(song)},album=true)}
            }
        }
        item {PrismaTextButton(onClick=onLibrary,modifier=Modifier.padding(horizontal=16.dp)) {Text("Ver toda la biblioteca");Icon(Icons.Rounded.ChevronRight,null)}}
    }
}
@Composable private fun HomeSongCard(song:Song,onPlay:()->Unit,onOptions:()->Unit,album:Boolean=false) {
    Column(Modifier.width(152.dp).combinedClickable(onClick=onPlay,onLongClick=onOptions,onLongClickLabel="Opciones de canción")) {
        Cover(song,Modifier.size(152.dp))
        Text(if(album)song.album else song.title,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=9.dp))
        Text(song.subtitle(LocalPersonalization.current.info),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

fun VisualTheme.palette():List<Color> = when(this) {
    VisualTheme.CHROMATIC->listOf(Color(0xff9580df),Color(0xffe990a4),Color(0xff65b8ce))
    VisualTheme.GLASS->listOf(Color(0xffe7efff),Color(0xffaac7ee),Color(0xff1455bd))
    VisualTheme.MINIMAL->listOf(Color(0xfffaf8f2),Color(0xffd5d2cb),Color(0xff535058))
    VisualTheme.NIGHT->listOf(Color(0xff121f1b),Color(0xff334c42),Color(0xff66e2ac))
    VisualTheme.STUDIO->listOf(Color(0xff302c3a),Color(0xff807193),Color(0xffc8b5ff))
    VisualTheme.SUNSET->listOf(Color(0xffffd3aa),Color(0xfff0a582),Color(0xff9b3d14))
}
fun VisualTheme.description():String = when(this) {
    VisualTheme.CHROMATIC->"Carátulas, color y movimiento"
    VisualTheme.GLASS->"Cristal limpio y navegación flotante"
    VisualTheme.MINIMAL->"Portadas grandes y líneas puras"
    VisualTheme.NIGHT->"Profundidad y contraste verde"
    VisualTheme.STUDIO->"Paneles definidos y pulso visual"
    VisualTheme.SUNSET->"Luz cálida y formas suaves"
}
@Composable fun ThemesScreen(theme:VisualTheme,mode:LightMode,onTheme:(VisualTheme)->Unit,onMode:(LightMode)->Unit) {
    val c=MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Text("Un reproductor con tu estilo",fontSize=21.sp,fontWeight=FontWeight.SemiBold);Text("Elige un diseño. Tu color personalizado se conserva; puedes restablecerlo en Ajustes para usar la paleta del tema.",fontSize=13.sp,color=c.onSurfaceVariant,modifier=Modifier.padding(top=6.dp))}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {LightMode.entries.forEach {m->PrismaFilterChip(selected=mode==m,onClick={onMode(m)},label={Text(m.title,fontSize=11.sp)})}}}
        items(VisualTheme.entries,key={it.name}) {v->
            Surface(onClick={onTheme(v)},shape=RoundedCornerShape(24.dp),color=c.surface.copy(alpha=.88f),border=BorderStroke(if(theme==v)2.dp else 1.dp,if(theme==v)c.primary else c.outlineVariant.copy(alpha=.4f)),modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    ThemePreview(v)
                    Column(Modifier.weight(1f).padding(horizontal=16.dp)) {
                        if(v==VisualTheme.STUDIO || v==VisualTheme.SUNSET) Text("NUEVO",fontSize=9.sp,letterSpacing=1.sp,color=c.primary,fontWeight=FontWeight.Bold)
                        Text(v.title,fontSize=19.sp,fontWeight=FontWeight.Bold)
                        Text(v.description(),fontSize=12.sp,color=c.onSurfaceVariant,modifier=Modifier.padding(top=5.dp))
                        Text(if(theme==v)"En uso" else "Aplicar tema",fontSize=11.sp,color=c.primary,modifier=Modifier.padding(top=8.dp))
                    }
                    Icon(if(theme==v)Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,null,tint=if(theme==v)c.primary else c.outline)
                }
            }
        }
        item {Spacer(Modifier.height(10.dp))}
    }
}
@Composable private fun ThemePreview(theme:VisualTheme) {
    val palette=theme.palette()
    val dark=theme==VisualTheme.NIGHT || theme==VisualTheme.STUDIO
    val fg=if(dark)Color.White else Color(0xff292532)
    Column(Modifier.width(72.dp).height(112.dp).clip(RoundedCornerShape(15.dp)).background(if(dark)palette.first() else palette.first().copy(alpha=.55f)).padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(if(theme==VisualTheme.MINIMAL)2.dp else 9.dp)).background(Brush.linearGradient(palette)),contentAlignment=Alignment.Center) {Icon(Icons.Rounded.GraphicEq,null,Modifier.size(28.dp),tint=fg.copy(alpha=.65f))}
        Box(Modifier.padding(top=8.dp).fillMaxWidth(.8f).height(3.dp).background(fg.copy(alpha=.65f),RoundedCornerShape(2.dp)))
        Box(Modifier.padding(top=4.dp).fillMaxWidth(.55f).height(2.dp).background(fg.copy(alpha=.3f),RoundedCornerShape(2.dp)))
        Row(Modifier.padding(top=7.dp),verticalAlignment=Alignment.CenterVertically) {Icon(Icons.Rounded.SkipPrevious,null,Modifier.size(12.dp),tint=fg);Icon(Icons.Rounded.PlayCircle,null,Modifier.size(21.dp),tint=palette.last());Icon(Icons.Rounded.SkipNext,null,Modifier.size(12.dp),tint=fg)}
    }
}
