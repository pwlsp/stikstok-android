package com.example.tikstok.data.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object OnboardingState {

    var pending by mutableStateOf(false)
        private set

    fun begin() { pending = true }

    fun clear() { pending = false }
}
