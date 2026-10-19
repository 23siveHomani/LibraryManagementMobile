package com.lms.mobile.ui

import android.view.Gravity
import android.widget.LinearLayout
import com.lms.mobile.logic.FineCalculator

/** The member's loans: current (with a Return button) and history. */
class LoansActivity : BaseActivity() {
    private var showHistory = false

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val user = currentUser() ?: return
        val settings = repo.settings(user.id)
        val now = System.currentTimeMillis()
        val finesDue = repo.finesDue(user.id)

        val c = vbox(20)
        c.addView(title("My Loans"), lp(MATCH, WRAP, 0f, 0, 0, 0, 12))

        val summary = card()
        summary.addView(muted("Fines due"))
        summary.addView(label("R $finesDue", 22f, if (finesDue > 0) Palette.danger else Palette.text, true))
        if (finesDue > 0) summary.addView(muted("Late fees are R${com.lms.mobile.Config.FINE_PER_DAY_RAND} per day. Pay at the library desk."))
        c.addView(summary, lp(MATCH, WRAP, 0f, 0, 0, 0, 12))

        val tabs = hbox()
        tabs.addView(chip("Current", !showHistory) { showHistory = false; render() }, lp(WRAP, WRAP, 0f, 0, 0, 8, 0))
        tabs.addView(chip("History", showHistory) { showHistory = true; render() })
        c.addView(tabs, lp(MATCH, WRAP, 0f, 0, 0, 0, 12))

        val loans = if (showHistory) repo.loanHistory(user.id) else repo.activeLoans(user.id)
        if (loans.isEmpty()) {
            c.addView(muted(if (showHistory) "No returned books yet." else "No books on loan right now."))
        }
        for (loan in loans) {
            val row = card()
            val top = hbox()
            top.addView(cover(loan.bookTitle, 40, 54))
            val text = vbox()
            text.addView(label(loan.bookTitle, 15f, Palette.text, true))
            text.addView(muted(loan.author))
            top.addView(text, lp(0, WRAP, 1f))
            top.addView(statusPill(FineCalculator.status(loan.dueAt, loan.returnedAt, now, settings.reminderDays)))
            row.addView(top)

            row.addView(muted("Borrowed " + formatDate(loan.borrowedAt) + "  ·  Due " + formatDate(loan.dueAt)), lp(MATCH, WRAP, 0f, 0, 8, 0, 0))
            if (loan.returnedAt != null) {
                row.addView(muted("Returned " + formatDate(loan.returnedAt)))
            } else {
                val late = FineCalculator.daysOverdue(loan.dueAt, now)
                if (late > 0) {
                    row.addView(
                        label("$late days late — fine so far R${FineCalculator.fineForDays(late)}", 13f, Palette.danger, true),
                        lp(MATCH, WRAP, 0f, 0, 6, 0, 0)
                    )
                }
                row.addView(outlineButton("Return book") {
                    async({ repo.returnLoan(loan.id) }) { result ->
                        toast(result.message)
                        render()
                    }
                }, lp(MATCH, WRAP, 0f, 0, 10, 0, 0))
            }
            c.addView(row, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        }
        setScreen(c, Tab.LOANS)
    }
}
