package com.autotext

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*

class BatchActivity : Activity() {
    private lateinit var store: Store
    private lateinit var status: TextView
    private lateinit var logView: TextView
    private lateinit var msg: EditText
    private lateinit var batch: EditText
    private lateinit var cc: EditText

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        store = Store(this)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }

        root.addView(TextView(this).apply { text = "WhatsApp message (sent to the next contacts in your list):" })
        msg = EditText(this).apply {
            setText(store.waMsg)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 4; gravity = Gravity.TOP
        }
        root.addView(msg)
        root.addView(TextView(this).apply { text = "Contacts per batch:" })
        batch = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(store.waBatch.toString()) }
        root.addView(batch)
        root.addView(TextView(this).apply { text = "Default country code (for numbers saved without +):" })
        cc = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(store.countryCode) }
        root.addView(cc)
        root.addView(TextView(this).apply { text = "Gap between contacts: random 10-50 seconds." })

        root.addView(button("1. Allow contacts") { requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 2) })
        root.addView(button("2. Turn on 'AutoText WhatsApp Batch' accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(button("Send next batch") { start() })
        root.addView(button("Stop") { WaBatchService.instance?.stopBatch() })
        root.addView(button("Reset bookmark to first contact") {
            store.waCursor = 0; refresh()
        })
        status = TextView(this).apply { setPadding(0, pad, 0, pad) }
        root.addView(status)
        logView = TextView(this)
        root.addView(logView)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun button(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }

    private fun save() {
        store.waMsg = msg.text.toString()
        store.waBatch = batch.text.toString().toIntOrNull() ?: 5
        store.countryCode = cc.text.toString()
    }

    private fun start() {
        save()
        val svc = WaBatchService.instance
        when {
            checkSelfPermission(Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED ->
                Toast.makeText(this, "Allow contacts first", Toast.LENGTH_LONG).show()
            svc == null -> Toast.makeText(this, "Turn on the accessibility service first", Toast.LENGTH_LONG).show()
            store.waMsg.isBlank() -> Toast.makeText(this, "Type a message first", Toast.LENGTH_LONG).show()
            else -> {
                Toast.makeText(this, "Starting - keep the phone unlocked", Toast.LENGTH_LONG).show()
                svc.startBatch()
            }
        }
    }

    override fun onResume() { super.onResume(); refresh() }
    override fun onPause() { super.onPause(); save() }

    private fun refresh() {
        val list = ContactsRepo.load(this)
        val next = if (list.isEmpty()) "no contacts loaded"
        else {
            val from = store.waCursor % list.size
            val n = minOf(store.waBatch, list.size)
            "next: " + (0 until n).joinToString(", ") { list[(from + it) % list.size].name.ifBlank { "?" } }
        }
        status.text = "Accessibility service: " + (if (WaBatchService.instance != null) "ON" else "OFF") +
                "\nContacts: ${list.size}  |  bookmark at ${store.waCursor + 1}\n$next\n\nRecent activity:"
        logView.text = store.logText().ifEmpty { "(none yet)" }
    }
}
