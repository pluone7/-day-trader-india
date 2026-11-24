package com.ramoverlay.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = OverlayPrefs(this)
        val pad = (20 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        status = TextView(this).apply { textSize = 18f }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "1. Open Accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        fun toggle(title: String, get: () -> Boolean, set: (Boolean) -> Unit) =
            root.addView(Switch(this).apply {
                text = title
                isChecked = get()
                setOnCheckedChangeListener { _, on ->
                    set(on)
                    OverlayService.refresh()
                }
            })
        toggle("Show RAM %", { prefs.showRam }, { prefs.showRam = it })
        toggle("Show battery %", { prefs.showBattery }, { prefs.showBattery = it })
        toggle("Show storage %", { prefs.showStorage }, { prefs.showStorage = it })

        root.addView(Switch(this).apply {
            text = "Always show (use if the pill never appears)"
            isChecked = prefs.alwaysShow
            setOnCheckedChangeListener { _, on ->
                prefs.alwaysShow = on
                OverlayService.refresh()
            }
        })

        root.addView(Button(this).apply {
            text = "2. Calibrate position"
            setOnClickListener {
                if (!OverlayService.isRunning()) {
                    Toast.makeText(this@MainActivity, "Turn on RAM Overlay in Accessibility first", Toast.LENGTH_LONG).show()
                } else {
                    OverlayService.setCalibrating(true)
                    Toast.makeText(
                        this@MainActivity,
                        "Pull down the panel, drag the pill over the chip, drag the blue corner to resize, tap the green check",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })

        root.addView(TextView(this).apply {
            gravity = Gravity.START
            setPadding(0, pad, 0, 0)
            text = "If the pill disappears after a while, set this app's battery usage to Unrestricted " +
                "and allow it to auto-start in your phone's settings."
        })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        status.text = if (OverlayService.isRunning()) "Service: ON" else "Service: OFF"
    }
}
