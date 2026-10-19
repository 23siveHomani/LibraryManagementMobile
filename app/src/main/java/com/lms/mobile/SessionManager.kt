package com.lms.mobile

import android.content.Context

/** Remembers who is signed in and a few device-level preferences (SharedPreferences). */
class SessionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lms_session", Context.MODE_PRIVATE)

    var userId: Long
        get() = prefs.getLong("user_id", -1L)
        set(value) { prefs.edit().putLong("user_id", value).apply() }

    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) { prefs.edit().putBoolean("dark_mode", value).apply() }

    var failedLogins: Int
        get() = prefs.getInt("failed_logins", 0)
        set(value) { prefs.edit().putInt("failed_logins", value).apply() }

    var lockedUntil: Long
        get() = prefs.getLong("locked_until", 0L)
        set(value) { prefs.edit().putLong("locked_until", value).apply() }

    val isSignedIn: Boolean get() = userId > 0

    fun signOut() {
        prefs.edit().remove("user_id").apply()
    }
}
