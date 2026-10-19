package com.lms.mobile.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.lms.mobile.Config
import com.lms.mobile.SessionManager
import com.lms.mobile.data.Repo
import com.lms.mobile.data.User
import com.lms.mobile.logic.LoginThrottle
import com.lms.mobile.logic.Validators

class LoginActivity : ThemedActivity() {
    private lateinit var session: SessionManager
    private lateinit var repo: Repo
    private lateinit var userField: EditText
    private lateinit var passField: EditText
    private lateinit var errorText: TextView
    private lateinit var loginButton: Button
    private lateinit var googleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        repo = Repo.get(this)

        val root = vbox()
        val header = vbox().apply {
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(48), dp(24), dp(44))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.primaryDark, Palette.primary)).apply {
                val r = dp(36).toFloat()
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
            }
        }
        header.addView(brandLogo(72))
        header.addView(label("LMS Mobile", 26f, 0xFFFFFFFF.toInt(), true).apply { gravity = Gravity.CENTER }, lp(MATCH, WRAP, 0f, 0, 12, 0, 2))
        header.addView(label("Your library, in your pocket", 13f, 0xFFDCE8F7.toInt()).apply { gravity = Gravity.CENTER })
        root.addView(header)

        val form = vbox(24)
        userField = input("Username or email")
        passField = input("Password", PASSWORD_TYPE)
        errorText = label("", 13f, Palette.danger, true).apply { visibility = android.view.View.GONE }
        loginButton = primaryButton("Log in") { attemptLogin() }
        googleButton = outlineButton("Continue with Google") { googleSignIn() }

        form.addView(caption("Username or email"))
        form.addView(userField, lp(MATCH, WRAP, 0f, 0, 6, 0, 14))
        form.addView(caption("Password"))
        form.addView(passField, lp(MATCH, WRAP, 0f, 0, 6, 0, 10))
        form.addView(errorText, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        form.addView(loginButton, lp(MATCH, WRAP, 0f, 0, 4, 0, 0))
        form.addView(linkText("Forgot password?") { forgotPassword() })
        form.addView(label("or", 12f, Palette.muted).apply { gravity = Gravity.CENTER }, lp(MATCH, WRAP, 0f, 0, 4, 0, 8))
        form.addView(googleButton)
        form.addView(linkText("Create a new account") { startActivity(Intent(this, RegisterActivity::class.java)) }, lp(MATCH, WRAP, 0f, 0, 8, 0, 0))
        form.addView(
            muted("Demo accounts: member naledi, librarian ahomani. Password: ${Config.DEMO_PASSWORD}", 11.5f).apply { gravity = Gravity.CENTER },
            lp(MATCH, WRAP, 0f, 0, 12, 0, 0)
        )
        root.addView(form)
        showScrollable(root)
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = android.view.View.VISIBLE
    }

    private fun setBusy(busy: Boolean) {
        loginButton.isEnabled = !busy
        googleButton.isEnabled = !busy
        loginButton.text = if (busy) "Logging in…" else "Log in"
    }

    private fun attemptLogin() {
        errorText.visibility = android.view.View.GONE
        userField.error = null
        passField.error = null

        val secondsLeft = LoginThrottle.secondsLeft(session.lockedUntil, System.currentTimeMillis())
        if (secondsLeft > 0) {
            showError("Too many failed attempts. Try again in $secondsLeft seconds.")
            return
        }
        val id = userField.text.toString().trim()
        val password = passField.text.toString()
        if (id.isEmpty()) { userField.error = "Enter your username or email"; return }
        if (password.isEmpty()) { passField.error = "Enter your password"; return }

        setBusy(true)
        async({ repo.login(id, password) }, fail = { setBusy(false); showError("Could not log in. Please try again.") }) { user ->
            setBusy(false)
            if (user == null) {
                val failures = session.failedLogins + 1
                val lockUntil = LoginThrottle.lockUntil(failures, System.currentTimeMillis())
                session.failedLogins = if (lockUntil > 0) 0 else failures
                session.lockedUntil = lockUntil
                showError(if (lockUntil > 0) "Too many failed attempts. Try again in ${Config.LOCKOUT_SECONDS} seconds." else "Incorrect username or password")
            } else {
                signedIn(user)
            }
        }
    }

    private fun signedIn(user: User) {
        session.userId = user.id
        session.failedLogins = 0
        session.lockedUntil = 0
        session.darkMode = repo.settings(user.id).darkMode
        startActivity(homeIntentFor(user).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun googleSignIn() {
        errorText.visibility = android.view.View.GONE
        if (!Config.googleConfigured()) {
            showError("Google sign-in is not set up yet. Add your OAuth client ID in Config.kt (see README).")
            return
        }
        setBusy(true)
        GoogleSso.signIn(this,
            onAccount = { email, name ->
                async({ repo.signInWithGoogle(email, name) }, fail = { setBusy(false); showError("Could not create your account.") }) { user ->
                    setBusy(false)
                    signedIn(user)
                }
            },
            onFailure = { message -> setBusy(false); showError(message) })
    }

    /** Simulated password reset (no email server): username and registered email must both match. */
    private fun forgotPassword() {
        val box = vbox(20)
        val u = input("Username")
        val e = input("Registered email", EMAIL_TYPE)
        val p = input("New password", PASSWORD_TYPE)
        box.addView(u, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        box.addView(e, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        box.addView(p)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Reset password")
            .setView(box)
            .setPositiveButton("Reset", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val checks = listOf(
                u to Validators.username(u.text.toString()),
                e to Validators.email(e.text.toString()),
                p to Validators.password(p.text.toString())
            )
            val bad = checks.firstOrNull { !it.second.ok }
            if (bad != null) {
                bad.first.error = bad.second.message
                return@setOnClickListener
            }
            val username = u.text.toString()
            val email = e.text.toString()
            val newPassword = p.text.toString()
            async({ repo.resetPassword(username, email, newPassword) }) { result ->
                toast(result.message)
                if (result.ok) dialog.dismiss()
            }
        }
    }
}
