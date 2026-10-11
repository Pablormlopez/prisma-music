@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.prisma.music

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { PrismaApp() } }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PrismaApp() {
    val context=LocalContext.current
    val prefs=remember { Preferences(context) }
    var personal by remember { mutableStateOf(prefs.personalization) }
    var actionSong by remember { mutableStateOf<Song?>(null) }
    var theme by remember { mutableStateOf(prefs.theme) }
    var mode by remember { mutableStateOf(prefs.mode) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var libraryVersion by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var currentId by remember { mutableStateOf("") }
    var position by remember { mutableLongStateOf(0) }
    var duration by remember { mutableLongStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var shuffled by remember { mutableStateOf(false) }
    var repeat by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    val pageStates=rememberSaveableStateHolder()
    val swipeThreshold=with(LocalDensity.current) {72.dp.toPx()}
    var page by rememberSaveable { mutableStateOf("Inicio") }
    var details by remember { mutableStateOf<Library.Details?>(null) }
    var lists by remember { mutableStateOf(prefs.playlists()) }
    var addToList by remember { mutableStateOf<List<Song>?>(null) }
    var newListDialog by remember { mutableStateOf(false) }
    var playerReturn by rememberSaveable {mutableStateOf("Inicio")}
    var recentIds by remember {mutableStateOf(prefs.recentIds())}
    var artistName by rememberSaveable {mutableStateOf("")}
    var artistAlbum by rememberSaveable {mutableStateOf<String?>(null)}
    var libraryCategory by rememberSaveable {mutableStateOf("Canciones")}
    fun openArtist(song:Song,album:Boolean=false) {artistName=song.artist;artistAlbum=if(album) song.album else null;page="Artista"}
    fun openPlayer() {playerReturn=page;page="Reproduciendo"}
    androidx.activity.compose.BackHandler(enabled=page=="Reproduciendo" || page=="Temas" || page=="Personalización" || page=="Artista") { page=when(page) {"Temas"->"Personalización";"Personalización"->"Ajustes";"Artista"->"Reproduciendo";else->playerReturn} }
    val scope=rememberCoroutineScope()
    val permission=if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED) }
    val requestPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted=it; if(it) libraryVersion++ }
    val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val lifecycle=LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _, event -> if(event==Lifecycle.Event.ON_RESUME) { granted=ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED; libraryVersion++ } }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(Unit) {
        val future=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()
        var disposed=false
        val listener=object : Player.Listener {
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) { error="No se pudo reproducir este archivo. ${e.errorCodeName}" }
        }
        future.addListener({ if(!disposed) runCatching { future.get() }.onSuccess { controller=it; it.addListener(listener) }.onFailure { error="No se pudo iniciar el reproductor." } },ContextCompat.getMainExecutor(context))
        onDispose { disposed=true; controller?.removeListener(listener); controller=null; MediaController.releaseFuture(future) }
    }
    LaunchedEffect(controller) {
        withContext(Dispatchers.Main.immediate) {
        while(controller!=null) {
            controller?.let { currentId=it.currentMediaItem?.mediaId ?: ""; position=it.currentPosition.coerceAtLeast(0); duration=it.duration.coerceAtLeast(0); playing=it.isPlaying; shuffled=it.shuffleModeEnabled; repeat=it.repeatMode }
            delay(250)
        }
        }
    }
    LaunchedEffect(granted,libraryVersion) {
        if(granted) { busy=true; runCatching { withContext(Dispatchers.IO) { Library.scan(context) } }.onSuccess { songs=it }.onFailure { error="No se pudo leer la biblioteca. Revisa el permiso de música." }; busy=false }
        else songs=emptyList()
    }
    val visible=remember(songs,libraryVersion) { Library.visible(songs,prefs) }
    val current=songs.find { it.id==currentId }
    LaunchedEffect(currentId) { if(currentId.isNotBlank()) {prefs.rememberSong(currentId);recentIds=prefs.recentIds()}; details=null; current?.let { s -> details=withContext(Dispatchers.IO) { Library.details(context,s) } } }
    val dark=when(mode) { LightMode.DARK->true; LightMode.LIGHT->false; LightMode.SYSTEM->isSystemInDarkTheme() }
    SideEffect { (context as? ComponentActivity)?.window?.let { window -> androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply {isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark} } }
    val baseAccent=accentColor(personal.accent) ?: when(theme) { VisualTheme.CHROMATIC->Color(details?.color ?: 0xffb0a2ff.toInt()); VisualTheme.GLASS->if(dark) Color(0xff8bd6f8) else Color(0xff1455bd); VisualTheme.MINIMAL->if(dark) Color(0xffc6c5ce) else Color(0xff494650); VisualTheme.NIGHT->if(dark) Color(0xff66e2ac) else Color(0xff006c43); VisualTheme.STUDIO->if(dark) Color(0xffc8b5ff) else Color(0xff6446a6); VisualTheme.SUNSET->if(dark) Color(0xffffbd91) else Color(0xff9b3d14) }
    // Keep readable controls even when a cover produces a very dark/light swatch.
    val accent by animateColorAsState(if(theme==VisualTheme.CHROMATIC || personal.accent.isNotEmpty()) androidx.compose.ui.graphics.lerp(baseAccent,if(dark) Color.White else Color.Black,if(dark) .55f else .6f) else baseAccent,animationSpec=tween(450),label="cover")
    val bg=if(dark) { if(theme==VisualTheme.NIGHT) Color(0xff060a09) else when(theme) {VisualTheme.GLASS->Color(0xff11161e);VisualTheme.MINIMAL->Color(0xff161616);VisualTheme.SUNSET->Color(0xff261910);else->Color(0xff101018)} } else when(theme) {VisualTheme.GLASS->Color(0xfffafbfe);VisualTheme.STUDIO->Color(0xfff3f0f9);VisualTheme.SUNSET->Color(0xfffff1e6);VisualTheme.MINIMAL->Color(0xfffffdf8);VisualTheme.NIGHT->Color(0xffeaf5ee);else->Color(0xfff6f5fa)}
    val colors=if(dark) darkColorScheme(primary=accent,secondary=accent,primaryContainer=androidx.compose.ui.graphics.lerp(bg,accent,.22f),onPrimaryContainer=Color(0xfff0eef7),secondaryContainer=androidx.compose.ui.graphics.lerp(bg,accent,.22f),onSecondaryContainer=Color(0xfff0eef7),onSecondary=Color(0xff101018),background=bg,surface=Color(0xff1c1c27),onPrimary=Color(0xff101018),onBackground=Color(0xfff0eef7),onSurface=Color(0xfff0eef7),onSurfaceVariant=Color(0xffd5d8e4),surfaceVariant=Color(0xff323341),outline=Color(0xff9399ac)) else lightColorScheme(primary=accent,secondary=accent,primaryContainer=androidx.compose.ui.graphics.lerp(bg,accent,.16f),onPrimaryContainer=Color(0xff101018),secondaryContainer=androidx.compose.ui.graphics.lerp(bg,accent,.16f),onSecondaryContainer=Color(0xff101018),onSecondary=Color.White,background=bg,surface=Color.White,onPrimary=Color.White)
    fun play(queue:List<Song>,song:Song) {
        if(controller==null) { error="El reproductor se está iniciando. Intenta de nuevo."; return }
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        controller?.run { setMediaItems(queue.map { it.mediaItem() },queue.indexOf(song).coerceAtLeast(0),0); prepare(); play() }
    }
    val glassLayer=androidx.compose.ui.graphics.rememberGraphicsLayer()
    val glassScene=remember(glassLayer) {GlassScene(glassLayer)}
    MaterialTheme(colorScheme=colors) {
        CompositionLocalProvider(LocalVisualTheme provides theme, LocalPersonalization provides personal, LocalGlassScene provides if(theme==VisualTheme.GLASS) glassScene else null) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(if(theme==VisualTheme.CHROMATIC || theme==VisualTheme.GLASS || personal.accent.isNotEmpty()) androidx.compose.ui.graphics.lerp(bg,accent,if(dark) .16f else .08f) else bg,bg)))) {
            if(theme==VisualTheme.GLASS) GlassBackdrop(details?.glassBackdrop,bg)
            Scaffold(containerColor=Color.Transparent,contentColor=colors.onBackground,bottomBar={
                Column {
                    current?.takeIf { page!="Reproduciendo" }?.let { song -> Surface(onClick={openPlayer()},modifier=Modifier.padding(horizontal=14.dp,vertical=5.dp).fillMaxWidth().then(if(theme==VisualTheme.GLASS) Modifier.sampleGlassBackdrop(22f).liquidSurface(22f) else Modifier).semantics {contentDescription="Abrir reproductor"},shape=RoundedCornerShape(22.dp),color=colors.surface.copy(alpha=if(theme==VisualTheme.GLASS) .20f else 1f),border=BorderStroke(1.dp,accent.copy(alpha=.15f))) {
                        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                            Cover(song,Modifier.size(44.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(song.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold); Text(song.artist,style=MaterialTheme.typography.labelMedium,color=colors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis) }
                            PrismaIconButton(onClick={ controller?.let { if(it.isPlaying) it.pause() else it.play() } }) { Icon(if(playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,if(playing) "Pausar" else "Reproducir") }
                            PrismaIconButton(onClick={controller?.seekToNextMediaItem()}) { Icon(Icons.Rounded.SkipNext,"Siguiente") }
                        }
                    } }
                    if(page!="Reproduciendo") FloatingNavigation(page) {if(it=="Biblioteca") libraryCategory="Canciones";page=it}
                }
            }) { padding ->
                val glassCollection=theme==VisualTheme.GLASS && (page=="Inicio" || page=="Biblioteca")
                CompositionLocalProvider(LocalCollectionBottomPadding provides if(glassCollection) padding.calculateBottomPadding()+24.dp else 24.dp) {
                Column(Modifier.fillMaxSize().padding(top=padding.calculateTopPadding(),bottom=if(glassCollection) 0.dp else padding.calculateBottomPadding()).then(if(theme==VisualTheme.GLASS) Modifier.captureGlassScene(glassScene) else Modifier)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=if(page=="Reproduciendo") 4.dp else 14.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(page=="Reproduciendo") PrismaIconButton(onClick={page=playerReturn}) { Icon(Icons.Rounded.KeyboardArrowDown,if(playerReturn=="Biblioteca") "Volver a la biblioteca" else "Cerrar reproductor") }
                        if(page=="Artista") PrismaIconButton(onClick={page="Reproduciendo"}) {Icon(Icons.Rounded.ArrowBack,"Volver al reproductor")}
                        if(page=="Temas" || page=="Personalización") PrismaIconButton(onClick={page=if(page=="Temas") "Personalización" else "Ajustes"}) {Icon(Icons.Rounded.ArrowBack,if(page=="Temas") "Volver a personalización" else "Volver a ajustes")}
                        Column(Modifier.weight(1f)) {
                            if(page!="Reproduciendo") Text("PRISMA MUSIC",fontSize=10.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold,color=accent)
                            Text(page,fontSize=if(page=="Reproduciendo") 14.sp else 32.sp,fontWeight=FontWeight.Bold)
                        }
                        if(page=="Inicio") PrismaIconButton(onClick={page="Temas"}) {Icon(Icons.Rounded.Palette,"Abrir temas")}
                        if(page=="Biblioteca" || page=="Inicio") PrismaIconButton(onClick={libraryVersion++}) { Icon(Icons.Rounded.Refresh,"Actualizar biblioteca") }
                    }
                    error?.let { message -> Surface(color=colors.errorContainer,shape=RoundedCornerShape(16.dp),modifier=Modifier.padding(horizontal=16.dp)) { Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) { Text(message,Modifier.weight(1f),color=colors.onErrorContainer,style=MaterialTheme.typography.bodySmall); PrismaIconButton(onClick={error=null}) { Icon(Icons.Rounded.Close,"Cerrar mensaje") } } } }
                    AnimatedContent(targetState=page,transitionSpec={contentMotion(personal.motion,personal.motionDuration).using(null)},label="page",modifier=Modifier.weight(1f).semantics {contentDescription="Contenido de $page"}.pointerInput(page,swipeThreshold) {
                        if(page in mainPages) {
                            var distance=0f
                            detectHorizontalDragGestures(onDragStart={distance=0f},onDragCancel={distance=0f},onDragEnd={
                                adjacentPage(page,distance,swipeThreshold)?.let {page=it}
                            }) {change,amount->change.consume();distance+=amount}
                        }
                    }) { route -> pageStates.SaveableStateProvider(if(route=="Biblioteca") "$route:$libraryCategory" else route) {when(route) {
                        "Inicio"->if(!granted) PermissionCard {requestPermission.launch(permission)} else HomeScreen(visible,busy,current,recentIds,onPlay={queue,song->play(queue,song);openPlayer()},onLibrary={libraryCategory="Canciones";page="Biblioteca"},onRecent={libraryCategory="Recién añadidas";page="Biblioteca"},onOptions={actionSong=it})
                        "Personalización"->PersonalizationScreen(personal,mode,{mode=it;prefs.mode=it},{personal=it;prefs.personalization=it},{page="Temas"})
                        "Temas"->ThemesScreen(theme,mode,{theme=it;prefs.theme=it},{mode=it;prefs.mode=it})
                        "Biblioteca"->if(!granted) PermissionCard { requestPermission.launch(permission) } else LibraryScreen(visible,busy,currentId,onPlay=::play,onAdd={addToList=it},onOptions={actionSong=it},initialCategory=libraryCategory)
                        "Listas"->PlaylistScreen(lists,visible,currentId,onCreate={newListDialog=true},onDelete={key->lists=lists-key; prefs.savePlaylists(lists)},onRemove={key,id->lists=lists+(key to lists.getValue(key).filterNot { it==id }); prefs.savePlaylists(lists)},onPlay=::play,onOptions={actionSong=it},onAddSongs={name,selection->lists=lists+(name to (lists[name].orEmpty()+selection.map {it.id}).distinct());prefs.savePlaylists(lists)})
                        "Artista"->ArtistScreen(artistName,artistAlbum,visible,currentId,onPlay=::play,onAdd={addToList=it},onOptions={actionSong=it})
                        "Sonido"->EqScreen(prefs) { config->prefs.eq=config; controller?.sendCustomCommand(SessionCommand("prisma.eq",Bundle.EMPTY),Bundle.EMPTY) }
                        "Ajustes"->SettingsScreen(prefs,songs,{libraryVersion++}, { controller?.sendCustomCommand(SessionCommand("prisma.settings",Bundle.EMPTY),Bundle.EMPTY) },onThemes={page="Personalización"})
                        "Reproduciendo"->if(current!=null) PlayerScreen(current,details,theme,prefs,position,duration,playing,shuffled,repeat,controller,onDismiss={page=playerReturn},onArtist={openArtist(it)},onAlbum={openArtist(it,true)}) else EmptyCard("Selecciona una canción","Tu música local aparecerá aquí.",Icons.Rounded.MusicNote)
                    } } }
                }
                }
            }
        }
        actionSong?.let { song -> SongActions(song,onDismiss={actionSong=null},onPlay={play(visible,song)},onNext={
            controller?.let { c -> if(c.mediaItemCount==0) play(listOf(song),song) else { c.addMediaItem(c.currentMediaItemIndex+1,song.mediaItem()); c.shuffleModeEnabled=false } } ?: run {error="El reproductor se está iniciando."}
        },onQueue={controller?.let { c -> if(c.mediaItemCount==0) play(listOf(song),song) else c.addMediaItem(song.mediaItem()) } ?: run {error="El reproductor se está iniciando."}},onList={addToList=listOf(song)},onDeleted={ deleted ->
            controller?.let { c -> for(i in c.mediaItemCount-1 downTo 0) {if(c.getMediaItemAt(i).mediaId==deleted.id) c.removeMediaItem(i)} }
            lists=lists.mapValues { (_,ids)->ids.filterNot {it==deleted.id} };prefs.savePlaylists(lists)
            songs=songs.filterNot {it.id==deleted.id};libraryVersion++;if(currentId==deleted.id) page="Biblioteca"
        },onError={error=it}) }
        if(newListDialog) NameDialog(onDismiss={newListDialog=false},onSave={name->if(name !in lists) { lists=lists+(name to emptyList()); prefs.savePlaylists(lists); newListDialog=false }},exists={it in lists})
        addToList?.let { selection -> AlertDialog(onDismissRequest={addToList=null},title={Text("Añadir ${selection.size} canciones a una lista")},text={ Column(Modifier.verticalScroll(rememberScrollState())) { if(lists.isEmpty()) Text("Crea tu primera lista en la pestaña Listas."); lists.keys.forEach { name-> PrismaTextButton(onClick={lists=lists+(name to (lists.getValue(name)+selection.map {it.id}).distinct());prefs.savePlaylists(lists);addToList=null},modifier=Modifier.fillMaxWidth()) {Text(name)} } } },confirmButton={PrismaTextButton(onClick={addToList=null}){Text("Cerrar")}}) }
        }
    }
}

