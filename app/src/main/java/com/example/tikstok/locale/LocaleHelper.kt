package com.example.tikstok.locale

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/**
 * Reads and updates the per-app language using the platform [LocaleManager] (API 33+).
 *
 * Setting the locale persists across restarts and makes Android recreate the activity so the
 * UI picks up the new resources automatically — callers don't need to reload anything.
 */
object LocaleHelper {

    fun current(context: Context): AppLanguage {
        val tags = context.getSystemService(LocaleManager::class.java)
            .applicationLocales
            .toLanguageTags()
        return AppLanguage.fromTag(tags)
    }

    fun set(context: Context, language: AppLanguage) {
        context.getSystemService(LocaleManager::class.java)
            .applicationLocales = LocaleList.forLanguageTags(language.tag)
    }
}
