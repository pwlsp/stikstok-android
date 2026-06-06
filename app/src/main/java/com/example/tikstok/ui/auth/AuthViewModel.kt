package com.example.tikstok.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tikstok.data.auth.AuthRepository
import com.example.tikstok.data.auth.OnboardingState
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
    var confirmPassword by mutableStateOf("")
        private set
    var isSignUp by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<AuthError?>(null)
        private set

    /** In sign-up mode the second field must match; only checked there. */
    val passwordsMatch: Boolean
        get() = !isSignUp || confirmPassword == password

    /** Firebase requires a 6-char minimum password; mirror that so the button enables sensibly. */
    val canSubmit: Boolean
        get() = !loading && email.trim().contains('@') && password.length >= 6 && passwordsMatch

    fun onEmailChange(value: String) { email = value; error = null }
    fun onPasswordChange(value: String) { password = value; error = null }
    fun onConfirmPasswordChange(value: String) { confirmPassword = value; error = null }
    fun toggleMode() { isSignUp = !isSignUp; confirmPassword = ""; error = null }

    fun submit() {
        if (!canSubmit) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                if (isSignUp) {
                    // Flag onboarding before the call returns: the gate's auth-state listener flips
                    // to "signed in" during sign-up, and we want it to land on the intro, not the app.
                    OnboardingState.begin()
                    AuthRepository.signUp(email, password)
                } else {
                    AuthRepository.signIn(email, password)
                }
                clearForm()
            } catch (e: Exception) {
                OnboardingState.clear()
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
                val isNewUser = AuthRepository.signInWithGoogle(idToken)
                if (isNewUser) OnboardingState.begin()
                clearForm()
            } catch (e: Exception) {
                error = AuthError.GOOGLE_FAILED
            } finally {
                loading = false
            }
        }
    }

    /**
     * Wipes the typed credentials after a successful sign-in. This ViewModel is activity-scoped, so
     * it outlives the login screen; without this, signing in (e.g. with Google) and later signing
     * out would show the previously typed email/password still sitting in the fields.
     */
    private fun clearForm() {
        email = ""
        password = ""
        confirmPassword = ""
        isSignUp = false
        error = null
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
