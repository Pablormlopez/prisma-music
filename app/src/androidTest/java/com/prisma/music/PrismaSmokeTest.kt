package com.prisma.music

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
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

/** Real app smoke test on Android 35. Uses an original generated tone, no user music. */
@RunWith(AndroidJUnit4::class)
class PrismaSmokeTest {
    @get:Rule(order=0) val permissions:GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_AUDIO,Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order=1) val compose = createAndroidComposeRule<MainActivity>()
    private fun nav(text:String) {compose.onNode(hasText(text) and hasClickAction()).performClick();compose.waitForIdle()}
    private fun shot(name:String) {
        val folder=File(compose.activity.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
        File(folder,"$name.png").outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun libraryPlaybackThemesAndDspControls() {
        val context=compose.activity
        val resolver=context.contentResolver
        val values=ContentValues().apply {put(MediaStore.Audio.Media.DISPLAY_NAME,"Prisma prueba.wav");put(MediaStore.Audio.Media.MIME_TYPE,"audio/wav");put(MediaStore.Audio.Media.RELATIVE_PATH,"Music/PrismaTest/");put(MediaStore.Audio.Media.TITLE,"Prisma prueba");put(MediaStore.Audio.Media.ARTIST,"Prisma Music");put(MediaStore.Audio.Media.ALBUM,"Prueba local");put(MediaStore.Audio.Media.IS_PENDING,1)}
        val uri=resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values)!!
        try {
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
            shot("02-reproductor")
            compose.onNodeWithContentDescription("Volver a la biblioteca").performClick()
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
                compose.onNodeWithText("Claro",substring=false).performScrollTo().performClick()
                compose.onNodeWithText("Chromatic Flow").performScrollTo()
                shot("05-tema-$index-claro")
            }
            compose.onNodeWithText("Según el sistema").performScrollTo().performClick()
            val prefs=Preferences(context)
            assertEquals(LightMode.SYSTEM,prefs.mode)
            assertEquals(VisualTheme.NIGHT,prefs.theme)
            assertTrue(prefs.eq.enabled)
        } finally {resolver.delete(uri,null,null)}
    }
}
