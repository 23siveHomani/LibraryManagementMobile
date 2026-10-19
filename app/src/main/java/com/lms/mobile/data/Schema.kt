package com.lms.mobile.data

/** DDL for the six SQLite tables. Executed once, the first time the database is created. */
object Schema {
    val CREATE_STATEMENTS: List<String> = listOf(
        """
        CREATE TABLE users (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            username      TEXT    NOT NULL UNIQUE COLLATE NOCASE,
            full_name     TEXT    NOT NULL,
            email         TEXT    NOT NULL UNIQUE COLLATE NOCASE,
            password_hash TEXT    NOT NULL,
            role          TEXT    NOT NULL CHECK (role IN ('MEMBER','LIBRARIAN')),
            auth_provider TEXT    NOT NULL DEFAULT 'LOCAL' CHECK (auth_provider IN ('LOCAL','GOOGLE')),
            created_at    INTEGER NOT NULL
        )
        """.trimIndent(),
        """
        CREATE TABLE books (
            id               INTEGER PRIMARY KEY AUTOINCREMENT,
            code             TEXT    NOT NULL UNIQUE,
            title            TEXT    NOT NULL,
            author           TEXT    NOT NULL,
            genre            TEXT    NOT NULL,
            total_copies     INTEGER NOT NULL CHECK (total_copies >= 0),
            available_copies INTEGER NOT NULL CHECK (available_copies >= 0)
        )
        """.trimIndent(),
        """
        CREATE TABLE loans (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id     INTEGER NOT NULL REFERENCES users(id),
            book_id     INTEGER NOT NULL REFERENCES books(id),
            borrowed_at INTEGER NOT NULL,
            due_at      INTEGER NOT NULL,
            returned_at INTEGER
        )
        """.trimIndent(),
        """
        CREATE TABLE fines (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id    INTEGER NOT NULL REFERENCES users(id),
            loan_id    INTEGER REFERENCES loans(id),
            amount     INTEGER NOT NULL CHECK (amount > 0),
            reason     TEXT    NOT NULL,
            paid       INTEGER NOT NULL DEFAULT 0 CHECK (paid IN (0,1)),
            created_at INTEGER NOT NULL
        )
        """.trimIndent(),
        """
        CREATE TABLE notifications (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id    INTEGER NOT NULL REFERENCES users(id),
            title      TEXT    NOT NULL,
            message    TEXT    NOT NULL,
            created_at INTEGER NOT NULL,
            is_read    INTEGER NOT NULL DEFAULT 0 CHECK (is_read IN (0,1))
        )
        """.trimIndent(),
        """
        CREATE TABLE user_settings (
            user_id               INTEGER PRIMARY KEY REFERENCES users(id),
            dark_mode             INTEGER NOT NULL DEFAULT 0 CHECK (dark_mode IN (0,1)),
            notifications_enabled INTEGER NOT NULL DEFAULT 1 CHECK (notifications_enabled IN (0,1)),
            reminder_days         INTEGER NOT NULL DEFAULT 2 CHECK (reminder_days BETWEEN 1 AND 7)
        )
        """.trimIndent(),
        "CREATE INDEX idx_loans_user ON loans(user_id)",
        "CREATE INDEX idx_loans_book ON loans(book_id)",
        "CREATE INDEX idx_fines_user ON fines(user_id)",
        "CREATE INDEX idx_notifications_user ON notifications(user_id)"
    )
}
