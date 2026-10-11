@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.prisma.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** MediaStore dates describe when audio was added, not when it was played or modified. */
fun recentlyAdded(songs:List<Song>):List<Song> = songs.sortedWith(compareByDescending<Song> {it.dateAdded}.thenByDescending {it.id.toLongOrNull() ?: 0L}.thenBy {it.title})

@Composable fun ArtistScreen(artist:String,initialAlbum:String?,songs:List<Song>,currentId:String,onPlay:(List<Song>,Song)->Unit,onAdd:(List<Song>)->Unit,onOptions:(Song)->Unit) {
    val collection=remember(songs,artist) {songs.filter {it.artist==artist}}
    var tab by rememberSaveable(artist) {mutableStateOf("Canciones")}
    var album by rememberSaveable(artist,initialAlbum) {mutableStateOf(initialAlbum)}
    val albums=remember(collection) {collection.groupBy {it.album}.toSortedMap()}
    androidx.activity.compose.BackHandler(album!=null) {album=null;tab="Álbumes"}
    val queue=if(album==null) collection else collection.filter {it.album==album}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        Text(artist,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("${collection.size} canciones · ${albums.size} álbumes en tu teléfono",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=8.dp))
        if(album!=null) {
            PrismaTextButton(onClick={album=null;tab="Álbumes"}) {Icon(Icons.Rounded.ArrowBack,null);Text("Todos los álbumes")}
            Text(album!!,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(vertical=8.dp))
        } else Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Canciones","Álbumes").forEach {label->PrismaFilterChip(tab==label,{tab=label},label={Text(label)})}
        }
        if(collection.isEmpty() || album!=null && queue.isEmpty()) EmptyCard("No hay canciones de este artista","Actualiza la biblioteca o revisa las carpetas excluidas.",Icons.Rounded.Person)
        else if(tab=="Álbumes" && album==null) LazyColumn(contentPadding=PaddingValues(bottom=20.dp)) {
            items(albums.entries.toList(),key={it.key}) {(title,tracks)->
                ListItem(headlineContent={Text(title,maxLines=2,overflow=TextOverflow.Ellipsis)},supportingContent={Text("${tracks.size} canciones")},leadingContent={Cover(tracks.first(),Modifier.size(54.dp))},trailingContent={Icon(Icons.Rounded.ChevronRight,null)},modifier=Modifier.clickable {album=title}.semantics {contentDescription="Ver canciones del álbum $title"})
            }
        } else {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                PrismaButton(onClick={onPlay(queue,queue.first())}) {Icon(Icons.Rounded.PlayArrow,null);Text("Reproducir")}
                PrismaOutlinedButton(onClick={onAdd(queue)}) {Icon(Icons.Rounded.PlaylistAdd,null);Text("Añadir a una lista")}
            }
            LazyColumn(contentPadding=PaddingValues(bottom=20.dp)) {items(queue,key={it.id}) {song->SongRow(song,currentId,{onPlay(queue,song)},onOptions={onOptions(song)})}}
        }
    }
}

@Composable fun PlaylistSongPicker(name:String,songs:List<Song>,existingIds:Set<String>,onDismiss:()->Unit,onAdd:(List<Song>)->Unit) {
    var search by rememberSaveable {mutableStateOf("")}
    var chosen by remember {mutableStateOf(emptySet<String>())}
    val available=remember(songs,existingIds) {songs.filter {it.id !in existingIds}}
    val filtered=remember(available,search) {available.filter {search.isBlank() || "${it.title} ${it.artist} ${it.album}".contains(search,true)}}
    val selection=available.filter {it.id in chosen}
    LaunchedEffect(available) {chosen=chosen.intersect(available.map {it.id}.toSet())}
    fun toggle(id:String) {chosen=if(id in chosen) chosen-id else chosen+id}
    ModalBottomSheet(onDismissRequest=onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.88f).padding(horizontal=20.dp)) {
            Text("Añadir canciones",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Text("A $name · las canciones ya incluidas no se repiten",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=8.dp))
            OutlinedTextField(search,{search=it},label={Text("Buscar canciones para añadir")},singleLine=true,leadingIcon={Icon(Icons.Rounded.Search,null)},modifier=Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("${selection.size} seleccionadas",Modifier.weight(1f))
                PrismaTextButton(enabled=filtered.isNotEmpty(),onClick={val ids=filtered.map {it.id}.toSet();chosen=if(chosen.containsAll(ids)) chosen-ids else chosen+ids}) {Text("Seleccionar visibles")}
            }
            if(filtered.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {Text(if(available.isEmpty()) "Todas las canciones de tu biblioteca ya están en esta lista." else "No hay coincidencias.")}
            else LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(vertical=8.dp)) {
                items(filtered,key={it.id}) {song->
                    Row(Modifier.fillMaxWidth().toggleable(value=song.id in chosen,role=Role.Checkbox,onValueChange={toggle(song.id)}).semantics {contentDescription="Seleccionar ${song.title} para $name"}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(song.id in chosen,null)
                        Cover(song,Modifier.size(44.dp))
                        Column(Modifier.weight(1f).padding(start=12.dp)) {Text(song.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold);Text("${song.artist} · ${song.album}",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)}
                    }
                }
            }
            PrismaButton(enabled=selection.isNotEmpty(),onClick={onAdd(selection);onDismiss()},modifier=Modifier.fillMaxWidth()) {Text("Añadir ${selection.size} canciones")}
            PrismaTextButton(onClick=onDismiss,modifier=Modifier.fillMaxWidth()) {Text("Cancelar")}
            Spacer(Modifier.height(16.dp))
        }
    }
}
