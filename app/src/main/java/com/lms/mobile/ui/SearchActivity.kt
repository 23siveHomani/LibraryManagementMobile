package com.lms.mobile.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.lms.mobile.data.Book
import com.lms.mobile.data.User

/** Book search with genre and availability filters; tapping a book lets the member borrow it (Figma Figure 14). */
class SearchActivity : BaseActivity() {
    private lateinit var searchField: EditText
    private lateinit var chipRow: LinearLayout
    private lateinit var results: LinearLayout
    private var genre: String? = null
    private var onlyAvailable = false
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = currentUser() ?: return

        val c = vbox(20)
        c.addView(title("Book Search"), lp(MATCH, WRAP, 0f, 0, 0, 0, 12))
        searchField = input("Search by title, author or code").apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh() }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        c.addView(searchField, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))

        chipRow = hbox()
        val scroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(chipRow)
        }
        c.addView(scroller, lp(MATCH, WRAP, 0f, 0, 0, 0, 14))

        results = vbox()
        c.addView(results)
        setScreen(c, Tab.SEARCH)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (user != null && ::results.isInitialized) refresh()
    }

    private fun refresh() {
        chipRow.removeAllViews()
        val genreLabel = (genre ?: "Genre") + " ▾"
        chipRow.addView(chip(genreLabel, genre != null) { pickGenre() }, lp(WRAP, WRAP, 0f, 0, 0, 8, 0))
        chipRow.addView(chip("Available", onlyAvailable) { onlyAvailable = !onlyAvailable; refresh() }, lp(WRAP, WRAP, 0f, 0, 0, 8, 0))
        if (genre != null || onlyAvailable) {
            chipRow.addView(chip("Clear", false) { genre = null; onlyAvailable = false; refresh() })
        }

        results.removeAllViews()
        val books = repo.searchBooks(searchField.text.toString(), genre, onlyAvailable)
        if (books.isEmpty()) {
            results.addView(muted("No books match your search. Try a different word or clear the filters.").apply { gravity = Gravity.CENTER },
                lp(MATCH, WRAP, 0f, 0, 24, 0, 0))
            return
        }
        for (book in books) {
            val row = card().apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setOnClickListener { showBook(book) }
            }
            row.addView(cover(book.title, 56, 76))
            val text = vbox()
            text.addView(label(book.title, 15f, Palette.text, true))
            text.addView(muted(book.author))
            text.addView(muted(book.genre + " · " + book.code), lp(MATCH, WRAP, 0f, 0, 0, 0, 6))
            text.addView(
                if (book.availableCopies > 0) pill("Available (${book.availableCopies})", Palette.success, Palette.successBg)
                else pill("On loan", Palette.warn, Palette.warnBg)
            )
            row.addView(text, lp(0, WRAP, 1f))
            results.addView(row, lp(MATCH, WRAP, 0f, 0, 0, 0, 10))
        }
    }

    private fun pickGenre() {
        val genres = repo.genres()
        val items = (listOf("All genres") + genres).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Filter by genre")
            .setItems(items) { _, which ->
                genre = if (which == 0) null else genres[which - 1]
                refresh()
            }
            .show()
    }

    private fun showBook(book: Book) {
        val member = user ?: return
        val builder = AlertDialog.Builder(this)
            .setTitle(book.title)
            .setMessage("${book.author}\nGenre: ${book.genre}\nCode: ${book.code}\nCopies available: ${book.availableCopies} of ${book.totalCopies}")
            .setNegativeButton("Close", null)
        if (book.availableCopies > 0) {
            builder.setPositiveButton("Borrow") { _, _ ->
                async({ repo.borrow(member.id, book.id) }) { result ->
                    toast(result.message)
                    refresh()
                }
            }
        }
        builder.show()
    }
}
