package com.pico.swan.focusbloom.domain.usecase

data class DropTargetBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

fun isDropInsideTarget(centerX: Float, centerY: Float, target: DropTargetBounds?): Boolean =
    target?.contains(centerX, centerY) == true
