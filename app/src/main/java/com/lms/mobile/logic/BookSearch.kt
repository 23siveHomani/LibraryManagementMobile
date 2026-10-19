package com.lms.mobile.logic

/** Builds the parameterised SQL for the book search screen (user text is never concatenated into SQL). */
object BookSearch {
    data class Query(val sql: String, val args: List<String>)

    fun escapeLike(text: String): String =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    fun build(text: String, genre: String?, onlyAvailable: Boolean): Query {
        val sql = StringBuilder(
            "SELECT id, code, title, author, genre, total_copies, available_copies FROM books WHERE 1=1"
        )
        val args = mutableListOf<String>()
        val t = text.trim()
        if (t.isNotEmpty()) {
            val like = "%" + escapeLike(t) + "%"
            sql.append(" AND (title LIKE ? ESCAPE '\\' OR author LIKE ? ESCAPE '\\' OR code LIKE ? ESCAPE '\\')")
            args.add(like); args.add(like); args.add(like)
        }
        if (!genre.isNullOrBlank()) {
            sql.append(" AND genre = ?")
            args.add(genre)
        }
        if (onlyAvailable) sql.append(" AND available_copies > 0")
        sql.append(" ORDER BY title COLLATE NOCASE")
        return Query(sql.toString(), args)
    }
}
