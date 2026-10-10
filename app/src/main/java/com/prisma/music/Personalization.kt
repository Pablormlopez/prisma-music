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
    var hex by remember(value.accent) {mutableStateOf(value.accent)}
    SectionTitle("PERSONALIZACIÓN","Diseña tu Prisma")
    Text("Combina cualquier tema con tu color y la vista que prefieras.",style=MaterialTheme.typography.bodySmall)
    Text("Mini temas de color")
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("Tema" to "","Lavanda" to "#9D87E8","Océano" to "#46BDE8","Menta" to "#58CBA4","Rosa" to "#E98BAC","Ámbar" to "#EAB45E").forEach { (name,color)->
            FilterChip(selected=value.accent==color,onClick={onChange(value.copy(accent=color))},label={Text(name)},leadingIcon={if(color.isNotEmpty()) Box(Modifier.size(16.dp).background(accentColor(color)!!,RoundedCornerShape(8.dp)))})
        }
    }
    OutlinedTextField(hex,{hex=it},label={Text("Color manual · #RRGGBB")},singleLine=true,modifier=Modifier.fillMaxWidth(),isError=hex.isNotBlank() && accentColor(hex)==null,supportingText={Text("Ejemplo: #58CBA4. El contraste se adapta al modo claro u oscuro.")})
    TextButton(onClick={onChange(value.copy(accent=hex.uppercase()))},enabled=accentColor(hex)!=null) {Text("Aplicar color")}
    Text("Vista de la biblioteca")
    LibraryLayout.entries.forEach {layout->FilterChip(selected=value.layout==layout,onClick={onChange(value.copy(layout=layout))},label={Text(layout.title)})}
    Text("Información bajo el título")
    SongInfoStyle.entries.forEach {style->FilterChip(selected=value.info==style,onClick={onChange(value.copy(info=style))},label={Text(style.title)})}
    OutlinedButton(onClick={onChange(Personalization())},modifier=Modifier.fillMaxWidth()) {Text("Restablecer personalización")}
}
