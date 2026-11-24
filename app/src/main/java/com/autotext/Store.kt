package com.autotext

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

class Store(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("autotext", Context.MODE_PRIVATE)

    var message: String
        get() = p.getString("msg", "Hi, thanks for calling. I'll get back to you soon.")!!
        set(v) = p.edit().putString("msg", v).apply()
    var enabled: Boolean
        get() = p.getBoolean("on", false)
        set(v) = p.edit().putBoolean("on", v).apply()
    var lastCheck: Long
        get() = p.getLong("last", System.currentTimeMillis())
        set(v) = p.edit().putLong("last", v).apply()

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    fun key(n: String): String = n.filter { it.isDigit() }.takeLast(10)

    fun alreadySentToday(n: String): Boolean = p.getString("s_" + key(n), "") == today()
    fun markSent(n: String) = p.edit().putString("s_" + key(n), today()).apply()

    fun log(line: String) {
        val t = SimpleDateFormat("dd MMM HH:mm", Locale.US).format(Date())
        val old = p.getString("log", "")!!.split("\n").filter { it.isNotBlank() }
        val all = (listOf("$t  $line") + old).take(40)
        p.edit().putString("log", all.joinToString("\n")).apply()
    }
    fun logText(): String = p.getString("log", "")!!
}
