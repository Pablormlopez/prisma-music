package com.prisma.music

import android.Manifest
import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.test.platform.app.InstrumentationRegistry

/** Real app smoke test on Android 35. Uses an original generated tone, no user music. */
@RunWith(AndroidJUnit4::class)
class PrismaSmokeTest {
    @get:Rule(order=0) val permissions:GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_AUDIO,Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order=1) val compose = createAndroidComposeRule<MainActivity>()
    private fun nav(text:String) {compose.onNode(hasText(text) and hasClickAction()).performClick();compose.waitForIdle()}
    private fun shot(name:String) {
        // Public test output survives Gradle uninstalling the app after tests.
        val resolver=compose.activity.contentResolver
        val values=ContentValues().apply {put(MediaStore.Downloads.DISPLAY_NAME,"$name.png");put(MediaStore.Downloads.MIME_TYPE,"image/png");put(MediaStore.Downloads.RELATIVE_PATH,"Download/PrismaTest/");put(MediaStore.Downloads.IS_PENDING,1)}
        val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values)!!
        resolver.openOutputStream(uri)!!.use {compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        resolver.update(uri,ContentValues().apply {put(MediaStore.Downloads.IS_PENDING,0)},null,null)
    }
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun emptyAudioBlocksAreSafeAndBypassPreservesPcm() {
        val eq=EqProcessor()
        eq.configure(androidx.media3.common.audio.AudioProcessor.AudioFormat(44100,2,androidx.media3.common.C.ENCODING_PCM_16BIT))
        eq.flush()
        eq.queueInput(androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER)
        assertEquals(0,eq.output.remaining())
        val input=ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN).putShort(1000).putShort(-1000)
        input.flip();eq.queueInput(input)
        val output=eq.output.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1000,output.short.toInt());assertEquals(-1000,output.short.toInt())
        eq.queueInput(androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER)
        eq.queueEndOfStream();assertTrue(eq.isEnded);eq.reset()
    }
    @Test fun libraryOrderUsesDatesAndLocaleTitles() {
        fun song(id:String,title:String,date:Long)=Song(id,android.net.Uri.EMPTY,title,"","","","","",0,0,"",android.net.Uri.EMPTY,date)
        val songs=listOf(song("3","Zulu",20),song("1","ábaco",10),song("2","Beta",20))
        assertEquals(listOf("1","2","3"),sortSongs(songs,SongSort.TITLE_ASC).map {it.id})
        assertEquals(listOf("3","2","1"),sortSongs(songs,SongSort.TITLE_DESC).map {it.id})
        assertEquals(listOf("3","2","1"),sortSongs(songs,SongSort.NEWEST).map {it.id})
        assertEquals(listOf("1","2","3"),sortSongs(songs,SongSort.OLDEST).map {it.id})
        assertTrue(sortSongs(emptyList(),SongSort.NEWEST).isEmpty())
        val prefs=Preferences(compose.activity)
        val previous=prefs.librarySort
        try {prefs.librarySort=SongSort.OLDEST;assertEquals(SongSort.OLDEST,Preferences(compose.activity).librarySort)}
        finally {prefs.librarySort=previous}
    }
    @Test fun mainMenusSupportSwipes() {
        compose.onNodeWithContentDescription("Contenido de Inicio").performTouchInput {swipeLeft()}
        compose.onNodeWithContentDescription("Contenido de Biblioteca").assertExists()
        compose.onNodeWithContentDescription("Contenido de Biblioteca").performTouchInput {swipeLeft()}
        compose.onNodeWithContentDescription("Contenido de Listas").assertExists()
        compose.onNodeWithContentDescription("Contenido de Listas").performTouchInput {swipeRight()}
        compose.onNodeWithContentDescription("Contenido de Biblioteca").assertExists()
        compose.onNodeWithContentDescription("Ordenar canciones").performClick()
        compose.onNodeWithText("Z a A",substring=false).performClick()
        compose.onNodeWithText("Orden: Z a A").assertExists()
        compose.onNodeWithContentDescription("Ordenar canciones").performClick()
        compose.onNodeWithText("A a Z",substring=false).performClick()
    }
    private fun dragQueue(from:Int,to:Int) {
        val handles=compose.onAllNodes(hasContentDescription("Mantén pulsado para arrastrar",substring=true))
        val source=handles[from].fetchSemanticsNode().boundsInRoot
        val destination=handles[to].fetchSemanticsNode().boundsInRoot
        handles[from].performTouchInput {
            down(center)
            advanceEventTime(700)
            moveTo(center + androidx.compose.ui.geometry.Offset(0f,destination.center.y-source.center.y),300)
            up()
        }
        compose.waitForIdle()
    }
    @Test fun libraryPlaybackThemesAndDspControls() {
        val context=compose.activity
        val resolver=context.contentResolver
        val values=ContentValues().apply {put(MediaStore.Audio.Media.DISPLAY_NAME,"Prisma prueba.wav");put(MediaStore.Audio.Media.MIME_TYPE,"audio/wav");put(MediaStore.Audio.Media.RELATIVE_PATH,"Music/PrismaTest/");put(MediaStore.Audio.Media.TITLE,"Prisma prueba");put(MediaStore.Audio.Media.ARTIST,"Prisma Music");put(MediaStore.Audio.Media.ALBUM,"Prueba local");put(MediaStore.Audio.Media.IS_PENDING,1)}
        val uri=resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values)!!
        val secondValues=ContentValues(values).apply {put(MediaStore.Audio.Media.DISPLAY_NAME,"Prisma segunda.flac");put(MediaStore.Audio.Media.TITLE,"Prisma segunda");put(MediaStore.Audio.Media.MIME_TYPE,"audio/flac")}
        val second=resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,secondValues)!!
        try {
            InstrumentationRegistry.getInstrumentation().context.assets.open("lyrics.flac").use {input->resolver.openOutputStream(second)!!.use {input.copyTo(it)}}
            resolver.update(second,ContentValues().apply {put(MediaStore.Audio.Media.IS_PENDING,0)},null,null)
            val rate=44100;val count=rate*30;val b=ByteBuffer.allocate(44+count*2).order(ByteOrder.LITTLE_ENDIAN)
            b.put("RIFF".toByteArray()).putInt(36+count*2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate*2).putShort(2).putShort(16).put("data".toByteArray()).putInt(count*2)
            repeat(count) {i->b.putShort((Math.sin(2*Math.PI*440*i/rate)*900).toInt().toShort())}
            resolver.openOutputStream(uri)!!.use {it.write(b.array())}
            resolver.update(uri,ContentValues().apply {put(MediaStore.Audio.Media.IS_PENDING,0)},null,null)
            nav("Biblioteca")
            compose.onNodeWithContentDescription("Actualizar biblioteca").performClick()
            compose.waitUntil(15000) {compose.onAllNodesWithText("Prisma prueba").fetchSemanticsNodes().isNotEmpty()}
            shot("01-biblioteca")
            compose.onNodeWithText("Prisma prueba").performClick()
            compose.waitUntil(15000) {compose.onAllNodesWithContentDescription("Pausar").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithContentDescription("Abrir reproductor").performClick()
            compose.waitUntil(5000) {compose.onAllNodesWithText("Reproduciendo").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithContentDescription("Abrir reproductor").assertDoesNotExist()
            compose.onAllNodesWithContentDescription("Pausar").assertCountEquals(1)
            compose.onNodeWithContentDescription("Pausar").performClick()
            compose.waitUntil(5000) {compose.onAllNodesWithContentDescription("Reproducir").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithContentDescription("Reproducir").performClick()
            compose.waitUntil(5000) {compose.onAllNodesWithContentDescription("Pausar").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithContentDescription("Línea de tiempo Chromatic Flow").assertExists()
            compose.onNodeWithContentDescription("Ocultar metadatos").performClick()
            compose.onNodeWithContentDescription("Información técnica de la canción").assertDoesNotExist()
            assertFalse(Preferences(context).showMetadata)
            compose.onNodeWithContentDescription("Mostrar metadatos").performClick()
            compose.onNodeWithContentDescription("Información técnica de la canción").assertExists()
            assertTrue(Preferences(context).showMetadata)
            shot("02-reproductor")
            compose.onNodeWithContentDescription("Diseño Chromatic Flow").performTouchInput {swipeLeft()}
            compose.waitUntil(10000) {compose.onAllNodesWithText("Prisma segunda").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Letra",substring=false).performClick()
            compose.waitUntil(15000) {compose.onAllNodesWithText("Primera línea incrustada").fetchSemanticsNodes().isNotEmpty()}
            shot("06-letra-incrustada")
            compose.onNodeWithText("Carátula",substring=false).performClick()
            compose.onNodeWithContentDescription("Diseño Chromatic Flow").performTouchInput {swipeRight()}
            compose.waitUntil(10000) {compose.onAllNodesWithText("Prisma prueba").fetchSemanticsNodes().isNotEmpty()}
            var connection:com.google.common.util.concurrent.ListenableFuture<MediaController>?=null
            compose.runOnIdle {connection=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()}
            val observer=connection!!.get(10,TimeUnit.SECONDS)
            // Pause explicitly: the short fixture can end between a rendered icon and a tap.
            compose.runOnIdle {observer.pause()}
            compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Reproducir").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Reproducir").assertExists()
            // Artist and album browsing never changes the paused session or queue.
            val scanned=Library.scan(context)
            val fixture=scanned.first {it.id==uri.lastPathSegment}
            assertTrue(fixture.dateAdded>0)
            val dates=scanned.mapIndexed {index,song->song.copy(dateAdded=(100-index).toLong())}
            assertEquals(dates.map {it.id},recentlyAdded(dates.reversed()).map {it.id})
            val beforeArtist=compose.runOnIdle {Triple(observer.currentMediaItem?.mediaId,observer.currentPosition,observer.mediaItemCount)}
            compose.onNodeWithContentDescription("Ver artista ${fixture.artist}").performClick()
            compose.onNodeWithText("Álbumes",substring=false).performClick()
            compose.onNodeWithContentDescription("Ver canciones del álbum ${fixture.album}").performClick()
            shot("17-artista-album")
            compose.runOnIdle {assertEquals(beforeArtist,Triple(observer.currentMediaItem?.mediaId,observer.currentPosition,observer.mediaItemCount));assertFalse(observer.playWhenReady)}
            compose.onNodeWithContentDescription("Volver al reproductor").performClick()
            compose.onNodeWithContentDescription("Ver álbum ${fixture.album}").assertDoesNotExist()
            compose.onNodeWithContentDescription("Volver a la biblioteca").performClick()
            nav("Ajustes")
            compose.onNodeWithContentDescription("Mostrar nombre del álbum").performScrollTo().performClick()
            assertTrue(Preferences(context).showPlayerAlbum)
            compose.onNodeWithContentDescription("Abrir reproductor").performClick()
            compose.onNodeWithContentDescription("Ver álbum ${fixture.album}").assertExists().performClick()
            compose.onNodeWithText("Todos los álbumes",substring=false).assertExists()
            compose.onNodeWithContentDescription("Volver al reproductor").performClick()
            compose.onNodeWithContentDescription("Cerrar reproductor").performClick()
            nav("Ajustes")
            compose.onNodeWithContentDescription("Mostrar nombre del álbum").performScrollTo().performClick()
            assertFalse(Preferences(context).showPlayerAlbum)
            nav("Biblioteca")
            compose.onNodeWithContentDescription("Abrir reproductor").assertExists()
            // A long press opens actions without replacing playback; deletion can be canceled.
            compose.onNode(hasText("Prisma prueba") and SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)).performTouchInput {longClick()}
            compose.onNodeWithText("Reproducir a continuación").assertExists()
            shot("09-menu-cancion")
            compose.onNodeWithText("Eliminar del teléfono").performScrollTo().performClick()
            compose.onNodeWithText("¿Eliminar del teléfono?").assertExists()
            compose.onNodeWithText("Cancelar",substring=false).performClick()
            val previousCount=compose.runOnIdle {observer.mediaItemCount}
            val previousId=compose.runOnIdle {observer.currentMediaItem?.mediaId}
            compose.onNodeWithText("Reproducir a continuación").performClick()
            compose.waitUntil(5000) {compose.runOnIdle {observer.mediaItemCount==previousCount+1}}
            compose.runOnIdle {assertEquals(previousId,observer.currentMediaItem?.mediaId);assertEquals(uri.lastPathSegment,observer.getMediaItemAt(observer.currentMediaItemIndex+1).mediaId)}
            compose.onNodeWithContentDescription("Abrir reproductor").performClick()
            compose.onNodeWithText("Desliza:",substring=true).assertDoesNotExist()
            // Duplicate IDs must remain separate queue entries; moving preserves playback.
            compose.runOnIdle {
                val first=observer.getMediaItemAt(observer.currentMediaItemIndex)
                val other=(0 until observer.mediaItemCount).map {observer.getMediaItemAt(it)}.first {it.mediaId!=first.mediaId}
                observer.setMediaItems(listOf(first,other,first),0,2000)
                observer.prepare();observer.pause();observer.repeatMode=androidx.media3.common.Player.REPEAT_MODE_OFF
            }
            compose.onNodeWithContentDescription("Ver cola de reproducción").performClick()
            compose.onNodeWithText("Cola de reproducción",substring=false).assertExists()
            compose.onNodeWithText("Sonando ahora",substring=false).assertExists()
            compose.onNodeWithText("A continuación",substring=false).assertExists()
            compose.onNodeWithContentDescription("Subir canción 3").assertDoesNotExist()
            compose.onNodeWithContentDescription("Quitar canción 3 de la cola").assertDoesNotExist()
            dragQueue(2,1)
            compose.waitUntil(5000) {compose.runOnIdle {observer.getMediaItemAt(1).mediaId==previousId}}
            compose.runOnIdle {assertEquals(previousId,observer.currentMediaItem?.mediaId);assertEquals(2000L,observer.currentPosition);assertFalse(observer.playWhenReady)}
            dragQueue(1,2)
            compose.waitUntil(5000) {compose.runOnIdle {observer.getMediaItemAt(2).mediaId==previousId}}
            compose.runOnIdle {observer.removeMediaItem(2)}
            compose.waitUntil(5000) {compose.runOnIdle {observer.mediaItemCount==2}}
            compose.runOnIdle {assertEquals(previousId,observer.currentMediaItem?.mediaId);assertEquals(2000L,observer.currentPosition)}
            // Shuffle order is read from the timeline; a manual move materializes that order.
            var shuffledOrder:List<String> = emptyList()
            compose.runOnIdle {observer.shuffleModeEnabled=true}
            compose.waitUntil(5000) {compose.runOnIdle {observer.shuffleModeEnabled}}
            compose.runOnIdle {shuffledOrder=queueEntries(observer).map {it.mediaId}}
            dragQueue(1,0)
            compose.waitUntil(5000) {compose.runOnIdle {!observer.shuffleModeEnabled}}
            compose.runOnIdle {assertEquals(shuffledOrder.reversed(),queueEntries(observer).map {it.mediaId});assertEquals(previousId,observer.currentMediaItem?.mediaId);assertEquals(2000L,observer.currentPosition)}
            val screenshot=File(context.cacheDir,"queue.png")
            assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(screenshot))
            val shotValues=ContentValues().apply {put(MediaStore.Downloads.DISPLAY_NAME,"13-cola.png");put(MediaStore.Downloads.MIME_TYPE,"image/png");put(MediaStore.Downloads.RELATIVE_PATH,"Download/PrismaTest/")}
            val shotUri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,shotValues)!!
            resolver.openOutputStream(shotUri)!!.use {out->screenshot.inputStream().use {it.copyTo(out)}}
            compose.onNodeWithContentDescription("Cerrar cola").performClick()
            compose.onNodeWithContentDescription("Diseño Chromatic Flow").performTouchInput {swipeDown()}
            compose.waitUntil(5000) {compose.onAllNodesWithContentDescription("Abrir reproductor").fetchSemanticsNodes().isNotEmpty()}
            nav("Ajustes")
            compose.onNode(hasText("Personalización",substring=false) and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText("MODO DE COLOR",substring=false).performScrollTo().assertExists()
            compose.onNodeWithText("Oscuro",substring=false).performScrollTo().performClick()
            assertEquals(LightMode.DARK,Preferences(context).mode)
            compose.onNodeWithText("Según el sistema",substring=false).performScrollTo().performClick()
            compose.onNodeWithText("Elegir un color personalizado").performScrollTo().performClick()
            compose.onNodeWithText("Usar este color").performClick()
            assertEquals("#9D87E8",Preferences(context).personalization.accent)
            compose.onNodeWithText("Menta",substring=false).performScrollTo().performClick()
            compose.onNodeWithText("Lista compacta",substring=false).performScrollTo().performClick()
            compose.onNodeWithText("Artista y álbum",substring=false).performScrollTo().performClick()
            assertEquals("#58CBA4",Preferences(context).personalization.accent)
            assertEquals(LibraryLayout.COMPACT,Preferences(context).personalization.layout)
            assertEquals(SongInfoStyle.ALBUM,Preferences(context).personalization.info)
            compose.onNodeWithText("Deslizamiento",substring=false).performScrollTo().performClick()
            assertEquals(MotionStyle.SLIDE,Preferences(context).personalization.motion)
            compose.onNodeWithText("Probar animación").performScrollTo().performClick()
            shot("10-personalizacion")
            compose.onNodeWithContentDescription("Volver a ajustes").performClick()
            compose.onNodeWithContentDescription("Duración de entrada").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(4f)}
            compose.onNodeWithContentDescription("Duración de salida").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(3f)}
            assertEquals(4f,Preferences(context).fadeInSeconds,0f)
            assertEquals(3f,Preferences(context).fadeOutSeconds,0f)
            compose.runOnIdle {observer.seekTo(2000)}
            compose.waitUntil(10000) {compose.runOnIdle {kotlin.math.abs(observer.volume-.5f)<.04f}}
            compose.onNodeWithContentDescription("Duración de entrada").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(0f)}
            compose.onNodeWithContentDescription("Duración de salida").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(0f)}
            compose.waitUntil(10000) {compose.runOnIdle {observer.volume==1f}}

            // Add directly from an empty playlist; cancel has no write, existing songs are omitted.
            nav("Listas")
            compose.onNodeWithText("Crear lista de reproducción").performClick()
            compose.onNodeWithText("Nombre",substring=false).performTextInput("Prisma selección")
            compose.onNodeWithText("Crear",substring=false).performClick()
            compose.onNodeWithText("Prisma selección",substring=false).performClick()
            compose.onNodeWithText("Añadir canciones",substring=false).performClick()
            compose.onNodeWithContentDescription("Seleccionar Prisma prueba para Prisma selección").performClick()
            compose.onNodeWithText("Cancelar",substring=false).performClick()
            assertTrue(Preferences(context).playlists().getValue("Prisma selección").isEmpty())
            compose.onNodeWithText("Añadir canciones",substring=false).performClick()
            compose.onNodeWithContentDescription("Seleccionar Prisma prueba para Prisma selección").performClick()
            compose.onNodeWithText("Añadir 1 canciones",substring=false).performClick()
            assertEquals(listOf(uri.lastPathSegment),Preferences(context).playlists().getValue("Prisma selección"))
            compose.onNodeWithText("Añadir canciones",substring=false).performClick()
            compose.onNodeWithContentDescription("Seleccionar Prisma prueba para Prisma selección").assertDoesNotExist()
            compose.onNodeWithText("Buscar canciones para añadir").performTextInput("sin coincidencias")
            compose.onNodeWithText("No hay coincidencias.").assertExists()
            compose.onNodeWithText("Buscar canciones para añadir").performTextClearance()
            compose.onNodeWithContentDescription("Seleccionar Prisma segunda para Prisma selección").performClick()
            shot("18-anadir-playlist")
            compose.onNodeWithText("Añadir 1 canciones",substring=false).performClick()
            assertEquals(setOf(uri.lastPathSegment,second.lastPathSegment),Preferences(context).playlists().getValue("Prisma selección").toSet())
            nav("Biblioteca")
            compose.onNodeWithText("Seleccionar varias",substring=false).performClick()
            compose.onNodeWithContentDescription("Seleccionar Prisma prueba").performClick()
            compose.onNodeWithContentDescription("Seleccionar Prisma segunda").performClick()
            shot("19-multiseleccion")
            compose.onNodeWithContentDescription("Añadir selección a una playlist").performClick()
            compose.onNodeWithText("Prisma selección",substring=false).performClick()
            assertEquals(2,Preferences(context).playlists().getValue("Prisma selección").size)
            compose.onNodeWithText("Cancelar",substring=false).performClick()
            nav("Inicio")
            compose.onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText("Tu colección, contigo"))).performScrollToNode(hasText("Recién añadidas"))
            compose.onNodeWithText("Canciones de tu biblioteca").assertDoesNotExist()
            compose.onNodeWithText("Ver todas",substring=false).performClick()
            compose.onNodeWithText("Últimas incorporadas al teléfono, primero las más nuevas").assertExists()
            nav("Biblioteca")
            compose.onNode(hasText("Prisma prueba") and SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)).assertExists()
            shot("11-lista-compacta")
            nav("Ajustes")
            compose.onNode(hasText("Personalización",substring=false) and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText("Cuadrícula",substring=false).performScrollTo().performClick()
            nav("Biblioteca")
            compose.onNodeWithContentDescription("Opciones de Prisma prueba").performClick()
            compose.onNodeWithText("Añadir al final de la cola").performClick()
            nav("Ajustes")
            compose.onNode(hasText("Personalización",substring=false) and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText("Restablecer personalización").performScrollTo().performClick()
            assertEquals(Personalization(),Preferences(context).personalization)
            nav("Sonido")
            compose.onNode(isToggleable()).performClick()
            compose.onNode(hasText("Graves") and hasClickAction()).performClick()
            compose.onNodeWithText("Guardar preset",substring=false).performScrollTo().performClick()
            compose.onNodeWithText("Nombre del preset").performTextInput("Mi prueba")
            compose.onNodeWithText("Guardar",substring=false).performClick()
            val saved=Preferences(context).eqPresets().getValue("Mi prueba")
            assertArrayEquals(Preferences(context).eq.gains,saved.gains,0.0)
            compose.onNodeWithText("Plano",substring=false).performScrollTo().performClick()
            compose.onNodeWithText("Mis presets · 1",substring=false).performScrollTo().performClick()
            compose.onNodeWithText("Mi prueba",substring=false).performClick()
            assertArrayEquals(saved.gains,Preferences(context).eq.gains,0.0)
            assertEquals(saved.preamp,Preferences(context).eq.preamp,0.0)
            assertEquals(saved.q,Preferences(context).eq.q,0.0)
            shot("03-ecualizador")
            nav("Inicio")
            compose.onNodeWithText("Tu colección, contigo").assertExists()
            compose.onNodeWithText("Vuelve a escuchar").assertExists()
            shot("14-inicio-chromatic")
            nav("Ajustes")
            compose.onNode(hasText("Personalización",substring=false) and hasClickAction()).performScrollTo().performClick()
            compose.onNode(hasText("Temas") and hasClickAction()).performScrollTo().performClick()
            listOf("Chromatic Flow","Liquid Glass","Pure Minimal","Black Night","Studio","Sunset").forEachIndexed {index,title->
                compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(title))
                compose.onNodeWithText(title).performClick()
                compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
                compose.onNodeWithText("Oscuro",substring=false).performScrollTo().performClick()
                compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(title))
                shot("04-tema-$index-oscuro")
                compose.onNodeWithContentDescription("Abrir reproductor").performClick()
                compose.onNodeWithContentDescription("Diseño $title").assertExists()
                compose.onNodeWithContentDescription("Línea de tiempo $title").assertExists()
                compose.onNodeWithContentDescription("Línea de tiempo $title").performSemanticsAction(SemanticsActions.SetProgress) { it(1000f) }
                shot("07-reproductor-$index-oscuro")
                compose.onNodeWithContentDescription("Cerrar reproductor").performClick()
                nav("Biblioteca")
                shot("08-biblioteca-$index-oscuro")
                nav("Ajustes")
            compose.onNode(hasText("Personalización",substring=false) and hasClickAction()).performScrollTo().performClick()
                compose.onNode(hasText("Temas") and hasClickAction()).performScrollTo().performClick()
                compose.onNodeWithText("Claro",substring=false).performScrollTo().performClick()
                compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(title))
                shot("05-tema-$index-claro")
                compose.onNodeWithContentDescription("Abrir reproductor").performClick()
                shot("15-reproductor-$index-claro")
                compose.onNodeWithContentDescription("Cerrar reproductor").performClick()
                nav("Inicio")
                shot("16-inicio-$index-claro")
                nav("Ajustes")
            compose.onNode(hasText("Personalización",substring=false) and hasClickAction()).performScrollTo().performClick()
                compose.onNode(hasText("Temas") and hasClickAction()).performScrollTo().performClick()
            }
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            compose.onNodeWithText("Según el sistema").performClick()
            val prefs=Preferences(context)
            assertEquals(LightMode.SYSTEM,prefs.mode)
            assertEquals(VisualTheme.SUNSET,prefs.theme)
            assertTrue(prefs.eq.enabled)
            // Only generated test audio is deleted. Exercise the native Android consent dialog.
            nav("Biblioteca")
            compose.onNodeWithContentDescription("Opciones de Prisma segunda").performClick()
            compose.onNodeWithText("Eliminar del teléfono").performScrollTo().performClick()
            compose.onNodeWithText("Eliminar archivo",substring=false).performClick()
            val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val deleteButton=device.wait(Until.findObject(By.text(Pattern.compile("(?i)delete|eliminar|allow|permitir"))),10000)
            assertNotNull("Android debe mostrar el consentimiento de eliminación",deleteButton)
            deleteButton!!.click()
            compose.waitUntil(10000) {resolver.query(second,arrayOf("_id"),null,null,null)?.use {it.count==0} ?: true}
            compose.waitUntil(10000) {runCatching {compose.onAllNodes(isRoot()).fetchSemanticsNodes().isNotEmpty() && compose.onAllNodesWithContentDescription("Opciones de Prisma segunda").fetchSemanticsNodes().isEmpty()}.getOrDefault(false)}
            compose.runOnIdle {for(i in 0 until observer.mediaItemCount) assertNotEquals(second.lastPathSegment,observer.getMediaItemAt(i).mediaId);observer.release()}
            shot("12-eliminacion-completada")
        } catch (t:Throwable) {
            runCatching {shot("99-failure")}
            runCatching {compose.onAllNodes(isRoot()).onLast().printToLog("PrismaTest")}
            throw t
        } finally {runCatching {resolver.delete(uri,null,null)};runCatching {resolver.delete(second,null,null)}}
    }
}
