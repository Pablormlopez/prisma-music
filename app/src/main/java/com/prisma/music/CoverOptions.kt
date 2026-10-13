@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.prisma.music
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
@Composable internal fun CoverOptions(prefs:Preferences,song:Song,onError:(String)->Unit) {
    val context=LocalContext.current
    var open by remember {mutableStateOf(false)}
    var importing by remember {mutableStateOf(song.id)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            prefs.saveAnimatedArtwork(importing,uri.toString());prefs.setAdvanced("animatedCover",true)
        }.onFailure {onError("No se pudo conservar el acceso a esa imagen.")}
    }}
    Box {
        PrismaIconButton(onClick={open=true}) {Icon(Icons.Rounded.MoreHoriz,"Opciones de portada")}
        DropdownMenu(open,{open=false}) {
            DropdownMenuItem(text={Text("Importar portada GIF/WebP")},onClick={open=false;importing=song.id;picker.launch(arrayOf("image/gif","image/webp"))})
            DropdownMenuItem(text={Text("Restaurar portada original")},onClick={open=false;prefs.saveAnimatedArtwork(song.id,null)})
        }
    }
}
