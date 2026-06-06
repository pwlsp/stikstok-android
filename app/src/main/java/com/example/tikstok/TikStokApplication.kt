package com.example.tikstok

import android.app.Application
import android.content.Context

class TikStokApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
