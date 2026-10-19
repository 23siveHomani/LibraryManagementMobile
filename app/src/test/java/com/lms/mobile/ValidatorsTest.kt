package com.lms.mobile

import com.lms.mobile.logic.Validators
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun validUsernamesPass() {
        assertTrue(Validators.username("naledi").ok)
        assertTrue(Validators.username("thabo.m").ok)
        assertTrue(Validators.username("user_123").ok)
    }

    @Test fun invalidUsernamesFail() {
        assertFalse(Validators.username("").ok)
        assertFalse(Validators.username("abc").ok)
        assertFalse(Validators.username("a".repeat(21)).ok)
        assertFalse(Validators.username("bad name").ok)
        assertFalse(Validators.username("x'; DROP TABLE users;--").ok)
    }

    @Test fun validEmailsPass() {
        assertTrue(Validators.email("name@example.com").ok)
        assertTrue(Validators.email("first.last+tag@mail.co.za").ok)
    }

    @Test fun invalidEmailsFail() {
        assertFalse(Validators.email("").ok)
        assertFalse(Validators.email("plainaddress").ok)
        assertFalse(Validators.email("a@b").ok)
        assertFalse(Validators.email("a@@b.com").ok)
        assertFalse(Validators.email("a b@example.com").ok)
    }

    @Test fun namesAcceptLettersAndCommonPunctuation() {
        assertTrue(Validators.fullName("Naledi Dlamini").ok)
        assertTrue(Validators.fullName("Pieter van Wyk").ok)
        assertTrue(Validators.fullName("Mary-Ann O'Neil").ok)
        assertFalse(Validators.fullName("").ok)
        assertFalse(Validators.fullName("X").ok)
        assertFalse(Validators.fullName("R2D2").ok)
    }

    @Test fun strongPasswordPasses() {
        assertTrue(Validators.password("Password@123").ok)
    }

    @Test fun weakPasswordsFailWithAHelpfulMessage() {
        assertTrue(Validators.password("Sh0rt!").message.contains("8"))
        assertTrue(Validators.password("alllowercase1!").message.contains("upper"))
        assertTrue(Validators.password("ALLUPPERCASE1!").message.contains("lower"))
        assertTrue(Validators.password("NoNumbersHere!").message.contains("number"))
        assertTrue(Validators.password("NoSymbols123").message.contains("symbol"))
        assertFalse(Validators.password("A1!" + "a".repeat(70)).ok)
    }

    @Test fun passwordsMustMatch() {
        assertTrue(Validators.passwordsMatch("abc", "abc").ok)
        assertFalse(Validators.passwordsMatch("abc", "abd").ok)
    }

    @Test fun bookCodesLookLikeLms001() {
        assertTrue(Validators.bookCode("LMS-001").ok)
        assertTrue(Validators.bookCode(" lms-014 ").ok)
        assertFalse(Validators.bookCode("").ok)
        assertFalse(Validators.bookCode("LMS-1").ok)
        assertFalse(Validators.bookCode("BOOK-001").ok)
    }
}
