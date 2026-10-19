package com.lms.mobile.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.lms.mobile.Config
import com.lms.mobile.security.PasswordHasher

/** SQLite database (the app's DBMS). Creates the tables and loads the simulated records on first run. */
class LmsDatabase(context: Context) : SQLiteOpenHelper(context, Config.DB_NAME, null, Config.DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        Schema.CREATE_STATEMENTS.forEach { db.execSQL(it) }
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 only: nothing to migrate yet.
    }

    private fun seed(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        fun at(daysAgo: Int) = now - SeedData.daysToMillis(daysAgo)

        // All demo accounts share one password, so it is hashed once (600,000 iterations is slow by design).
        val demoHash = PasswordHasher.encode(Config.DEMO_PASSWORD)

        for (u in SeedData.users) {
            db.insertOrThrow("users", null, ContentValues().apply {
                put("id", u.id)
                put("username", u.username)
                put("full_name", u.fullName)
                put("email", u.email)
                put("password_hash", demoHash)
                put("role", u.role.name)
                put("auth_provider", "LOCAL")
                put("created_at", at(120))
            })
        }
        for (b in SeedData.books) {
            db.insertOrThrow("books", null, ContentValues().apply {
                put("id", b.id)
                put("code", b.code)
                put("title", b.title)
                put("author", b.author)
                put("genre", b.genre)
                put("total_copies", b.copies)
                put("available_copies", SeedData.availableCopies(b))
            })
        }
        for (l in SeedData.loans) {
            db.insertOrThrow("loans", null, ContentValues().apply {
                put("id", l.id)
                put("user_id", l.userId)
                put("book_id", l.bookId)
                put("borrowed_at", at(l.borrowedDaysAgo))
                put("due_at", now + SeedData.daysToMillis(l.dueInDays))
                if (l.returnedDaysAgo != null) put("returned_at", at(l.returnedDaysAgo)) else putNull("returned_at")
            })
        }
        for (f in SeedData.fines) {
            db.insertOrThrow("fines", null, ContentValues().apply {
                put("id", f.id)
                put("user_id", f.userId)
                if (f.loanId != null) put("loan_id", f.loanId) else putNull("loan_id")
                put("amount", f.amount)
                put("reason", f.reason)
                put("paid", if (f.paid) 1 else 0)
                put("created_at", at(f.daysAgo))
            })
        }
        for (n in SeedData.notices) {
            db.insertOrThrow("notifications", null, ContentValues().apply {
                put("id", n.id)
                put("user_id", n.userId)
                put("title", n.title)
                put("message", n.message)
                put("created_at", at(n.daysAgo))
                put("is_read", if (n.read) 1 else 0)
            })
        }
        for (s in SeedData.settings) {
            db.insertOrThrow("user_settings", null, ContentValues().apply {
                put("user_id", s.userId)
                put("dark_mode", if (s.dark) 1 else 0)
                put("notifications_enabled", if (s.notifications) 1 else 0)
                put("reminder_days", s.reminderDays)
            })
        }
    }
}
