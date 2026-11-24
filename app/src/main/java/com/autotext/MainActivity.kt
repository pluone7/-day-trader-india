package com.autotext

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private lateinit var store: Store
    private lateinit var logView: TextView
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        store = Store(this)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }

        root.addView(TextView(this).apply { text = "Message sent to every caller (once per number per day):" })
        val edit = EditText(this).apply {
            setText(store.message)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 4; gravity = Gravity.TOP
        }
        root.addView(edit)

        val sw = Switch(this).apply { text = "Auto-send ON"; isChecked = store.enabled }
        root.addView(sw)

        root.addView(Button(this).apply {
            text = "Save message"
            setOnClickListener {
                store.message = edit.text.toString()
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(Button(this).apply {
            text = "Grant permissions (call log + SMS + phone)"
            setOnClickListener {
                requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE), 1)
            }
        })
        root.addView(Button(this).apply {
            text = "Allow background running (battery)"
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        })
        root.addView(Button(this).apply {
            text = "Check calls now"
            setOnClickListener {
                store.message = edit.text.toString()
                WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<CallTextWorker>().build())
                Toast.makeText(context, "Checking...", Toast.LENGTH_SHORT).show()
            }
        })
        status = TextView(this).apply { setPadding(0, pad, 0, pad) }
        root.addView(status)
        logView = TextView(this)
        root.addView(logView)

        sw.setOnCheckedChangeListener { _, on ->
            store.message = edit.text.toString()
            store.enabled = on
            val wm = WorkManager.getInstance(this)
            if (on) {
                store.lastCheck = System.currentTimeMillis() // only calls from now on
                wm.enqueueUniquePeriodicWork(
                    "autotext", ExistingPeriodicWorkPolicy.UPDATE,
                    PeriodicWorkRequestBuilder<CallTextWorker>(15, TimeUnit.MINUTES).build()
                )
            } else wm.cancelUniqueWork("autotext")
            refresh()
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val ok = checkSelfPermission(Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        status.text = "Permissions: " + (if (ok) "granted" else "NOT granted") +
                "\nAuto-send: " + (if (store.enabled) "ON (texts right after each call ends)" else "OFF") +
                "\n\nRecent activity:"
        logView.text = store.logText().ifEmpty { "(none yet)" }
    }
}
