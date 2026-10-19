package com.lms.mobile.ui

import android.app.AlertDialog
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import com.lms.mobile.data.Role
import com.lms.mobile.data.Settings
import com.lms.mobile.logic.Validators

/** Profile and settings: edit details, change password, theme, reminders, help and logout. */
class ProfileActivity : BaseActivity() {
    private var reminderDays = -1

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val user = currentUser() ?: return
        val settings = repo.settings(user.id)
        if (reminderDays < 0) reminderDays = settings.reminderDays

        val c = vbox(20)
        val isMember = user.role == Role.MEMBER
        if (!isMember) c.addView(backLink())

        val head = hbox()
        head.addView(label(user.initial, 22f, Palette.primary, true).apply {
            gravity = Gravity.CENTER
            background = shape(Palette.chip, 32)
        }, lp(dp(64), dp(64), 0f, 0, 0, 14, 0))
        val who = vbox()
        who.addView(title(user.fullName))
        who.addView(muted("@${user.username} · ${if (isMember) "Member" else "Librarian"}"))
        who.addView(muted(if (user.authProvider == "GOOGLE") "Signed in with Google" else user.email))
        head.addView(who, lp(0, WRAP, 1f))
        c.addView(head, lp(MATCH, WRAP, 0f, 0, 0, 0, 16))

        // ---- edit profile
        val profile = card()
        profile.addView(heading("Edit profile"), lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        val nameField = input("Full name", NAME_TYPE).apply { setText(user.fullName) }
        val emailField = input("Email", EMAIL_TYPE).apply { setText(user.email) }
        profile.addView(caption("Full name"))
        profile.addView(nameField, lp(MATCH, WRAP, 0f, 0, 6, 0, 10))
        profile.addView(caption("Email"))
        profile.addView(emailField, lp(MATCH, WRAP, 0f, 0, 6, 0, 12))
        profile.addView(primaryButton("Save profile") {
            val n = Validators.fullName(nameField.text.toString())
            val e = Validators.email(emailField.text.toString())
            nameField.error = if (n.ok) null else n.message
            emailField.error = if (e.ok) null else e.message
            if (n.ok && e.ok) {
                val result = repo.updateProfile(user.id, nameField.text.toString(), emailField.text.toString())
                toast(result.message)
                if (result.ok) render()
            }
        })
        c.addView(profile, lp(MATCH, WRAP, 0f, 0, 0, 0, 14))

        // ---- change password (local accounts only)
        if (user.authProvider == "LOCAL") {
            val pw = card()
            pw.addView(heading("Change password"), lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
            val current = input("Current password", PASSWORD_TYPE)
            val next = input("New password", PASSWORD_TYPE)
            val again = input("Repeat new password", PASSWORD_TYPE)
            pw.addView(current, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
            pw.addView(next, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
            pw.addView(again, lp(MATCH, WRAP, 0f, 0, 0, 0, 12))
            pw.addView(primaryButton("Change password") {
                val v = Validators.password(next.text.toString())
                val m = Validators.passwordsMatch(next.text.toString(), again.text.toString())
                current.error = if (current.text.isEmpty()) "Enter your current password" else null
                next.error = if (v.ok) null else v.message
                again.error = if (m.ok) null else m.message
                if (current.text.isNotEmpty() && v.ok && m.ok) {
                    val old = current.text.toString()
                    val new = next.text.toString()
                    async({ repo.changePassword(user.id, old, new) }) { result ->
                        toast(result.message)
                        if (result.ok) { current.text.clear(); next.text.clear(); again.text.clear() }
                        else current.error = result.message
                    }
                }
            })
            c.addView(pw, lp(MATCH, WRAP, 0f, 0, 0, 0, 14))
        }

        // ---- settings
        val set = card()
        set.addView(heading("Settings"), lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        val dark = Switch(this).apply {
            text = "Dark mode"
            textSize = 15f
            setTextColor(Palette.text)
            isChecked = settings.darkMode
        }
        val alerts = Switch(this).apply {
            text = "Loan reminders and notifications"
            textSize = 15f
            setTextColor(Palette.text)
            isChecked = settings.notificationsEnabled
        }
        set.addView(dark, lp(MATCH, WRAP, 0f, 0, 0, 0, 12))
        set.addView(alerts, lp(MATCH, WRAP, 0f, 0, 0, 0, 12))
        set.addView(caption("Remind me before the due date"))
        val days = hbox()
        fun drawDays() {
            days.removeAllViews()
            for (d in intArrayOf(1, 2, 3, 5, 7)) {
                days.addView(chip("$d day" + (if (d > 1) "s" else ""), d == reminderDays) { reminderDays = d; drawDays() }, lp(WRAP, WRAP, 0f, 0, 0, 6, 0))
            }
        }
        drawDays()
        set.addView(days, lp(MATCH, WRAP, 0f, 0, 8, 0, 12))
        set.addView(primaryButton("Save settings") {
            val s = Settings(dark.isChecked, alerts.isChecked, reminderDays)
            repo.saveSettings(user.id, s)
            session.darkMode = s.darkMode
            toast("Settings saved")
            recreate()
        })
        c.addView(set, lp(MATCH, WRAP, 0f, 0, 0, 0, 14))

        c.addView(outlineButton("Help and support") { startActivity(Intent(this, HelpActivity::class.java)) }, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        c.addView(dangerButton("Log out") {
            AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Do you want to log out of LMS Mobile?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log out") { _, _ ->
                    session.signOut()
                    startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                }
                .show()
        })
        setScreen(c, if (isMember) Tab.PROFILE else null)
    }
}
