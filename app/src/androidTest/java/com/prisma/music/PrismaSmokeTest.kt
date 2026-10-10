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
            compose.onNodeWithContentDescription("Actualizar biblioteca").performClick()
            compose.waitUntil(15000) {compose.onAllNodesWithText("Prisma prueba").fetchSemanticsNodes().isNotEmpty()}
            shot("01-biblioteca")
            compose.onNodeWithText("Prisma prueba").performClick()
            compose.waitUntil(15000) {compose.onAllNodesWithContentDescription("Pausar").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithContentDescription("Abrir reproductor").performClick()
            compose.waitUntil(5000) {compose.onAllNodesWithText("Reproduciendo").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithContentDescription("Abrir reproductor").assertDoesNotExist()
            compose.onAllNodesWithContentDescription("Pausar").assertCountEquals(1)
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
            // The short FLAC fixture can finish while lyrics/screenshots are checked.
            // Both playing and naturally ended playback are valid here.
            if (compose.onAllNodesWithContentDescription("Pausar").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription("Pausar").performClick()
            }
            compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Reproducir").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Reproducir").assertExists()
            compose.onNodeWithContentDescription("Volver a la biblioteca").performClick()
            compose.onNodeWithContentDescription("Abrir reproductor").assertExists()
            // A long press opens actions without replacing playback; deletion can be canceled.
            compose.onNode(hasText("Prisma prueba") and SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)).performTouchInput {longClick()}
            compose.onNodeWithText("Reproducir a continuación").assertExists()
            shot("09-menu-cancion")
            compose.onNodeWithText("Eliminar del teléfono").performScrollTo().performClick()
            compose.onNodeWithText("¿Eliminar del teléfono?").assertExists()
            compose.onNodeWithText("Cancelar",substring=false).performClick()
            var connection:com.google.common.util.concurrent.ListenableFuture<MediaController>?=null
            compose.runOnIdle {connection=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()}
            val observer=connection!!.get(10,TimeUnit.SECONDS)
            val previousCount=compose.runOnIdle {observer.mediaItemCount}
            val previousId=compose.runOnIdle {observer.currentMediaItem?.mediaId}
            compose.onNodeWithText("Reproducir a continuación").performClick()
            compose.waitUntil(5000) {compose.runOnIdle {observer.mediaItemCount==previousCount+1}}
            compose.runOnIdle {assertEquals(previousId,observer.currentMediaItem?.mediaId);assertEquals(uri.lastPathSegment,observer.getMediaItemAt(observer.currentMediaItemIndex+1).mediaId)}
            compose.onNodeWithContentDescription("Abrir reproductor").performClick()
            compose.onNodeWithContentDescription("Diseño Chromatic Flow").performTouchInput {swipeDown()}
            compose.waitUntil(5000) {compose.onAllNodesWithContentDescription("Abrir reproductor").fetchSemanticsNodes().isNotEmpty()}
            nav("Ajustes")
            compose.onNodeWithText("Elegir un color personalizado").performScrollTo().performClick()
            compose.onNodeWithText("Usar este color").performClick()
            assertEquals("#9D87E8",Preferences(context).personalization.accent)
            compose.onNodeWithText("Menta",substring=false).performScrollTo().performClick()
            compose.onNodeWithText("Lista compacta",substring=false).performScrollTo().performClick()
            compose.onNodeWithText("Artista y álbum",substring=false).performScrollTo().performClick()
            assertEquals("#58CBA4",Preferences(context).personalization.accent)
            assertEquals(LibraryLayout.COMPACT,Preferences(context).personalization.layout)
            assertEquals(SongInfoStyle.ALBUM,Preferences(context).personalization.info)
            shot("10-personalizacion")
            nav("Música")
            compose.onNode(hasText("Prisma prueba") and SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)).assertExists()
            shot("11-lista-compacta")
            nav("Ajustes")
            compose.onNodeWithText("Cuadrícula",substring=false).performScrollTo().performClick()
            nav("Música")
            compose.onNodeWithContentDescription("Opciones de Prisma prueba").performClick()
            compose.onNodeWithText("Añadir al final de la cola").performClick()
            nav("Ajustes")
            compose.onNodeWithText("Restablecer personalización").performScrollTo().performClick()
            assertEquals(Personalization(),Preferences(context).personalization)
            nav("Sonido")
            compose.onNode(isToggleable()).performClick()
            compose.onNode(hasText("Graves") and hasClickAction()).performClick()
            shot("03-ecualizador")
            nav("Ajustes")
            listOf("Chromatic Flow","Liquid Glass","Pure Minimal","Black Night").forEachIndexed {index,title->
                compose.onNodeWithText(title).performScrollTo().performClick()
                compose.onNodeWithText("Oscuro",substring=false).performScrollTo().performClick()
                compose.onNodeWithText("Chromatic Flow").performScrollTo()
                shot("04-tema-$index-oscuro")
                compose.onNodeWithContentDescription("Abrir reproductor").performClick()
                compose.onNodeWithContentDescription("Diseño $title").assertExists()
                compose.onNodeWithContentDescription("Línea de tiempo $title").assertExists()
                compose.onNodeWithContentDescription("Línea de tiempo $title").performSemanticsAction(SemanticsActions.SetProgress) { it(1000f) }
                shot("07-reproductor-$index-oscuro")
                nav("Música")
                shot("08-biblioteca-$index-oscuro")
                nav("Ajustes")
                compose.onNodeWithText("Claro",substring=false).performScrollTo().performClick()
                compose.onNodeWithText("Chromatic Flow").performScrollTo()
                shot("05-tema-$index-claro")
            }
            compose.onNodeWithText("Según el sistema").performScrollTo().performClick()
            val prefs=Preferences(context)
            assertEquals(LightMode.SYSTEM,prefs.mode)
            assertEquals(VisualTheme.NIGHT,prefs.theme)
            assertTrue(prefs.eq.enabled)
            // Only generated test audio is deleted. Exercise the native Android consent dialog.
            nav("Música")
            compose.onNodeWithContentDescription("Opciones de Prisma segunda").performClick()
            compose.onNodeWithText("Eliminar del teléfono").performScrollTo().performClick()
            compose.onNodeWithText("Eliminar archivo",substring=false).performClick()
            val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val deleteButton=device.wait(Until.findObject(By.res("android","button1")),5000) ?: device.findObject(By.text(Pattern.compile("(?i)delete|eliminar|allow|permitir")))
            deleteButton?.click()
            compose.waitUntil(10000) {resolver.query(second,arrayOf("_id"),null,null,null)?.use {it.count==0} ?: true}
            compose.waitUntil(5000) {compose.onAllNodesWithContentDescription("Opciones de Prisma segunda").fetchSemanticsNodes().isEmpty()}
            compose.runOnIdle {for(i in 0 until observer.mediaItemCount) assertNotEquals(second.lastPathSegment,observer.getMediaItemAt(i).mediaId);observer.release()}
            shot("12-eliminacion-completada")
        } catch (t:Throwable) {
            runCatching {shot("99-failure")}
            compose.onAllNodes(isRoot()).onLast().printToLog("PrismaTest")
            throw t
        } finally {runCatching {resolver.delete(uri,null,null)};runCatching {resolver.delete(second,null,null)}}
    }
}
