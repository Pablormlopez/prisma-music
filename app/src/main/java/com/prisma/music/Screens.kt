@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.prisma.music

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.abs
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun SettingsScreen(theme:VisualTheme,mode:LightMode,prefs:Preferences,songs:List<Song>,personal:Personalization,onPersonal:(Personalization)->Unit,onTheme:(VisualTheme)->Unit,onMode:(LightMode)->Unit,onLibraryChanged:()->Unit,onPlaybackChanged:()->Unit) {
    var hide by remember { mutableStateOf(prefs.hideWhatsApp) }
    var excluded by remember { mutableStateOf(prefs.excluded) }
    var metadata by remember { mutableStateOf(prefs.showMetadata) }
    var screenOn by remember { mutableStateOf(prefs.keepScreenOn) }
    var silence by remember { mutableStateOf(prefs.skipSilence) }
    var speed by remember { mutableFloatStateOf(prefs.playbackSpeed) }
    var minimum by remember { mutableIntStateOf(prefs.minimumDuration) }
    var deadline by remember { mutableLongStateOf(prefs.sleepDeadline) }
    LaunchedEffect(Unit) { while(true) { deadline=prefs.sleepDeadline; kotlinx.coroutines.delay(1000) } }
    var foldersDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle("APARIENCIA","Elige tu ambiente")
        VisualTheme.entries.forEach { v ->
            val swatches=when(v) { VisualTheme.CHROMATIC->listOf(Color(0xff9580df),Color(0xffe990a4),Color(0xff65b8ce));VisualTheme.GLASS->listOf(Color(0xffc5dfeb),Color(0xff8296da),Color(0xffe5f2ff));VisualTheme.MINIMAL->listOf(Color(0xffeae8e4),Color(0xff92919a),Color(0xff535058));VisualTheme.NIGHT->listOf(Color(0xff121f1b),Color(0xff334c42),Color(0xff66e2ac)) }
            Surface(onClick={onTheme(v)},shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=if(v==VisualTheme.GLASS) .6f else 1f),border=BorderStroke(if(theme==v) 2.dp else 1.dp,if(theme==v) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(57.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(swatches)),contentAlignment=Alignment.Center) {Icon(Icons.Rounded.MusicNote,null,tint=Color.White)}
                    Column(Modifier.padding(horizontal=14.dp).weight(1f)) {Text(v.title,fontWeight=FontWeight.SemiBold);Text(when(v) {VisualTheme.CHROMATIC->"Colores de tu carátula";VisualTheme.GLASS->"Cristal, luz y transparencias";VisualTheme.MINIMAL->"Simple, limpio y sereno";VisualTheme.NIGHT->"Contraste y acentos verdes"},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    RadioButton(selected=theme==v,onClick=null)
                }
            }
        }
        PersonalizationSettings(personal,onPersonal)
        SectionTitle("MODO DE COLOR","La luz cambia. Tu música sigue.")
        LightMode.entries.forEach { v -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {onMode(v)}.padding(8.dp),verticalAlignment=Alignment.CenterVertically) {RadioButton(selected=mode==v,onClick={onMode(v)});Text(v.title)} }
        SectionTitle("REPRODUCTOR","Personaliza la escucha")
        SettingSwitch("Mostrar información del archivo", "Calidad técnica: formato, bitrate, frecuencia y canales", metadata) { metadata=it; prefs.showMetadata=it }
        SettingSwitch("Mantener pantalla encendida", "Solo mientras el reproductor completo esté abierto", screenOn) { screenOn=it; prefs.keepScreenOn=it }
        SettingSwitch("Omitir silencios", "Salta tramos silenciosos; puede alterar pausas musicales", silence) { silence=it; prefs.skipSilence=it; onPlaybackChanged() }
        Text("Velocidad · %.2f×".format(speed),fontWeight=FontWeight.SemiBold)
        Slider(value=speed,onValueChange={speed=it},onValueChangeFinished={prefs.playbackSpeed=speed;onPlaybackChanged()},valueRange=.5f..2f,steps=5)
        TextButton(onClick={speed=1f;prefs.playbackSpeed=1f;onPlaybackChanged()}) {Text("Velocidad normal")}
        SectionTitle("TEMPORIZADOR","Pausar automáticamente")
        Text(if(deadline>0) "Se pausará en ${((deadline-System.currentTimeMillis()).coerceAtLeast(0)+59999)/60000} min" else "Desactivado",style=MaterialTheme.typography.bodySmall)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(15,30,60,90).forEach { minutes -> OutlinedButton(onClick={deadline=System.currentTimeMillis()+minutes*60000L;prefs.sleepDeadline=deadline;onPlaybackChanged()}) {Text("$minutes min")} }
        }
        if(deadline>0) TextButton(onClick={deadline=0;prefs.sleepDeadline=0;onPlaybackChanged()}) {Text("Cancelar temporizador")}
        SectionTitle("BIBLIOTECA","Solo lo que quieres escuchar")
        Text("Duración mínima · $minimum s",fontWeight=FontWeight.SemiBold)
        Slider(value=minimum.toFloat(),onValueChange={minimum=it.toInt()},onValueChangeFinished={prefs.minimumDuration=minimum;onLibraryChanged()},valueRange=0f..120f,steps=7)
        Text("Oculta audios cortos sin borrar archivos. 0 s incluye todas las duraciones.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Column(Modifier.weight(1f)) {Text("Excluir audios de WhatsApp",fontWeight=FontWeight.SemiBold);Text("También notas de voz y WhatsApp Business",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=hide,onCheckedChange={hide=it;prefs.hideWhatsApp=it;onLibraryChanged()})}
        OutlinedButton(onClick={foldersDialog=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) {Icon(Icons.Rounded.FolderOpen,null);Text("Elegir carpetas · ${excluded.size} excluidas",Modifier.padding(8.dp))}
        Text("Se muestran las carpetas que Android ha registrado con archivos de audio. Las carpetas privadas de otras apps pueden no ser accesibles.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        SectionTitle("PRISMA MUSIC","1.0.3 · Beta de código abierto")
        Text("Sin anuncios, sin cuenta y sin subir tu música. Las letras incrustadas se leen automáticamente. Puedes importar LRC o TXT si el archivo no contiene letra. El motor usa PCM de 16 bits para aplicar el ecualizador; los datos mostrados describen el archivo original.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
    }
    if(foldersDialog) AlertDialog(onDismissRequest={foldersDialog=false},title={Text("Carpetas de música")},text={Column {
        Text("Marca las carpetas que quieres incluir. La exclusión de WhatsApp tiene prioridad.",style=MaterialTheme.typography.bodySmall)
        LazyColumn(Modifier.heightIn(max=360.dp)) {items(songs.map {it.folder}.distinct().sorted()) {folder->Row(Modifier.fillMaxWidth().clickable {excluded=if(folder in excluded) excluded-folder else excluded+folder;prefs.excluded=excluded;onLibraryChanged()},verticalAlignment=Alignment.CenterVertically) {Checkbox(checked=folder !in excluded,onCheckedChange={included->excluded=if(included) excluded-folder else excluded+folder;prefs.excluded=excluded;onLibraryChanged()});Text(folder.ifBlank {"Sin carpeta"},maxLines=3,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)}}}
    }},confirmButton={TextButton(onClick={foldersDialog=false}){Text("Listo")}})
}
@Composable fun SettingSwitch(title:String,description:String,checked:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end=12.dp)) {Text(title,fontWeight=FontWeight.SemiBold);Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Switch(checked=checked,onCheckedChange=onChange)
    }
}
@Composable fun SectionTitle(label:String,title:String) {Column(Modifier.padding(top=12.dp,bottom=4.dp)) {Text(label,fontSize=10.sp,letterSpacing=2.sp,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=5.dp))}}

