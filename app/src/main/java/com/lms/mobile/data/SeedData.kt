package com.lms.mobile.data

import com.lms.mobile.logic.FineCalculator.DAY_MS

/**
 * Simulated data loaded the first time the app runs. Every table has at least ten records.
 * Dates are stored relative to "now" so the overdue / due-soon states are always realistic.
 * All demo accounts use the password in Config.DEMO_PASSWORD.
 */
object SeedData {
    data class U(val id: Long, val username: String, val fullName: String, val email: String, val role: Role)
    data class B(val id: Long, val code: String, val title: String, val author: String, val genre: String, val copies: Int)
    /** [dueInDays] is negative when the loan is already overdue. */
    data class L(val id: Long, val userId: Long, val bookId: Long, val borrowedDaysAgo: Int, val dueInDays: Int, val returnedDaysAgo: Int?)
    data class F(val id: Long, val userId: Long, val loanId: Long?, val amount: Int, val reason: String, val paid: Boolean, val daysAgo: Int)
    data class N(val id: Long, val userId: Long, val title: String, val message: String, val daysAgo: Int, val read: Boolean)
    data class S(val userId: Long, val dark: Boolean, val notifications: Boolean, val reminderDays: Int)

    val users = listOf(
        U(1, "ahomani", "Asive Homani", "ahomani@lms.example", Role.LIBRARIAN),
        U(2, "lerato.m", "Lerato Mkhize", "lerato.mkhize@lms.example", Role.LIBRARIAN),
        U(3, "naledi", "Naledi Dlamini", "naledi.dlamini@lms.example", Role.MEMBER),
        U(4, "thabo.m", "Thabo Mokoena", "thabo.mokoena@lms.example", Role.MEMBER),
        U(5, "zanele", "Zanele Khumalo", "zanele.khumalo@lms.example", Role.MEMBER),
        U(6, "sipho.n", "Sipho Ndlovu", "sipho.ndlovu@lms.example", Role.MEMBER),
        U(7, "ayanda", "Ayanda Jacobs", "ayanda.jacobs@lms.example", Role.MEMBER),
        U(8, "kagiso", "Kagiso Molefe", "kagiso.molefe@lms.example", Role.MEMBER),
        U(9, "nomsa", "Nomsa Zulu", "nomsa.zulu@lms.example", Role.MEMBER),
        U(10, "pieter.v", "Pieter van Wyk", "pieter.vanwyk@lms.example", Role.MEMBER),
        U(11, "fatima", "Fatima Patel", "fatima.patel@lms.example", Role.MEMBER),
        U(12, "lwazi", "Lwazi Sithole", "lwazi.sithole@lms.example", Role.MEMBER)
    )

    val books = listOf(
        B(1, "LMS-001", "Things Fall Apart", "Chinua Achebe", "Fiction", 3),
        B(2, "LMS-002", "Sapiens", "Yuval Noah Harari", "History", 1),
        B(3, "LMS-003", "Atomic Habits", "James Clear", "Self-help", 2),
        B(4, "LMS-004", "Dune", "Frank Herbert", "Science fiction", 1),
        B(5, "LMS-005", "Long Walk to Freedom", "Nelson Mandela", "Biography", 1),
        B(6, "LMS-006", "Cry, the Beloved Country", "Alan Paton", "Fiction", 2),
        B(7, "LMS-007", "Born a Crime", "Trevor Noah", "Biography", 2),
        B(8, "LMS-008", "The Alchemist", "Paulo Coelho", "Fiction", 3),
        B(9, "LMS-009", "Clean Code", "Robert C. Martin", "Technology", 2),
        B(10, "LMS-010", "The Pragmatic Programmer", "Andrew Hunt and David Thomas", "Technology", 2),
        B(11, "LMS-011", "A Brief History of Time", "Stephen Hawking", "Science", 2),
        B(12, "LMS-012", "Educated", "Tara Westover", "Biography", 2),
        B(13, "LMS-013", "Americanah", "Chimamanda Ngozi Adichie", "Fiction", 2),
        B(14, "LMS-014", "The Hobbit", "J.R.R. Tolkien", "Fantasy", 3),
        B(15, "LMS-015", "Thinking, Fast and Slow", "Daniel Kahneman", "Psychology", 2)
    )

    val loans = listOf(
        L(1, 3, 1, 2, 12, null),
        L(2, 3, 2, 19, -5, null),
        L(3, 3, 3, 1, 13, null),
        L(4, 4, 4, 10, 4, null),
        L(5, 5, 7, 30, -16, 12),
        L(6, 6, 9, 5, 9, null),
        L(7, 7, 5, 25, -11, null),
        L(8, 8, 14, 20, -6, 8),
        L(9, 9, 12, 40, -26, 20),
        L(10, 10, 8, 3, 11, null),
        L(11, 11, 13, 30, -16, 18),
        L(12, 12, 11, 8, 6, null),
        L(13, 3, 6, 45, -31, 27)
    )

    val fines = listOf(
        F(1, 3, 13, 20, "Late return: 4 days", false, 27),
        F(2, 5, 5, 20, "Late return: 4 days", true, 12),
        F(3, 9, 9, 30, "Late return: 6 days", true, 20),
        F(4, 4, null, 30, "Replacement library card", true, 60),
        F(5, 6, null, 50, "Damaged cover", false, 15),
        F(6, 10, null, 30, "Replacement library card", false, 9),
        F(7, 11, 11, 25, "Damaged pages", true, 18),
        F(8, 12, null, 20, "Late return: 4 days", true, 90),
        F(9, 9, null, 10, "Reservation not collected", true, 33),
        F(10, 5, null, 40, "Water damage", false, 5)
    )

    val notices = listOf(
        N(1, 3, "Book overdue", "Sapiens was due 5 days ago. Please return it to avoid more fines.", 0, false),
        N(2, 3, "Due soon", "Things Fall Apart is due in 12 days.", 1, false),
        N(3, 3, "Fine outstanding", "You have an unpaid fine of R20 for Cry, the Beloved Country.", 7, true),
        N(4, 3, "Welcome", "Welcome to LMS Mobile. Search for a book to get started.", 90, true),
        N(5, 4, "Due soon", "Dune is due in 4 days.", 0, false),
        N(6, 5, "Fine outstanding", "You have an unpaid fine of R40 for water damage.", 5, false),
        N(7, 6, "Fine outstanding", "You have an unpaid fine of R50 for a damaged cover.", 15, false),
        N(8, 7, "Book overdue", "Long Walk to Freedom was due 11 days ago.", 0, false),
        N(9, 8, "Thank you", "The Hobbit was returned on time.", 8, true),
        N(10, 10, "Fine outstanding", "You have an unpaid fine of R30 for a replacement library card.", 9, false),
        N(11, 11, "Thank you", "Americanah was returned on time.", 18, true),
        N(12, 12, "Due soon", "A Brief History of Time is due in 6 days.", 1, false)
    )

    val settings = listOf(
        S(1, false, true, 2), S(2, false, true, 2), S(3, false, true, 2), S(4, false, true, 3),
        S(5, false, true, 2), S(6, false, true, 1), S(7, false, false, 2), S(8, false, true, 2),
        S(9, false, true, 5), S(10, true, true, 2), S(11, false, true, 2), S(12, false, true, 2)
    )

    fun availableCopies(book: B): Int = book.copies - loans.count { it.bookId == book.id && it.returnedDaysAgo == null }

    fun daysToMillis(days: Int): Long = days * DAY_MS
}
