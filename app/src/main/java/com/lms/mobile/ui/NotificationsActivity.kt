package com.lms.mobile.ui

/** List of notifications for the signed-in user. Unread items are highlighted, then marked as read. */
class NotificationsActivity : BaseActivity() {
    override fun onResume() {
        super.onResume()
        val user = currentUser() ?: return
        val notices = repo.notifications(user.id)
        val c = vbox(20)
        c.addView(backLink())
        c.addView(title("Notifications"), lp(MATCH, WRAP, 0f, 0, 4, 0, 12))
        if (notices.isEmpty()) c.addView(muted("You have no notifications."))
        for (n in notices) {
            val card = card().apply {
                if (!n.isRead) background = shape(Palette.chip, 14, Palette.primary)
            }
            card.addView(label(n.title, 15f, Palette.text, true))
            card.addView(label(n.message, 13.5f, Palette.text), lp(MATCH, WRAP, 0f, 0, 4, 0, 4))
            card.addView(muted(formatDate(n.createdAt)))
            c.addView(card, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        }
        setScreen(c, null)
        repo.markAllRead(user.id)
    }
}
