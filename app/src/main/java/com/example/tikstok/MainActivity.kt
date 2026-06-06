package com.example.tikstok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.tikstok.data.auth.AuthRepository
import com.example.tikstok.data.auth.OnboardingState
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.ui.TikStokApp
import com.example.tikstok.ui.auth.LoginScreen
import com.example.tikstok.ui.onboarding.OnboardingScreen
import com.example.tikstok.ui.theme.TikStokTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TikStokTheme {
                AuthGate()
            }
        }
    }
}

/**
 * Shows the login screen while signed out, the first-run intro for a brand-new account, a brief
 * loading state while the portfolio syncs from Firestore, and the app otherwise. The swap is driven
 * by Firebase's auth-state listener, so signing in (any method) or signing out flips the UI with no
 * manual navigation.
 */
@Composable
private fun AuthGate() {
    var signedIn by remember { mutableStateOf(AuthRepository.currentUser != null) }
    DisposableEffect(Unit) {
        val listener = AuthRepository.addAuthStateListener { user ->
            signedIn = user != null
            if (user == null) {
                // Signed out: drop any stale onboarding flag and clear the per-user portfolio.
                OnboardingState.clear()
                PortfolioStore.reset()
            } else {
                // Bind the user so write-through works immediately (incl. during onboarding), and
                // mirror the real email into the in-memory account.
                PortfolioStore.bindUser(user.uid)
                user.email?.let { PortfolioStore.updateEmail(it) }
            }
        }
        onDispose { AuthRepository.removeAuthStateListener(listener) }
    }

    // A returning user (not onboarding) loads their profiles from Firestore before the app shows.
    val needsLoad = signedIn && !OnboardingState.pending && !PortfolioStore.loaded
    LaunchedEffect(needsLoad) {
        if (needsLoad) AuthRepository.currentUser?.uid?.let { PortfolioStore.load(it) }
    }

    when {
        !signedIn -> LoginScreen()
        OnboardingState.pending -> OnboardingScreen(onDone = { OnboardingState.clear() })
        !PortfolioStore.loaded -> LoadingScreen()
        else -> TikStokApp()
    }
}

/** Full-screen spinner shown while the signed-in user's portfolio loads from Firestore. */
@Composable
private fun LoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
