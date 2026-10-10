package com.prisma.music

import org.junit.Assert.*
import org.junit.Test

class SongFadeTest {
    @Test fun disabledFadesLeaveVolumeAlone() {
        listOf(0L,500L,9000L,10000L).forEach {assertEquals(1f,songFadeGain(it,10000,0f,0f),0f)}
    }
    @Test fun fadesHaveCorrectBoundariesAndRecomputeAfterSeek() {
        assertEquals(0f,songFadeGain(0,10000,2f,3f),0f)
        assertEquals(.5f,songFadeGain(1000,10000,2f,3f),.001f)
        assertEquals(1f,songFadeGain(4000,10000,2f,3f),0f)
        assertEquals(.5f,songFadeGain(8500,10000,2f,3f),.001f)
        assertEquals(0f,songFadeGain(10000,10000,2f,3f),0f)
        assertEquals(1f,songFadeGain(5000,10000,2f,3f),0f)
    }
    @Test fun shortSongsFitBothFadesWithoutClipping() {
        assertEquals(1f,songFadeGain(1000,2000,12f,12f),.001f)
        for(position in 0L..2000L step 10) assertTrue(songFadeGain(position,2000,12f,12f) in 0f..1f)
    }
    @Test fun unknownDurationStillAllowsFadeInAndNeverInventsFadeOut() {
        assertEquals(.5f,songFadeGain(1000,-9223372036854775807,2f,12f),.001f)
        assertEquals(1f,songFadeGain(5000,0,2f,12f),0f)
    }
}
