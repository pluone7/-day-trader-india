package com.autotext

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import kotlin.random.Random

/**
 * Sends the WhatsApp batch. Accessibility services may start activities from the background,
 * so the whole loop lives here and keeps working after the app screen is closed.
 */
class WaBatchService : AccessibilityService() {
    companion object {
        @Volatile var instance: WaBatchService? = null
        val running: Boolean get() = instance?.job?.isActive == true
        private val PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean {
        job?.cancel(); instance = null
        return super.onUnbind(intent)
    }

    fun startBatch() {
        if (job?.isActive == true) return
        job = scope.launch { runBatch() }
    }

    fun stopBatch() { job?.cancel() }

    private suspend fun runBatch() {
        val s = Store(this)
        val contacts = ContactsRepo.load(this)
        if (contacts.isEmpty()) { s.log("Batch: no contacts (grant contacts permission)"); return }
        val n = minOf(s.waBatch, contacts.size)
        s.log("Batch started: next $n contacts")
        try {
            for (i in 0 until n) {
                val idx = s.waCursor % contacts.size
                val c = contacts[idx]
                val ok = sendTo(c, s)
                s.waCursor = (idx + 1) % contacts.size
                s.log((if (ok) "Sent to " else "Skipped (no WhatsApp/failed) ") + c.name.ifBlank { c.number })
                if (i < n - 1) {
                    val gap = Random.nextLong(10_000, 50_001)
                    s.log("Waiting ${gap / 1000}s")
                    delay(gap)
                }
            }
            s.log("Batch finished")
        } catch (e: CancellationException) {
            s.log("Batch stopped")
            throw e
        } finally {
            startActivity(Intent(this, BatchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private suspend fun sendTo(c: Contact, s: Store): Boolean {
        val num = ContactsRepo.normalize(c.number, s.countryCode)
        val uri = Uri.parse("https://wa.me/$num?text=" + Uri.encode(s.waMsg))
        var launched = false
        for (pkg in PACKAGES) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                launched = true; break
            } catch (e: Exception) { /* try next package */ }
        }
        if (!launched) { s.log("WhatsApp not installed"); return false }
        delay(2000)
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            val btn = findSend()
            if (btn != null) {
                delay(800) // let the chat finish loading
                if (clickNode(btn)) { delay(1500); return true }
            }
            delay(600)
        }
        performGlobalAction(GLOBAL_ACTION_BACK) // closes the "invalid number" dialog / chat
        delay(500)
        return false
    }

    private fun findSend(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        for (id in listOf("com.whatsapp:id/send", "com.whatsapp.w4b:id/send")) {
            root.findAccessibilityNodeInfosByViewId(id).firstOrNull { it.isEnabled }?.let { return it }
        }
        return root.findAccessibilityNodeInfosByText("Send").firstOrNull { it.isEnabled && it.isClickable }
    }

    private fun clickNode(n: AccessibilityNodeInfo): Boolean {
        var cur: AccessibilityNodeInfo? = n
        while (cur != null) {
            if (cur.isClickable && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            cur = cur.parent
        }
        return false
    }
}
