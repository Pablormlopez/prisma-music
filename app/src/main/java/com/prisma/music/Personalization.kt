package com.prisma.music

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
    SectionTitle("PERSONALIZACIÓN","Diseña tu Prisma")
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
