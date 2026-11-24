package com.ramoverlay.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

/** The opaque dark pill. In calibration mode: drag body = move, drag blue corner = resize, tap check = save. */
class PillView(ctx: Context) : FrameLayout(ctx) {
    var calibrating = false
        set(v) {
            field = v
            done.visibility = if (v) View.VISIBLE else View.GONE
            handle.visibility = if (v) View.VISIBLE else View.GONE
        }
    var onMove: ((Int, Int) -> Unit)? = null
    var onResize: ((Int, Int) -> Unit)? = null
    var onDone: (() -> Unit)? = null

    private val dp = ctx.resources.displayMetrics.density
    private val label = TextView(ctx).apply {
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        text = "RAM --%"
    }
    private val done = TextView(ctx).apply {
        text = "✓"
        setTextColor(Color.GREEN)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        gravity = Gravity.CENTER
        visibility = View.GONE
        setOnClickListener { onDone?.invoke() }
    }
    private val handle = View(ctx).apply {
        setBackgroundColor(0xFF4FC3F7.toInt())
        visibility = View.GONE
    }
    private var lastX = 0f
    private var lastY = 0f

    init {
        background = GradientDrawable().apply {
            setColor(0xFF111111.toInt())
            cornerRadius = 1000f
        }
        addView(label, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(done, LayoutParams((36 * dp).toInt(), (36 * dp).toInt(), Gravity.TOP or Gravity.END))
        addView(handle, LayoutParams((24 * dp).toInt(), (24 * dp).toInt(), Gravity.BOTTOM or Gravity.END))
        handle.setOnTouchListener { _, e -> drag(e) { dx, dy -> onResize?.invoke(dx, dy) } }
    }

    fun setPercent(p: Int) {
        label.text = "RAM $p%"
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!calibrating) return false
        return drag(e) { dx, dy -> onMove?.invoke(dx, dy) }
    }

    private fun drag(e: MotionEvent, cb: (Int, Int) -> Unit): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = e.rawX; lastY = e.rawY }
            MotionEvent.ACTION_MOVE -> {
                cb((e.rawX - lastX).toInt(), (e.rawY - lastY).toInt())
                lastX = e.rawX; lastY = e.rawY
            }
        }
        return true
    }
}
