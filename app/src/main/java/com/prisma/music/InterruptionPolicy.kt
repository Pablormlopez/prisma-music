package com.prisma.music

/** Intent to resume survives temporary interruptions, but never an explicit stop. */
internal class InterruptionPolicy {
    var recording = false; private set
    var focusBlocked = false; private set
    var resumePending = false; private set
    var ducked = false; private set
    fun interrupt(wasPlaying: Boolean, temporary: Boolean, resume: Boolean) {
        resumePending = temporary && resume && (wasPlaying || resumePending)
        focusBlocked = temporary
        ducked = false
    }
    fun recording(active: Boolean, wasPlaying: Boolean, resume: Boolean) {
        if(active && !recording) resumePending = resume && (wasPlaying || resumePending)
        recording = active
    }
    fun gain(): Boolean {
        focusBlocked = false; ducked = false
        return takeResume()
    }
    fun duck() { ducked = true }
    fun takeResume(): Boolean {
        if(recording || focusBlocked || !resumePending) return false
        resumePending = false
        return true
    }
    fun stop() { resumePending = false; ducked = false }
}
