package com.prisma.music

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class AlbumPlayerTest {
    @get:Rule val compose=createComposeRule()
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val song=Song("atmosphere-test",Uri.EMPTY,"Luz de la tarde","Prisma","Colores", "", "", "test.flac",180000,0,"audio/flac",Uri.EMPTY)
    private fun cover():Bitmap=Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888).also {bitmap->
        val canvas=Canvas(bitmap);canvas.drawColor(0xffe7b476.toInt())
        val paint=Paint(Paint.ANTI_ALIAS_FLAG);paint.color=0xff436e63.toInt();canvas.drawCircle(100f,300f,190f,paint)
        paint.color=0xffb96677.toInt();canvas.drawCircle(350f,140f,170f,paint)
        paint.color=0xfff4e8d6.toInt();paint.textSize=38f;canvas.drawText("PRISMA",110f,170f,paint)
    }
    private fun shot(name:String) {
        val resolver=context.contentResolver
        val values=ContentValues().apply {put(MediaStore.Downloads.DISPLAY_NAME,"$name.png");put(MediaStore.Downloads.MIME_TYPE,"image/png");put(MediaStore.Downloads.RELATIVE_PATH,"Download/PrismaTest/");put(MediaStore.Downloads.IS_PENDING,1)}
        val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values)!!
        resolver.openOutputStream(uri)!!.use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        resolver.update(uri,ContentValues().apply {put(MediaStore.Downloads.IS_PENDING,0)},null,null)
    }
    @Test fun ambientOpensWithLocalLyricsAndClosesBackToPlayer() {
        val prefs=Preferences(context)
        val art=cover()
        val details=Library.Details("FLAC",art,0xffe7b476.toInt(),"[00:00.00]Ambiente de prueba",FrostedArtwork.create(art))
        compose.setContent {MaterialTheme {
            PlayerScreen(song,details,VisualTheme.GLASS,prefs,1000,180000,false,false,0,null,{},{},{})
        }}
        compose.onNodeWithContentDescription("Modo ambiente").performScrollTo().performClick()
        compose.onNodeWithText("Ambiente de prueba").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cerrar modo ambiente").performClick()
        compose.onNodeWithContentDescription("Modo ambiente").assertExists()
    }
    @Test fun allThemesShareAtmosphereAndRestoreTheirOwnLayout() {
        val prefs=Preferences(context)
        var selected by mutableStateOf(VisualTheme.GLASS)
        var immersive by mutableStateOf(true)
        compose.setContent {MaterialTheme {CompositionLocalProvider(
            LocalVisualTheme provides selected,
            LocalAdvanced provides AdvancedSettings(immersiveAllThemes=immersive),
            LocalPersonalization provides Personalization(motion=MotionStyle.NONE)
        ) {Box(Modifier.size(360.dp,700.dp)) {
            PlayerScreen(song,null,selected,prefs,0,180000,false,false,0,null,{},{},{})
        }}}}
        VisualTheme.entries.forEach {theme ->
            compose.runOnIdle {selected=theme;immersive=true}
            compose.onNodeWithContentDescription("Diseño ${theme.title}").assertExists()
            compose.onNodeWithContentDescription("Volumen multimedia").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Cerrar reproductor").performScrollTo().assertIsDisplayed()
            if(theme!=VisualTheme.GLASS && theme!=VisualTheme.CHROMATIC) {
                compose.runOnIdle {immersive=false}
                compose.onNodeWithText("Carátula",substring=false).performScrollTo().assertIsDisplayed()
            }
        }
    }
    @Test fun glassArtworkLyricsAndSmallScreenRemainUsable() {
        val prefs=Preferences(context);val previous=prefs.showMetadata;prefs.showMetadata=false
        val art=cover();val details=Library.Details("FLAC · 44.1 kHz",art,0xffe7b476.toInt(),"[00:00.00]Luz entre los colores\n[00:10.00]Una nueva canción",FrostedArtwork.create(art))
        var dismissed=false;var artist=false
        try {
            compose.setContent {MaterialTheme {CompositionLocalProvider(LocalVisualTheme provides VisualTheme.GLASS,LocalPersonalization provides Personalization(motion=MotionStyle.NONE)) {
                Box(Modifier.size(360.dp,700.dp)) {PlayerScreen(song,details,VisualTheme.GLASS,prefs,1000,180000,true,false,0,null,{dismissed=true},{artist=true},{})}
            }}}
            compose.onNodeWithContentDescription("Diseño Liquid Glass").assertExists()
            compose.onNodeWithContentDescription("Línea de tiempo Liquid Glass").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(5000f)}
            compose.onNodeWithContentDescription("Volumen multimedia").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Cerrar reproductor").performScrollTo()
            shot("40-liquid-glass-album")
            compose.onNodeWithContentDescription("Ver letra").performScrollTo().performClick()
            compose.onNodeWithText("Luz entre los colores").assertExists()
            compose.onNodeWithContentDescription("Deslizar hacia abajo para cerrar").performScrollTo()
            shot("41-liquid-glass-lyrics")
            compose.onNodeWithContentDescription("Ver carátula").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Ver artista Prisma").performScrollTo().performClick()
            assertTrue(artist)
            compose.onNodeWithContentDescription("Cerrar reproductor").performScrollTo().performClick()
            assertTrue(dismissed)
        } finally {prefs.showMetadata=previous}
    }
    @Test fun missingCoverAndShortDisplayKeepControlsReachable() {
        val prefs=Preferences(context)
        compose.setContent {MaterialTheme {CompositionLocalProvider(LocalVisualTheme provides VisualTheme.CHROMATIC) {
            Box(Modifier.size(320.dp,460.dp)) {PlayerScreen(song,null,VisualTheme.CHROMATIC,prefs,0,0,false,false,0,null,{},{},{})}
        }}}
        compose.onNodeWithContentDescription("Diseño Chromatic Flow").assertExists()
        compose.onNodeWithContentDescription("Volumen multimedia").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Importar letra").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Ver cola de reproducción").assertIsNotEnabled()
        shot("42-short-display-fallback")
    }
}