@Composable fun EqScreen(prefs:Preferences,onChange:(DspEngine.Config)->Unit) {
    var config by remember {mutableStateOf(prefs.eq)}
    fun update(enabled:Boolean=config.enabled,gains:DoubleArray=config.gains,q:Double=config.q,preamp:Double=config.preamp,bass:Double=config.bass,mids:Double=config.mids,treble:Double=config.treble,voice:Double=config.voice) {config=DspEngine.Config(enabled,gains,q,preamp,bass,mids,treble,voice);onChange(config)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()) {Column(Modifier.weight(1f)) {Text("Escucha los detalles",fontWeight=FontWeight.Bold,fontSize=21.sp);Text("10 bandas · procesamiento propio",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(config.enabled,{update(enabled=it)})}
        Text("Los controles se aplican a la música reproducida en Prisma. Con el ecualizador activo, el limitador controla los picos.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Plano","Graves","Voces","Suave").forEach { name -> OutlinedButton(onClick={val gains=when(name){"Graves"->doubleArrayOf(4.0,4.0,3.0,1.0,0.0,0.0,0.0,0.0,1.0,1.0);"Voces"->doubleArrayOf(-2.0,-1.0,0.0,0.0,1.0,2.0,3.0,2.0,0.0,0.0);"Suave"->doubleArrayOf(1.0,1.0,0.0,0.0,-1.0,-1.0,-2.0,-2.0,-1.0,-1.0);else->DoubleArray(10)};update(enabled=true,gains=gains,q=1.0,preamp=if(name=="Plano") 0.0 else -4.0,bass=0.0,mids=0.0,treble=0.0,voice=0.0)}) {Text(name)} }
        }
        SectionTitle("ECUALIZADOR","Bandas de frecuencia")
        DspEngine.FREQUENCIES.forEachIndexed { index,freq -> SoundSlider(if(freq>=1000) "${freq.toInt()/1000} kHz" else "${freq.toInt()} Hz",config.gains[index],"dB",config.enabled) {value->val gains=config.gains.clone();gains[index]=value;update(gains=gains)} }
        SectionTitle("CONTROL FINO","Tono y presencia")
        SoundSlider("Preamplificador",config.preamp,"dB",config.enabled) {update(preamp=it)}
        SoundSlider("Graves",config.bass,"dB",config.enabled) {update(bass=it)}
        SoundSlider("Medios",config.mids,"dB",config.enabled) {update(mids=it)}
        SoundSlider("Agudos",config.treble,"dB",config.enabled) {update(treble=it)}
        SoundSlider("Presencia de voces",config.voice,"dB",config.enabled) {update(voice=it)}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("Ancho de banda · Q");Text("%.2f".format(config.q),color=MaterialTheme.colorScheme.primary)}
        Slider(value=config.q.toFloat(),onValueChange={update(q=it.toDouble())},valueRange=.3f..4f,enabled=config.enabled)
        Text("Q bajo: ajuste más amplio. Q alto: ajuste más preciso. Realzar voces modifica su zona de frecuencias; no separa la voz de los instrumentos.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick={config=DspEngine.Config.flat();onChange(config)},modifier=Modifier.fillMaxWidth().padding(vertical=18.dp)) {Text("Restablecer sonido")}
    }
}
@Composable fun SoundSlider(label:String,value:Double,unit:String,enabled:Boolean,onChange:(Double)->Unit) {Column {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(label,style=MaterialTheme.typography.bodyMedium);Text("%+.1f %s".format(value,unit),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)};Slider(value=value.toFloat(),onValueChange={onChange(it.toDouble())},valueRange=-12f..12f,enabled=enabled,modifier=Modifier.height(38.dp))}}

