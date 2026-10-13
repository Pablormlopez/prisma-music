package com.prisma.music

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.Looper

/** Sole focus owner for both decoders; never captures audio or reads notifications. */
internal class AudioInterruptions(
    context: Context,
    private val options: () -> AdvancedSettings,
    private val playing: () -> Boolean,
    private val pause: () -> Unit,
    private val resume: () -> Unit
) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    internal val policy = InterruptionPolicy()
    private var held = false
    private var registered = false
    private var internalChange = false
    val gain: Float get() = if(policy.ducked) .2f else 1f
    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        when(change) {
            AudioManager.AUDIOFOCUS_GAIN -> { held = true; if(policy.gain()) resume() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if(options().pauseNotifications) lose(true) else policy.duck()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> lose(true)
            AudioManager.AUDIOFOCUS_LOSS -> { held = false; lose(false) }
        }
    }
    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        // Always receive transient duck callbacks so the preference applies to both players.
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener(listener, handler).build()
    private val recordings = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
            updateRecording(configs.isNotEmpty())
        }
    }
    fun start() {
        registered = runCatching { manager.registerAudioRecordingCallback(recordings, handler); true }.getOrDefault(false)
        refreshRecording()
    }
    private fun refreshRecording() {
        runCatching { updateRecording(manager.activeRecordingConfigurations.isNotEmpty()) }
    }
    private fun updateRecording(active: Boolean) {
        val blocked = active && options().pauseRecording
        policy.recording(blocked, playing(), options().resumeInterruptions)
        if(blocked) pauseInternally()
        else if(policy.takeResume()) resume()
    }
    private fun pauseInternally() {
        internalChange = true
        try { pause() } finally { internalChange = false }
    }
    private fun lose(temporary: Boolean) {
        policy.interrupt(playing(), temporary, options().resumeInterruptions)
        pauseInternally()
    }
    /** Called whenever the primary changes playback intent, including noisy output events. */
    fun playbackIntent(play: Boolean) {
        if(internalChange) return
        if(!play) { stop(); return }
        refreshRecording()
        if(policy.recording || policy.focusBlocked) { pauseInternally(); return }
        if(!held) {
            held = manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if(!held) { policy.stop(); pauseInternally() }
        }
    }
    fun explicitCommand() { policy.stop() }
    fun settingsChanged() {
        if(!options().resumeInterruptions) policy.stop()
        refreshRecording()
    }
    fun stop() {
        policy.stop()
        // Retain a temporary focus request until GAIN to permit the opted-in resume.
        if(!policy.focusBlocked && held) { manager.abandonAudioFocusRequest(request); held = false }
    }
    fun close() {
        if(registered) manager.unregisterAudioRecordingCallback(recordings)
        manager.abandonAudioFocusRequest(request); held = false; policy.stop()
    }
}
