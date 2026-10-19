package com.lms.mobile.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.lms.mobile.Config
import com.lms.mobile.logic.BookSearch
import com.lms.mobile.logic.FineCalculator
import com.lms.mobile.security.PasswordHasher

/**
 * All database access and business rules in one place.
 * Every query is parameterised ("?" placeholders), which prevents SQL injection.
 */
class Repo private constructor(private val helper: LmsDatabase) {

    companion object {
        @Volatile private var instance: Repo? = null

        fun get(context: Context): Repo =
            instance ?: synchronized(this) {
                instance ?: Repo(LmsDatabase(context.applicationContext)).also { instance = it }
            }
    }

    private val db: SQLiteDatabase get() = helper.writableDatabase

    /** Opens the database (creating and seeding it on the first run). Call from a background thread. */
    fun warmUp() {
        db.version
    }

    // ---------------------------------------------------------------- users

    private val userColumns = "id, username, full_name, email, role, auth_provider, created_at"

    private fun mapUser(c: Cursor) = User(
        id = c.getLong(0),
        username = c.getString(1),
        fullName = c.getString(2),
        email = c.getString(3),
        role = Role.valueOf(c.getString(4)),
        authProvider = c.getString(5),
        createdAt = c.getLong(6)
    )

    fun getUser(id: Long): User? =
        db.rawQuery("SELECT $userColumns FROM users WHERE id = ?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) mapUser(it) else null
        }

    /** Returns the user when the username (or email) and password are correct, otherwise null. */
    fun login(identifier: String, password: String): User? {
        val id = identifier.trim()
        db.rawQuery(
            "SELECT $userColumns, password_hash FROM users WHERE username = ? OR email = ?",
            arrayOf(id, id)
        ).use {
            if (it.moveToFirst() && PasswordHasher.verify(password, it.getString(7))) return mapUser(it)
        }
        return null
    }

    fun register(username: String, fullName: String, email: String, password: String): ActionResult {
        val u = username.trim()
        val e = email.trim()
        if (exists("SELECT 1 FROM users WHERE username = ?", u)) return ActionResult(false, "That username is already taken")
        if (exists("SELECT 1 FROM users WHERE email = ?", e)) return ActionResult(false, "That email is already registered")
        val hash = PasswordHasher.encode(password)
        createUser(u, fullName.trim(), e, hash, "LOCAL")
        return ActionResult(true, "Account created")
    }

    private fun createUser(username: String, fullName: String, email: String, hash: String, provider: String): Long {
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            val id = db.insertOrThrow("users", null, ContentValues().apply {
                put("username", username)
                put("full_name", fullName)
                put("email", email)
                put("password_hash", hash)
                put("role", Role.MEMBER.name)
                put("auth_provider", provider)
                put("created_at", now)
            })
            db.insertOrThrow("user_settings", null, ContentValues().apply { put("user_id", id) })
            db.insertOrThrow("notifications", null, ContentValues().apply {
                put("user_id", id)
                put("title", "Welcome")
                put("message", "Welcome to LMS Mobile. Search for a book to get started.")
                put("created_at", now)
            })
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    /** Single sign-on: find the account for a Google email, or create a member account for it. */
    fun signInWithGoogle(email: String, displayName: String?): User {
        val e = email.trim()
        db.rawQuery("SELECT $userColumns FROM users WHERE email = ?", arrayOf(e)).use {
            if (it.moveToFirst()) return mapUser(it)
        }
        var base = e.substringBefore('@').lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '_' }
        if (base.length < 4) base = base + "user"
        base = base.take(16)
        var candidate = base
        var n = 1
        while (exists("SELECT 1 FROM users WHERE username = ?", candidate)) {
            candidate = base + n
            n++
        }
        val id = createUser(candidate, displayName?.takeIf { it.isNotBlank() } ?: candidate, e, "GOOGLE", "GOOGLE")
        return getUser(id)!!
    }

    fun updateProfile(userId: Long, fullName: String, email: String): ActionResult {
        val e = email.trim()
        if (exists("SELECT 1 FROM users WHERE email = ? AND id <> ?", e, userId.toString()))
            return ActionResult(false, "That email is used by another account")
        db.update("users", ContentValues().apply {
            put("full_name", fullName.trim())
            put("email", e)
        }, "id = ?", arrayOf(userId.toString()))
        return ActionResult(true, "Profile updated")
    }

    fun changePassword(userId: Long, current: String, new: String): ActionResult {
        val stored = db.rawQuery("SELECT password_hash FROM users WHERE id = ?", arrayOf(userId.toString())).use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: return ActionResult(false, "Account not found")
        if (!PasswordHasher.verify(current, stored)) return ActionResult(false, "Current password is incorrect")
        setPassword(userId, new)
        return ActionResult(true, "Password changed")
    }

