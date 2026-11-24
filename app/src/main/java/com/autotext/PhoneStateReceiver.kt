package com.autotext

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Runs even when the app is closed: when a call ends, check the call log shortly after.
class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (i.getStringExtra(TelephonyManager.EXTRA_STATE) != TelephonyManager.EXTRA_STATE_IDLE) return
        if (!Store(c).enabled) return
        val req = OneTimeWorkRequestBuilder<CallTextWorker>()
            .setInitialDelay(8, TimeUnit.SECONDS) // let Android write the call log entry first
            .build()
        WorkManager.getInstance(c).enqueueUniqueWork("calltext-after-call", ExistingWorkPolicy.REPLACE, req)
    }
}
