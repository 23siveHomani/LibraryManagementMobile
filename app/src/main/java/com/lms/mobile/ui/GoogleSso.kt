package com.lms.mobile.ui

import android.app.Activity
import android.os.CancellationSignal
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.lms.mobile.Config
import java.util.concurrent.Executor

/**
 * Single sign-on with a Google account through the Android Credential Manager.
 * The Google account chooser is shown by Android; the app never sees the Google password.
 */
object GoogleSso {
    fun signIn(activity: Activity, onAccount: (email: String, name: String?) -> Unit, onFailure: (String) -> Unit) {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(Config.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val mainThread = Executor { command -> activity.runOnUiThread(command) }

        CredentialManager.create(activity).getCredentialAsync(
            activity,
            request,
            CancellationSignal(),
            mainThread,
            object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    val credential = result.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        try {
                            val google = GoogleIdTokenCredential.createFrom(credential.data)
                            onAccount(google.id, google.displayName)
                        } catch (e: GoogleIdTokenParsingException) {
                            onFailure("Could not read the Google account details")
                        }
                    } else {
                        onFailure("Unsupported sign-in credential")
                    }
                }

                override fun onError(e: GetCredentialException) {
                    onFailure(e.message ?: "Google sign-in was cancelled")
                }
            }
        )
    }
}
