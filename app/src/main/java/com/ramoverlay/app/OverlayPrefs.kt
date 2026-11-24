package com.ramoverlay.app

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

data class Bounds(val x: Int, val y: Int, val w: Int, val h: Int)

/** Keeps the box inside the screen and at least [minSize] px in each dimension. */
fun clampBounds(b: Bounds, screenW: Int, screenH: Int, minSize: Int = 48): Bounds {
    val w = b.w.coerceIn(minSize, maxOf(minSize, screenW))
    val h = b.h.coerceIn(minSize, maxOf(minSize, screenH))
    val x = b.x.coerceIn(0, maxOf(0, screenW - w))
    val y = b.y.coerceIn(0, maxOf(0, screenH - h))
    return Bounds(x, y, w, h)
}

class OverlayPrefs(private val ctx: Context) {
    private val sp = ctx.getSharedPreferences("ramoverlay", Context.MODE_PRIVATE)

    var alwaysShow: Boolean
        get() = sp.getBoolean("always", false)
        set(v) { sp.edit().putBoolean("always", v).apply() }

    var showRam: Boolean
        get() = sp.getBoolean("ram", true)
        set(v) { sp.edit().putBoolean("ram", v).apply() }

    var showBattery: Boolean
        get() = sp.getBoolean("bat", false)
        set(v) { sp.edit().putBoolean("bat", v).apply() }

    var showStorage: Boolean
        get() = sp.getBoolean("sto", false)
        set(v) { sp.edit().putBoolean("sto", v).apply() }

    var bounds: Bounds
        get() {
            if (!sp.contains("x")) return defaultBounds()
            return Bounds(sp.getInt("x", 0), sp.getInt("y", 0), sp.getInt("w", 0), sp.getInt("h", 0))
        }
        set(b) {
            sp.edit().putInt("x", b.x).putInt("y", b.y).putInt("w", b.w).putInt("h", b.h).apply()
        }

    private fun defaultBounds(): Bounds {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        (ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(dm)
        val d = ctx.resources.displayMetrics.density
        return Bounds(
            (dm.widthPixels * 0.6).toInt(), (dm.heightPixels * 0.28).toInt(),
            (160 * d).toInt(), (40 * d).toInt()
        )
    }
}
