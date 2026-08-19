package com.pico.swan.focusbloom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.ui.design.PicoTheme
import com.pico.swan.focusbloom.data.repository.SharedPreferencesFocusBloomRepository
import com.pico.swan.focusbloom.platform.FocusBloomScreenshotExporter
import com.pico.swan.focusbloom.domain.model.CompletionChoice
import com.pico.swan.focusbloom.domain.model.FocusDuration
import com.pico.swan.focusbloom.domain.model.FocusPhase
import com.pico.swan.focusbloom.domain.usecase.FocusTimer
import com.pico.swan.focusbloom.domain.usecase.DropTargetBounds
import com.pico.swan.focusbloom.domain.usecase.formatRemaining
import com.pico.swan.focusbloom.domain.usecase.isDropInsideTarget
import com.pico.swan.focusbloom.ui.components.BloomFlower
import com.pico.swan.focusbloom.ui.components.BloomPot
import com.pico.swan.focusbloom.ui.components.BloomSurface
import com.pico.swan.focusbloom.ui.components.FocusCard
import com.pico.swan.focusbloom.ui.components.FocusDropTarget
import com.pico.swan.focusbloom.ui.components.PrimaryBloomButton
import com.pico.swan.focusbloom.ui.components.SecondaryBloomButton
import com.pico.swan.focusbloom.ui.components.SpatialDragCard
import com.pico.swan.focusbloom.ui.components.StatsChip
import com.pico.swan.focusbloom.ui.components.TitleBlock
import com.pico.swan.focusbloom.ui.components.BloomTextArea
import com.pico.swan.focusbloom.ui.components.BloomText
import com.pico.swan.focusbloom.ui.components.BloomLabel
import com.pico.swan.focusbloom.ui.components.BloomProgress

@Composable
fun FocusBloomScreen(
    model: FocusBloomViewModel? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    val resolvedViewModel = model ?: viewModel(factory = FocusBloomViewModelFactory(SharedPreferencesFocusBloomRepository(context)))
    val screenshotExporter = remember(context, view) { FocusBloomScreenshotExporter(context, view) }
    val state by resolvedViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.phase) {
        while (state.phase == FocusPhase.FOCUSING) {
            kotlinx.coroutines.delay(500L)
            resolvedViewModel.onEvent(FocusBloomEvent.Tick)
        }
    }
    when (state.phase) {
        FocusPhase.HOME -> HomeScreen(state, resolvedViewModel::onEvent)
        FocusPhase.EDITING -> EditingScreen(state, resolvedViewModel::onEvent)
        FocusPhase.FOCUSING -> FocusScreen(state, resolvedViewModel::onEvent)
        FocusPhase.PAUSED -> PauseScreen(state, resolvedViewModel::onEvent)
        FocusPhase.COMPLETE -> CompleteScreen(state, resolvedViewModel::onEvent, screenshotExporter::export)
        FocusPhase.HISTORY -> HistoryScreen(state, resolvedViewModel::onEvent)
    }
}

@Composable
private fun HomeScreen(state: FocusBloomUiState, onEvent: (FocusBloomEvent) -> Unit) {
    BloomSurface {
        Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                TitleBlock("Focus Bloom", "把开始专注变成一个小仪式")
                StatsChip("最近两周", "${state.history.size} 朵花")
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                BloomPot(modifier = Modifier.weight(0.8f))
                Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    BloomText("写下这一轮只想完成的一件事，然后放进花盆。", style = PicoTheme.typography.displaySmall)
                    BloomText("专注时，其他念头可以放进右侧的“稍后再说”，它们不会被忘记。", style = PicoTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryBloomButton("开始一轮专注", onClick = { onEvent(FocusBloomEvent.Begin) })
                        SecondaryBloomButton("最近两周的花园", onClick = { onEvent(FocusBloomEvent.ShowHistory) })
                    }
                }
                FocusDropTarget(label = "稍后再说", helper = "收纳干扰", modifier = Modifier.weight(0.8f))
            }
        }
    }
}

