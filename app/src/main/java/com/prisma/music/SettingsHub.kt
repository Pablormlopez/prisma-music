package com.prisma.music

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable internal fun SettingsHub(onCategory:(String)->Unit) {
    var search by rememberSaveable {mutableStateOf("")}
    val categories=listOf(
        Triple("Apariencia","Temas, colores, portada, animaciones, volumen, letras, gestos y frecuencia de pantalla",Icons.Rounded.Palette),
        Triple("Reproductor y audio","Normalización, mezcla, desvanecidos, salida, interrupciones, velocidad y temporizador",Icons.Rounded.GraphicEq),
        Triple("Biblioteca","Carpetas, exclusiones, WhatsApp y duración mínima",Icons.Rounded.FolderOpen),
        Triple("Acerca de Prisma","Versión, privacidad y código abierto",Icons.Rounded.Info)
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Configuración",style=MaterialTheme.typography.headlineLarge)
        OutlinedTextField(search,{search=it},placeholder={Text("Buscar ajustes")},leadingIcon={Icon(Icons.Rounded.Search,null)},singleLine=true,shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth())
        val results=categories.filter {(it.first+" "+it.second).contains(search.trim(),ignoreCase=true)}
        results.forEach {(title,subtitle,icon)->
            Surface(onClick={onCategory(title)},shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh,modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Icon(icon,null,tint=MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    Icon(Icons.Rounded.ChevronRight,null)
                }
            }
        }
        if(results.isEmpty()) Text("No se encontraron categorías.")
    }
}