@Composable fun PlayerScreen(song:Song,details:Library.Details?,theme:VisualTheme,prefs:Preferences,position:Long,duration:Long,playing:Boolean,shuffle:Boolean,repeat:Int,controller:MediaController?,onDismiss:()->Unit) {
    val view=androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view,prefs.keepScreenOn) {
        val previous=view.keepScreenOn
        view.keepScreenOn=prefs.keepScreenOn
        onDispose {view.keepScreenOn=previous}
    }
    var metadataVisible by remember { mutableStateOf(prefs.showMetadata) }
    var lyricsVisible by rememberSaveable {mutableStateOf(false)}
    var importedText by remember(song.id) {mutableStateOf(prefs.lyrics(song.id))}
    val lyricText=details?.embeddedLyrics?.takeIf {it.isNotBlank()} ?: importedText
    val embedded=details?.embeddedLyrics?.isNotBlank()==true
    val swipeThreshold=with(LocalDensity.current) {70.dp.toPx()}
    var drag by remember {mutableFloatStateOf(0f)}
    var verticalDrag by remember {mutableFloatStateOf(0f)}
    var closing by remember {mutableStateOf(false)}
    val screenHeight=with(LocalDensity.current) {LocalConfiguration.current.screenHeightDp.dp.toPx()}
    val closeThreshold=with(LocalDensity.current) {96.dp.toPx()}
    val offset by animateFloatAsState(if(closing) screenHeight else verticalDrag,animationSpec=if(closing) tween(240, easing=FastOutSlowInEasing) else spring(stiffness=900f),label="dismissPlayer")
    val latestDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(closing) {if(closing) {kotlinx.coroutines.delay(260);latestDismiss()}}
    val latestController by rememberUpdatedState(controller)
    val swipe=Modifier.pointerInput(song.id,lyricsVisible) {
        var axis=0
        detectDragGestures(onDragStart={axis=0;drag=0f;verticalDrag=0f},onDragCancel={drag=0f;verticalDrag=0f},onDragEnd={
            if(axis==2 && verticalDrag>closeThreshold) closing=true
            else if(axis==1) {if(drag < -swipeThreshold) latestController?.seekToNextMediaItem() else if(drag > swipeThreshold) latestController?.seekToPreviousMediaItem()}
            drag=0f;verticalDrag=0f
        }) {change,amount->
            if(axis==0) axis=if(abs(amount.x)>abs(amount.y)) 1 else 2
            if(axis==1) {change.consume();drag+=amount.x}
            else if(!lyricsVisible) {change.consume();verticalDrag=(verticalDrag+amount.y).coerceAtLeast(0f)}
        }
    }
    var lyricError by remember(song.id) {mutableStateOf<String?>(null)}
    var importId by remember {mutableStateOf(song.id)}
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    val import=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null) scope.launch {
        runCatching {withContext(Dispatchers.IO) {context.contentResolver.openInputStream(uri)?.use {stream->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0;while(true) {val n=stream.read(buffer);if(n<0) break;total+=n;require(total<=1_048_576) {"Archivo demasiado grande (máximo 1 MB)."};out.write(buffer,0,n)};out.toByteArray().toString(Charsets.UTF_8)} ?: error("No se pudo abrir el archivo.")}}.onSuccess {text->prefs.saveLyrics(importId,text);if(importId==song.id) importedText=text;lyricError=null}.onFailure {lyricError=it.message ?: "No se pudo leer la letra."}
    }}
    val end=if(duration>0) duration else song.duration
    var seeking by remember(song.id) {mutableStateOf<Float?>(null)}
    Column(Modifier.fillMaxSize().graphicsLayer {translationY=offset;alpha=1f-(offset/screenHeight).coerceIn(0f,.7f)}.padding(horizontal=24.dp)) {
        Box(Modifier.fillMaxWidth().height(32.dp).semantics {contentDescription="Deslizar hacia abajo para cerrar"}.pointerInput(Unit) {
            detectVerticalDragGestures(onDragStart={verticalDrag=0f},onDragCancel={verticalDrag=0f},onDragEnd={if(verticalDrag>closeThreshold) closing=true;verticalDrag=0f}) {change,amount->change.consume();verticalDrag=(verticalDrag+amount).coerceAtLeast(0f)}
        },contentAlignment=Alignment.Center) {Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.45f)))}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {FilterChip(!lyricsVisible,{lyricsVisible=false},label={Text("Carátula")});Spacer(Modifier.width(8.dp));FilterChip(lyricsVisible,{lyricsVisible=true},label={Text("Letra")});IconButton(onClick={metadataVisible=!metadataVisible;prefs.showMetadata=metadataVisible}) {Icon(Icons.Rounded.Info,if(metadataVisible) "Ocultar metadatos" else "Mostrar metadatos",tint=if(metadataVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}}
        Box(Modifier.weight(1f).fillMaxWidth().then(if(lyricsVisible) Modifier else swipe),contentAlignment=Alignment.Center) {
            if(lyricsVisible) {
                val lines=remember(lyricText) {Lyrics.parse(lyricText)}
                if(lines.isEmpty()) Column(horizontalAlignment=Alignment.CenterHorizontally) {Icon(Icons.Rounded.Lyrics,null,Modifier.size(64.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(16.dp));Text(if(details==null) "Leyendo letra incrustada…" else "No se encontró una letra incrustada",fontWeight=FontWeight.Bold);Text("También puedes importar un archivo LRC o TXT.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=10.dp));Button(onClick={importId=song.id;import.launch(arrayOf("text/*","application/octet-stream"))}){Text("Importar letra")}}
                else {
                    val active=lines.indexOfLast {it.time!=null && it.time<=position}
                    val state=rememberLazyListState()
                    var follow by remember(song.id) {mutableStateOf(true)}
                    LaunchedEffect(active,follow,lyricText) {if(follow && active>=0) state.animateScrollToItem(active,0)}
                    Column {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {FilterChip(follow,{follow=!follow},label={Text(if(follow) "Seguir canción" else "Leer libremente")});Text(if(embedded) "Del archivo" else "Importada",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp)}
                        LazyColumn(state=state,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(top=16.dp,bottom=130.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {itemsIndexed(lines) {index,line ->
                            val color by animateColorAsState(if(active==index || line.time==null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,label="lyricColor")
                            val scale by animateFloatAsState(if(active==index && theme!=VisualTheme.MINIMAL) 1.035f else 1f,label="lyricScale")
                            Text(line.text.ifBlank {"♪"},color=color,fontSize=if(theme==VisualTheme.MINIMAL) 22.sp else 26.sp,fontWeight=if(active==index) FontWeight.Bold else FontWeight.Medium,modifier=Modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(12.dp)).then(if(theme==VisualTheme.GLASS && active==index) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha=.08f)) else Modifier).clickable(enabled=line.time!=null) {line.time?.let {controller?.seekTo(it)}}.padding(8.dp))
                        }}
                    }
                }
                lyricError?.let {Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.align(Alignment.BottomCenter))}
            } else {
                AnimatedContent(targetState=Pair(song,theme),contentKey={it.first.id to it.second},transitionSpec={fadeIn(tween(340, easing=FastOutSlowInEasing)) togetherWith fadeOut(tween(220))},label="themedCover") { target ->
                    ThemeArtwork(target.first,if(target.first.id==song.id) details else null,target.second)
                }

            }
        }
        Text("Desliza: ← siguiente · anterior → · ↓ cerrar",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=6.dp))
        Text(song.title,fontSize=if(theme==VisualTheme.MINIMAL) 30.sp else 23.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=18.dp))
        Text(song.artist,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        AnimatedVisibility(metadataVisible,enter=fadeIn(tween(220))+expandVertically(tween(280)),exit=fadeOut(tween(150))+shrinkVertically(tween(280))) {
            Text(details?.text ?: "Leyendo información del archivo…",fontSize=10.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=7.dp).semantics {contentDescription="Información técnica de la canción"})
        }
        val smoothPosition = key(song.id) {
            val value by animateFloatAsState(position.toFloat().coerceIn(0f,end.toFloat().coerceAtLeast(1f)),animationSpec=tween(240,easing=LinearEasing),label="playbackProgress")
            value
        }
        ThemeTimeline(theme,seeking ?: smoothPosition,end.toFloat().coerceAtLeast(1f),end>0,onChange={seeking=it},onFinished={seeking?.let {controller?.seekTo(it.toLong())};seeking=null})
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(time((seeking?.toLong() ?: position)),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(time(end),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Row(Modifier.fillMaxWidth().padding(vertical=12.dp).then(if(theme==VisualTheme.GLASS) Modifier.crystal().padding(vertical=8.dp) else Modifier),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={controller?.shuffleModeEnabled=!shuffle}) {Icon(Icons.Rounded.Shuffle,if(shuffle) "Desactivar aleatorio" else "Activar aleatorio",tint=if(shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}
            IconButton(onClick={controller?.seekToPrevious()}) {Icon(Icons.Rounded.SkipPrevious,"Anterior",Modifier.size(34.dp))}
            FilledIconButton(onClick={controller?.let {if(it.isPlaying) it.pause() else it.play()}},modifier=Modifier.size(64.dp),shape=CircleShape) {Icon(if(playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,if(playing) "Pausar" else "Reproducir",Modifier.size(38.dp))}
            IconButton(onClick={controller?.seekToNextMediaItem()}) {Icon(Icons.Rounded.SkipNext,"Siguiente",Modifier.size(34.dp))}
            IconButton(onClick={controller?.repeatMode=when(repeat){Player.REPEAT_MODE_OFF->Player.REPEAT_MODE_ALL;Player.REPEAT_MODE_ALL->Player.REPEAT_MODE_ONE;else->Player.REPEAT_MODE_OFF}}) {Icon(if(repeat==Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,"Cambiar repetición",tint=if(repeat!=Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }
}
