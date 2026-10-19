package com.lms.mobile

import com.lms.mobile.logic.BookSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookSearchTest {
    @Test fun emptySearchHasNoParameters() {
        val q = BookSearch.build("", null, false)
        assertTrue(q.args.isEmpty())
        assertTrue(q.sql.endsWith("ORDER BY title COLLATE NOCASE"))
    }

    @Test fun textIsPassedAsAParameterNotConcatenated() {
        val hostile = "x' OR '1'='1"
        val q = BookSearch.build(hostile, null, false)
        assertFalse(q.sql.contains(hostile))
        assertEquals(3, q.args.size)
        assertTrue(q.args.all { it.contains(hostile) })
    }

    @Test fun filtersAddConditions() {
        val q = BookSearch.build("dune", "Science fiction", true)
        assertTrue(q.sql.contains("genre = ?"))
        assertTrue(q.sql.contains("available_copies > 0"))
        assertEquals("Science fiction", q.args.last())
    }

    @Test fun likeWildcardsAreEscaped() {
        assertEquals("100\\%", BookSearch.escapeLike("100%"))
        assertEquals("a\\_b", BookSearch.escapeLike("a_b"))
        assertEquals("a\\\\b", BookSearch.escapeLike("a\\b"))
    }
}
