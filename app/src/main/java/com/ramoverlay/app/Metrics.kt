package com.ramoverlay.app

import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

/** Joins the enabled readings as "RAM 62% · BAT 38% · STO 71%"; null means that reading is off. */
fun buildLabel(ram: Int?, battery: Int?, storage: Int?): String =
    listOfNotNull(
        ram?.let { "RAM $it%" },
        battery?.let { "BAT $it%" },
        storage?.let { "STO $it%" }
    ).joinToString(" · ")

fun readBatteryPercent(ctx: Context): Int {
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
}

/** Used share of internal storage (the /data partition). */
fun readStoragePercent(): Int {
    val fs = StatFs(Environment.getDataDirectory().path)
    return percentUsed(fs.totalBytes, fs.availableBytes)
}
