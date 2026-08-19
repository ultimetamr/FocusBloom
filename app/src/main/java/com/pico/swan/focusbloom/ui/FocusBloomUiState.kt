package com.pico.swan.focusbloom.ui

import com.pico.swan.focusbloom.domain.model.CompletionChoice
import com.pico.swan.focusbloom.domain.model.FocusDraft
import com.pico.swan.focusbloom.domain.model.FocusHistoryEntry
import com.pico.swan.focusbloom.domain.model.FocusPhase
import com.pico.swan.focusbloom.domain.model.FocusTimerSnapshot

data class FocusBloomUiState(
    val phase: FocusPhase = FocusPhase.HOME,
    val draft: FocusDraft = FocusDraft(),
    val timer: FocusTimerSnapshot? = null,
    val history: List<FocusHistoryEntry> = emptyList(),
    val dismissedDistractions: Set<Int> = emptySet(),
    val completionChoice: CompletionChoice? = null,
    val isFirstVisit: Boolean = true,
)

sealed interface FocusBloomEvent {
    data object Begin : FocusBloomEvent
    data object ShowHistory : FocusBloomEvent
    data object BackHome : FocusBloomEvent
    data class DurationSelected(val minutes: Int) : FocusBloomEvent
    data class TaskChanged(val value: String) : FocusBloomEvent
    data class DistractionChanged(val index: Int, val value: String) : FocusBloomEvent
    data object StartFocus : FocusBloomEvent
    data object MainTaskDropped : FocusBloomEvent
    data class DismissDistraction(val index: Int) : FocusBloomEvent
    data object Pause : FocusBloomEvent
    data object Resume : FocusBloomEvent
    data object ResetCurrent : FocusBloomEvent
    data object Tick : FocusBloomEvent
    data object OpenCompletion : FocusBloomEvent
    data class Complete(val choice: CompletionChoice) : FocusBloomEvent
    data object ContinueNextRound : FocusBloomEvent
}
