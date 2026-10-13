package com.prisma.music

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class PlaybackService:MediaSessionService() {
    private var session:MediaSession?=null
    private var primary:ExoPlayer?=null
    private var incoming:ExoPlayer?=null
    private var mix:AutoMix?=null
    @Volatile private var options=AdvancedSettings()
    @Volatile private var eqConfig=DspEngine.Config.flat()
    private var fadeIn=0f;private var fadeOut=0f
    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private val tick=object:Runnable {override fun run() {
        primary?.let {p->
            val base=if(options.crossfade>0) 1f else songFadeGain(p.currentPosition,p.duration,fadeIn,fadeOut)
            p.volume=base*(mix?.tick() ?: 1f)
        }
        handler.postDelayed(this,if(primary?.isPlaying==true) 30 else 250)
    }}
    private val sleepCheck=object:Runnable {override fun run() {
        val prefs=Preferences(this@PlaybackService)
        if(prefs.sleepDeadline>0 && System.currentTimeMillis()>=prefs.sleepDeadline) {primary?.pause();mix?.cancel();prefs.sleepDeadline=0}
        handler.postDelayed(this,1000)
    }}
    private fun createPlayer(focus:Boolean):ExoPlayer {
        val processor=EqProcessor()
        val factory=object:DefaultRenderersFactory(this) {
            override fun buildAudioSink(context:android.content.Context,enableFloatOutput:Boolean,enableAudioTrackPlaybackParams:Boolean):AudioSink {
                if(options.floatOutput) {
                    val sink=DefaultAudioSink.Builder(context).setEnableFloatOutput(true).setEnableAudioTrackPlaybackParams(true).build()
                    return PrecisionAudioSink(sink,{options},{eqConfig})
                }
                processor.settings=eqConfig
                return DefaultAudioSink.Builder(context).setEnableFloatOutput(false).setAudioProcessors(arrayOf(processor,ListeningProcessor {options})).build()
            }
        }
        val p=ExoPlayer.Builder(this,factory).build()
        p.setAudioAttributes(attributes(),focus)
        p.setHandleAudioBecomingNoisy(true)
        p.setWakeMode(C.WAKE_MODE_LOCAL)
        if(focus) p.addListener(object:Player.Listener {
            override fun onMediaItemTransition(mediaItem:androidx.media3.common.MediaItem?,reason:Int) {
                if(reason==Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) mix?.handoff(mediaItem) else mix?.cancel()
            }
            override fun onPositionDiscontinuity(oldPosition:Player.PositionInfo,newPosition:Player.PositionInfo,reason:Int) {if(reason==Player.DISCONTINUITY_REASON_SEEK) mix?.cancel()}
            override fun onTimelineChanged(timeline:androidx.media3.common.Timeline,reason:Int) {if(reason==Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)mix?.cancel()}
            override fun onIsPlayingChanged(isPlaying:Boolean) {if(!isPlaying)mix?.cancel()}
        }) else p.addListener(object:Player.Listener {override fun onPlayerError(error:androidx.media3.common.PlaybackException) {mix?.cancel()}})
        // The compatible chain receives live EQ updates through the service command.
        compatibleProcessors.add(processor)
        return p
    }
    private val compatibleProcessors=mutableListOf<EqProcessor>()
    private fun attributes()=AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setSpatializationBehavior(if(options.spatial) C.SPATIALIZATION_BEHAVIOR_AUTO else C.SPATIALIZATION_BEHAVIOR_NEVER).build()
    private fun readSettings() {
        val prefs=Preferences(this)
        options=prefs.advanced;eqConfig=prefs.eq;fadeIn=prefs.fadeInSeconds;fadeOut=prefs.fadeOutSeconds
        compatibleProcessors.forEach {it.settings=eqConfig}
        primary?.apply {setPlaybackSpeed(prefs.playbackSpeed);skipSilenceEnabled=prefs.skipSilence && !options.floatOutput;setAudioAttributes(attributes(),true)}
        incoming?.apply {setPlaybackSpeed(prefs.playbackSpeed);skipSilenceEnabled=false;setAudioAttributes(attributes(),false)}
        mix?.seconds=options.crossfade
    }
    private fun rebuildOutput() {
        val old=primary ?: return
        mix?.cancel()
        val items=(0 until old.mediaItemCount).map {old.getMediaItemAt(it)}
        val index=old.currentMediaItemIndex.coerceAtLeast(0);val position=old.currentPosition
        val play=old.playWhenReady;val repeat=old.repeatMode;val shuffle=old.shuffleModeEnabled
        val ordered=mutableListOf<Int>();val timeline=old.currentTimeline
        var i=timeline.getFirstWindowIndex(true)
        while(i!=C.INDEX_UNSET && ordered.size<timeline.windowCount) {ordered+=i;i=timeline.getNextWindowIndex(i,Player.REPEAT_MODE_OFF,true)}
        compatibleProcessors.clear()
        val next=createPlayer(true);val second=createPlayer(false)
        if(items.isNotEmpty()) next.setMediaItems(items,index,position)
        if(ordered.isNotEmpty()) next.setShuffleOrder(androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder(ordered.toIntArray(),0L))
        next.repeatMode=repeat;next.shuffleModeEnabled=shuffle
        primary=next;incoming?.release();incoming=second
        mix=AutoMix(next,second)
        readSettings()
        session?.setPlayer(next)
        old.release()
        if(items.isNotEmpty()) {next.prepare();next.playWhenReady=play}
    }
    override fun onCreate() {
        super.onCreate();readSettings()
        val p=createPlayer(true);val second=createPlayer(false)
        primary=p;incoming=second;mix=AutoMix(p,second)
        readSettings()
        val activity=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session=MediaSession.Builder(this,p).setSessionActivity(activity).setCallback(object:MediaSession.Callback {
            override fun onConnect(session:MediaSession,controller:MediaSession.ControllerInfo)=MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(SessionCommand("prisma.eq",Bundle.EMPTY)).add(SessionCommand("prisma.settings",Bundle.EMPTY)).build()).build()
            override fun onCustomCommand(session:MediaSession,controller:MediaSession.ControllerInfo,command:SessionCommand,args:Bundle):ListenableFuture<SessionResult> {
                if(command.customAction=="prisma.eq") {eqConfig=Preferences(this@PlaybackService).eq;compatibleProcessors.forEach {it.settings=eqConfig};return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))}
                if(command.customAction=="prisma.settings") {
                    val wasFloat=options.floatOutput;mix?.cancel();readSettings()
                    if(wasFloat!=options.floatOutput)rebuildOutput()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
        }).build()
        handler.post(tick);handler.post(sleepCheck)
    }
    override fun onGetSession(controllerInfo:MediaSession.ControllerInfo)=session
    override fun onDestroy() {handler.removeCallbacksAndMessages(null);mix?.cancel();incoming?.release();session?.release();primary?.release();session=null;primary=null;incoming=null;super.onDestroy()}
    override fun onTaskRemoved(rootIntent:Intent?) {if(options.stopOnDismiss || primary?.playWhenReady!=true) {primary?.pause();mix?.cancel();stopSelf()}}
}
