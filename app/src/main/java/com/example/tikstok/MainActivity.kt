package com.example.tikstok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tikstok.ui.TikStokApp
import com.example.tikstok.ui.theme.TikStokTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TikStokTheme {
                TikStokApp()
            }
        }
    }
}
