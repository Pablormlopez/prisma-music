package com.prisma.music

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/** Linked stereo RMS normalization and stereo width; does not claim surround decoding. */
internal class ListeningDsp(private val rate:Int,private val channels:Int) {
    var normalize=false
    var targetDb=-18f
    var width=1f
    private var energy=0.0
    private var gain=1.0
    private var limiter=1.0
    fun process(samples:FloatArray) {
        if(!normalize && width==1f) return
        val envelope=exp(-1.0/(rate*.4))
        val release=exp(-1.0/(rate*.15))
        for(frame in 0..samples.size-channels step channels) {
            var power=0.0
            for(c in 0 until channels) power+=samples[frame+c].toDouble().pow(2)
            energy=energy*envelope+(power/channels)*(1-envelope)
            val rms=sqrt(energy)
            val desired=if(normalize && rms>.003) (10.0.pow(targetDb.coerceIn(-24f,-14f)/20.0)/rms).coerceIn(.25,4.0) else 1.0
            val smoothing=exp(-1.0/(rate*if(desired<gain) .15 else 2.0))
            gain=desired+(gain-desired)*smoothing
            if(channels==2 && width!=1f) {
                val mid=(samples[frame]+samples[frame+1])*.5f
                val side=(samples[frame]-samples[frame+1])*.5f*width.coerceIn(0f,1.8f)
                samples[frame]=mid+side;samples[frame+1]=mid-side
            }
            var peak=0.0
            for(c in 0 until channels) {val v=(samples[frame+c]*gain).takeIf {it.isFinite()} ?: 0.0;samples[frame+c]=v.toFloat();peak=max(peak,abs(v))}
            val limit=if(peak>.966) .966/peak else 1.0
            limiter=if(limit<limiter) limit else limit+(limiter-limit)*release
            for(c in 0 until channels) samples[frame+c]=(samples[frame+c]*limiter).toFloat().coerceIn(-1f,1f)
        }
    }
}
internal fun mixGains(progress:Float):Pair<Float,Float> {
    val t=progress.coerceIn(0f,1f)*PI/2
    val a=cos(t).toFloat();val b=sin(t).toFloat();val ceiling=max(1f,a+b)
    return a/ceiling to b/ceiling
}
/** Decoder PCM to floating point; keeps 24-bit samples instead of quantizing them to 16 bits. */
internal object PcmSamples {
    fun decode(input:ByteBuffer,bits:Int,isFloat:Boolean=false):FloatArray {
        val b=input.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val bytes=bits/8
        require(bytes in 1..4 && b.remaining()%bytes==0)
        return FloatArray(b.remaining()/bytes) {
            if(isFloat) b.float.let {v->if(v.isFinite()) v else 0f}
            else when(bits) {
                8->((b.get().toInt() and 255)-128)/128f
                16->b.short/32768f
                24->{val v=(b.get().toInt() and 255) or ((b.get().toInt() and 255) shl 8) or (b.get().toInt() shl 16);v/8388608f}
                else->b.int/2147483648f
            }
        }
    }
}
