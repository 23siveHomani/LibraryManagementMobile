package com.lms.mobile.logic

/** Input validation used by the register, login, profile and librarian screens. */
object Validators {
    data class Result(val ok: Boolean, val message: String = "") {
        companion object {
            val OK = Result(true)
            fun fail(message: String) = Result(false, message)
        }
    }

    private val USERNAME = Regex("^[A-Za-z0-9_.]{4,20}$")
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val NAME = Regex("^[\\p{L}][\\p{L} '.-]{1,59}$")
    private val BOOK_CODE = Regex("^LMS-\\d{3}$", RegexOption.IGNORE_CASE)

    fun username(value: String): Result = when {
        value.isBlank() -> Result.fail("Enter a username")
        !USERNAME.matches(value) -> Result.fail("4 to 20 characters: letters, numbers, dot or underscore")
        else -> Result.OK
    }

    fun email(value: String): Result = when {
        value.isBlank() -> Result.fail("Enter your email address")
        value.length > 100 -> Result.fail("Email address is too long")
        !EMAIL.matches(value) -> Result.fail("Enter a valid email address, e.g. name@example.com")
        else -> Result.OK
    }

    fun fullName(value: String): Result = when {
        value.isBlank() -> Result.fail("Enter your full name")
        !NAME.matches(value.trim()) -> Result.fail("Use letters only (2 to 60 characters)")
        else -> Result.OK
    }

    /** At least 8 characters with an upper-case letter, a lower-case letter, a digit and a symbol. */
    fun password(value: String): Result = when {
        value.length < 8 -> Result.fail("Password must be at least 8 characters")
        value.length > 64 -> Result.fail("Password must be 64 characters or fewer")
        value.none { it.isUpperCase() } -> Result.fail("Add at least one upper-case letter")
        value.none { it.isLowerCase() } -> Result.fail("Add at least one lower-case letter")
        value.none { it.isDigit() } -> Result.fail("Add at least one number")
        value.all { it.isLetterOrDigit() } -> Result.fail("Add at least one symbol, e.g. ! @ # $")
        else -> Result.OK
    }

    fun passwordsMatch(a: String, b: String): Result =
        if (a == b) Result.OK else Result.fail("Passwords do not match")

    fun bookCode(value: String): Result = when {
        value.isBlank() -> Result.fail("Enter the book code")
        !BOOK_CODE.matches(value.trim()) -> Result.fail("Book codes look like LMS-001")
        else -> Result.OK
    }
}
