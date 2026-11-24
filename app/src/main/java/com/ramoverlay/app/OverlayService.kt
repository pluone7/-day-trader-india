package com.ramoverlay.app

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class OverlayService : AccessibilityService() {

    companion object {
        @Volatile private var instance: OverlayService? = null

        fun isRunning(): Boolean = instance != null

        fun setCalibrating(on: Boolean) {
            instance?.let { s -> s.handler.post { s.applyCalibrating(on) } }
        }

        fun refresh() {
            instance?.let { s -> s.handler.post { s.tick(forceText = true) } }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private lateinit var prefs: OverlayPrefs
    private var pill: PillView? = null
    private lateinit var lp: WindowManager.LayoutParams
    private var calibrating = false
    private var ticks = 0

    private val ticker = object : Runnable {
        override fun run() {
            ticks++
            tick(forceText = ticks % 4 == 0) // 500 ms visibility check, 2000 ms text refresh
            handler.postDelayed(this, 500)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = OverlayPrefs(this)

        val view = PillView(this)
        val b = clamped(prefs.bounds)
        lp = WindowManager.LayoutParams(
            b.w, b.h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags(touchable = false),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = b.x
            y = b.y
        }
        view.visibility = View.GONE
        view.onMove = { dx, dy ->
            val c = clamped(Bounds(lp.x + dx, lp.y + dy, lp.width, lp.height))
            applyBounds(c)
        }
        view.onResize = { dx, dy ->
            val c = clamped(Bounds(lp.x, lp.y, lp.width + dx, lp.height + dy))
            applyBounds(c)
        }
        view.onDone = {
            prefs.bounds = Bounds(lp.x, lp.y, lp.width, lp.height)
            applyCalibrating(false)
        }
        wm.addView(view, lp)
        pill = view
        handler.post(ticker)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        tick(forceText = false)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        pill?.let { runCatching { wm.removeView(it) } }
        pill = null
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun flags(touchable: Boolean): Int {
        var f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (!touchable) f = f or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return f
    }

    private fun screen(): DisplayMetrics {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        return dm
    }

    private fun clamped(b: Bounds): Bounds {
        val dm = screen()
        return clampBounds(b, dm.widthPixels, dm.heightPixels)
    }

    private fun applyBounds(b: Bounds) {
        val v = pill ?: return
        lp.x = b.x; lp.y = b.y; lp.width = b.w; lp.height = b.h
        wm.updateViewLayout(v, lp)
    }

    private fun applyCalibrating(on: Boolean) {
        val v = pill ?: return
        calibrating = on
        v.calibrating = on
        lp.flags = flags(touchable = on)
        if (!on) {
            // restore the saved box in case calibration was abandoned
            val b = clamped(prefs.bounds)
            lp.x = b.x; lp.y = b.y; lp.width = b.w; lp.height = b.h
        }
        wm.updateViewLayout(v, lp)
        tick(forceText = true)
    }

    private fun shadeOpen(): Boolean = try {
        val infos = windows.map { w ->
            val r = Rect()
            w.getBoundsInScreen(r)
            WinInfo(w.root?.packageName?.toString(), r.top, r.bottom)
        }
        isShadeOpen(infos, screen().heightPixels)
    } catch (e: Exception) {
        false
    }

    private fun tick(forceText: Boolean) {
        val v = pill ?: return
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val show = calibrating || prefs.alwaysShow || (portrait && shadeOpen())
        val vis = if (show) View.VISIBLE else View.GONE
        val becameVisible = v.visibility != vis && show
        if (v.visibility != vis) v.visibility = vis
        if (show && (forceText || becameVisible)) v.setPercent(readRamPercent(this))
    }
}
