package com.ramoverlay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPrefsTest {
    private val sw = 1080
    private val sh = 2400

    @Test fun widerThanScreenShrinks() =
        assertEquals(Bounds(0, 0, 1080, 100), clampBounds(Bounds(0, 0, 2000, 100), sw, sh))

    @Test fun negativeXBecomesZero() =
        assertEquals(Bounds(0, 500, 100, 100), clampBounds(Bounds(-50, 500, 100, 100), sw, sh))

    @Test fun pastBottomMovesUp() =
        assertEquals(Bounds(10, 2300, 100, 100), clampBounds(Bounds(10, 2390, 100, 100), sw, sh))

    @Test fun tooSmallRaisedToMin() =
        assertEquals(Bounds(10, 10, 48, 48), clampBounds(Bounds(10, 10, 5, 5), sw, sh))

    @Test fun landscapeSwapKeepsBoxInside() =
        assertEquals(Bounds(1280, 392, 160, 48), clampBounds(Bounds(1500, 900, 160, 40), 1440, 440))
}
