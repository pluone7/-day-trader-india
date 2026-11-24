package com.autotext

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract

data class Contact(val name: String, val number: String)

object ContactsRepo {
    /** All phone contacts, alphabetical, one entry per distinct number. */
    fun load(c: Context): List<Contact> {
        if (c.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return emptyList()
        val seen = HashSet<String>()
        val out = ArrayList<Contact>()
        c.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC, " +
                    "${ContactsContract.CommonDataKinds.Phone.NUMBER} ASC"
        )?.use { cur ->
            while (cur.moveToNext()) {
                val name = cur.getString(0) ?: ""
                val num = cur.getString(1) ?: continue
                val digits = num.filter { it.isDigit() }
                if (digits.length < 7) continue
                if (seen.add(digits.takeLast(10))) out.add(Contact(name, num))
            }
        }
        return out
    }

    /** Digits only, with country code, as wa.me expects. */
    fun normalize(number: String, countryCode: String): String {
        val digits = number.filter { it.isDigit() }
        val cc = countryCode.filter { it.isDigit() }.ifEmpty { "91" }
        return when {
            number.trim().startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.length == 10 -> cc + digits
            digits.startsWith("0") -> cc + digits.drop(1)
            else -> digits
        }
    }
}
