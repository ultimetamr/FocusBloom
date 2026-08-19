package com.pico.swan.focusbloom.domain

import com.pico.swan.focusbloom.domain.model.TimerStatus
import com.pico.swan.focusbloom.domain.usecase.FocusTimer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FocusTimerTest {
    @Test
    fun pauseFreezesTimeUntilResume() {
        val started = FocusTimer.start(300_000L, 1_000L)
        val paused = FocusTimer.pause(started, 61_000L)
        assertEquals(240_000L, paused.remainingMs)
        val resumed = FocusTimer.resume(paused, 121_000L)
        val afterResume = FocusTimer.tick(resumed, 181_000L)
        assertEquals(180_000L, afterResume.remainingMs)
        assertEquals(TimerStatus.RUNNING, afterResume.status)
    }

    @Test
    fun restoreUsesWallClockAcrossBackgroundAndCompletesExactly() {
        val started = FocusTimer.start(300_000L, 23 * 60 * 60 * 1000L + 59 * 60 * 1000L)
        val restored = FocusTimer.restore(started, 24 * 60 * 60 * 1000L + 60_000L)
        assertEquals(180_000L, restored.remainingMs)
        val completed = FocusTimer.restore(started, 24 * 60 * 60 * 1000L + 4 * 60 * 1000L)
        assertEquals(0L, completed.remainingMs)
        assertEquals(TimerStatus.COMPLETED, completed.status)
    }

    @Test
    fun progressIsBoundedAndReachesBloomAtCompletion() {
        val started = FocusTimer.start(100L, 0L)
        assertTrue(FocusTimer.progress(started) in 0f..1f)
        assertEquals(1f, FocusTimer.progress(FocusTimer.tick(started, 100L)))
    }
}
