package com.ramoverlay.app

import android.app.ActivityManager
import android.content.Context
import kotlin.math.roundToInt

/** Used memory as a whole percent of total, clamped to 0..100. */
fun percentUsed(total: Long, avail: Long): Int {
    if (total <= 0L) return 0
    val used = (total - avail).coerceIn(0L, total)
    return (used * 100.0 / total).roundToInt()
}

fun readRamPercent(ctx: Context): Int {
    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    return percentUsed(info.totalMem, info.availMem)
}
