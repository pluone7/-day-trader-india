package com.ramoverlay.app

data class WinInfo(val pkg: String?, val top: Int, val bottom: Int)

private const val SYSTEM_UI = "com.android.systemui"

/** The panel counts as open when a System UI window covers at least 40% of the screen height. */
fun isShadeOpen(wins: List<WinInfo>, screenH: Int): Boolean {
    if (screenH <= 0) return false
    return wins.any { it.pkg == SYSTEM_UI && (it.bottom - it.top) * 100L >= screenH * 40L }
}
