package com.example.tikstok.data.auth

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper over [FirebaseAuth] — the single entry point for sign-in, sign-up, sign-out and
 * account deletion. Suspends on Firebase's `Task`s via [await] so callers stay on coroutines.
 *
 * The current user is observed through [addAuthStateListener]; the auth gate in `MainActivity`
 * swaps between the login screen and the app whenever this changes. Per-profile portfolio data
 * (Firestore/Room) is wired on top of this in later stages.
 */
object AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Signs in with an existing email/password account. Throws on bad credentials. */
    suspend fun signIn(email: String, password: String): FirebaseUser =
        auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Sign-in returned no user")

    /** Creates a new email/password account and signs into it. */
    suspend fun signUp(email: String, password: String): FirebaseUser =
        auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Sign-up returned no user")

    /**
     * Exchanges a Google ID token (from Credential Manager) for a Firebase session. Returns true
     * when this is the account's first sign-in, so the caller can route a new user into onboarding.
     */
    suspend fun signInWithGoogle(idToken: String): Boolean {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        result.user ?: error("Google sign-in returned no user")
        return result.additionalUserInfo?.isNewUser == true
    }

    /**
     * Whether the account signs in with an email/password — the only case where changing the
     * password here makes sense. Google-only accounts manage their password with Google instead.
     */
    fun isEmailPasswordUser(): Boolean =
        auth.currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    /**
     * Re-authenticates with [currentPassword] (Firebase requires a fresh login before sensitive
     * changes) and sets [newPassword]. Throws `FirebaseAuthInvalidCredentialsException` when the
     * current password is wrong, so the caller can flag exactly that.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: error("Not signed in")
        val email = user.email ?: error("Account has no email")
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    fun signOut() = auth.signOut()

    /**
     * Permanently deletes the signed-in account. Firebase may reject this with
     * `FirebaseAuthRecentLoginRequiredException` if the session is old — the caller should surface
     * that and ask the user to re-authenticate.
     */
    suspend fun deleteAccount() {
        auth.currentUser?.delete()?.await()
    }

    fun addAuthStateListener(onChange: (FirebaseUser?) -> Unit): FirebaseAuth.AuthStateListener {
        val listener = FirebaseAuth.AuthStateListener { onChange(it.currentUser) }
        auth.addAuthStateListener(listener)
        return listener
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
}
