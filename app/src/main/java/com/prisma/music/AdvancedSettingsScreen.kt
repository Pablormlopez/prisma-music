@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.prisma.music

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION") internal fun refreshRates(activity:Activity):List<Float> {
    val display=activity.windowManager.defaultDisplay;val mode=display.mode
    return display.supportedModes.filter {it.physicalWidth==mode.physicalWidth && it.physicalHeight==mode.physicalHeight}.map {it.refreshRate}.distinct().sorted()
}
@Composable internal fun AdvancedWindowOptions(options:AdvancedSettings) {
    val activity=LocalActivity.current ?: return
    DisposableEffect(activity,options.refreshHz,options.performance) {
        val window=activity.window;val previous=window.attributes.preferredRefreshRate
        val supported=refreshRates(activity)
        val rate=if(options.performance) supported.firstOrNull() ?: 0f else supported.firstOrNull {kotlin.math.abs(it-options.refreshHz)<.1f} ?: 0f
        window.attributes=window.attributes.apply {preferredRefreshRate=rate}
        onDispose {window.attributes=window.attributes.apply {preferredRefreshRate=previous}}
    }
}
private fun spatialStatus(context:Context,details:Library.Details?):String {
    if(Build.VERSION.SDK_INT<33)return "La espacialización del sistema requiere Android 13 o posterior."
    return runCatching {
        val spatial=(context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).spatializer
        val mask=when(details?.channels ?: 2) {1->AudioFormat.CHANNEL_OUT_MONO;2->AudioFormat.CHANNEL_OUT_STEREO;6->AudioFormat.CHANNEL_OUT_5POINT1;8->AudioFormat.CHANNEL_OUT_7POINT1_SURROUND;else->return "El formato actual no tiene una distribución de canales compatible."}
        val format=AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(details?.sampleRate ?: 48000).setChannelMask(mask).build()
        val attributes=android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build()
        when {
            !spatial.isAvailable || !spatial.canBeSpatialized(attributes,format)->"No disponible para el formato y la salida actuales. FLAC estéreo no añade canales envolventes."
            !spatial.isEnabled->"Compatible, pero desactivado en los ajustes de sonido de Android."
            else->"Espacialización de Android disponible y activada para esta salida."
        }
    }.getOrDefault("Android no permite consultar la espacialización de esta salida.")
}
@Composable internal fun AdvancedAudioSettings(prefs:Preferences,details:Library.Details?,onPlaybackChanged:()->Unit) {
    val options=rememberAdvanced(prefs);val context=LocalContext.current
    var status by remember {mutableStateOf(spatialStatus(context,details))}
    LaunchedEffect(details) {while(true) {status=spatialStatus(context,details);delay(2000)}}
    SectionTitle("AUDIO AVANZADO","Tu música, con más control")
    SettingSwitch("Salida PCM flotante", "Procesamiento en 32-bit float; conserva la precisión de PCM de 24 bits. La salida final depende de Android y del DAC. Cambiarla reinicia brevemente la salida.",options.floatOutput) {prefs.setAdvanced("floatOutput",it);onPlaybackChanged()}
    SettingSwitch("Normalizar volumen", "Normalización adaptativa RMS entre pistas. Ajusta el nivel y limita picos; modifica la señal, sin recodificar tus archivos FLAC.",options.normalize) {prefs.setAdvanced("normalize",it);onPlaybackChanged()}
    if(options.normalize) AdvancedSlider("Nivel de normalización",options.targetDb,-24f..-14f,"dBFS") {prefs.setAdvanced("targetDb",it);onPlaybackChanged()}
    AdvancedSlider("Mezcla entre canciones",options.crossfade,0f..12f,"s") {prefs.setAdvanced("crossfade",it);onPlaybackChanged()}
    Text("0 s desactiva la mezcla. Superpone el final y el inicio reales, también con la pantalla apagada. En pistas cortas el tiempo se reduce; con repetición de una canción no se aplica. Los desvanecidos individuales se usan con mezcla desactivada.",style=MaterialTheme.typography.bodySmall)
    SettingSwitch("Audio espacial del sistema",status,options.spatial) {prefs.setAdvanced("spatial",it);onPlaybackChanged()}
    Text("La opción permite a Android espacializar cuando el contenido y los auriculares son compatibles; no convierte una pista estéreo en Dolby Atmos.",style=MaterialTheme.typography.bodySmall)
    AdvancedSlider("Amplitud estéreo",options.stereoWidth,0f..1.8f,"×") {prefs.setAdvanced("stereoWidth",it);onPlaybackChanged()}
    Text("1× conserva el estéreo; 0× lo centra. El efecto de amplitud funciona en pistas de dos canales y es independiente del audio espacial.",style=MaterialTheme.typography.bodySmall)
    SettingSwitch("Pausar al grabar", "Pausa ambas pistas cuando Android informa una grabación activa. No usa el micrófono ni lee mensajes; la detección depende del sistema.",options.pauseRecording) {prefs.setAdvanced("pauseRecording",it);onPlaybackChanged()}
    SettingSwitch("Pausar con notificaciones", "Si el sonido de una notificación solicita foco, pausa. Desactivado baja temporalmente el volumen de ambas pistas. Los mensajes silenciosos no interrumpen.",options.pauseNotifications) {prefs.setAdvanced("pauseNotifications",it);onPlaybackChanged()}
    SettingSwitch("Reanudar tras interrupciones", "Vuelve a reproducir si estaba sonando, termina la interrupción y se recupera el foco. Una pausa manual, desconectar auriculares o el temporizador lo impiden.",options.resumeInterruptions) {prefs.setAdvanced("resumeInterruptions",it);onPlaybackChanged()}
    SettingSwitch("Detener al cerrar desde recientes", "Pausa la música y cierra el servicio al quitar Prisma de recientes. Ir a Inicio o apagar la pantalla conserva la reproducción.",options.stopOnDismiss) {prefs.setAdvanced("stopOnDismiss",it);onPlaybackChanged()}
}
@Composable private fun AdvancedSlider(title:String,value:Float,range:ClosedFloatingPointRange<Float>,unit:String,onSave:(Float)->Unit) {
    var current by remember(value) {mutableFloatStateOf(value)}
    Text("$title · %.1f $unit".format(current))
    Slider(current,{current=it},valueRange=range,onValueChangeFinished={onSave(current)},modifier=Modifier.semantics {contentDescription=title})
}
@Composable internal fun AdvancedAppearanceSettings(prefs:Preferences) {
    val options=rememberAdvanced(prefs);val context=LocalContext.current
    SectionTitle("APARIENCIA AVANZADA","Portada y fluidez")
    SettingSwitch("Modo ligero", "Reduce el movimiento, usa un fondo sencillo y solicita la menor frecuencia compatible para ahorrar recursos.",options.performance) {prefs.setAdvanced("performance",it)}
    Text("Frecuencia de actualización")
    val activity=LocalActivity.current
    val rates=remember(activity) {activity?.let {refreshRates(it)} ?: emptyList()}
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        PrismaFilterChip(options.refreshHz==0f,{prefs.setAdvanced("refreshHz",0f)},label={Text("Automática")})
        rates.forEach {rate->PrismaFilterChip(kotlin.math.abs(options.refreshHz-rate)<.1f,{prefs.setAdvanced("refreshHz",rate)},label={Text("${rate.toInt()} Hz")})}
    }
    Text("Se muestran solo los modos compatibles. Android puede reducir la frecuencia por batería, temperatura o sus propios ajustes.",style=MaterialTheme.typography.bodySmall)
    SettingSwitch("Reducir desenfoque", "Sustituye el fondo de carátula difuminado por colores suaves del álbum.",options.reduceBlur) {prefs.setAdvanced("reduceBlur",it)}
    SettingSwitch("Ambiente del álbum en todos los temas", "Comparte el fondo, las transiciones y la portada inmersiva de Liquid Glass. Desactívalo para recuperar la composición original de los demás temas.",options.immersiveAllThemes) {prefs.setAdvanced("immersiveAllThemes",it)}
    SettingSwitch("Portada a pantalla completa", "Integra la portada en el fondo de todos los temas con ambiente del álbum. Desactivado usa una carátula centrada.",options.fullCover) {prefs.setAdvanced("fullCover",it)}
    SettingSwitch("Gradiente de malla", "Un ambiente de luces suaves con el color del álbum.",options.meshGradient) {prefs.setAdvanced("meshGradient",it)}
    SettingSwitch("Portada animada", "Movimiento sutil al reproducir, o una imagen GIF/WebP que importes. Se detiene al pausar, salir de la app o reducir animaciones.",options.animatedCover) {prefs.setAdvanced("animatedCover",it)}
    if(options.animatedCover) Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        listOf("Respirar","Deslizar","Girar").forEach {mode->PrismaFilterChip(options.coverMotion==mode,{prefs.setAdvanced("coverMotion",mode)},label={Text(mode)})}
    }
    SectionTitle("LETRAS Y GESTOS","Lectura a tu medida")
    AdvancedSlider("Tamaño de las letras",options.lyricSize,18f..36f,"sp") {prefs.setAdvanced("lyricSize",it)}
    SettingSwitch("Centrar letras", "Alinea los versos al centro en el reproductor y el modo ambiente.",options.centerLyrics) {prefs.setAdvanced("centerLyrics",it)}
    SettingSwitch("Deslizar para cambiar canción", "Desliza la carátula a izquierda o derecha. Los botones siguen disponibles.",options.swipeTracks) {prefs.setAdvanced("swipeTracks",it)}
    SettingSwitch("Letras sincronizadas", "Sigue los tiempos de las letras LRC o incrustadas que ya los contienen.",options.syncLyrics) {prefs.setAdvanced("syncLyrics",it)}
    SettingSwitch("Difuminar versos inactivos", "Resalta el verso actual con un desenfoque suave en los demás.",options.blurLyrics) {prefs.setAdvanced("blurLyrics",it)}
    SettingSwitch("Mostrar volumen en el reproductor", "Conserva los botones físicos de volumen aunque ocultes la barra.",options.showVolume) {prefs.setAdvanced("showVolume",it)}
}
