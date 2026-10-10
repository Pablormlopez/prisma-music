package com.prisma.music

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalPersonalization=staticCompositionLocalOf { Personalization() }
fun Song.subtitle(style:SongInfoStyle):String = when(style) {
    SongInfoStyle.ARTIST -> artist
    SongInfoStyle.ALBUM -> "$artist · $album"
    SongInfoStyle.FILE -> name
}
fun accentColor(hex:String):Color? = hex.takeIf {it.matches(Regex("#[0-9a-fA-F]{6}"))}?.let {Color(android.graphics.Color.parseColor(it))}

@Composable fun PersonalizationSettings(value:Personalization,onChange:(Personalization)->Unit) {
    var picker by remember {mutableStateOf(false)}
    var hex by remember(value.accent) {mutableStateOf(value.accent)}
    SectionTitle("COLORES","Tu acento personal")
    Text("Combina cualquier tema con tu color y la vista que prefieras.",style=MaterialTheme.typography.bodySmall)
    Text("Mini temas de color")
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("Tema" to "","Lavanda" to "#9D87E8","Océano" to "#46BDE8","Menta" to "#58CBA4","Rosa" to "#E98BAC","Ámbar" to "#EAB45E").forEach { (name,color)->
            FilterChip(selected=value.accent==color,onClick={onChange(value.copy(accent=color))},label={Text(name)},leadingIcon={if(color.isNotEmpty()) Box(Modifier.size(16.dp).background(accentColor(color)!!,RoundedCornerShape(8.dp)))})
        }
    }
    OutlinedButton(onClick={picker=true},modifier=Modifier.fillMaxWidth()) {Text("Elegir un color personalizado")}
    if(picker) ColorPicker(value.accent,onDismiss={picker=false},onSave={onChange(value.copy(accent=it));picker=false})
    OutlinedTextField(hex,{hex=it},label={Text("Color manual · #RRGGBB")},singleLine=true,modifier=Modifier.fillMaxWidth(),isError=hex.isNotBlank() && accentColor(hex)==null,supportingText={Text("Ejemplo: #58CBA4. El contraste se adapta al modo claro u oscuro.")})
    TextButton(onClick={onChange(value.copy(accent=hex.uppercase()))},enabled=accentColor(hex)!=null) {Text("Aplicar color")}
    Text("Vista de la biblioteca")
    LibraryLayout.entries.forEach {layout->FilterChip(selected=value.layout==layout,onClick={onChange(value.copy(layout=layout))},label={Text(layout.title)})}
    Text("Información bajo el título")
    SongInfoStyle.entries.forEach {style->FilterChip(selected=value.info==style,onClick={onChange(value.copy(info=style))},label={Text(style.title)})}
    OutlinedButton(onClick={onChange(Personalization())},modifier=Modifier.fillMaxWidth()) {Text("Restablecer personalización")}
}

@Composable fun ColorPicker(initial:String,onDismiss:()->Unit,onSave:(String)->Unit) {
    val hsv=remember {FloatArray(3).also {android.graphics.Color.colorToHSV(runCatching {android.graphics.Color.parseColor(initial)}.getOrDefault(0xff9d87e8.toInt()),it)}}
    var hue by remember {mutableFloatStateOf(hsv[0])}
    var saturation by remember {mutableFloatStateOf(hsv[1])}
    var brightness by remember {mutableFloatStateOf(hsv[2])}
    val color=android.graphics.Color.HSVToColor(floatArrayOf(hue,saturation,brightness))
    AlertDialog(onDismissRequest=onDismiss,title={Text("Tu color")},text={Column {
        Box(Modifier.fillMaxWidth().height(64.dp).background(Color(color),RoundedCornerShape(16.dp)))
        Text("Tono");Slider(hue,{hue=it},valueRange=0f..360f)
        Text("Intensidad");Slider(saturation,{saturation=it})
        Text("Luminosidad");Slider(brightness,{brightness=it})
    }},confirmButton={TextButton(onClick={onSave("#%06X".format(color and 0xffffff))}){Text("Usar este color")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}

@Composable fun PersonalizationScreen(value:Personalization,onChange:(Personalization)->Unit,onThemes:()->Unit) {
    var preview by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle("TEMAS","Elige tu ambiente")
        OutlinedButton(onClick=onThemes,modifier=Modifier.fillMaxWidth()) {Text("Temas")}
        SectionTitle("ANIMACIONES","El movimiento también es tuyo")
        MotionStyle.entries.forEach {style->
            FilterChip(value.motion==style,{onChange(value.copy(motion=style))},label={Text(style.title)})
            if(value.motion==style) Text(style.description,style=MaterialTheme.typography.bodySmall)
        }
        if(value.motion!=MotionStyle.NONE) {
            Text("Duración · ${value.motionDuration} ms")
            Slider(value.motionDuration.toFloat(),{onChange(value.copy(motionDuration=(it/50).toInt()*50))},valueRange=250f..900f,steps=12)
        }
        OutlinedButton(onClick={preview=!preview},modifier=Modifier.fillMaxWidth()) {Text("Probar animación")}
        androidx.compose.animation.AnimatedContent(targetState=preview,transitionSpec={contentMotion(value.motion,value.motionDuration)},label="motionPreview") {alternate->
            Surface(color=if(alternate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()) {
                Text(if(alternate) "Tu próxima canción" else "Tu música, a tu ritmo",modifier=Modifier.padding(24.dp))
            }
        }
        Text("Se aplica a las pantallas, las portadas y el gesto de cambio de canción. Sin animaciones reduce el movimiento visual.",style=MaterialTheme.typography.bodySmall)
        PersonalizationSettings(value,onChange)
        Spacer(Modifier.height(24.dp))
    }
}
