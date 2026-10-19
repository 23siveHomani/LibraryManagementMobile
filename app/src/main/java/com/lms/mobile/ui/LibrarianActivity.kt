package com.lms.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.lms.mobile.data.User
import com.lms.mobile.logic.Validators

/**
 * Librarian console (Part 1 sitemap): issue a loan, look up members (and return their books) and review unpaid fines.
 * Camera barcode scanning is not included in this version; book codes are typed in.
 */
class LibrarianActivity : BaseActivity() {
    private enum class Panel(val title: String) { ISSUE("Issue loan"), MEMBERS("Members"), FINES("Fines") }

    private var panel = Panel.ISSUE
    private var memberQuery = ""
    private var user: User? = null
    private lateinit var body: LinearLayout
    private lateinit var segments: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = currentUser() ?: return

        val c = vbox(20)
        val top = hbox()
        val names = vbox()
        names.addView(title("Librarian Console"))
        names.addView(muted("Signed in as ${user!!.fullName}"))
        top.addView(names, lp(0, WRAP, 1f))
        top.addView(label(user!!.initial, 16f, Palette.primary, true).apply {
            gravity = Gravity.CENTER
            background = shape(Palette.chip, 22)
            setOnClickListener { startActivity(Intent(this@LibrarianActivity, ProfileActivity::class.java)) }
        }, lp(dp(44), dp(44)))
        c.addView(top, lp(MATCH, WRAP, 0f, 0, 0, 0, 14))

        segments = hbox()
        c.addView(segments, lp(MATCH, WRAP, 0f, 0, 0, 0, 14))
        body = vbox()
        c.addView(body)
        setScreen(c, null)
        draw()
    }

    private fun draw() {
        segments.removeAllViews()
        for (p in Panel.values()) {
            segments.addView(chip(p.title, p == panel) { panel = p; draw() }, lp(WRAP, WRAP, 0f, 0, 0, 8, 0))
        }
        body.removeAllViews()
        when (panel) {
            Panel.ISSUE -> drawIssue()
            Panel.MEMBERS -> drawMembers()
            Panel.FINES -> drawFines()
        }
    }

    // ------------------------------------------------------------ issue a loan
    private fun drawIssue() {
        val card = card()
        card.addView(heading("Issue a book"), lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        val member = input("Member username, e.g. naledi")
        val code = input("Book code, e.g. LMS-001")
        val message: TextView = label("", 13.5f, Palette.success, true).apply { visibility = android.view.View.GONE }
        card.addView(caption("Member username"))
        card.addView(member, lp(MATCH, WRAP, 0f, 0, 6, 0, 12))
        card.addView(caption("Book code"))
        card.addView(code, lp(MATCH, WRAP, 0f, 0, 6, 0, 12))
        card.addView(message, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        card.addView(primaryButton("Issue loan") {
            message.visibility = android.view.View.GONE
            val m = Validators.username(member.text.toString())
            val b = Validators.bookCode(code.text.toString())
            member.error = if (m.ok) null else m.message
            code.error = if (b.ok) null else b.message
            if (m.ok && b.ok) {
                val memberName = member.text.toString()
                val bookCode = code.text.toString()
                async({ repo.issueLoan(memberName, bookCode) }) { result ->
                    message.text = result.message
                    message.setTextColor(if (result.ok) Palette.success else Palette.danger)
                    message.visibility = android.view.View.VISIBLE
                    if (result.ok) code.text.clear()
                }
            }
        })
        body.addView(card)
        body.addView(muted("Tip: the book code is on the spine label. A camera scanner is planned for a later release."), lp(MATCH, WRAP, 0f, 0, 12, 0, 0))
    }

    // ------------------------------------------------------------ member lookup
    private fun drawMembers() {
        val search: EditText = input("Search members by name, username or email").apply { setText(memberQuery) }
        val list = vbox()
        body.addView(search, lp(MATCH, WRAP, 0f, 0, 0, 0, 12))
        body.addView(list)
        fun fill() {
            list.removeAllViews()
            val members = repo.findMembers(memberQuery)
            if (members.isEmpty()) list.addView(muted("No members match your search."))
            for (m in members) {
                val card = card()
                val loans = repo.activeLoans(m.id)
                val fines = repo.finesDue(m.id)
                card.addView(label(m.fullName, 15f, Palette.text, true))
                card.addView(muted("@${m.username} · ${m.email}"))
                card.addView(
                    muted("${loans.size} active loan(s) · fines due R$fines"),
                    lp(MATCH, WRAP, 0f, 0, 4, 0, 6)
                )
                for (loan in loans) {
                    val row = hbox()
                    row.addView(muted(loan.bookTitle + " (due " + formatDate(loan.dueAt) + ")"), lp(0, WRAP, 1f))
                    row.addView(chip("Return", false) {
                        async({ repo.returnLoan(loan.id) }) { result ->
                            toast(result.message)
                            fill()
                        }
                    })
                    card.addView(row, lp(MATCH, WRAP, 0f, 0, 4, 0, 0))
                }
                list.addView(card, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
            }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                memberQuery = s?.toString() ?: ""
                fill()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        fill()
    }

    // ------------------------------------------------------------ fines overview
    private fun drawFines() {
        val list = vbox()
        body.addView(list)
        fun fill() {
            list.removeAllViews()
            val fines = repo.unpaidFines()
            list.addView(label("Outstanding: R${fines.sumOf { it.amount }} across ${fines.size} fine(s)", 15f, Palette.danger, true), lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
            if (fines.isEmpty()) list.addView(muted("There are no unpaid fines."))
            for (f in fines) {
                val card = card()
                val row = hbox()
                val text = vbox()
                text.addView(label("@${f.username}", 15f, Palette.text, true))
                text.addView(muted(f.reason + " · " + formatDate(f.createdAt)))
                row.addView(text, lp(0, WRAP, 1f))
                row.addView(label("R${f.amount}", 17f, Palette.danger, true), lp(WRAP, WRAP, 0f, 0, 0, 10, 0))
                card.addView(row)
                card.addView(outlineButton("Mark as paid") {
                    val result = repo.markFinePaid(f.id)
                    toast(result.message)
                    fill()
                }, lp(MATCH, WRAP, 0f, 0, 10, 0, 0))
                list.addView(card, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
            }
        }
        fill()
    }
}
