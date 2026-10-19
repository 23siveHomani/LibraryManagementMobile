package com.lms.mobile.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.TextView
import com.lms.mobile.Config

/** Help system: expandable frequently asked questions and a link to email support. */
class HelpActivity : BaseActivity() {

    private val faqs = listOf(
        "How do I create an account?" to
            "On the login screen choose \"Create a new account\". Enter your name, a username, your email and a strong password (8+ characters with upper case, lower case, a number and a symbol).",
        "How do I sign in with Google?" to
            "Tap \"Continue with Google\" on the login screen and pick your Google account. The first time you do this, a member account is created for you automatically.",
        "How do I find and borrow a book?" to
            "Open the Search tab, type a title, author or code, and use the Genre and Available filters. Tap a book and choose Borrow. Loans last ${Config.LOAN_DAYS} days and you can have up to ${Config.MAX_ACTIVE_LOANS} books at a time.",
        "How are late fees worked out?" to
            "A fine of R${Config.FINE_PER_DAY_RAND} is charged for every full day a book is late, up to a maximum of R${Config.FINE_CAP_RAND} per book. Fines are paid at the library desk.",
        "How do I return a book?" to
            "Open the Loans tab and tap Return book on the loan. If the book is late, the fine is added to your account.",
        "I forgot my password" to
            "On the login screen tap \"Forgot password?\". Enter your username, your registered email and a new password.",
        "How do I change my settings?" to
            "Open the Profile tab. You can switch dark mode on or off, turn reminders on or off, and choose how many days before the due date you are reminded. Tap Save settings.",
        "Why was I locked out of the login screen?" to
            "After ${Config.MAX_FAILED_LOGINS} wrong passwords in a row the login form locks for ${Config.LOCKOUT_SECONDS} seconds to protect your account."
    )

    override fun onResume() {
        super.onResume()
        if (currentUser() == null) return
        val c = vbox(20)
        c.addView(backLink())
        c.addView(title("Help and support"), lp(MATCH, WRAP, 0f, 0, 4, 0, 4))
        c.addView(muted("Tap a question to see the answer."), lp(MATCH, WRAP, 0f, 0, 0, 0, 14))

        for ((question, answer) in faqs) {
            val card = card()
            val q = label("▸  $question", 15f, Palette.text, true)
            val a: TextView = label(answer, 14f, Palette.text).apply { visibility = View.GONE }
            card.addView(q)
            card.addView(a, lp(MATCH, WRAP, 0f, 0, 8, 0, 0))
            card.setOnClickListener {
                val open = a.visibility == View.VISIBLE
                a.visibility = if (open) View.GONE else View.VISIBLE
                q.text = (if (open) "▸  " else "▾  ") + question
            }
            c.addView(card, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        }

        c.addView(primaryButton("Email support") {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Config.SUPPORT_EMAIL))
                .putExtra(Intent.EXTRA_SUBJECT, "LMS Mobile support request")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                toast("No email app found. Write to ${Config.SUPPORT_EMAIL}")
            }
        }, lp(MATCH, WRAP, 0f, 0, 8, 0, 8))
        c.addView(muted("LMS Mobile version 1.0", 11.5f).apply { gravity = android.view.Gravity.CENTER })
        setScreen(c, null)
    }
}
