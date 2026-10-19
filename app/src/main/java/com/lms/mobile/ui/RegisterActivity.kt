package com.lms.mobile.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import com.lms.mobile.logic.Validators
import com.lms.mobile.SessionManager
import com.lms.mobile.data.Repo

class RegisterActivity : ThemedActivity() {
    private lateinit var repo: Repo
    private lateinit var session: SessionManager
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = Repo.get(this)
        session = SessionManager(this)

        val form = vbox(24)
        form.addView(linkText("‹ Back to login") { finish() }.apply { gravity = Gravity.START })
        form.addView(title("Create account"), lp(MATCH, WRAP, 0f, 0, 4, 0, 4))
        form.addView(muted("Join the library to search, borrow and track your books."), lp(MATCH, WRAP, 0f, 0, 0, 0, 18))

        val name = input("e.g. Naledi Dlamini", NAME_TYPE)
        val username = input("4 to 20 letters, numbers, . or _")
        val email = input("name@example.com", EMAIL_TYPE)
        val password = input("Create a password", PASSWORD_TYPE)
        val confirm = input("Repeat the password", PASSWORD_TYPE)

        fun field(labelText: String, view: EditText) {
            form.addView(caption(labelText))
            form.addView(view, lp(MATCH, WRAP, 0f, 0, 6, 0, 14))
        }
        field("Full name", name)
        field("Username", username)
        field("Email", email)
        field("Password", password)
        field("Confirm password", confirm)
        form.addView(muted("Password: 8+ characters with upper case, lower case, a number and a symbol."), lp(MATCH, WRAP, 0f, 0, 0, 0, 14))

        button = primaryButton("Create account") {
            val checks = listOf(
                name to Validators.fullName(name.text.toString()),
                username to Validators.username(username.text.toString()),
                email to Validators.email(email.text.toString()),
                password to Validators.password(password.text.toString()),
                confirm to Validators.passwordsMatch(password.text.toString(), confirm.text.toString())
            )
            checks.forEach { it.first.error = null }
            val bad = checks.firstOrNull { !it.second.ok }
            if (bad != null) {
                bad.first.error = bad.second.message
                bad.first.requestFocus()
            } else {
                submit(name.text.toString(), username.text.toString(), email.text.toString(), password.text.toString(), username, email)
            }
        }
        form.addView(button)
        showScrollable(form)
    }

    private fun submit(fullName: String, username: String, email: String, password: String, usernameField: EditText, emailField: EditText) {
        button.isEnabled = false
        button.text = "Creating account…"
        async({
            val result = repo.register(username, fullName, email, password)
            if (result.ok) repo.login(username, password) to result else null to result
        }, fail = { button.isEnabled = true; button.text = "Create account"; toast("Could not create the account. Please try again.") }) { (user, result) ->
            button.isEnabled = true
            button.text = "Create account"
            if (user == null) {
                if (result.message.contains("username")) usernameField.error = result.message
                else if (result.message.contains("email")) emailField.error = result.message
                else toast(result.message)
            } else {
                session.userId = user.id
                session.darkMode = false
                toast("Welcome, ${user.firstName}!")
                startActivity(homeIntentFor(user).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
        }
    }
}
