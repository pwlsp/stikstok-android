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

@Composable
private fun AuthGate() {
    var signedIn by remember { mutableStateOf(AuthRepository.currentUser != null) }
    DisposableEffect(Unit) {
        val listener = AuthRepository.addAuthStateListener { user ->
            signedIn = user != null
            if (user == null) {
                OnboardingState.clear()
                PortfolioStore.reset()
            } else {
                PortfolioStore.bindUser(user.uid)
                user.email?.let { PortfolioStore.updateEmail(it) }
            }
        }
        onDispose { AuthRepository.removeAuthStateListener(listener) }
    }

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

@Composable
private fun LoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
