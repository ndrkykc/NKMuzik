package com.nkapps.nkmuzik.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageManager {

    fun getInitialLanguage(): String {
        val systemLang = Locale.getDefault().language
        return if (systemLang == "tr") "tr" else "en"
    }

    fun setLanguage(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales.get(0)?.language ?: getInitialLanguage()
        } else {
            getInitialLanguage()
        }
    }
}