@Composable fun Cover(song:Song?,modifier:Modifier=Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha=.25f),MaterialTheme.colorScheme.surface))),contentAlignment=Alignment.Center) {
        Icon(Icons.Rounded.MusicNote,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.fillMaxSize(.4f))
        if(song!=null) AsyncImage(model=song.art,contentDescription="Carátula de ${song.album}",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
    }
}
@Composable fun EmptyCard(title:String,subtitle:String,icon:ImageVector) {
    Column(Modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) { Icon(icon,null,Modifier.size(64.dp),tint=MaterialTheme.colorScheme.primary); Spacer(Modifier.height(18.dp)); Text(title,fontSize=21.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant) }
}
@Composable fun PermissionCard(onGrant:()->Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) { Cover(null,Modifier.size(130.dp)); Spacer(Modifier.height(24.dp)); Text("Tu colección empieza aquí",fontSize=23.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(10.dp)); Text("Permite leer la música de tu teléfono. Prisma funciona sin conexión y excluye los audios de WhatsApp de forma predeterminada."); Spacer(Modifier.height(20.dp)); PrismaButton(onClick=onGrant){Text("Permitir acceso a mi música")} }
}
@Composable fun SongRow(song:Song,currentId:String,onPlay:()->Unit,onAdd:(()->Unit)?=null,onOptions:(()->Unit)?=null) {
    val visualTheme=LocalVisualTheme.current
    val personal=LocalPersonalization.current
    val compact=personal.layout==LibraryLayout.COMPACT
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(if(visualTheme==VisualTheme.NIGHT) 4.dp else 16.dp)).combinedClickable(onClick=onPlay,onLongClick=onOptions,onLongClickLabel="Opciones de canción").padding(horizontal=8.dp,vertical=if(compact) 4.dp else 9.dp),verticalAlignment=Alignment.CenterVertically) {
        if(visualTheme!=VisualTheme.MINIMAL && !compact) {Cover(song,Modifier.size(if(visualTheme==VisualTheme.NIGHT) 42.dp else 52.dp)); Spacer(Modifier.width(14.dp))} else {Text("♪",fontSize=22.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(end=18.dp))}; Column(Modifier.weight(1f)) { Text(song.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold,color=if(currentId==song.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface); Text(song.subtitle(personal.info),maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall) }
        Text(time(song.duration),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(onOptions!=null) PrismaIconButton(onClick=onOptions) { Icon(Icons.Rounded.MoreVert,"Opciones de ${song.title}") }
        else if(onAdd!=null) PrismaIconButton(onClick=onAdd) { Icon(Icons.Rounded.PlaylistAdd,"Añadir ${song.title} a una lista") }
    }
}
@Composable fun LibraryScreen(songs:List<Song>,busy:Boolean,currentId:String,onPlay:(List<Song>,Song)->Unit,onAdd:(List<Song>)->Unit,onOptions:(Song)->Unit,initialCategory:String="Canciones") {
    val visualTheme=LocalVisualTheme.current
    val personal=LocalPersonalization.current
    val context=LocalContext.current
    val sortPrefs=remember(context) {Preferences(context)}
    var sort by remember {mutableStateOf(sortPrefs.librarySort)}
    var sortMenu by remember {mutableStateOf(false)}
    var recentSort by rememberSaveable {mutableStateOf(SongSort.NEWEST)}
    var choosing by rememberSaveable {mutableStateOf(false)}
    var chosen by remember {mutableStateOf(setOf<String>())}
    var search by rememberSaveable {mutableStateOf("")}
    var category by rememberSaveable(initialCategory) {mutableStateOf(initialCategory)}
    var selected by rememberSaveable(initialCategory) {mutableStateOf<String?>(null)}
    val filtered=remember(songs,search) {songs.filter {search.isBlank() || "${it.title} ${it.artist} ${it.album} ${it.genre}".contains(search,true)}}
    fun key(s:Song)=when(category) {"Artistas"->s.artist; "Álbumes"->"${s.album} · ${s.artist}"; "Géneros"->s.genre; else->s.folder.ifBlank {"Sin carpeta"}}
    val songCategory=category=="Canciones" || category=="Recién añadidas"
    val activeSort=if(category=="Recién añadidas") recentSort else sort
    val queue=remember(filtered,category,selected,activeSort) {sortSongs(if(songCategory || selected==null) filtered else filtered.filter {key(it)==selected},activeSort)}
    val selection=songs.filter {it.id in chosen}
    fun toggle(group:List<Song>) {val ids=group.map {it.id}.toSet();chosen=if(chosen.containsAll(ids)) chosen-ids else chosen+ids}
    androidx.activity.compose.BackHandler(choosing) {choosing=false;chosen=emptySet()}
    LaunchedEffect(songs) {chosen=chosen.intersect(songs.map {it.id}.toSet())}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Busca en tu colección")},leadingIcon={Icon(Icons.Rounded.Search,null)},singleLine=true,shape=RoundedCornerShape(22.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)) {listOf("Canciones","Recién añadidas","Artistas","Álbumes","Géneros","Carpetas").forEach {title->PrismaFilterChip(category==title,{category=title;selected=null},label={Text(title)})}}
        Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
                PrismaTextButton(onClick={choosing=!choosing;chosen=emptySet()}) {Text(if(choosing) "Cancelar" else "Seleccionar varias")}
                if(choosing) {
                    Text("${selection.size}",Modifier.weight(1f),fontWeight=FontWeight.Bold)
                    PrismaIconButton(onClick={toggle(queue)}) {Icon(Icons.Rounded.SelectAll,"Seleccionar o desmarcar las canciones visibles")}
                    FilledTonalButton(enabled=selection.isNotEmpty(),onClick={onAdd(selection)},modifier=Modifier.semantics {contentDescription="Añadir selección a una playlist"}) {Icon(Icons.Rounded.PlaylistAdd,null);Text("Añadir")}
                } else {Spacer(Modifier.weight(1f)); Text("${queue.size} canciones",style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(end=12.dp))}
            }
        }
        if(songCategory || selected!=null) Box {
            PrismaTextButton(onClick={sortMenu=true},modifier=Modifier.semantics {contentDescription="Ordenar canciones"}) {Icon(Icons.Rounded.Sort,null);Text("Orden: ${activeSort.title}",Modifier.padding(start=8.dp))}
            DropdownMenu(expanded=sortMenu,onDismissRequest={sortMenu=false}) {
                val options=if(category=="Recién añadidas") listOf(SongSort.NEWEST,SongSort.OLDEST) else SongSort.entries
                options.forEach {option->DropdownMenuItem(text={Text(option.title)},leadingIcon={if(activeSort==option) Icon(Icons.Rounded.Check,null)},onClick={
                    if(category=="Recién añadidas") recentSort=option else {sort=option;sortPrefs.librarySort=option}
                    sortMenu=false
                })}
            }
        }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(selected!=null) Row(verticalAlignment=Alignment.CenterVertically) {PrismaTextButton(onClick={selected=null},modifier=Modifier.weight(1f)) {Icon(Icons.Rounded.ArrowBack,null);Text(selected!!,maxLines=1,overflow=TextOverflow.Ellipsis)};PrismaIconButton(onClick={onAdd(queue)}) {Icon(Icons.Rounded.PlaylistAdd,"Añadir todas las canciones de este grupo a una playlist")}}
        val grid=!choosing && (personal.layout==LibraryLayout.GRID || (personal.layout==LibraryLayout.THEME && (visualTheme==VisualTheme.CHROMATIC || visualTheme==VisualTheme.GLASS)))
        if(filtered.isEmpty()) EmptyCard(if(busy) "Buscando música…" else "No hay canciones",if(search.isNotBlank()) "Prueba otra búsqueda." else "Copia música al teléfono y pulsa Actualizar.",Icons.Rounded.LibraryMusic)
        else if(grid && (songCategory || selected!=null)) LazyVerticalGrid(columns=GridCells.Fixed(2),contentPadding=PaddingValues(bottom=if(LocalVisualTheme.current==VisualTheme.GLASS) LocalCollectionBottomPadding.current else 16.dp)) {items(queue,key={it.id}) {song->ThemeSongTile(song,{onPlay(queue,song)},{onOptions(song)})}}
        else LazyColumn(contentPadding=PaddingValues(bottom=if(LocalVisualTheme.current==VisualTheme.GLASS) LocalCollectionBottomPadding.current else 16.dp)) {
            if(!songCategory && selected==null) items(filtered.groupBy(::key).toSortedMap().entries.toList(),key={it.key}) {group->
                Surface(onClick={if(choosing) toggle(group.value) else selected=group.key},shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.75f),modifier=Modifier.padding(vertical=5.dp).fillMaxWidth()) {
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(choosing) Checkbox(group.value.all {it.id in chosen},{toggle(group.value)})
                        Cover(group.value.first(),Modifier.size(54.dp));Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {Text(group.key,maxLines=2,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold);Text("${group.value.size} canciones",style=MaterialTheme.typography.bodySmall)}
                        if(!choosing) PrismaIconButton(onClick={onAdd(group.value)}) {Icon(Icons.Rounded.PlaylistAdd,"Añadir ${group.key} a una playlist")}
                    }
                }
            } else items(queue,key={it.id}) {song->
                Row(verticalAlignment=Alignment.CenterVertically) {
                    if(choosing) Checkbox(song.id in chosen,{toggle(listOf(song))},modifier=Modifier.semantics {contentDescription="Seleccionar ${song.title}"})
                    Box(Modifier.weight(1f)) {SongRow(song,currentId,{if(choosing) toggle(listOf(song)) else onPlay(queue,song)},onOptions=if(choosing) null else {{onOptions(song)}})}
                }
            }
        }
    }
}
@Composable fun NameDialog(onDismiss:()->Unit,onSave:(String)->Unit,exists:(String)->Boolean) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Nueva lista")},text={OutlinedTextField(name,{name=it},label={Text("Nombre")},singleLine=true,isError=exists(name.trim()),supportingText={if(exists(name.trim())) Text("Ya existe una lista con ese nombre.")})},confirmButton={PrismaTextButton(onClick={onSave(name.trim())},enabled=name.isNotBlank() && !exists(name.trim())) {Text("Crear")}},dismissButton={PrismaTextButton(onClick=onDismiss){Text("Cancelar")}})
}
@Composable fun PlaylistScreen(lists:Map<String,List<String>>,songs:List<Song>,currentId:String,onCreate:()->Unit,onDelete:(String)->Unit,onRemove:(String,String)->Unit,onPlay:(List<Song>,Song)->Unit,onOptions:(Song)->Unit,onAddSongs:(String,List<Song>)->Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }; var delete by remember { mutableStateOf<String?>(null) }
    var adding by remember {mutableStateOf(false)}
    androidx.activity.compose.BackHandler(selected!=null && !adding) {selected=null}
    selected?.takeIf {adding}?.let {name->PlaylistSongPicker(name,songs,lists[name].orEmpty().toSet(),onDismiss={adding=false},onAdd={onAddSongs(name,it)})}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        if(selected==null) {
            PrismaButton(onClick=onCreate,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Icon(Icons.Rounded.Add,null);Text("Crear lista de reproducción",Modifier.padding(8.dp))}
            if(lists.isEmpty()) EmptyCard("Una lista para cada momento","Crea una lista y añade canciones desde tu biblioteca.",Icons.Rounded.QueueMusic)
            else LazyColumn { items(lists.entries.toList(),key={it.key}) { (name,ids)-> ListItem(headlineContent={Text(name,fontWeight=FontWeight.SemiBold)},supportingContent={Text("${ids.size} canciones")},leadingContent={Icon(Icons.Rounded.QueueMusic,null)},trailingContent={PrismaIconButton(onClick={delete=name}){Icon(Icons.Rounded.DeleteOutline,"Eliminar lista $name")}},modifier=Modifier.clip(RoundedCornerShape(16.dp)).clickable {selected=name},colors=ListItemDefaults.colors(containerColor=Color.Transparent)) } }
        } else {
            PrismaTextButton(onClick={selected=null}) {Icon(Icons.Rounded.ArrowBack,null);Text(selected!!)}
            PrismaOutlinedButton(onClick={adding=true},modifier=Modifier.fillMaxWidth()) {Icon(Icons.Rounded.PlaylistAdd,null);Text("Añadir canciones",Modifier.padding(start=8.dp))}
            val queue=lists[selected].orEmpty().mapNotNull { id->songs.find {it.id==id} }
            if(queue.isEmpty()) EmptyCard("Esta lista está vacía","Pulsa Añadir canciones para empezar. Las canciones de carpetas excluidas no se muestran.",Icons.Rounded.PlaylistAdd)
            else LazyColumn { items(queue,key={it.id}) {song-> Row(verticalAlignment=Alignment.CenterVertically) { Box(Modifier.weight(1f)){SongRow(song,currentId,{onPlay(queue,song)},onOptions={onOptions(song)})}; PrismaIconButton(onClick={onRemove(selected!!,song.id)}){Icon(Icons.Rounded.RemoveCircleOutline,"Quitar de la lista")} } } }
        }
    }
    delete?.let { name->AlertDialog(onDismissRequest={delete=null},title={Text("Eliminar lista")},text={Text("Se eliminará «$name». Los archivos de música se conservarán.")},confirmButton={PrismaTextButton(onClick={onDelete(name);delete=null}){Text("Eliminar")}},dismissButton={PrismaTextButton(onClick={delete=null}){Text("Cancelar")}}) }
}
fun time(ms:Long):String { val s=ms.coerceAtLeast(0)/1000;return "%d:%02d".format(s/60,s%60) }
