package com.notifyhub.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds global settings (polling interval, background-sync switch) plus the
 * per-project bearer tokens. Backed by EncryptedSharedPreferences so tokens are
 * encrypted at rest (and excluded from Android auto-backup, see
 * backup_rules.xml / data_extraction_rules.xml).
 */
data class ConnectionSettings(
    val pollIntervalSeconds: Int = 20,
    val serviceEnabled: Boolean = false
)

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "notifyhub_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ConnectionSettings> = _settings.asStateFlow()

    private fun load(): ConnectionSettings = ConnectionSettings(
        pollIntervalSeconds = prefs.getInt(KEY_INTERVAL, 20),
        serviceEnabled = prefs.getBoolean(KEY_ENABLED, false)
    )

    fun update(settings: ConnectionSettings) {
        prefs.edit()
            .putInt(KEY_INTERVAL, settings.pollIntervalSeconds)
            .putBoolean(KEY_ENABLED, settings.serviceEnabled)
            .apply()
        _settings.value = settings
    }

    fun setServiceEnabled(enabled: Boolean) {
        update(_settings.value.copy(serviceEnabled = enabled))
    }

    fun current(): ConnectionSettings = _settings.value

    fun getProjectToken(projectId: String): String =
        prefs.getString(PROJECT_TOKEN_PREFIX + projectId, "") ?: ""

    fun setProjectToken(projectId: String, token: String) {
        prefs.edit().putString(PROJECT_TOKEN_PREFIX + projectId, token).apply()
    }

    fun removeProjectToken(projectId: String) {
        prefs.edit().remove(PROJECT_TOKEN_PREFIX + projectId).apply()
    }

    /** Pre-per-project global server URL and token, or null once migrated / never set. */
    fun legacyConnection(): Pair<String, String>? {
        val url = prefs.getString(KEY_URL, "") ?: ""
        if (url.isBlank()) return null
        return url to (prefs.getString(KEY_TOKEN, "") ?: "")
    }

    fun clearLegacyConnection() {
        prefs.edit().remove(KEY_URL).remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val KEY_URL = "server_base_url"
        private const val KEY_TOKEN = "api_token"
        private const val KEY_INTERVAL = "poll_interval_seconds"
        private const val KEY_ENABLED = "service_enabled"
        private const val PROJECT_TOKEN_PREFIX = "project_token_"

        @Volatile
        private var INSTANCE: SettingsStore? = null

        fun getInstance(context: Context): SettingsStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsStore(context.applicationContext).also { INSTANCE = it }
            }
    }
}
