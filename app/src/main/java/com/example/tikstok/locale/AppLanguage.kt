package com.example.tikstok.locale

import androidx.annotation.StringRes
import com.example.tikstok.R

/**
 * Languages the app ships translations for. The [tag] is a BCP-47 language tag and must
 * match an entry in res/xml/locales_config.xml and a matching values-<tag> resource folder.
 */
enum class AppLanguage(val tag: String, @param:StringRes val labelRes: Int) {
    ENGLISH("en", R.string.language_english),
    POLISH("pl", R.string.language_polish);

    companion object {
        val DEFAULT = ENGLISH

        /** Resolves a (possibly null/empty/region-qualified) language tag to a known language. */
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { !tag.isNullOrEmpty() && tag.startsWith(it.tag) } ?: DEFAULT
    }
}