    /** Simulated "forgot password": allowed when the username and registered email both match. */
    fun resetPassword(username: String, email: String, new: String): ActionResult {
        val id = db.rawQuery(
            "SELECT id FROM users WHERE username = ? AND email = ? AND auth_provider = 'LOCAL'",
            arrayOf(username.trim(), email.trim())
        ).use { if (it.moveToFirst()) it.getLong(0) else null }
            ?: return ActionResult(false, "No account matches that username and email")
        setPassword(id, new)
        return ActionResult(true, "Password reset. You can log in now")
    }

    private fun setPassword(userId: Long, password: String) {
        db.update("users", ContentValues().apply { put("password_hash", PasswordHasher.encode(password)) },
            "id = ?", arrayOf(userId.toString()))
    }

    fun findMembers(text: String): List<User> {
        val like = "%" + BookSearch.escapeLike(text.trim()) + "%"
        val out = mutableListOf<User>()
        db.rawQuery(
            "SELECT $userColumns FROM users WHERE role = 'MEMBER' AND " +
                "(username LIKE ? ESCAPE '\\' OR full_name LIKE ? ESCAPE '\\' OR email LIKE ? ESCAPE '\\') " +
                "ORDER BY full_name COLLATE NOCASE",
            arrayOf(like, like, like)
        ).use { while (it.moveToNext()) out.add(mapUser(it)) }
        return out
    }

    // ---------------------------------------------------------------- books

    private fun mapBook(c: Cursor) = Book(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getInt(5), c.getInt(6))

    fun genres(): List<String> {
        val out = mutableListOf<String>()
        db.rawQuery("SELECT DISTINCT genre FROM books ORDER BY genre", null).use { while (it.moveToNext()) out.add(it.getString(0)) }
        return out
    }

    fun searchBooks(text: String, genre: String?, onlyAvailable: Boolean): List<Book> {
        val q = BookSearch.build(text, genre, onlyAvailable)
        val out = mutableListOf<Book>()
        db.rawQuery(q.sql, q.args.toTypedArray()).use { while (it.moveToNext()) out.add(mapBook(it)) }
        return out
    }

    // ---------------------------------------------------------------- loans

    private val loanSelect =
        "SELECT l.id, l.user_id, u.username, l.book_id, b.title, b.author, l.borrowed_at, l.due_at, l.returned_at " +
            "FROM loans l JOIN users u ON u.id = l.user_id JOIN books b ON b.id = l.book_id "

    private fun mapLoan(c: Cursor) = LoanView(
        c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getString(4), c.getString(5),
        c.getLong(6), c.getLong(7), if (c.isNull(8)) null else c.getLong(8)
    )

    fun activeLoans(userId: Long): List<LoanView> = loans("WHERE l.user_id = ? AND l.returned_at IS NULL ORDER BY l.due_at", userId)

    fun loanHistory(userId: Long): List<LoanView> = loans("WHERE l.user_id = ? AND l.returned_at IS NOT NULL ORDER BY l.returned_at DESC", userId)

    private fun loans(tail: String, userId: Long): List<LoanView> {
        val out = mutableListOf<LoanView>()
        db.rawQuery(loanSelect + tail, arrayOf(userId.toString())).use { while (it.moveToNext()) out.add(mapLoan(it)) }
        return out
    }

    fun borrow(userId: Long, bookId: Long): ActionResult {
        db.beginTransaction()
        try {
            val available = longOf("SELECT available_copies FROM books WHERE id = ?", bookId.toString())
                ?: return ActionResult(false, "Book not found")
            if (available <= 0) return ActionResult(false, "No copies are available right now")
            if (exists("SELECT 1 FROM loans WHERE user_id = ? AND book_id = ? AND returned_at IS NULL", userId.toString(), bookId.toString()))
                return ActionResult(false, "You already have this book on loan")
            val active = longOf("SELECT COUNT(*) FROM loans WHERE user_id = ? AND returned_at IS NULL", userId.toString()) ?: 0L
            if (active >= Config.MAX_ACTIVE_LOANS) return ActionResult(false, "Loan limit reached (${Config.MAX_ACTIVE_LOANS} books)")
            val now = System.currentTimeMillis()
            val due = now + Config.LOAN_DAYS * FineCalculator.DAY_MS
            db.insertOrThrow("loans", null, ContentValues().apply {
                put("user_id", userId)
                put("book_id", bookId)
                put("borrowed_at", now)
                put("due_at", due)
            })
            db.execSQL("UPDATE books SET available_copies = available_copies - 1 WHERE id = ?", arrayOf<Any>(bookId))
            db.setTransactionSuccessful()
            return ActionResult(true, "Borrowed. Due in ${Config.LOAN_DAYS} days")
        } finally {
            db.endTransaction()
        }
    }

