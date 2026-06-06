package com.example.tikstok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.tikstok.data.auth.AuthRepository
import com.example.tikstok.ui.TikStokApp
import com.example.tikstok.ui.auth.LoginScreen
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
 * Shows the login screen while signed out and the app once signed in. The swap is driven by
 * Firebase's auth-state listener, so signing in (any method) or signing out flips the UI with no
 * manual navigation.
 */
@Composable
private fun AuthGate() {
    var signedIn by remember { mutableStateOf(AuthRepository.currentUser != null) }
    DisposableEffect(Unit) {
        val listener = AuthRepository.addAuthStateListener { user -> signedIn = user != null }
        onDispose { AuthRepository.removeAuthStateListener(listener) }
    }
    if (signedIn) TikStokApp() else LoginScreen()
}
