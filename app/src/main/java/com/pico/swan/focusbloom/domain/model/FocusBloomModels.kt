package com.pico.swan.focusbloom.domain.model

enum class FocusDuration(val minutes: Int) {
    FIVE(5),
    FIFTEEN(15),
    TWENTY_FIVE(25),
}

enum class FocusPhase {
    HOME,
    EDITING,
    FOCUSING,
    PAUSED,
    COMPLETE,
    HISTORY,
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
}

enum class CompletionChoice(val label: String) {
    DONE("已完成"),
    PARTIAL("部分完成"),
    NEXT_ROUND("继续下一轮"),
}

data class FocusTimerSnapshot(
    val durationMs: Long,
    val startedAtEpochMs: Long? = null,
    val pausedAtEpochMs: Long? = null,
    val pausedDurationMs: Long = 0L,
    val remainingMs: Long = durationMs,
    val status: TimerStatus = TimerStatus.IDLE,
)

data class FocusDraft(
    val duration: FocusDuration = FocusDuration.FIFTEEN,
    val task: String = "",
    val distractions: List<String> = emptyList(),
)

data class FocusHistoryEntry(
    val completedAtEpochMs: Long,
    val task: String,
    val durationMinutes: Int,
    val choice: CompletionChoice,
)
