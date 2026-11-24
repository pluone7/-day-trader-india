package com.ramoverlay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MetricsTest {
    @Test fun ramOnly() = assertEquals("RAM 62%", buildLabel(62, null, null))
    @Test fun allThree() = assertEquals("RAM 62% · BAT 38% · STO 71%", buildLabel(62, 38, 71))
    @Test fun batteryAndStorage() = assertEquals("BAT 5% · STO 0%", buildLabel(null, 5, 0))
    @Test fun nothingEnabledIsEmpty() = assertEquals("", buildLabel(null, null, null))
}
