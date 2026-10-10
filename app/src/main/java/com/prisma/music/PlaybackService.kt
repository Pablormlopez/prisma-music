package com.prisma.music

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
class PlaybackService : MediaSessionService() {
    private var fadeIn=0f
    private var fadeOut=0f
    private val fadeCheck=object:Runnable {
        override fun run() {
            session?.player?.let {updateFade(it)}
            handler.postDelayed(this,if(session?.player?.isPlaying==true) 30 else 250)
        }
    }
    private fun updateFade(player:androidx.media3.common.Player) {
        player.volume=songFadeGain(player.currentPosition,player.duration,fadeIn,fadeOut)
    }
    private var session: MediaSession? = null
    private val eq = EqProcessor()
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val sleepCheck = object : Runnable {
        override fun run() {
            val prefs = Preferences(this@PlaybackService)
            if (prefs.sleepDeadline > 0 && System.currentTimeMillis() >= prefs.sleepDeadline) {
                session?.player?.pause()
                prefs.sleepDeadline = 0
            }
            handler.postDelayed(this, 1000)
        }
    }
    private fun applySettings(player: ExoPlayer) {
        val prefs = Preferences(this)
        player.setPlaybackSpeed(prefs.playbackSpeed)
        player.skipSilenceEnabled = prefs.skipSilence
        fadeIn=prefs.fadeInSeconds;fadeOut=prefs.fadeOutSeconds
        updateFade(player)
    }
    override fun onCreate() {
        super.onCreate()
        eq.settings = Preferences(this).eq
        val factory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: android.content.Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context).setEnableFloatOutput(false).setAudioProcessors(arrayOf(eq)).build()
        }
        val player = ExoPlayer.Builder(this, factory).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        applySettings(player)
        player.addListener(object:androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(mediaItem:androidx.media3.common.MediaItem?,reason:Int) {updateFade(player)}
            override fun onPositionDiscontinuity(oldPosition:androidx.media3.common.Player.PositionInfo,newPosition:androidx.media3.common.Player.PositionInfo,reason:Int) {updateFade(player)}
        })
        handler.post(sleepCheck)
        val activity = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, player).setSessionActivity(activity).setCallback(object : MediaSession.Callback {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(SessionCommand("prisma.eq",Bundle.EMPTY)).add(SessionCommand("prisma.settings",Bundle.EMPTY)).build()).build()
            }
            override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                if(command.customAction == "prisma.eq") { eq.settings = Preferences(this@PlaybackService).eq; return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS)) }
                if(command.customAction == "prisma.settings") { applySettings(player); return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS)) }
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
        }).build()
        handler.post(fadeCheck)
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() { handler.removeCallbacks(sleepCheck); handler.removeCallbacks(fadeCheck); session?.run { player.release(); release() }; session=null; super.onDestroy() }
    override fun onTaskRemoved(rootIntent: Intent?) { if(session?.player?.playWhenReady != true) stopSelf() }
}
