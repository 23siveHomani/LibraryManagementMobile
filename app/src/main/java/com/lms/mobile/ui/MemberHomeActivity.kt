package com.lms.mobile.ui

import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.lms.mobile.logic.FineCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Member home: overdue banner, stat cards, notifications and the list of current loans (Figma Figure 13). */
class MemberHomeActivity : BaseActivity() {

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val user = currentUser() ?: return
        val settings = repo.settings(user.id)
        val loans = repo.activeLoans(user.id)
        val now = System.currentTimeMillis()
        val overdue = loans.count { it.dueAt < now }
        val finesDue = repo.finesDue(user.id)
        val unread = repo.unreadCount(user.id)

        val c = vbox(20)

        val top = hbox()
        val names = vbox()
        names.addView(title("Hi, ${user.firstName}"))
        names.addView(muted("Member since " + SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(user.createdAt))))
        top.addView(names, lp(0, WRAP, 1f))
        val avatar = label(user.initial, 16f, Palette.primary, true).apply {
            gravity = Gravity.CENTER
            background = shape(Palette.chip, 22)
            setOnClickListener { startActivity(Intent(this@MemberHomeActivity, ProfileActivity::class.java)) }
        }
        top.addView(avatar, lp(dp(44), dp(44)))
        c.addView(top, lp(MATCH, WRAP, 0f, 0, 0, 0, 14))

        if (overdue > 0) {
            val banner = label(
                if (overdue == 1) "1 book overdue — tap to view" else "$overdue books overdue — tap to view",
                14f, Palette.danger, true
            ).apply {
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = shape(Palette.dangerBg, 12, Palette.danger)
                setOnClickListener { startActivity(Intent(this@MemberHomeActivity, LoansActivity::class.java)) }
            }
            c.addView(banner, lp(MATCH, WRAP, 0f, 0, 0, 0, 14))
        }

        val stats = hbox().apply { gravity = Gravity.TOP }
        stats.addView(statCard("Active Loans", loans.size.toString(), Palette.text), lp(0, WRAP, 1f, 0, 0, 6, 0))
        stats.addView(statCard("Fines Due", "R $finesDue", if (finesDue > 0) Palette.danger else Palette.text), lp(0, WRAP, 1f, 6, 0, 0, 0))
        c.addView(stats, lp(MATCH, WRAP, 0f, 0, 0, 0, 14))

        val notes = card().apply {
            setOnClickListener { startActivity(Intent(this@MemberHomeActivity, NotificationsActivity::class.java)) }
        }
        val noteRow = hbox()
        noteRow.addView(label("Notifications", 15f, Palette.text, true), lp(0, WRAP, 1f))
        if (unread > 0) noteRow.addView(pill("$unread new", Palette.primary, Palette.chip))
        else noteRow.addView(muted("All caught up"))
        notes.addView(noteRow)
        c.addView(notes, lp(MATCH, WRAP, 0f, 0, 0, 0, 18))

        c.addView(heading("Current Loans"), lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        if (loans.isEmpty()) {
            c.addView(muted("You have no books on loan. Use Search to find your next read."))
        }
        for (loan in loans) {
            val row = card().apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(cover(loan.bookTitle))
            val text = vbox()
            text.addView(label(loan.bookTitle, 15f, Palette.text, true))
            text.addView(muted("Due " + formatDate(loan.dueAt)))
            row.addView(text, lp(0, WRAP, 1f))
            row.addView(statusPill(FineCalculator.status(loan.dueAt, loan.returnedAt, now, settings.reminderDays)))
            c.addView(row, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        }
        setScreen(c, Tab.HOME)
    }

    private fun statCard(caption: String, value: String, valueColour: Int): View {
        val card = card()
        card.addView(muted(caption))
        card.addView(label(value, 26f, valueColour, true))
        return card
    }
}
