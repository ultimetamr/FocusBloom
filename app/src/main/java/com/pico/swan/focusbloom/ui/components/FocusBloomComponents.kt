package com.pico.swan.focusbloom.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TextArea
import com.pico.spatial.ui.foundation.haptic.controllerHapticFeedback
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import kotlin.math.roundToInt

@Composable
fun BloomSurface(content: @Composable () -> Unit) = Box(Modifier.fillMaxSize()) { content() }

@Composable
fun BloomText(text: String, style: androidx.compose.ui.text.TextStyle) = Text(text, style = style)

@Composable
fun BloomLabel(text: String) = Text(text, style = PicoTheme.typography.labelMedium)

@Composable
fun TitleBlock(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = PicoTheme.typography.displaySmall)
        Text(subtitle, style = PicoTheme.typography.bodyLarge)
    }
}

@Composable
fun StatsChip(label: String, value: String) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = PicoTheme.typography.labelMedium)
        Text(value, style = PicoTheme.typography.titleMedium)
    }
}

@Composable
fun PrimaryBloomButton(label: String, onClick: () -> Unit) =
    BloomButton(label, onClick, primary = true)

@Composable
fun SecondaryBloomButton(label: String, onClick: () -> Unit) =
    BloomButton(label, onClick, primary = false)

@Composable
private fun BloomButton(label: String, onClick: () -> Unit, primary: Boolean) {
    Button(onClick = onClick) { Text(label, style = PicoTheme.typography.labelLarge) }
}

@Composable
fun BloomTextArea(value: String, onValueChange: (String) -> Unit, modifier: Modifier, placeholder: String) =
    TextArea(value = value, onValueChange = onValueChange, modifier = modifier)

@Composable
fun BloomPot(modifier: Modifier = Modifier, onBoundsChanged: (DropTargetBoundsPx) -> Unit = {}) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BloomFlower(progress = 0.2f, modifier = Modifier.size(150.dp))
        Box(
            Modifier
                .size(width = 180.dp, height = 90.dp)
                .onGloballyPositioned { onBoundsChanged(DropTargetBoundsPx.from(it.boundsInRoot())) }
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF8F5E42)),
        )
        Text("把任务放进这里", style = PicoTheme.typography.labelMedium)
    }
}

@Composable
fun FocusDropTarget(label: String, helper: String, modifier: Modifier = Modifier, onBoundsChanged: (DropTargetBoundsPx) -> Unit = {}) {
    Column(
        modifier
            .onGloballyPositioned { onBoundsChanged(DropTargetBoundsPx.from(it.boundsInRoot())) }
            .clip(RoundedCornerShape(26.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = PicoTheme.typography.titleMedium)
        Text(helper, style = PicoTheme.typography.bodyMedium)
        Text("抓起卡片放入", style = PicoTheme.typography.labelMedium)
    }
}

@Composable
fun FocusCard(title: String, body: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BloomFlower(progress = 1f, modifier = Modifier.size(48.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = PicoTheme.typography.labelMedium)
            Text(body, style = PicoTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SpatialDragCard(text: String, onDropped: (Offset) -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var centerInRoot by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .onGloballyPositioned { centerInRoot = it.boundsInRoot().center }
            .clip(RoundedCornerShape(20.dp))
            .spatialHoverEffect(enabled = true)
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        onDropped(centerInRoot + dragOffset)
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = { dragOffset = Offset.Zero },
                    onDrag = { change, amount ->
                        change.consume()
                        dragOffset += amount
                    },
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { onDropped(centerInRoot) },
            )
            .controllerHapticFeedback(interactionSource = interactionSource)
            .padding(16.dp),
    ) {
        Text(text, style = PicoTheme.typography.titleMedium)
    }
}

data class DropTargetBoundsPx(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val center get() = Offset((left + right) / 2f, (top + bottom) / 2f)
    companion object {
        fun from(rect: androidx.compose.ui.geometry.Rect) = DropTargetBoundsPx(rect.left, rect.top, rect.right, rect.bottom)
    }
}

@Composable
fun BloomFlower(progress: Float, modifier: Modifier = Modifier) {
    val stage = (progress.coerceIn(0f, 1f) * 5f).toInt().coerceAtMost(4)
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF8F5E42)))
        if (stage >= 1) {
            Box(Modifier.size(88.dp).clip(CircleShape).background(Color(0xFF6BAF78)))
        }
        if (stage >= 2) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFE9A46F)))
        }
        if (stage >= 3) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF3C969)))
        }
        if (stage >= 4) {
            Text("✦", style = PicoTheme.typography.displayMedium)
        }
    }
}

@Composable
fun BloomProgress(progress: Float) {
    Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxSize().background(Color(0xFF6BAF78)))
        Spacer(Modifier.weight((1f - progress).coerceIn(0f, 1f)))
    }
}
