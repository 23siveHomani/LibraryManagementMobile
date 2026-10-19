package com.lms.mobile

import com.lms.mobile.data.Role
import com.lms.mobile.data.SeedData
import com.lms.mobile.logic.FineCalculator
import com.lms.mobile.logic.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the simulated data meets the brief: at least ten records per table and consistent relationships. */
class SeedDataTest {
    @Test fun everyTableHasAtLeastTenRecords() {
        assertTrue(SeedData.users.size >= 10)
        assertTrue(SeedData.books.size >= 10)
        assertTrue(SeedData.loans.size >= 10)
        assertTrue(SeedData.fines.size >= 10)
        assertTrue(SeedData.notices.size >= 10)
        assertTrue(SeedData.settings.size >= 10)
    }

    @Test fun keysAreUnique() {
        assertEquals(SeedData.users.size, SeedData.users.map { it.id }.toSet().size)
        assertEquals(SeedData.users.size, SeedData.users.map { it.username.lowercase() }.toSet().size)
        assertEquals(SeedData.users.size, SeedData.users.map { it.email.lowercase() }.toSet().size)
        assertEquals(SeedData.books.size, SeedData.books.map { it.code }.toSet().size)
        assertEquals(SeedData.loans.size, SeedData.loans.map { it.id }.toSet().size)
        assertEquals(SeedData.fines.size, SeedData.fines.map { it.id }.toSet().size)
    }

    @Test fun foreignKeysPointToExistingRows() {
        val userIds = SeedData.users.map { it.id }.toSet()
        val bookIds = SeedData.books.map { it.id }.toSet()
        val loanIds = SeedData.loans.map { it.id }.toSet()
        assertTrue(SeedData.loans.all { it.userId in userIds && it.bookId in bookIds })
        assertTrue(SeedData.fines.all { it.userId in userIds && (it.loanId == null || it.loanId in loanIds) })
        assertTrue(SeedData.notices.all { it.userId in userIds })
        assertTrue(SeedData.settings.all { it.userId in userIds })
    }

    @Test fun seededUsernamesAndEmailsPassTheAppValidation() {
        assertTrue(SeedData.users.all { Validators.username(it.username).ok })
        assertTrue(SeedData.users.all { Validators.email(it.email).ok })
        assertTrue(SeedData.users.all { Validators.fullName(it.fullName).ok })
        assertTrue(SeedData.books.all { Validators.bookCode(it.code).ok })
        assertTrue(Validators.password(Config.DEMO_PASSWORD).ok)
    }

    @Test fun availableCopiesMatchActiveLoans() {
        for (b in SeedData.books) {
            val available = SeedData.availableCopies(b)
            assertTrue("${b.title} has negative availability", available >= 0)
            assertTrue(available <= b.copies)
        }
    }

    @Test fun thereAreBothLibrariansAndMembers() {
        assertTrue(SeedData.users.any { it.role == Role.LIBRARIAN })
        assertTrue(SeedData.users.count { it.role == Role.MEMBER } >= 8)
    }

    @Test fun lateReturnFinesMatchTheFineRule() {
        for (f in SeedData.fines) {
            val loan = SeedData.loans.firstOrNull { it.id == f.loanId } ?: continue
            if (!f.reason.startsWith("Late return")) continue
            val returned = loan.returnedDaysAgo ?: continue
            val lateDays = -loan.dueInDays - returned
            assertEquals("fine ${f.id}", FineCalculator.fineForDays(lateDays), f.amount)
        }
    }

    @Test fun demoMemberNalediMatchesTheFigmaScreen() {
        // Naledi: 3 active loans, one overdue by 5 days (R25) plus an unpaid R20 fine = R45.
        val active = SeedData.loans.filter { it.userId == 3L && it.returnedDaysAgo == null }
        assertEquals(3, active.size)
        val accrued = active.sumOf { FineCalculator.fineForDays(-it.dueInDays) }
        val unpaid = SeedData.fines.filter { it.userId == 3L && !it.paid }.sumOf { it.amount }
        assertEquals(45, accrued + unpaid)
    }
}
