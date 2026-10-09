package com.prisma.music

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

@UnstableApi
class EqProcessor : BaseAudioProcessor() {
    @Volatile var settings: DspEngine.Config = DspEngine.Config.flat()
    private var applied: DspEngine.Config? = null
    private var engine: DspEngine? = null
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        engine = DspEngine(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        applied = null
        return inputAudioFormat
    }
    override fun onFlush() {
        if (inputAudioFormat.sampleRate > 0) engine = DspEngine(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        applied = null
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        // Media3 also queues EMPTY_BUFFER while draining its pipeline. Never copy
        // it into BaseAudioProcessor's initial EMPTY_BUFFER (the same instance).
        if (!inputBuffer.hasRemaining()) return
        val size = inputBuffer.remaining()
        val output = replaceOutputBuffer(size).order(ByteOrder.LITTLE_ENDIAN)
        val config = settings
        if (!config.enabled) { output.put(inputBuffer); output.flip(); return }
        val data = FloatArray(size / 2)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (i in data.indices) data[i] = inputBuffer.short / 32768f
        if (applied !== config) { engine?.configure(config); applied = config }
        engine?.process(data)
        data.forEach { output.putShort((it * 32768f).toInt().coerceIn(-32768, 32767).toShort()) }
        output.flip()
    }
}
