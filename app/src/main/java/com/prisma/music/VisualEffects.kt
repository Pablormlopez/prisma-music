@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.prisma.music

import android.graphics.Bitmap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** Small, precomputed blur: works on Android 8+, never processes artwork per frame. */
object FrostedArtwork {
    fun create(source: Bitmap): Bitmap {
        val small = Bitmap.createScaledBitmap(source, 64, 64, true)
        var pixels = IntArray(64 * 64)
        small.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        repeat(3) {
            val output = IntArray(pixels.size)
            for (y in 0 until 64) for (x in 0 until 64) {
                var red=0; var green=0; var blue=0; var count=0
                for (dy in -4..4) for (dx in -4..4) {
                    val color=pixels[(y+dy).coerceIn(0,63)*64+(x+dx).coerceIn(0,63)]
                    red += color shr 16 and 255; green += color shr 8 and 255; blue += color and 255; count++
                }
                output[y*64+x] = android.graphics.Color.rgb(red/count,green/count,blue/count)
            }
            pixels=output
        }
        return Bitmap.createBitmap(pixels,64,64,Bitmap.Config.ARGB_8888)
    }
}

@Composable fun GlassBackdrop(bitmap:Bitmap?,background:Color) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(background)
            drawRect(Brush.radialGradient(listOf(Color(0xff569cd9).copy(alpha=.30f),Color.Transparent),center=Offset(size.width*.12f,size.height*.22f),radius=size.width*1.15f))
            drawRect(Brush.radialGradient(listOf(Color(0xffbd8ddb).copy(alpha=.24f),Color.Transparent),center=Offset(size.width*.95f,size.height*.78f),radius=size.width*1.05f))
        }
        if(bitmap!=null && !LocalAdvanced.current.reduceBlur && !LocalAdvanced.current.performance) Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(background.copy(alpha=.62f),background.copy(alpha=.72f),background.copy(alpha=.60f)))))
    }
}

/** Highlights follow the shape, leaving foreground controls readable. */
fun Modifier.crystal() = liquidSurface()

@Composable fun ThemeTimeline(theme:VisualTheme,value:Float,end:Float,enabled:Boolean,onChange:(Float)->Unit,onFinished:()->Unit) {
    val colors=MaterialTheme.colorScheme
    val primary=colors.primary
    val inactive=colors.onSurface.copy(alpha=.22f)
    val interactions=remember {MutableInteractionSource()}
    val dragged by interactions.collectIsDraggedAsState()
    val pressed by interactions.collectIsPressedAsState()
    val pressure by liquidResponse(enabled && (dragged || pressed))
    Slider(interactionSource=interactions,value=value.coerceIn(0f,end),onValueChange=onChange,onValueChangeFinished=onFinished,valueRange=0f..end,enabled=enabled,
        modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).semantics {contentDescription="Línea de tiempo ${theme.title}"},
        thumb={
            val shape=if(theme==VisualTheme.NIGHT) RoundedCornerShape(3.dp) else CircleShape
            val width=if(theme==VisualTheme.MINIMAL) 6.dp else if(theme==VisualTheme.GLASS) 24.dp else 16.dp
            if(theme.hasAlbumAtmosphere()) Box(Modifier.size(8.dp,8.dp).clip(CircleShape).background(Color.White))
            else if(theme==VisualTheme.GLASS) Box(Modifier.size(32.dp,24.dp).graphicsLayer {scaleX=1f+pressure*.22f;scaleY=1f+pressure*.12f}.liquidSurface(16f,colors.surface.copy(alpha=.35f),pressure))
            else Box(Modifier.size(width,if(theme==VisualTheme.MINIMAL) 18.dp else 24.dp).clip(shape).background(if(theme==VisualTheme.GLASS) colors.surface.copy(alpha=.9f) else primary).border(1.dp,if(theme==VisualTheme.GLASS) primary.copy(alpha=.65f) else primary,shape))
        },
        track={state->Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val progress=((state.value-state.valueRange.start)/(state.valueRange.endInclusive-state.valueRange.start)).coerceIn(0f,1f)
            val mid=size.height/2
            if(theme.hasAlbumAtmosphere()) {
                drawLine(inactive,Offset(0f,mid),Offset(size.width,mid),3.dp.toPx(),StrokeCap.Round)
                drawLine(Color.White,Offset(0f,mid),Offset(size.width*progress,mid),3.dp.toPx(),StrokeCap.Round)
            } else when(theme) {
                VisualTheme.CHROMATIC->{
                    val path=Path()
                    for(i in 0..160) {val x=size.width*i/160f;val y=mid+sin(i/160f*8*Math.PI).toFloat()*4.dp.toPx();if(i==0) path.moveTo(x,y) else path.lineTo(x,y)}
                    drawPath(path,inactive,style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
                    clipRect(right=size.width*progress) {drawPath(path,Brush.horizontalGradient(listOf(primary,Color(0xffe990b7),primary)),style=Stroke(4.dp.toPx(),cap=StrokeCap.Round))}
                }
                VisualTheme.GLASS->{
                    val h=12.dp.toPx();val r=CornerRadius(h/2)
                    drawRoundRect(inactive,Offset(0f,mid-h/2),Size(size.width,h),r)
                    drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha=.9f),primary.copy(alpha=.7f),primary)),Offset(0f,mid-h/2),Size(size.width*progress,h),r)
                    drawLine(Color.White.copy(alpha=.55f),Offset(h/2,mid-h/2+2.dp.toPx()),Offset((size.width-h/2).coerceAtLeast(h/2),mid-h/2+2.dp.toPx()),1.dp.toPx(),StrokeCap.Round)
                }
                VisualTheme.MINIMAL->{
                    drawLine(inactive,Offset(0f,mid),Offset(size.width,mid),1.dp.toPx())
                    drawLine(primary,Offset(0f,mid),Offset(size.width*progress,mid),2.dp.toPx())
                }
                VisualTheme.STUDIO->{
                    val step=size.width/48
                    for(i in 0 until 48) {
                        val h=(5f+15f*kotlin.math.abs(sin(i*1.7f)))*density
                        drawRoundRect(if((i+.5f)/48<=progress) primary else inactive,Offset(i*step,mid-h/2),Size((step-2.dp.toPx()).coerceAtLeast(1f),h),CornerRadius(2.dp.toPx()))
                    }
                }
                VisualTheme.SUNSET->{
                    drawLine(inactive,Offset(0f,mid),Offset(size.width,mid),5.dp.toPx(),StrokeCap.Round)
                    drawLine(Brush.horizontalGradient(listOf(Color(0xffeaa070),primary)),Offset(0f,mid),Offset(size.width*progress,mid),7.dp.toPx(),StrokeCap.Round)
                    for(i in 1..9) drawCircle(colors.surface.copy(alpha=.65f),1.5.dp.toPx(),Offset(size.width*i/10,mid))
                }
                VisualTheme.NIGHT->{
                    val step=size.width/40
                    for(i in 0 until 40) drawRoundRect(if((i+.5f)/40<=progress) primary else inactive,Offset(i*step,mid-3.dp.toPx()),Size((step-2.dp.toPx()).coerceAtLeast(1f),6.dp.toPx()),CornerRadius(1.dp.toPx()))
                }
                VisualTheme.MONOCHROME->{
                    drawLine(inactive,Offset(0f,mid),Offset(size.width,mid),2.dp.toPx(),StrokeCap.Round)
                    drawLine(primary,Offset(0f,mid),Offset(size.width*progress,mid),2.dp.toPx(),StrokeCap.Round)
                }
            }
        }}
    )
}
