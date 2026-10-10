package com.prisma.music

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import org.json.JSONArray
import org.json.JSONObject

/** Volume envelope based on media position: pauses freeze it, seeks recompute it. */
internal fun songFadeGain(positionMs:Long,durationMs:Long,fadeInSeconds:Float,fadeOutSeconds:Float):Float {
    val position=positionMs.coerceAtLeast(0).toFloat()
    val duration=durationMs.takeIf {it>0}?.toFloat()
    val enter=fadeInSeconds.coerceIn(0f,12f)*1000f
    val exit=fadeOutSeconds.coerceIn(0f,12f)*1000f
    // Fit overlapping fades to short songs so a full-volume point remains possible.
    val scale=if(duration!=null && enter+exit>duration) duration/(enter+exit) else 1f
    val start=if(enter>0) (position/(enter*scale)).coerceIn(0f,1f) else 1f
    val end=if(exit>0 && duration!=null) ((duration-position)/(exit*scale)).coerceIn(0f,1f) else 1f
    return minOf(start,end)
}

enum class MotionStyle(val title:String,val description:String) {
    SOFT("Fundido suave","Cambios progresivos sin movimiento lateral"),
    SLIDE("Deslizamiento","Las pantallas y portadas se deslizan suavemente"),
    ZOOM("Zoom delicado","Un acercamiento pequeño acompañado de fundido"),
    NONE("Sin animaciones","Cambios inmediatos y gestos sin efectos")
}
fun contentMotion(style:MotionStyle,duration:Int,direction:Int=1):ContentTransform {
    val ms=duration.coerceIn(250,900)
    val enter=when(style) {
        MotionStyle.SOFT->fadeIn(tween(ms,easing=FastOutSlowInEasing))
        MotionStyle.SLIDE->slideInHorizontally(tween(ms,easing=FastOutSlowInEasing)) {it*direction/5}+fadeIn(tween(ms))
        MotionStyle.ZOOM->scaleIn(tween(ms,easing=FastOutSlowInEasing),initialScale=.96f)+fadeIn(tween(ms))
        MotionStyle.NONE->fadeIn(tween(0))
    }
    val exit=when(style) {
        MotionStyle.SLIDE->slideOutHorizontally(tween(ms,easing=FastOutSlowInEasing)) {-it*direction/5}+fadeOut(tween(ms))
        MotionStyle.ZOOM->scaleOut(tween(ms,easing=FastOutSlowInEasing),targetScale=1.025f)+fadeOut(tween(ms))
        MotionStyle.SOFT->fadeOut(tween(ms,easing=FastOutSlowInEasing))
        MotionStyle.NONE->fadeOut(tween(0))
    }
    return enter togetherWith exit
}
internal fun encodeEq(v:DspEngine.Config):JSONObject=JSONObject().put("enabled",v.enabled).put("gains",JSONArray(v.gains.toList())).put("q",v.q).put("preamp",v.preamp).put("bass",v.bass).put("mids",v.mids).put("treble",v.treble).put("voice",v.voice)
internal fun decodeEq(j:JSONObject):DspEngine.Config {
    val gains=j.optJSONArray("gains")
    return DspEngine.Config(j.optBoolean("enabled"),DoubleArray(10) {gains?.optDouble(it,0.0)?.takeIf {v->v.isFinite()}?.coerceIn(-12.0,12.0) ?: 0.0},j.optDouble("q",1.0),j.optDouble("preamp",0.0),j.optDouble("bass",0.0),j.optDouble("mids",0.0),j.optDouble("treble",0.0),j.optDouble("voice",0.0))
}
val builtInEqPresets:Map<String,DspEngine.Config> = linkedMapOf(
    "Plano" to DoubleArray(10),
    "Graves" to doubleArrayOf(4.0,4.0,3.0,1.0,0.0,0.0,0.0,0.0,1.0,1.0),
    "Voces" to doubleArrayOf(-2.0,-1.0,0.0,0.0,1.0,2.0,3.0,2.0,0.0,0.0),
    "Suave" to doubleArrayOf(1.0,1.0,0.0,0.0,-1.0,-1.0,-2.0,-2.0,-1.0,-1.0)
).mapValues {(name,gains)->DspEngine.Config(true,gains,1.0,if(name=="Plano") 0.0 else -4.0,0.0,0.0,0.0,0.0)}
