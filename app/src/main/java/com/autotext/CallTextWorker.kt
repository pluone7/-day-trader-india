package com.autotext

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import android.telephony.SmsManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CallTextWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        val s = Store(c)
        if (!s.enabled) return Result.success()
        if (c.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
            c.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
        ) {
            s.log("Permissions missing - open the app and grant them")
            return Result.success()
        }
        val since = s.lastCheck
        val now = System.currentTimeMillis()
        val numbers = LinkedHashSet<String>()
        // Incoming, outgoing and missed (also rejected/blocked) all appear in the call log.
        c.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER),
            "${CallLog.Calls.DATE} > ?", arrayOf(since.toString()),
            "${CallLog.Calls.DATE} ASC"
        )?.use { cur ->
            while (cur.moveToNext()) {
                val n = cur.getString(0)
                if (!n.isNullOrBlank() && n.any { it.isDigit() }) numbers.add(n)
            }
        }
        val text = s.message.trim()
        if (text.isEmpty()) { s.lastCheck = now; return Result.success() }
        val sms = if (Build.VERSION.SDK_INT >= 31) c.getSystemService(SmsManager::class.java) else SmsManager.getDefault()
        for (n in numbers) {
            if (s.alreadySentToday(n)) continue
            try {
                val parts = sms.divideMessage(text)
                if (parts.size > 1) sms.sendMultipartTextMessage(n, null, parts, null, null)
                else sms.sendTextMessage(n, null, text, null, null)
                s.markSent(n)
                s.log("Sent to $n")
            } catch (e: Exception) {
                s.log("Failed for $n: ${e.message}")
            }
        }
        s.lastCheck = now
        return Result.success()
    }
}
