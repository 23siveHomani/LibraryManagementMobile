package com.lms.mobile.ui

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import com.lms.mobile.SessionManager
import com.lms.mobile.data.Repo
import com.lms.mobile.data.Role
import com.lms.mobile.data.User

/** Applies the chosen light or dark palette to the window before any view is built. */
open class ThemedActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Palette.dark = SessionManager(this).darkMode
        window.decorView.setBackgroundColor(Palette.bg)
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = Palette.bg
            window.navigationBarColor = Palette.bg
            window.decorView.systemUiVisibility =
                if (Palette.dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    /** Wraps [content] in a scroll view; the soft keyboard resizes the window so fields stay visible. */
    protected fun showScrollable(content: View, bottomBar: View? = null) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
        root.addView(scroll, lp(MATCH, 0, 1f))
        if (bottomBar != null) root.addView(bottomBar, lp(MATCH, WRAP))
        setContentView(root)
    }

    protected fun homeIntentFor(user: User): Intent =
        Intent(this, if (user.role == Role.LIBRARIAN) LibrarianActivity::class.java else MemberHomeActivity::class.java)
}

enum class Tab(val title: String) { HOME("Home"), SEARCH("Search"), LOANS("Loans"), PROFILE("Profile") }

/** Base class for screens that need a signed-in user; also draws the bottom tab bar from the Part 1 wireframes. */
abstract class BaseActivity : ThemedActivity() {
    protected lateinit var session: SessionManager
    protected lateinit var repo: Repo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        repo = Repo.get(this)
    }

    /** The signed-in user, or null after sending the person back to the login screen. */
    protected fun currentUser(): User? {
        val user = if (session.isSignedIn) repo.getUser(session.userId) else null
        if (user == null) {
            session.signOut()
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
        return user
    }

    protected fun setScreen(content: View, tab: Tab?) {
        showScrollable(content, if (tab != null) bottomNav(tab) else null)
    }

    private fun bottomNav(selected: Tab): View {
        val bar = hbox().apply {
            background = shape(Palette.card, 0, Palette.border)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        for (tab in Tab.values()) {
            val colour = if (tab == selected) Palette.primary else Palette.muted
            val item = vbox().apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setOnClickListener { go(tab, selected) }
            }
            val icon = View(this)
            val g = GradientDrawable()
            g.cornerRadius = dp(6).toFloat()
            g.setStroke(dp(2), colour)
            g.setColor(Palette.card)
            icon.background = g
            item.addView(icon, lp(dp(24), dp(24), 0f, 0, 0, 0, 2))
            item.addView(label(tab.title, 11.5f, colour, tab == selected))
            bar.addView(item, lp(0, WRAP, 1f))
        }
        return bar
    }

    private fun go(target: Tab, current: Tab) {
        if (target == current) return
        val cls = when (target) {
            Tab.HOME -> MemberHomeActivity::class.java
            Tab.SEARCH -> SearchActivity::class.java
            Tab.LOANS -> LoansActivity::class.java
            Tab.PROFILE -> ProfileActivity::class.java
        }
        val intent = Intent(this, cls).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (target == Tab.HOME) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        if (current != Tab.HOME && target != Tab.HOME) finish()
    }

    protected fun backLink(): View = linkText("‹ Back") { finish() }.apply { gravity = Gravity.START }
}
