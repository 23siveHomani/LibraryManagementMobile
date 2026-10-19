package com.lms.mobile.data

enum class Role { MEMBER, LIBRARIAN }

data class User(
    val id: Long,
    val username: String,
    val fullName: String,
    val email: String,
    val role: Role,
    val authProvider: String,
    val createdAt: Long
) {
    val firstName: String get() = fullName.trim().substringBefore(' ')
    val initial: String get() = fullName.trim().firstOrNull()?.uppercase() ?: "?"
}

data class Book(
    val id: Long,
    val code: String,
    val title: String,
    val author: String,
    val genre: String,
    val totalCopies: Int,
    val availableCopies: Int
)

data class LoanView(
    val id: Long,
    val userId: Long,
    val username: String,
    val bookId: Long,
    val bookTitle: String,
    val author: String,
    val borrowedAt: Long,
    val dueAt: Long,
    val returnedAt: Long?
)

data class FineView(
    val id: Long,
    val userId: Long,
    val username: String,
    val amount: Int,
    val reason: String,
    val paid: Boolean,
    val createdAt: Long
)

data class Notice(
    val id: Long,
    val title: String,
    val message: String,
    val createdAt: Long,
    val isRead: Boolean
)

data class Settings(
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val reminderDays: Int = 2
)

/** Outcome of an action that the UI shows to the user as a message. */
data class ActionResult(val ok: Boolean, val message: String)
