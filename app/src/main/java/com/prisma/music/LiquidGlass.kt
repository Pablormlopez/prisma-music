package com.prisma.music

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

@Stable class GlassScene(val layer:GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}
val LocalGlassScene=staticCompositionLocalOf<GlassScene?> {null}
val LocalCollectionBottomPadding=staticCompositionLocalOf {24.dp}

fun Modifier.captureGlassScene(scene:GlassScene)=composed {
    val advanced=LocalAdvanced.current
    if(advanced.performance || advanced.reduceBlur) this else this.onGloballyPositioned {scene.origin=it.positionInRoot()}.drawWithContent {
        scene.layer.record {this@drawWithContent.drawContent()}
        drawLayer(scene.layer)
    }
}

/** Sample only the content layer, never the glass itself, avoiding recursive captures. */
fun Modifier.sampleGlassBackdrop(radius:Float)=composed {
    val scene=LocalGlassScene.current
    val advanced=LocalAdvanced.current
    val sample=rememberGraphicsLayer()
    var origin by remember {mutableStateOf(Offset.Zero)}
    val blur=with(LocalDensity.current) {7.dp.toPx()}
    val backdrop=MaterialTheme.colorScheme.background
    if(scene==null) this else this.onGloballyPositioned {origin=it.positionInRoot()}.clip(RoundedCornerShape(radius.dp)).drawWithContent {
        if(android.os.Build.VERSION.SDK_INT>=31 && !advanced.reduceBlur && !advanced.performance) {
            sample.renderEffect=BlurEffect(blur,blur,TileMode.Clamp)
            sample.record {
                // Composite an opaque scene before blurring, so original sharp text
                // cannot bleed through transparent pixels of the blurred sample.
                drawRect(backdrop)
                translate(scene.origin.x-origin.x,scene.origin.y-origin.y) {drawLayer(scene.layer)}
            }
            drawLayer(sample)
        } else {
            // Older Android versions keep the translucent tint without duplicating text.
            drawRect(backdrop.copy(alpha=.92f))
        }
        drawContent()
    }
}

/** Static optics, animated geometry: no CPU bitmap allocations during a gesture. */
fun Modifier.liquidSurface(radius:Float=28f, tint:Color=Color.Transparent, pressure:Float=0f):Modifier =
    clip(RoundedCornerShape(radius.dp)).drawWithCache {
        val corner=androidx.compose.ui.geometry.CornerRadius(radius.dp.toPx())
        val thickness=1.dp.toPx()
        val sheen=Brush.linearGradient(listOf(Color.White.copy(alpha=.16f+pressure*.08f),Color.White.copy(alpha=.025f),Color.White.copy(alpha=.075f)),Offset.Zero,Offset(size.width,size.height))
        val rim=Brush.linearGradient(listOf(Color.White.copy(alpha=.72f),Color.White.copy(alpha=.10f),Color.White.copy(alpha=.04f),Color.White.copy(alpha=.42f)),Offset.Zero,Offset(size.width,size.height))
        onDrawWithContent {
            drawRoundRect(tint,cornerRadius=corner)
            drawRoundRect(sheen,cornerRadius=corner)
            drawContent()
            drawRoundRect(rim,topLeft=Offset(thickness/2,thickness/2),size=androidx.compose.ui.geometry.Size((size.width-thickness).coerceAtLeast(0f),(size.height-thickness).coerceAtLeast(0f)),cornerRadius=corner,style=Stroke(thickness))
            // A second inset reflection gives the edge optical thickness without a solid outline.
            drawRoundRect(Color.White.copy(alpha=.08f),topLeft=Offset(2*thickness,2*thickness),size=androidx.compose.ui.geometry.Size((size.width-4*thickness).coerceAtLeast(0f),(size.height-4*thickness).coerceAtLeast(0f)),cornerRadius=corner,style=Stroke(thickness/2))
        }
    }

@Composable internal fun liquidResponse(pressed:Boolean):State<Float> = animateFloatAsState(
    if(pressed) 1f else 0f,
    animationSpec=if(LocalPersonalization.current.motion==MotionStyle.NONE) snap() else spring(dampingRatio=.66f,stiffness=420f),label="glassPressure")

