package com.ramoverlay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class RamReaderTest {
    @Test fun normalCase() = assertEquals(75, percentUsed(8000, 2000))
    @Test fun zeroTotalIsZero() = assertEquals(0, percentUsed(0, 0))
    @Test fun availAboveTotalClampsToZero() = assertEquals(0, percentUsed(1000, 2000))
    @Test fun negativeAvailClampsTo100() = assertEquals(100, percentUsed(1000, -5))
}
