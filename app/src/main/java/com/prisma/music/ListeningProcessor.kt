package com.prisma.music
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
@UnstableApi internal class ListeningProcessor(private val settings:()->AdvancedSettings):BaseAudioProcessor() {
    private var dsp:ListeningDsp?=null
    override fun onConfigure(inputAudioFormat:AudioProcessor.AudioFormat):AudioProcessor.AudioFormat {
        if(inputAudioFormat.encoding!=C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        dsp=ListeningDsp(inputAudioFormat.sampleRate,inputAudioFormat.channelCount);return inputAudioFormat
    }
    override fun onFlush() {if(inputAudioFormat.sampleRate>0) dsp=ListeningDsp(inputAudioFormat.sampleRate,inputAudioFormat.channelCount)}
    override fun queueInput(inputBuffer:ByteBuffer) {
        if(!inputBuffer.hasRemaining())return
        val output=replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.LITTLE_ENDIAN)
        val config=settings()
        if(!config.normalize && config.stereoWidth==1f) {output.put(inputBuffer);output.flip();return}
        val samples=PcmSamples.decode(inputBuffer,16)
        dsp?.apply {normalize=config.normalize;targetDb=config.targetDb;width=config.stereoWidth;process(samples)}
        samples.forEach {output.putShort((it*32768).toInt().coerceIn(-32768,32767).toShort())}
        inputBuffer.position(inputBuffer.limit());output.flip()
    }
}
