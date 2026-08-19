package com.pico.swan.focusbloom.domain.usecase

import com.pico.swan.focusbloom.domain.model.FocusTimerSnapshot
import com.pico.swan.focusbloom.domain.model.TimerStatus

object FocusTimer {
    fun start(durationMs: Long, nowEpochMs: Long): FocusTimerSnapshot = FocusTimerSnapshot(
        durationMs = durationMs,
        startedAtEpochMs = nowEpochMs,
        remainingMs = durationMs,
        status = TimerStatus.RUNNING,
    )

    fun pause(snapshot: FocusTimerSnapshot, nowEpochMs: Long): FocusTimerSnapshot {
        if (snapshot.status != TimerStatus.RUNNING) return snapshot
        val current = tick(snapshot, nowEpochMs)
        return if (current.status == TimerStatus.COMPLETED) {
            current
        } else {
            current.copy(status = TimerStatus.PAUSED, pausedAtEpochMs = nowEpochMs)
        }
    }

    fun resume(snapshot: FocusTimerSnapshot, nowEpochMs: Long): FocusTimerSnapshot {
        if (snapshot.status != TimerStatus.PAUSED) return snapshot
        val pausedAt = snapshot.pausedAtEpochMs ?: nowEpochMs
        return snapshot.copy(
            pausedAtEpochMs = null,
            pausedDurationMs = snapshot.pausedDurationMs + (nowEpochMs - pausedAt).coerceAtLeast(0L),
            status = TimerStatus.RUNNING,
        )
    }

    fun tick(snapshot: FocusTimerSnapshot, nowEpochMs: Long): FocusTimerSnapshot {
        if (snapshot.status != TimerStatus.RUNNING) return snapshot
        val startedAt = snapshot.startedAtEpochMs ?: return snapshot
        val elapsed = (nowEpochMs - startedAt - snapshot.pausedDurationMs).coerceAtLeast(0L)
        val remaining = (snapshot.durationMs - elapsed).coerceAtLeast(0L)
        return snapshot.copy(
            remainingMs = remaining,
            status = if (remaining == 0L) TimerStatus.COMPLETED else TimerStatus.RUNNING,
        )
    }

    fun restore(snapshot: FocusTimerSnapshot, nowEpochMs: Long): FocusTimerSnapshot = tick(snapshot, nowEpochMs)

    fun progress(snapshot: FocusTimerSnapshot): Float =
        ((snapshot.durationMs - snapshot.remainingMs).toDouble() / snapshot.durationMs.coerceAtLeast(1L))
            .coerceIn(0.0, 1.0)
            .toFloat()
}

fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = ((remainingMs + 999L) / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
