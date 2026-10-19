package com.lms.mobile.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.lms.mobile.SessionManager
import com.lms.mobile.data.Repo
import com.lms.mobile.data.User

/** Splash screen from the Part 1 sitemap. Opens (and on first run seeds) the database, then routes to Login or Home. */
class SplashActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = vbox(24).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.primaryDark, Palette.primary))
        }
        root.addView(brandLogo(88))
        root.addView(label("LMS Mobile", 28f, 0xFFFFFFFF.toInt(), true).apply { gravity = Gravity.CENTER }, lp(MATCH, WRAP, 0f, 0, 16, 0, 4))
        root.addView(label("Your library, in your pocket", 14f, 0xFFDCE8F7.toInt()).apply { gravity = Gravity.CENTER })
        root.addView(label("Setting things up…", 12f, 0xFFDCE8F7.toInt()).apply { gravity = Gravity.CENTER }, lp(MATCH, WRAP, 0f, 0, 40, 0, 0))
        setContentView(root)

        val started = System.currentTimeMillis()
        val appContext = applicationContext
        async({
            val repo = Repo.get(appContext)
            repo.warmUp()
            val session = SessionManager(appContext)
            val user: User? = if (session.isSignedIn) repo.getUser(session.userId) else null
            val wait = 900L - (System.currentTimeMillis() - started)
            if (wait > 0) Thread.sleep(wait)
            user
        }, fail = { openLogin() }) { user ->
            if (user == null) openLogin() else {
                startActivity(homeIntentFor(user))
                finish()
            }
        }
    }

    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

/** White circle with three "book" lines, used on the splash and login screens. */
fun android.content.Context.brandLogo(sizeDp: Int): View {
    val circle = vbox().apply {
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFFFFFFFF.toInt())
        }
    }
    val barWidths = intArrayOf(sizeDp / 2, sizeDp / 2, sizeDp / 3)
    for (w in barWidths) {
        val bar = View(this)
        bar.background = shape(Palette.primary, 3)
        circle.addView(bar, LinearLayout.LayoutParams(dp(w), dp(sizeDp / 12)).apply { setMargins(0, dp(2), 0, dp(2)) })
    }
    circle.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    return circle
}
