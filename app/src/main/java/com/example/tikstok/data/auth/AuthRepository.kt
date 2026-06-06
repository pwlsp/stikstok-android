package com.example.tikstok.data.auth

import com.example.tikstok.data.portfolio.PortfolioRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signIn(email: String, password: String): FirebaseUser =
        auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Sign-in returned no user")

    suspend fun signUp(email: String, password: String): FirebaseUser =
        auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Sign-up returned no user")

    suspend fun signInWithGoogle(idToken: String): Boolean {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        result.user ?: error("Google sign-in returned no user")
        return result.additionalUserInfo?.isNewUser == true
    }

    fun isEmailPasswordUser(): Boolean =
        auth.currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: error("Not signed in")
        val email = user.email ?: error("Account has no email")
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    fun signOut() = auth.signOut()

    suspend fun deleteAccount() {
        val user = auth.currentUser ?: return
        PortfolioRepository.deleteAllUserData(user.uid)
        user.delete().await()
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
