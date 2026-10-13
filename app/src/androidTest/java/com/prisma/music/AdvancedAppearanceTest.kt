package com.prisma.music
import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Rule
import org.junit.Assert.*
class AdvancedAppearanceTest {
    @get:Rule val compose=createComposeRule()
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private fun screenshot(name:String) {
        val resolver=context.contentResolver
        val values=ContentValues().apply {put(MediaStore.Downloads.DISPLAY_NAME,"$name.png");put(MediaStore.Downloads.MIME_TYPE,"image/png");put(MediaStore.Downloads.RELATIVE_PATH,"Download/PrismaTest/");put(MediaStore.Downloads.IS_PENDING,1)}
        val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values)!!
        resolver.openOutputStream(uri)!!.use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        resolver.update(uri,ContentValues().apply {put(MediaStore.Downloads.IS_PENDING,0)},null,null)
    }
    @Test fun appearanceControlsPersistAndCanBeReversed() {
        val prefs=Preferences(context);val previous=prefs.advanced
        try {
            compose.setContent {MaterialTheme {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {AdvancedAppearanceSettings(prefs)}}}
            compose.onNodeWithContentDescription("Portada animada").performScrollTo().performClick()
            assertEquals(!previous.animatedCover,prefs.advanced.animatedCover)
            if(!prefs.advanced.animatedCover)compose.onNodeWithContentDescription("Portada animada").performClick()
            compose.onNodeWithText("Deslizar",substring=false).performScrollTo().performClick()
            assertEquals("Deslizar",prefs.advanced.coverMotion)
            screenshot("50-apariencia-portada")
            compose.onNodeWithContentDescription("Portada a pantalla completa").performScrollTo().performClick()
            assertEquals(!previous.fullCover,prefs.advanced.fullCover)
            compose.onNodeWithContentDescription("Reducir desenfoque").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Modo de alto rendimiento").performScrollTo().performClick()
            screenshot("51-apariencia-rendimiento")
        } finally {
            prefs.setAdvanced("animatedCover",previous.animatedCover);prefs.setAdvanced("coverMotion",previous.coverMotion)
            prefs.setAdvanced("fullCover",previous.fullCover);prefs.setAdvanced("reduceBlur",previous.reduceBlur);prefs.setAdvanced("performance",previous.performance)
        }
    }
    @Test fun advancedAudioControlsWriteTheRealSettings() {
        val prefs=Preferences(context);val original=prefs.advanced
        try {
            compose.setContent {MaterialTheme {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {AdvancedAudioSettings(prefs,null,{})}}}
            compose.onNodeWithContentDescription("Normalizar volumen").performScrollTo().performClick()
            assertEquals(!original.normalize,prefs.advanced.normalize)
            compose.onNodeWithContentDescription("Mezcla entre canciones").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) {it(3f)}
            assertEquals(3f,prefs.advanced.crossfade,.01f)
            compose.onNodeWithContentDescription("Amplitud estéreo").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) {it(1.4f)}
            assertEquals(1.4f,prefs.advanced.stereoWidth,.01f)
            screenshot("52-audio-avanzado")
        } finally {prefs.setAdvanced("normalize",original.normalize);prefs.setAdvanced("crossfade",original.crossfade);prefs.setAdvanced("stereoWidth",original.stereoWidth)}
    }
}
