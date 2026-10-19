package com.lms.mobile

/** Central place for the rules and settings that are used across the app. */
object Config {
    /**
     * OAuth 2.0 *Web application* client ID from Google Cloud Console (APIs & Services > Credentials).
     * Replace the placeholder before using "Continue with Google". See README.md, section "Single sign-on".
     */
    const val GOOGLE_WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"

    const val DB_NAME = "lms.db"
    const val DB_VERSION = 1

    const val PBKDF2_ITERATIONS = 600_000

    const val LOAN_DAYS = 14
    const val MAX_ACTIVE_LOANS = 5
    const val FINE_PER_DAY_RAND = 5
    const val FINE_CAP_RAND = 150

    const val MAX_FAILED_LOGINS = 5
    const val LOCKOUT_SECONDS = 30

    const val SUPPORT_EMAIL = "support@lms.example"
    const val DEMO_PASSWORD = "Password@123"

    fun googleConfigured(): Boolean = !GOOGLE_WEB_CLIENT_ID.startsWith("YOUR_")
}
