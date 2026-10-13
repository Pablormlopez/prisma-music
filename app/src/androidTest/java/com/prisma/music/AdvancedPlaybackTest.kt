package com.prisma.music

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.session.SessionCommand
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AdvancedPlaybackTest {
    @get:Rule(order=0) val permissions=GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_AUDIO,Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order=1) val compose=createAndroidComposeRule<MainActivity>()
    @Test fun precisionSinkKeepsTwentyFourBitsAcrossPartialWrites() {
        val output=mutableListOf<Float>();var format:Format?=null
        val fake=Proxy.newProxyInstance(AudioSink::class.java.classLoader,arrayOf(AudioSink::class.java)) {_,method,args->
            when(method.name) {
                "configure"->{format=args!![0] as Format;null}
                "supportsFormat"->true
                "getFormatSupport"->2
                "handleBuffer"->{val b=args!![0] as ByteBuffer;output+=b.float;!b.hasRemaining()}
                else->when(method.returnType) {java.lang.Boolean.TYPE->false;java.lang.Integer.TYPE->0;java.lang.Long.TYPE->0L;else->null}
            }
        } as AudioSink
        val sink=PrecisionAudioSink(fake,{AdvancedSettings()},{DspEngine.Config.flat()})
        sink.configure(Format.Builder().setSampleMimeType(MimeTypes.AUDIO_RAW).setSampleRate(48000).setChannelCount(1).setPcmEncoding(C.ENCODING_PCM_24BIT).build(),0,null)
        assertEquals(C.ENCODING_PCM_FLOAT,format!!.pcmEncoding)
        val pcm=ByteBuffer.wrap(byteArrayOf(1,0,0,0,0,-128)).order(ByteOrder.LITTLE_ENDIAN)
        assertFalse(sink.handleBuffer(pcm,0,1));assertEquals(0,pcm.position())
        assertTrue(sink.handleBuffer(pcm,0,1));assertEquals(pcm.limit(),pcm.position())
        assertEquals(1f/8388608,output[0],0f);assertEquals(-1f,output[1],0f)
        sink.flush()
    }
    @Test fun realOverlapPauseHandoffAndOutputSwitchKeepTheSession() {
        val context=compose.activity;val resolver=context.contentResolver
        val prefs=Preferences(context);val original=prefs.advanced
        fun fixture(name:String):android.net.Uri {
            val values=ContentValues().apply {put(MediaStore.Audio.Media.DISPLAY_NAME,name);put(MediaStore.Audio.Media.MIME_TYPE,"audio/flac");put(MediaStore.Audio.Media.RELATIVE_PATH,"Music/PrismaAdvancedTest/");put(MediaStore.Audio.Media.IS_PENDING,1)}
            val uri=resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values)!!
            InstrumentationRegistry.getInstrumentation().context.assets.open(name).use {input->resolver.openOutputStream(uri)!!.use {input.copyTo(it)}}
            resolver.update(uri,ContentValues().apply {put(MediaStore.Audio.Media.IS_PENDING,0)},null,null);return uri
        }
        val a=fixture("mix-a.flac");val b=fixture("mix-b.flac")
        var connection:com.google.common.util.concurrent.ListenableFuture<MediaController>?=null
        compose.runOnIdle {connection=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()}
        val controller=connection!!.get(10,TimeUnit.SECONDS)
        fun apply() {val future=compose.runOnIdle {controller.sendCustomCommand(SessionCommand("prisma.settings",Bundle.EMPTY),Bundle.EMPTY)};assertEquals(0,future.get(10,TimeUnit.SECONDS).resultCode)}
        fun status():Bundle {val future=compose.runOnIdle {controller.sendCustomCommand(SessionCommand("prisma.test.audio",Bundle.EMPTY),Bundle.EMPTY)};return future.get(5,TimeUnit.SECONDS).extras}
        try {
            prefs.setAdvanced("floatOutput",true);prefs.setAdvanced("normalize",true);prefs.setAdvanced("crossfade",2f);apply()
            compose.runOnIdle {controller.setMediaItems(listOf(MediaItem.Builder().setMediaId("mix-a").setUri(a).build(),MediaItem.Builder().setMediaId("mix-b").setUri(b).build()));controller.repeatMode=Player.REPEAT_MODE_OFF;controller.shuffleModeEnabled=false;controller.prepare();controller.play()}
            compose.waitUntil(15000) {status().getBoolean("mixing") && status().getBoolean("incomingPlaying")}
            assertTrue(status().getLong("incomingPosition")>=0)
            compose.runOnIdle {controller.pause()}
            compose.waitUntil(5000) {!status().getBoolean("incomingPlaying")}
            compose.runOnIdle {controller.seekTo(0,1000);controller.play()}
            compose.waitUntil(15000) {compose.runOnIdle {controller.currentMediaItem?.mediaId=="mix-b"}}
            val carried=compose.runOnIdle {controller.currentPosition}
            assertTrue("Incoming audio position must carry into the main player: $carried",carried>=600)
            val before=compose.runOnIdle {controller.currentPosition}
            prefs.setAdvanced("floatOutput",false);prefs.setAdvanced("stereoWidth",1.3f);apply()
            compose.waitUntil(10000) {compose.runOnIdle {controller.currentMediaItem?.mediaId=="mix-b" && controller.isPlaying}}
            compose.runOnIdle {assertEquals(2,controller.mediaItemCount);assertTrue(controller.currentPosition>=before-300)}
            assertFalse(status().getBoolean("floatOutput"))
            compose.runOnIdle {controller.repeatMode=Player.REPEAT_MODE_ONE;controller.seekTo(10500)}
            compose.waitForIdle();assertFalse(status().getBoolean("mixing"))
        } finally {
            compose.runOnIdle {controller.pause();controller.clearMediaItems()}
            prefs.setAdvanced("normalize",original.normalize);prefs.setAdvanced("crossfade",original.crossfade);prefs.setAdvanced("floatOutput",original.floatOutput);prefs.setAdvanced("stereoWidth",original.stereoWidth)
            apply();compose.runOnIdle {controller.release()}
            resolver.delete(a,null,null);resolver.delete(b,null,null)
        }
    }
}
