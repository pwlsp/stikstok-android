package com.example.tikstok.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tikstok.data.auth.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch

/** A categorised auth failure; the screen maps each to a localized message. */
enum class AuthError { INVALID_CREDENTIALS, EMAIL_IN_USE, WEAK_PASSWORD, NETWORK, GOOGLE_FAILED, UNKNOWN }

/**
 * Drives the login/sign-up form: email + password fields, the login↔sign-up mode toggle, the busy
 * flag, and the last error. On success it does nothing visible — the auth-state listener in the gate
 * notices the new user and swaps the login screen for the app.
 */
class AuthViewModel : ViewModel() {

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isSignUp by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<AuthError?>(null)
        private set

    /** Firebase requires a 6-char minimum password; mirror that so the button enables sensibly. */
    val canSubmit: Boolean
        get() = !loading && email.trim().contains('@') && password.length >= 6

    fun onEmailChange(value: String) { email = value; error = null }
    fun onPasswordChange(value: String) { password = value; error = null }
    fun toggleMode() { isSignUp = !isSignUp; error = null }

    fun submit() {
        if (!canSubmit) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                if (isSignUp) AuthRepository.signUp(email, password)
                else AuthRepository.signIn(email, password)
            } catch (e: Exception) {
                error = e.toAuthError()
            } finally {
                loading = false
            }
        }
    }

    /** Called by the screen once Credential Manager hands back a Google ID token. */
    fun onGoogleToken(idToken: String) {
        loading = true
        error = null
        viewModelScope.launch {
            try {
                AuthRepository.signInWithGoogle(idToken)
            } catch (e: Exception) {
                error = AuthError.GOOGLE_FAILED
            } finally {
                loading = false
            }
        }
    }

    /** The screen owns the Credential Manager dialog; it reports start/failure of that flow here. */
    fun onGoogleStart() { loading = true; error = null }
    fun onGoogleFailed() { loading = false; error = AuthError.GOOGLE_FAILED }
    fun onGoogleCancelled() { loading = false }

    private fun Exception.toAuthError(): AuthError = when (this) {
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
        is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
        is FirebaseAuthInvalidUserException -> AuthError.INVALID_CREDENTIALS
        is FirebaseAuthInvalidCredentialsException -> AuthError.INVALID_CREDENTIALS
        is FirebaseNetworkException -> AuthError.NETWORK
        else -> AuthError.UNKNOWN
    }
}
