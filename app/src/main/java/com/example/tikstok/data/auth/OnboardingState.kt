package com.example.tikstok.data.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Whether the freshly signed-in user still needs the first-run intro (nickname + first profile).
 * Set when a brand-new account is created (email sign-up, or a first-time Google sign-in) so the
 * auth gate shows the onboarding wizard instead of dropping straight into the app. Session-scoped —
 * like the rest of the portfolio state, this lives only until Firestore persistence lands.
 */
object OnboardingState {

    var pending by mutableStateOf(false)
        private set

    /** A new account was created — route through onboarding. */
    fun begin() { pending = true }

    /** Onboarding finished, or was abandoned (e.g. sign-out) — go to the app/login as normal. */
    fun clear() { pending = false }
}
