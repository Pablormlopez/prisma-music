package com.prisma.music

import android.content.SharedPreferences
import androidx.compose.runtime.*

internal data class AdvancedSettings(
    val normalize:Boolean=false,val targetDb:Float=-18f,val crossfade:Float=0f,
    val stereoWidth:Float=1f,val spatial:Boolean=false,val floatOutput:Boolean=true,
    val refreshHz:Float=0f,val performance:Boolean=false,val reduceBlur:Boolean=false,
    val animatedCover:Boolean=false,val coverMotion:String="Respirar",val fullCover:Boolean=true,
    val meshGradient:Boolean=false,val syncLyrics:Boolean=true,val blurLyrics:Boolean=false,
    val showVolume:Boolean=true,val stopOnDismiss:Boolean=false,val artworkRevision:Int=0,val immersiveAllThemes:Boolean=true,
    val lyricSize:Float=26f,val centerLyrics:Boolean=false,val swipeTracks:Boolean=true,
    val pauseNotifications:Boolean=true,val pauseRecording:Boolean=true,val resumeInterruptions:Boolean=false
)
internal val LocalAdvanced=staticCompositionLocalOf {AdvancedSettings()}
internal val LocalArtworkPlaying=staticCompositionLocalOf {false}
@Composable internal fun rememberAdvanced(prefs:Preferences):AdvancedSettings {
    var value by remember(prefs) {mutableStateOf(prefs.advanced)}
    DisposableEffect(prefs) {
        val listener=SharedPreferences.OnSharedPreferenceChangeListener {_,_->value=prefs.advanced}
        prefs.listen(listener)
        onDispose {prefs.unlisten(listener)}
    }
    return value
}