@Composable fun PrismaIconButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable ()->Unit) {
    if(LocalVisualTheme.current!=VisualTheme.GLASS) {IconButton(onClick,modifier,enabled,content=content);return}
    val interactions=remember {MutableInteractionSource()}
    val pressed by interactions.collectIsPressedAsState()
    val pressure by liquidResponse(pressed && enabled)
    IconButton(onClick=onClick,enabled=enabled,interactionSource=interactions,
        modifier=modifier.graphicsLayer {scaleX=1f+pressure*.10f;scaleY=1f-pressure*.06f}.liquidSurface(50f,MaterialTheme.colorScheme.surface.copy(alpha=.12f),pressure),content=content)
}

@Composable fun PrismaPlayButton(onClick:()->Unit,modifier:Modifier=Modifier,shape:Shape=CircleShape,content:@Composable ()->Unit) {
    if(LocalVisualTheme.current!=VisualTheme.GLASS) {FilledIconButton(onClick,modifier,shape=shape,content=content);return}
    val interactions=remember {MutableInteractionSource()}
    val pressed by interactions.collectIsPressedAsState()
    val pressure by liquidResponse(pressed)
    IconButton(onClick=onClick,interactionSource=interactions,modifier=modifier.graphicsLayer {scaleX=1f+pressure*.12f;scaleY=1f-pressure*.07f}.liquidSurface(50f,MaterialTheme.colorScheme.primary.copy(alpha=.22f),pressure),content=content)
}

internal fun glassRouteIndex(page:String)=when(page) {"Artista"->1;"Temas","Personalización"->4;else->mainPages.indexOf(page).coerceAtLeast(0)}
internal fun glassDragIndex(position:Float) = position.roundToInt().coerceIn(0,mainPages.lastIndex)

/** Independent leading/trailing springs create the liquid stretch seen in the reference. */
@Composable fun LiquidNavigation(page:String,onPage:(String)->Unit) {
    val colors=MaterialTheme.colorScheme
    val personal=LocalPersonalization.current
    val sources=remember {List(5) {MutableInteractionSource()}}
    val pressed=sources.map {it.collectIsPressedAsState().value}
    val active=glassRouteIndex(page)
    var drag by remember {mutableStateOf<Float?>(null)}
    val latestOnPage by rememberUpdatedState(onPage)
    val target=drag ?: pressed.indexOfFirst {it}.takeIf {it>=0}?.toFloat() ?: active.toFloat()
    val moving=drag!=null || pressed.any {it}
    val pressure by liquidResponse(moving)
    val left by animateFloatAsState(target,if(personal.motion==MotionStyle.NONE) snap() else spring(.78f,380f),label="glassLeadingEdge")
    val right by animateFloatAsState(target+1f,if(personal.motion==MotionStyle.NONE) snap() else spring(.68f,560f),label="glassTrailingEdge")
    val icons=listOf(Icons.Rounded.Home,Icons.Rounded.LibraryMusic,Icons.Rounded.QueueMusic,Icons.Rounded.Equalizer,Icons.Rounded.Tune)
    BoxWithConstraints(Modifier.navigationBarsPadding().padding(horizontal=14.dp,vertical=8.dp).fillMaxWidth().sampleGlassBackdrop(38f).liquidSurface(38f,colors.surface.copy(alpha=.22f)).padding(6.dp)) {
        val slot=maxWidth/5
        val slotPx=with(LocalDensity.current) {slot.toPx()}
        Box(Modifier.offset(x=slot*left.coerceIn(0f,4f)).width((slot*(right-left).coerceIn(.85f,1.65f)).coerceAtMost(maxWidth-slot*left.coerceIn(0f,4f))).height(58.dp)
            .graphicsLayer {scaleY=1f+pressure*.09f}.liquidSurface(30f,colors.surface.copy(alpha=.28f),pressure))
        Row(Modifier.fillMaxWidth().semantics {contentDescription="Navegación Liquid Glass"}.pointerInput(slotPx) {
            detectHorizontalDragGestures(onDragStart={drag=(it.x/slotPx-.5f).coerceIn(0f,4f)},onDragCancel={drag=null},onDragEnd={
                drag?.let {latestOnPage(mainPages[glassDragIndex(it)])};drag=null
            }) {change,amount->change.consume();drag=((drag ?: active.toFloat())+amount/slotPx).coerceIn(0f,4f)}
        },verticalAlignment=Alignment.CenterVertically) {
            mainPages.forEachIndexed {index,route->
                val selected=active==index
                val proximity=(1f-abs(target-index)).coerceIn(0f,1f)
                Column(Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(30.dp)).semantics {this.selected=selected}
                    .clickable(interactionSource=sources[index],indication=null,role=Role.Tab) {onPage(route)},verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(icons[index],null,Modifier.size(25.dp).graphicsLayer {scaleX=1f+pressure*.16f*proximity;scaleY=scaleX},tint=if(selected) colors.primary else colors.onSurfaceVariant)
                    Text(route,fontSize=10.sp,fontWeight=if(selected) FontWeight.Bold else FontWeight.Medium,color=if(selected) colors.primary else colors.onSurfaceVariant,maxLines=1)
                }
            }
        }
    }
}

