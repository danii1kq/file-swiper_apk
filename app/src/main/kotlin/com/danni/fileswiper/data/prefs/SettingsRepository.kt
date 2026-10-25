package com.danni.fileswiper.data.prefs

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("fileswiper_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    )

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE)

    private val _clearedMonths = MutableStateFlow(
        prefs.getStringSet(KEY_CLEARED_MONTHS, emptySet())?.toSet() ?: emptySet()
    )

    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    val language: StateFlow<String> = _language.asStateFlow()
    val clearedMonths: StateFlow<Set<String>> = _clearedMonths.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit { putString(KEY_THEME, mode.name) }
    }

    fun setLanguage(tag: String) {
        _language.value = tag
        prefs.edit { putString(KEY_LANGUAGE, tag) }
    }

    fun addClearedMonth(key: String) {
        if (!_clearedMonths.value.add(key)) return
        prefs.edit { putStringSet(KEY_CLEARED_MONTHS, _clearedMonths.value) }
    }

    fun addClearedMonths(keys: Collection<String>) {
        if (keys.isEmpty()) return
        _clearedMonths.value + keys
        prefs.edit { putStringSet(KEY_CLEARED_MONTHS, _clearedMonths.value) }
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_LANGUAGE = "language"
        const val KEY_CLEARED_MONTHS = "cleared_months"
        const val DEFAULT_LANGUAGE = "en"
    }
}