    fun returnLoan(loanId: Long): ActionResult {
        db.beginTransaction()
        try {
            val row = db.rawQuery(
                "SELECT user_id, book_id, due_at FROM loans WHERE id = ? AND returned_at IS NULL",
                arrayOf(loanId.toString())
            ).use { if (it.moveToFirst()) Triple(it.getLong(0), it.getLong(1), it.getLong(2)) else null }
                ?: return ActionResult(false, "This loan was already returned")
            val now = System.currentTimeMillis()
            db.update("loans", ContentValues().apply { put("returned_at", now) }, "id = ?", arrayOf(loanId.toString()))
            db.execSQL("UPDATE books SET available_copies = available_copies + 1 WHERE id = ?", arrayOf<Any>(row.second))
            val days = FineCalculator.daysOverdue(row.third, now)
            val fine = FineCalculator.fineForDays(days)
            if (fine > 0) {
                db.insertOrThrow("fines", null, ContentValues().apply {
                    put("user_id", row.first)
                    put("loan_id", loanId)
                    put("amount", fine)
                    put("reason", "Late return: $days days")
                    put("created_at", now)
                })
            }
            db.setTransactionSuccessful()
            return ActionResult(true, if (fine > 0) "Returned. Late fine of R$fine added ($days days)" else "Returned on time. Thank you")
        } finally {
            db.endTransaction()
        }
    }

    /** Librarian: issue a book to a member by username and book code. */
    fun issueLoan(memberUsername: String, bookCode: String): ActionResult {
        val memberId = longOf("SELECT id FROM users WHERE username = ? AND role = 'MEMBER'", memberUsername.trim())
            ?: return ActionResult(false, "No member with that username")
        val bookId = longOf("SELECT id FROM books WHERE code = ? COLLATE NOCASE", bookCode.trim())
            ?: return ActionResult(false, "No book with that code")
        return borrow(memberId, bookId)
    }

    // ---------------------------------------------------------------- fines

    /** Unpaid fines plus the late fee that has built up on books that are still out and overdue. */
    fun finesDue(userId: Long): Int {
        val unpaid = longOf("SELECT COALESCE(SUM(amount), 0) FROM fines WHERE user_id = ? AND paid = 0", userId.toString()) ?: 0L
        val now = System.currentTimeMillis()
        val accrued = activeLoans(userId).sumOf { FineCalculator.fine(it.dueAt, now) }
        return unpaid.toInt() + accrued
    }

    fun unpaidFines(): List<FineView> {
        val out = mutableListOf<FineView>()
        db.rawQuery(
            "SELECT f.id, f.user_id, u.username, f.amount, f.reason, f.paid, f.created_at " +
                "FROM fines f JOIN users u ON u.id = f.user_id WHERE f.paid = 0 ORDER BY f.created_at DESC", null
        ).use {
            while (it.moveToNext()) out.add(FineView(it.getLong(0), it.getLong(1), it.getString(2), it.getInt(3), it.getString(4), it.getInt(5) == 1, it.getLong(6)))
        }
        return out
    }

    fun markFinePaid(fineId: Long): ActionResult {
        val n = db.update("fines", ContentValues().apply { put("paid", 1) }, "id = ? AND paid = 0", arrayOf(fineId.toString()))
        return if (n > 0) ActionResult(true, "Fine marked as paid") else ActionResult(false, "Fine not found")
    }

    // ---------------------------------------------------------------- notifications and settings

    fun notifications(userId: Long): List<Notice> {
        val out = mutableListOf<Notice>()
        db.rawQuery(
            "SELECT id, title, message, created_at, is_read FROM notifications WHERE user_id = ? ORDER BY created_at DESC",
            arrayOf(userId.toString())
        ).use { while (it.moveToNext()) out.add(Notice(it.getLong(0), it.getString(1), it.getString(2), it.getLong(3), it.getInt(4) == 1)) }
        return out
    }

    fun unreadCount(userId: Long): Int =
        (longOf("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0", userId.toString()) ?: 0L).toInt()

    fun markAllRead(userId: Long) {
        db.update("notifications", ContentValues().apply { put("is_read", 1) }, "user_id = ?", arrayOf(userId.toString()))
    }

    fun settings(userId: Long): Settings =
        db.rawQuery("SELECT dark_mode, notifications_enabled, reminder_days FROM user_settings WHERE user_id = ?", arrayOf(userId.toString())).use {
            if (it.moveToFirst()) Settings(it.getInt(0) == 1, it.getInt(1) == 1, it.getInt(2)) else Settings()
        }

    fun saveSettings(userId: Long, s: Settings) {
        val cv = ContentValues().apply {
            put("user_id", userId)
            put("dark_mode", if (s.darkMode) 1 else 0)
            put("notifications_enabled", if (s.notificationsEnabled) 1 else 0)
            put("reminder_days", s.reminderDays)
        }
        db.insertWithOnConflict("user_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ---------------------------------------------------------------- helpers

    private fun exists(sql: String, vararg args: String): Boolean =
        db.rawQuery(sql, args.toList().toTypedArray()).use { it.moveToFirst() }

    private fun longOf(sql: String, vararg args: String): Long? =
        db.rawQuery(sql, args.toList().toTypedArray()).use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }
}
