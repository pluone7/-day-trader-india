package com.ramoverlay.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShadeDetectorTest {
    private val h = 2400

    @Test fun tallSystemUiWindowIsOpen() =
        assertTrue(isShadeOpen(listOf(WinInfo("com.android.systemui", 0, 1200)), h))

    @Test fun statusBarOnlyIsClosed() =
        assertFalse(isShadeOpen(listOf(WinInfo("com.android.systemui", 0, 120)), h))

    @Test fun tallWindowFromOtherPackageIsClosed() =
        assertFalse(isShadeOpen(listOf(WinInfo("com.some.app", 0, 2400)), h))

    @Test fun noWindowsIsClosed() = assertFalse(isShadeOpen(emptyList(), h))

    @Test fun nullPackageIsClosed() =
        assertFalse(isShadeOpen(listOf(WinInfo(null, 0, 2400)), h))
}
