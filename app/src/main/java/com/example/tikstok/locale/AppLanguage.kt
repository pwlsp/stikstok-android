package com.example.tikstok.locale

import androidx.annotation.StringRes
import com.example.tikstok.R

enum class AppLanguage(val tag: String, @param:StringRes val labelRes: Int) {
    ENGLISH("en", R.string.language_english),
    POLISH("pl", R.string.language_polish);

    companion object {
        val DEFAULT = ENGLISH

        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { !tag.isNullOrEmpty() && tag.startsWith(it.tag) } ?: DEFAULT
    }
}
