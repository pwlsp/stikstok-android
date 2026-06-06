package com.example.tikstok.data.auth

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

    /** Exchanges a Google ID token (from Credential Manager) for a Firebase session. */
    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credential).await().user
            ?: error("Google sign-in returned no user")
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