@Composable
private fun EditingScreen(state: FocusBloomUiState, onEvent: (FocusBloomEvent) -> Unit) {
    var potBounds by remember { mutableStateOf<com.pico.swan.focusbloom.ui.components.DropTargetBoundsPx?>(null) }
    BloomSurface {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TitleBlock("准备这一轮", "只选一件事，剩下的先放到稍后再说")
                SecondaryBloomButton("回到首页", onClick = { onEvent(FocusBloomEvent.BackHome) })
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BloomLabel("主任务 · 必填")
                    BloomTextArea(state.draft.task, { onEvent(FocusBloomEvent.TaskChanged(it)) }, Modifier.fillMaxWidth().height(150.dp), "例如：完成项目提案的第一版")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        SpatialDragCard(
                            text = state.draft.task.ifBlank { "主任务卡：写下这一轮要完成的事" },
                            onDropped = { center ->
                                val target = potBounds?.let(::toDomainBounds)
                                if (isDropInsideTarget(center.x, center.y, target)) onEvent(FocusBloomEvent.MainTaskDropped)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        BloomPot(modifier = Modifier.weight(0.8f), onBoundsChanged = { potBounds = it })
                    }
                    BloomLabel("专注时长")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocusDuration.values().forEach { duration ->
                            val selected = state.draft.duration == duration
                            if (selected) PrimaryBloomButton("${duration.minutes} 分钟", onClick = { onEvent(FocusBloomEvent.DurationSelected(duration.minutes)) })
                            else SecondaryBloomButton("${duration.minutes} 分钟", onClick = { onEvent(FocusBloomEvent.DurationSelected(duration.minutes)) })
                        }
                    }
                }
                Column(Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BloomLabel("可选 · 最多三条干扰事项")
                    repeat(3) { index ->
                        BloomTextArea(state.draft.distractions.getOrNull(index).orEmpty(), { onEvent(FocusBloomEvent.DistractionChanged(index, it)) }, Modifier.fillMaxWidth().height(74.dp), "想到什么就先记下")
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecondaryBloomButton("取消", onClick = { onEvent(FocusBloomEvent.BackHome) })
                        PrimaryBloomButton("把任务放进花盆", onClick = { onEvent(FocusBloomEvent.StartFocus) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusScreen(state: FocusBloomUiState, onEvent: (FocusBloomEvent) -> Unit) {
    val timer = state.timer ?: return
    val progress = FocusTimer.progress(timer)
    var laterBounds by remember { mutableStateOf<com.pico.swan.focusbloom.ui.components.DropTargetBoundsPx?>(null) }
    BloomSurface {
        Row(Modifier.fillMaxSize().padding(28.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1.5f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BloomLabel("正在专注")
                        BloomText(state.draft.task, style = PicoTheme.typography.titleMedium)
                    }
                    SecondaryBloomButton("暂停", onClick = { onEvent(FocusBloomEvent.Pause) })
                }
                Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    BloomFlower(progress = progress, modifier = Modifier.size(250.dp))
                    Spacer(Modifier.height(14.dp))
                    BloomText(formatRemaining(timer.remainingMs), style = PicoTheme.typography.displayMedium)
                    BloomProgress(progress)
                    BloomText("花朵会安静地长大，不需要你做任何额外操作。", style = PicoTheme.typography.bodyMedium)
                }
            }
            Column(Modifier.weight(0.8f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusDropTarget(
                    "稍后再说",
                    "把干扰放入这里",
                    Modifier.fillMaxWidth().height(128.dp),
                    onBoundsChanged = { laterBounds = it },
                )
                BloomLabel("可以先放下的念头")
                state.draft.distractions.mapIndexedNotNull { index, text ->
                    if (text.isBlank() || index in state.dismissedDistractions) null else index to text
                }.forEach { (index, text) ->
                    SpatialDragCard(
                        text,
                        onDropped = { center ->
                            val target = laterBounds?.let(::toDomainBounds)
                            if (isDropInsideTarget(center.x, center.y, target)) onEvent(FocusBloomEvent.DismissDistraction(index))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.weight(1f))
                SecondaryBloomButton("结束这一轮", onClick = { onEvent(FocusBloomEvent.OpenCompletion) })
            }
        }
    }
}

@Composable
private fun PauseScreen(state: FocusBloomUiState, onEvent: (FocusBloomEvent) -> Unit) {
    BloomSurface {
        Column(Modifier.fillMaxSize().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            BloomFlower(
                progress = state.timer?.let(FocusTimer::progress) ?: 0f,
                modifier = Modifier.size(220.dp),
            )
            Spacer(Modifier.height(18.dp))
            BloomText("这一轮已暂停", style = PicoTheme.typography.displaySmall)
            BloomText("${state.draft.task} · ${formatRemaining(state.timer?.remainingMs ?: 0L)}", style = PicoTheme.typography.bodyLarge)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryBloomButton("继续专注", onClick = { onEvent(FocusBloomEvent.Resume) })
                SecondaryBloomButton("重置这一轮", onClick = { onEvent(FocusBloomEvent.ResetCurrent) })
                SecondaryBloomButton("回到首页", onClick = { onEvent(FocusBloomEvent.BackHome) })
            }
        }
    }
}

@Composable
private fun CompleteScreen(state: FocusBloomUiState, onEvent: (FocusBloomEvent) -> Unit, onExportScreenshot: () -> Result<Unit>) {
    BloomSurface {
        Column(Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            BloomFlower(1f, Modifier.size(260.dp))
            Spacer(Modifier.height(12.dp))
            BloomText("花开了", style = PicoTheme.typography.displayMedium)
            BloomText(state.draft.task, style = PicoTheme.typography.titleMedium)
            BloomText("这一轮 ${state.draft.duration.minutes} 分钟已经完成。", style = PicoTheme.typography.bodyLarge)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompletionChoice.values().forEach { choice ->
                    if (state.completionChoice == choice) PrimaryBloomButton(choice.label, onClick = {})
                    else SecondaryBloomButton(choice.label, onClick = { onEvent(FocusBloomEvent.Complete(choice)) })
                }
            }
            Spacer(Modifier.height(14.dp))
            SecondaryBloomButton("保存当前截图", onClick = { onExportScreenshot() })
            Spacer(Modifier.height(8.dp))
            SecondaryBloomButton("查看最近两周花园", onClick = { onEvent(FocusBloomEvent.ShowHistory) })
        }
    }
}

@Composable
private fun HistoryScreen(state: FocusBloomUiState, onEvent: (FocusBloomEvent) -> Unit) {
    BloomSurface {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TitleBlock("最近两周的花园", "每一朵花都代表一次被认真对待的时间")
                SecondaryBloomButton("回到首页", onClick = { onEvent(FocusBloomEvent.BackHome) })
            }
            if (state.history.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    BloomFlower(0.2f, Modifier.size(180.dp))
                    BloomText("还没有花朵，先完成第一轮专注吧。", style = PicoTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(state.history) { _, entry ->
                        FocusCard("${entry.durationMinutes} 分钟 · ${entry.choice.label}", entry.task)
                    }
                }
            }
        }
    }
}

class FocusBloomViewModelFactory(private val repository: com.pico.swan.focusbloom.data.repository.FocusBloomRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = FocusBloomViewModel(repository) as T
}

private fun toDomainBounds(bounds: com.pico.swan.focusbloom.ui.components.DropTargetBoundsPx) = DropTargetBounds(
    left = bounds.left,
    top = bounds.top,
    right = bounds.right,
    bottom = bounds.bottom,
)
