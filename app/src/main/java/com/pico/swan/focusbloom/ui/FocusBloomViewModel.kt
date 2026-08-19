package com.pico.swan.focusbloom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pico.swan.focusbloom.data.repository.FocusBloomRepository
import com.pico.swan.focusbloom.data.repository.PersistedFocusSession
import com.pico.swan.focusbloom.domain.model.CompletionChoice
import com.pico.swan.focusbloom.domain.model.FocusDuration
import com.pico.swan.focusbloom.domain.model.FocusPhase
import com.pico.swan.focusbloom.domain.usecase.FocusTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FocusBloomViewModel(
    private val repository: FocusBloomRepository,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FocusBloomUiState())
    val uiState: StateFlow<FocusBloomUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val active = repository.loadActiveSession()
            val history = repository.loadHistory()
            if (active == null) {
                _uiState.update { it.copy(history = history) }
            } else {
                val restored = FocusTimer.restore(active.timer, nowEpochMs())
                _uiState.update {
                    it.copy(
                        phase = when (restored.status) {
                            com.pico.swan.focusbloom.domain.model.TimerStatus.COMPLETED -> FocusPhase.COMPLETE
                            com.pico.swan.focusbloom.domain.model.TimerStatus.PAUSED -> FocusPhase.PAUSED
                            else -> FocusPhase.FOCUSING
                        },
                        draft = active.draft,
                        timer = restored,
                        history = history,
                        isFirstVisit = false,
                    )
                }
                repository.saveActiveSession(active.copy(timer = restored))
            }
        }
    }

    fun onEvent(event: FocusBloomEvent) {
        when (event) {
            FocusBloomEvent.Begin -> _uiState.update { it.copy(phase = FocusPhase.EDITING, isFirstVisit = false) }
            FocusBloomEvent.ShowHistory -> _uiState.update { it.copy(phase = FocusPhase.HISTORY) }
            FocusBloomEvent.BackHome -> _uiState.update { it.copy(phase = FocusPhase.HOME) }
            is FocusBloomEvent.DurationSelected -> _uiState.update { state ->
                state.copy(draft = state.draft.copy(duration = FocusDuration.entries.first { it.minutes == event.minutes }))
            }
            is FocusBloomEvent.TaskChanged -> _uiState.update { it.copy(draft = it.draft.copy(task = event.value)) }
            is FocusBloomEvent.DistractionChanged -> _uiState.update { state ->
                val updated = state.draft.distractions.toMutableList().apply {
                    while (size < 3) add("")
                    set(event.index, event.value)
                }
                state.copy(draft = state.draft.copy(distractions = updated))
            }
            FocusBloomEvent.StartFocus, FocusBloomEvent.MainTaskDropped -> startFocus()
            is FocusBloomEvent.DismissDistraction -> {
                _uiState.update { it.copy(dismissedDistractions = it.dismissedDistractions + event.index) }
                persistCurrent()
            }
            FocusBloomEvent.Pause -> pause()
            FocusBloomEvent.Resume -> resume()
            FocusBloomEvent.ResetCurrent -> resetCurrent()
            FocusBloomEvent.Tick -> tick()
            FocusBloomEvent.OpenCompletion -> _uiState.update { it.copy(phase = FocusPhase.COMPLETE) }
            is FocusBloomEvent.Complete -> complete(event.choice)
            FocusBloomEvent.ContinueNextRound -> resetCurrent()
        }
    }

    private fun startFocus() {
        val state = _uiState.value
        if (state.draft.task.trim().isEmpty()) return
        val timer = FocusTimer.start(state.draft.duration.minutes * 60_000L, nowEpochMs())
        _uiState.update {
            it.copy(
                phase = FocusPhase.FOCUSING,
                timer = timer,
                dismissedDistractions = emptySet(),
                completionChoice = null,
            )
        }
        persistCurrent()
    }

    private fun pause() {
        val timer = _uiState.value.timer ?: return
        val paused = FocusTimer.pause(timer, nowEpochMs())
        _uiState.update { it.copy(phase = FocusPhase.PAUSED, timer = paused) }
        persistCurrent()
    }

    private fun resume() {
        val timer = _uiState.value.timer ?: return
        val resumed = FocusTimer.resume(timer, nowEpochMs())
        _uiState.update { it.copy(phase = FocusPhase.FOCUSING, timer = resumed) }
        persistCurrent()
    }

    private fun tick() {
        val timer = _uiState.value.timer ?: return
        val updated = FocusTimer.tick(timer, nowEpochMs())
        _uiState.update {
            it.copy(
                phase = if (updated.status == com.pico.swan.focusbloom.domain.model.TimerStatus.COMPLETED) FocusPhase.COMPLETE else it.phase,
                timer = updated,
            )
        }
        persistCurrent()
    }

    private fun resetCurrent() {
        viewModelScope.launch { repository.clearActiveSession() }
        _uiState.update { state ->
            state.copy(
                phase = FocusPhase.EDITING,
                timer = null,
                dismissedDistractions = emptySet(),
                completionChoice = null,
            )
        }
    }

    private fun complete(choice: CompletionChoice) {
        val state = _uiState.value
        if (state.completionChoice != null) return
        viewModelScope.launch {
            repository.saveHistory(
                com.pico.swan.focusbloom.domain.model.FocusHistoryEntry(
                    completedAtEpochMs = nowEpochMs(),
                    task = state.draft.task.trim(),
                    durationMinutes = state.draft.duration.minutes,
                    choice = choice,
                ),
            )
            repository.clearActiveSession()
            _uiState.update {
                it.copy(
                    phase = FocusPhase.COMPLETE,
                    completionChoice = choice,
                    history = repository.loadHistory(),
                )
            }
        }
    }

    private fun persistCurrent() {
        val state = _uiState.value
        val timer = state.timer ?: return
        viewModelScope.launch {
            repository.saveActiveSession(PersistedFocusSession(state.draft, timer))
        }
    }
}