@Composable fun PrismaButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,shape:Shape=ButtonDefaults.shape,content:@Composable RowScope.()->Unit) {
    if(LocalVisualTheme.current!=VisualTheme.GLASS) {Button(onClick,modifier,enabled,shape=shape,content=content);return}
    GlassAction(onClick,modifier,enabled,shape,true,content)
}
@Composable fun PrismaOutlinedButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,shape:Shape=ButtonDefaults.outlinedShape,content:@Composable RowScope.()->Unit) {
    if(LocalVisualTheme.current!=VisualTheme.GLASS) {OutlinedButton(onClick,modifier,enabled,shape=shape,content=content);return}
    GlassAction(onClick,modifier,enabled,shape,false,content)
}
@Composable fun PrismaTextButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit) {
    if(LocalVisualTheme.current!=VisualTheme.GLASS) {TextButton(onClick,modifier,enabled,content=content);return}
    GlassAction(onClick,modifier,enabled,ButtonDefaults.textShape,false,content)
}
@Composable private fun GlassAction(onClick:()->Unit,modifier:Modifier,enabled:Boolean,shape:Shape,emphasized:Boolean,content:@Composable RowScope.()->Unit) {
    val interactions=remember {MutableInteractionSource()}
    val pressed by interactions.collectIsPressedAsState()
    val pressure by liquidResponse(pressed && enabled)
    val colors=MaterialTheme.colorScheme
    TextButton(onClick=onClick,enabled=enabled,shape=shape,interactionSource=interactions,
        modifier=modifier.graphicsLayer {scaleX=1f+pressure*.035f;scaleY=1f-pressure*.05f}.liquidSurface(28f,(if(emphasized) colors.primary else colors.surface).copy(alpha=if(emphasized) .18f else .10f),pressure),content=content)
}
@Composable fun PrismaFilterChip(selected:Boolean,onClick:()->Unit,label:@Composable ()->Unit,modifier:Modifier=Modifier,leadingIcon:(@Composable ()->Unit)?=null) {
    if(LocalVisualTheme.current!=VisualTheme.GLASS) {FilterChip(selected,onClick,label,modifier=modifier,leadingIcon=leadingIcon);return}
    val interactions=remember {MutableInteractionSource()}
    val pressed by interactions.collectIsPressedAsState()
    val pressure by liquidResponse(pressed)
    val colors=MaterialTheme.colorScheme
    FilterChip(selected,onClick,label,leadingIcon=leadingIcon,interactionSource=interactions,shape=RoundedCornerShape(20.dp),border=null,
        colors=FilterChipDefaults.filterChipColors(containerColor=Color.Transparent,selectedContainerColor=Color.Transparent),
        modifier=modifier.graphicsLayer {scaleX=1f+pressure*.05f;scaleY=1f-pressure*.04f}.liquidSurface(20f,(if(selected) colors.primary else colors.surface).copy(alpha=if(selected) .18f else .08f),pressure))
}
