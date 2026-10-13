package com.prisma.music

import org.junit.Assert.*
import org.junit.Test

class InterruptionPolicyTest {
    @Test fun transientResumesOnlyAfterRecordingAndFocusBothEnd() {
        val p=InterruptionPolicy()
        p.recording(true,true,true);p.interrupt(false,true,true)
        assertFalse(p.gain());assertTrue(p.resumePending)
        p.recording(false,false,true);assertTrue(p.takeResume());assertFalse(p.takeResume())
    }
    @Test fun recordingEndsFirstButDoesNotOverrideAnotherAppsFocus() {
        val p=InterruptionPolicy()
        p.interrupt(true,true,true);p.recording(true,false,true)
        p.recording(false,false,true);assertFalse(p.takeResume());assertTrue(p.gain())
    }
    @Test fun manualPauseTimerOrHeadphoneRemovalCancelsResume() {
        val p=InterruptionPolicy()
        p.interrupt(true,true,true);p.stop();assertFalse(p.gain())
        p.recording(true,true,true);p.stop();p.recording(false,false,true);assertFalse(p.takeResume())
    }
    @Test fun permanentLossNeverResumesAndAllowsLaterExplicitPlay() {
        val p=InterruptionPolicy()
        p.interrupt(true,true,true);p.interrupt(false,false,true)
        assertFalse(p.gain());assertFalse(p.focusBlocked)
    }
    @Test fun defaultDisabledResumeAndAlreadyPausedStaySilent() {
        val p=InterruptionPolicy()
        p.interrupt(true,true,false);assertFalse(p.gain())
        p.recording(true,false,true);p.recording(false,false,true);assertFalse(p.takeResume())
    }
    @Test fun duckClearsOnGainOrExplicitStop() {
        val p=InterruptionPolicy()
        p.duck();assertTrue(p.ducked);assertFalse(p.gain());assertFalse(p.ducked)
        p.duck();p.stop();assertFalse(p.ducked)
    }
}
