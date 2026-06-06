package com.example.tikstok.locale

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

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
