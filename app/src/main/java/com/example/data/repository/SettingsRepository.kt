package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("autovision_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SERVER_URL = "server_url"
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:8000"
    }

    private val _serverUrl = MutableStateFlow(
        prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    )
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun updateServerUrl(url: String) {
        val trimmed = url.trim()
        val sanitized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        prefs.edit().putString(KEY_SERVER_URL, sanitized).apply()
        _serverUrl.value = sanitized
    }

    fun getServerUrl(): String = _serverUrl.value
}
