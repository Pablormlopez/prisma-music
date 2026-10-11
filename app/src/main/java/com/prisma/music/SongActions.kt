@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.prisma.music

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun SongActions(song:Song,onDismiss:()->Unit,onPlay:()->Unit,onNext:()->Unit,onQueue:()->Unit,onList:()->Unit,onDeleted:(Song)->Unit,onError:(String)->Unit) {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    var confirmDelete by remember {mutableStateOf(false)}
    var info by remember {mutableStateOf(false)}
    var detail by remember {mutableStateOf<Library.Details?>(null)}
    var deleting by remember {mutableStateOf(false)}
    var retryAfterConsent by remember {mutableStateOf(false)}
    fun finished() {onDeleted(song);onDismiss()}
    fun deleteDirect() {deleting=true;scope.launch {
        try {
            val count=withContext(Dispatchers.IO) {context.contentResolver.delete(song.uri,null,null)}
            if(count>0) finished() else onError("El archivo ya no existe o Android no pudo eliminarlo.")
        } catch(e:Exception) {onError("No se pudo eliminar el archivo: ${e.message ?: "permiso denegado"}")}
        finally {deleting=false;confirmDelete=false}
    }}
    val consent=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {result->
        if(result.resultCode==Activity.RESULT_OK) {if(retryAfterConsent) deleteDirect() else finished()}
        else {deleting=false;confirmDelete=false}
    }
    val legacyPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {allowed->
        if(allowed) deleteDirect() else {deleting=false;confirmDelete=false;onError("Permiso de almacenamiento denegado. La canción se conserva.")}
    }
    fun requestDelete() {
        deleting=true
        if(Build.VERSION.SDK_INT>=30) {
            runCatching {MediaStore.createDeleteRequest(context.contentResolver,listOf(song.uri))}.onSuccess {retryAfterConsent=false;consent.launch(IntentSenderRequest.Builder(it.intentSender).build())}.onFailure {deleting=false;onError("Android no pudo solicitar la eliminación.")}
        } else if(Build.VERSION.SDK_INT==29) scope.launch {
            try {
                val count=withContext(Dispatchers.IO) {context.contentResolver.delete(song.uri,null,null)}
                if(count>0) finished() else onError("No se pudo eliminar el archivo.")
                deleting=false;confirmDelete=false
            } catch(e:RecoverableSecurityException) {retryAfterConsent=true;consent.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())}
            catch(e:Exception) {deleting=false;confirmDelete=false;onError("No se pudo eliminar el archivo.")}
        } else if(ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED) deleteDirect()
        else legacyPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    if(info) {
        LaunchedEffect(song.id) {detail=withContext(Dispatchers.IO) {Library.details(context,song)}}
        AlertDialog(onDismissRequest={info=false},title={Text("Información de la canción")},text={Column(Modifier.verticalScroll(rememberScrollState())) {Text(song.title);Text(song.artist);Text(song.album);Text(song.name);Text(song.folder);Spacer(Modifier.height(12.dp));Text(detail?.text ?: "Leyendo información…")}},confirmButton={PrismaTextButton(onClick={info=false}){Text("Volver")}})
    } else if(confirmDelete) AlertDialog(onDismissRequest={if(!deleting) confirmDelete=false},title={Text("¿Eliminar del teléfono?")},text={Text("Se borrará el archivo «${song.name}» y se quitará de tus listas y de la cola. Esta acción no se puede deshacer.")},confirmButton={PrismaTextButton(onClick=::requestDelete,enabled=!deleting){Text(if(deleting) "Esperando a Android…" else "Eliminar archivo")}},dismissButton={PrismaTextButton(onClick={confirmDelete=false},enabled=!deleting){Text("Cancelar")}})
    else ModalBottomSheet(onDismissRequest=onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp)) {
            Text(song.title,style=MaterialTheme.typography.titleLarge)
            Text(song.artist,style=MaterialTheme.typography.bodySmall)
            listOf("Reproducir ahora" to onPlay,"Reproducir a continuación" to onNext,"Añadir al final de la cola" to onQueue,"Añadir a una lista" to onList).forEach {(title,action)->PrismaTextButton(onClick={onDismiss();action()},modifier=Modifier.fillMaxWidth()){Text(title)}}
            PrismaTextButton(onClick={runCatching {context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type=song.mime.ifBlank {"audio/*"};putExtra(Intent.EXTRA_STREAM,song.uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartir canción"))}.onFailure {onError("No se pudo compartir este archivo.")};onDismiss()},modifier=Modifier.fillMaxWidth()){Text("Compartir archivo")}
            PrismaTextButton(onClick={info=true},modifier=Modifier.fillMaxWidth()){Text("Información de la canción")}
            PrismaTextButton(onClick={confirmDelete=true},modifier=Modifier.fillMaxWidth()){Text("Eliminar del teléfono",color=MaterialTheme.colorScheme.error)}
            Spacer(Modifier.height(24.dp))
        }
    }
}
