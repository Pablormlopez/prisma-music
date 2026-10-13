package com.prisma.music
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class AnimatedCoverTest {
    @get:Rule val compose=createComposeRule()
    @Test fun gifMovesDuringPlaybackAndFreezesWhenPaused() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.cacheDir,"cover-motion-test.gif")
        InstrumentationRegistry.getInstrumentation().context.assets.open("cover-motion.gif").use {input->file.outputStream().use {input.copyTo(it)}}
        val playing=mutableStateOf(true)
        try {
            compose.setContent {MaterialTheme {CompositionLocalProvider(LocalAdvanced provides AdvancedSettings(animatedCover=true),LocalArtworkPlaying provides playing.value) {ImportedArtwork(Uri.fromFile(file).toString(),Modifier.size(200.dp))}}}
            fun color():Int {val bitmap=compose.onNodeWithContentDescription("Portada personalizada").captureToImage().asAndroidBitmap();return bitmap.getPixel(bitmap.width/2,bitmap.height/2)}
            compose.waitUntil(10000) {val c=color();(Color.red(c)>200 && Color.blue(c)<50)||(Color.blue(c)>200 && Color.red(c)<50)}
            val before=color()
            compose.waitUntil(5000) {color()!=before}
            compose.runOnIdle {playing.value=false};compose.waitForIdle();Thread.sleep(200)
            val paused=color();Thread.sleep(300);assertEquals(paused,color())
        } finally {file.delete()}
    }
}
