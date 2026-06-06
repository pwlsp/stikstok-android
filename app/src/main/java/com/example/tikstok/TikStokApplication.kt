package com.example.tikstok

import android.app.Application
import android.content.Context

/**
 * Holds the application context so process-wide singletons (the Room cache) can be built lazily
 * without threading a Context through every ViewModel. Registered in the manifest via
 * `android:name`.
 */
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
