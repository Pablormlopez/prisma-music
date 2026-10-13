package com.prisma.music

import android.graphics.drawable.Animatable
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import androidx.compose.ui.layout.ContentScale

internal val LocalAnimatedArtwork=staticCompositionLocalOf<String?> {null}
@Composable internal fun artworkActive():Boolean {
    val life=LocalLifecycleOwner.current
    var resumed by remember(life) {mutableStateOf(life.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
    DisposableEffect(life) {
        val observer=LifecycleEventObserver {_,_->resumed=life.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)}
        life.lifecycle.addObserver(observer);onDispose {life.lifecycle.removeObserver(observer)}
    }
    return resumed && LocalArtworkPlaying.current && LocalAdvanced.current.animatedCover && !LocalAdvanced.current.performance && LocalPersonalization.current.motion!=MotionStyle.NONE
}
@Composable internal fun coverMovement():Modifier {
    val active=artworkActive();val type=LocalAdvanced.current.coverMotion
    if(!active) return Modifier
    val transition=rememberInfiniteTransition(label="animatedArtwork")
    val phase by transition.animateFloat(0f,1f,infiniteRepeatable(tween(if(type=="Girar") 40000 else 16000,easing=LinearEasing),if(type=="Girar") RepeatMode.Restart else RepeatMode.Reverse),label="coverMotion")
    return Modifier.graphicsLayer {
        when(type) {
            "Deslizar"->{scaleX=1.06f;scaleY=1.06f;translationX=(phase-.5f)*size.width*.045f}
            "Girar"->{rotationZ=phase*360f;scaleX=.90f;scaleY=.90f}
            else->{scaleX=1.015f+phase*.035f;scaleY=scaleX}
        }
    }
}
@Composable internal fun ImportedArtwork(uri:String,modifier:Modifier) {
    val context=LocalContext.current
    val loader=remember(context) {ImageLoader.Builder(context).components {if(Build.VERSION.SDK_INT>=28)add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())}.build()}
    var drawable by remember(uri) {mutableStateOf<Animatable?>(null)}
    val active=artworkActive()
    DisposableEffect(drawable,active) {if(active)drawable?.start() else drawable?.stop();onDispose {drawable?.stop()}}
    DisposableEffect(loader) {onDispose {loader.shutdown()}}
    AsyncImage(model=uri,imageLoader=loader,contentDescription="Portada personalizada",contentScale=ContentScale.Crop,modifier=modifier,onSuccess={drawable=it.result.drawable as? Animatable})
}
