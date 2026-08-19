package com.pico.swan.focusbloom.domain

import com.pico.swan.focusbloom.domain.usecase.DropTargetBounds
import com.pico.swan.focusbloom.domain.usecase.isDropInsideTarget
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DropTargetHitTestTest {
    private val pot = DropTargetBounds(100f, 100f, 300f, 300f)

    @Test
    fun acceptsCardCenterInsidePot() {
        assertTrue(isDropInsideTarget(200f, 240f, pot))
    }

    @Test
    fun rejectsCardCenterOutsidePotOrWithoutTarget() {
        assertFalse(isDropInsideTarget(40f, 240f, pot))
        assertFalse(isDropInsideTarget(200f, 240f, null))
    }
}
