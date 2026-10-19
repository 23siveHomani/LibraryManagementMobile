package com.lms.mobile.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.lms.mobile.logic.FineCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Colours for the light and dark themes (chosen in Profile > Settings). */
object Palette {
    var dark = false
    val bg get() = if (dark) 0xFF0F172A.toInt() else 0xFFF5F7FB.toInt()
    val card get() = if (dark) 0xFF1E293B.toInt() else 0xFFFFFFFF.toInt()
    val text get() = if (dark) 0xFFF1F5F9.toInt() else 0xFF0F172A.toInt()
    val muted get() = if (dark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
    val border get() = if (dark) 0xFF334155.toInt() else 0xFFE2E8F0.toInt()
    val field get() = if (dark) 0xFF0B1220.toInt() else 0xFFF8FAFC.toInt()
    val primary = 0xFF2B6CB9.toInt()
    val primaryDark = 0xFF1E3A5F.toInt()
    val chip get() = if (dark) 0xFF1E3A5F.toInt() else 0xFFE8F0FB.toInt()
    val danger = 0xFFB91C1C.toInt()
    val dangerBg get() = if (dark) 0xFF4C1D1D.toInt() else 0xFFFEE2E2.toInt()
    val success = 0xFF15803D.toInt()
    val successBg get() = if (dark) 0xFF14351F.toInt() else 0xFFDCFCE7.toInt()
    val warn = 0xFFB45309.toInt()
    val warnBg get() = if (dark) 0xFF4A3410.toInt() else 0xFFFEF3C7.toInt()
}

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun Context.shape(fill: Int, radiusDp: Int, stroke: Int? = null): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(fill)
    g.cornerRadius = dp(radiusDp).toFloat()
    if (stroke != null) g.setStroke(dp(1), stroke)
    return g
}

fun Context.lp(w: Int = MATCH, h: Int = WRAP, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0): LinearLayout.LayoutParams {
    val p = LinearLayout.LayoutParams(w, h, weight)
    p.setMargins(dp(l), dp(t), dp(r), dp(b))
    return p
}

fun Context.vbox(pad: Int = 0): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
}

fun Context.hbox(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

fun Context.label(text: String, sp: Float = 14f, color: Int = Palette.text, bold: Boolean = false): TextView =
    TextView(this).apply {
        this.text = text
        textSize = sp
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

fun Context.title(text: String): TextView = label(text, 22f, Palette.text, true)

fun Context.heading(text: String): TextView = label(text, 16f, Palette.text, true)

fun Context.muted(text: String, sp: Float = 12.5f): TextView = label(text, sp, Palette.muted)

fun Context.card(pad: Int = 14): LinearLayout = vbox(pad).apply { background = shape(Palette.card, 14, Palette.border) }

fun Context.pill(text: String, fg: Int, bg: Int): TextView = label(text, 11.5f, fg, true).apply {
    setPadding(dp(10), dp(3), dp(10), dp(3))
    background = shape(bg, 12)
}

fun Context.primaryButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
    this.text = text
    isAllCaps = true
    setTextColor(Color.WHITE)
    setTypeface(typeface, Typeface.BOLD)
    background = shape(Palette.primary, 12)
    stateListAnimator = null
    minHeight = dp(52)
    setOnClickListener { onClick() }
}

fun Context.outlineButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
    this.text = text
    isAllCaps = false
    setTextColor(Palette.primary)
    background = shape(Palette.card, 12, Palette.primary)
    stateListAnimator = null
    minHeight = dp(48)
    setOnClickListener { onClick() }
}

fun Context.dangerButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
    this.text = text
    isAllCaps = false
    setTextColor(Palette.danger)
    background = shape(Palette.card, 12, Palette.danger)
    stateListAnimator = null
    minHeight = dp(48)
    setOnClickListener { onClick() }
}

fun Context.linkText(text: String, onClick: () -> Unit): TextView = label(text, 14f, Palette.primary, true).apply {
    gravity = Gravity.CENTER
    setPadding(dp(8), dp(10), dp(8), dp(10))
    setOnClickListener { onClick() }
}

fun Context.chip(text: String, selected: Boolean, onClick: () -> Unit): TextView =
    label(text, 13f, if (selected) Color.WHITE else Palette.primary, true).apply {
        setPadding(dp(14), dp(8), dp(14), dp(8))
        background = shape(if (selected) Palette.primary else Palette.chip, 18)
        setOnClickListener { onClick() }
    }

fun Context.input(hint: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText = EditText(this).apply {
    this.hint = hint
    inputType = type
    setSingleLine(true)
    textSize = 15f
    setTextColor(Palette.text)
    setHintTextColor(Palette.muted)
    setPadding(dp(14), dp(12), dp(14), dp(12))
    background = shape(Palette.field, 12, Palette.border)
}

const val PASSWORD_TYPE = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
const val EMAIL_TYPE = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
const val NAME_TYPE = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS

/** Field caption above an input, e.g. "USERNAME". */
fun Context.caption(text: String): TextView = label(text.uppercase(), 11.5f, Palette.muted, true)

/** Coloured square used as a book-cover placeholder; the colour is derived from the title. */
fun Context.cover(title: String, sizeW: Int = 48, sizeH: Int = 64): View {
    val colours = intArrayOf(0xFFEF7B5E.toInt(), 0xFF4A9AA5.toInt(), 0xFFE0B13C.toInt(), 0xFF4D7DE0.toInt(), 0xFF8E6BBF.toInt(), 0xFF5BA86F.toInt())
    val v = View(this)
    v.background = shape(colours[Math.abs(title.hashCode()) % colours.size], 8)
    v.layoutParams = lp(dp(sizeW), dp(sizeH), 0f, 0, 0, 12, 0)
    return v
}

fun Activity.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

private val DATE_FORMAT = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
fun formatDate(millis: Long): String = DATE_FORMAT.format(Date(millis))

fun Context.statusPill(status: FineCalculator.Status): TextView = when (status) {
    FineCalculator.Status.OVERDUE -> pill("Overdue", Palette.danger, Palette.dangerBg)
    FineCalculator.Status.DUE_SOON -> pill("Due soon", Palette.warn, Palette.warnBg)
    FineCalculator.Status.ON_TIME -> pill("On time", Palette.success, Palette.successBg)
    FineCalculator.Status.RETURNED -> pill("Returned", Palette.muted, Palette.chip)
}

/** Runs [work] on a background thread, then [done] on the UI thread. Keeps hashing and database work off the UI thread. */
fun <T> Activity.async(work: () -> T, fail: (Throwable) -> Unit = { toast("Something went wrong. Please try again.") }, done: (T) -> Unit) {
    Thread {
        try {
            val result = work()
            runOnUiThread { if (!isFinishing) done(result) }
        } catch (t: Throwable) {
            runOnUiThread { if (!isFinishing) fail(t) }
        }
    }.start()
}
