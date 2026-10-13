package com.prisma.music
import org.junit.Test
import org.junit.Assert.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*
class AdvancedAudioTest {
    @Test fun twentyFourBitPrecisionAndBufferPositionArePreserved() {
        val source=ByteBuffer.wrap(byteArrayOf(1,0,0,0,0,-128))
        val values=PcmSamples.decode(source,24)
        assertEquals(1f/8388608,values[0],0f);assertEquals(-1f,values[1],0f)
        assertEquals(0,source.position())
        val pcm=ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putFloat(Float.NaN).putFloat(.25f).apply {flip()}
        assertArrayEquals(floatArrayOf(0f,.25f),PcmSamples.decode(pcm,32,true),0f)
    }
    @Test fun mixingNeverAmplifiesCorrelatedFullScaleTracks() {
        for(step in 0..100) {val (out,incoming)=mixGains(step/100f);assertTrue(out>=0 && incoming>=0);assertTrue(out+incoming<=1.000001f)}
        assertEquals(1f,mixGains(0f).first,0f);assertEquals(0f,mixGains(0f).second,0f)
        assertEquals(1f,mixGains(1f).second,.00001f)
    }
    @Test fun normalizationReducesLargeDifferencesWithoutClippingOrLiftingSilence() {
        fun normalized(amplitude:Float):Double {
            val dsp=ListeningDsp(8000,2).apply {normalize=true}
            var last=FloatArray(0)
            repeat(80) {block->last=FloatArray(1600) {i->(sin(2*PI*200*(block*800+i/2)/8000)*amplitude).toFloat()};dsp.process(last);assertTrue(last.all {it.isFinite() && abs(it)<=1f})}
            return sqrt(last.map {it.toDouble()*it}.average())
        }
        val loud=normalized(.7f);val quiet=normalized(.06f)
        assertTrue(loud/quiet<1.5)
        val silence=FloatArray(8000)
        ListeningDsp(8000,1).apply {normalize=true;process(silence)}
        assertTrue(silence.all {it==0f})
    }
    @Test fun stereoWidthPreservesCenterAndDoesNotAffectSurroundChannels() {
        val centered=floatArrayOf(.3f,.3f)
        ListeningDsp(48000,2).apply {width=1.8f;process(centered)}
        assertEquals(centered[0],centered[1],0f)
        val surround=floatArrayOf(.1f,.2f,.3f,.4f,.5f,.6f)
        val original=surround.copyOf()
        ListeningDsp(48000,6).apply {width=1.8f;process(surround)}
        assertArrayEquals(original,surround,.00001f)
    }
}
