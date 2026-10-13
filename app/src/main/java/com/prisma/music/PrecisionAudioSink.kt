package com.prisma.music

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Processes decoder PCM before the floating-point sink; buffers survive partial AudioTrack writes. */
@UnstableApi
internal class PrecisionAudioSink(sink:AudioSink,private val settings:()->AdvancedSettings,private val eq:()->DspEngine.Config):ForwardingAudioSink(sink) {
    private var bits=16
    private var floatInput=false
    private var raw=false
    private var channels=2
    private var rate=0
    private var engine:DspEngine?=null
    private var listening:ListeningDsp?=null
    private var applied:DspEngine.Config?=null
    private var pending:ByteBuffer?=null
    private var original:ByteBuffer?=null
    private var data=FloatArray(0)
    private var converted=ByteBuffer.allocateDirect(0)
    private fun convertedFormat(f:Format)=if(f.sampleMimeType==MimeTypes.AUDIO_RAW) f.buildUpon().setPcmEncoding(C.ENCODING_PCM_FLOAT).build() else f
    override fun supportsFormat(format:Format)=super.supportsFormat(convertedFormat(format))
    override fun getFormatSupport(format:Format)=super.getFormatSupport(convertedFormat(format))
    override fun configure(inputFormat:Format,specifiedBufferSize:Int,outputChannels:IntArray?) {
        raw=inputFormat.sampleMimeType==MimeTypes.AUDIO_RAW
        if(raw) {
            floatInput=inputFormat.pcmEncoding==C.ENCODING_PCM_FLOAT
            bits=when(inputFormat.pcmEncoding) {C.ENCODING_PCM_8BIT->8;C.ENCODING_PCM_24BIT->24;C.ENCODING_PCM_32BIT,C.ENCODING_PCM_FLOAT->32;else->16}
            channels=inputFormat.channelCount;rate=inputFormat.sampleRate
            engine=DspEngine(inputFormat.sampleRate,channels)
            listening=ListeningDsp(inputFormat.sampleRate,channels)
            applied=null
        }
        pending=null;original=null
        super.configure(convertedFormat(inputFormat),specifiedBufferSize,outputChannels)
    }
    override fun handleBuffer(buffer:ByteBuffer,presentationTimeUs:Long,encodedAccessUnitCount:Int):Boolean {
        if(!raw) return super.handleBuffer(buffer,presentationTimeUs,encodedAccessUnitCount)
        if(!buffer.hasRemaining() && pending==null) return true
        if(pending==null) {
            data=PcmSamples.decode(buffer,bits,floatInput)
            val config=eq()
            if(applied!==config) {engine?.configure(config);applied=config}
            engine?.process(data)
            val options=settings()
            listening?.apply {normalize=options.normalize;targetDb=options.targetDb;width=options.stereoWidth;process(data)}
            if(converted.capacity()<data.size*4) converted=ByteBuffer.allocateDirect(data.size*4).order(ByteOrder.LITTLE_ENDIAN)
            converted.clear();data.forEach {converted.putFloat(it)};converted.flip()
            pending=converted;original=buffer
        }
        check(original===buffer) {"PCM buffer changed before output was consumed"}
        val handled=super.handleBuffer(pending!!,presentationTimeUs,encodedAccessUnitCount)
        if(handled) {buffer.position(buffer.limit());pending=null;original=null}
        return handled
    }
    override fun flush() {pending=null;original=null;applied=null;if(raw && rate>0) {engine=DspEngine(rate,channels);listening=ListeningDsp(rate,channels)};super.flush()}
    override fun reset() {pending=null;original=null;engine=null;listening=null;applied=null;super.reset()}
}
