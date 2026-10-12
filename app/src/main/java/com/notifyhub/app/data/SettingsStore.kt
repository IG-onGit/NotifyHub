package com.notifyhub.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds connection settings: your server's base URL, the bearer token sent on
 * every request, and the polling interval. Backed by EncryptedSharedPreferences
 * so the token is encrypted at rest (and excluded from Android auto-backup, see
 * backup_rules.xml / data_extraction_rules.xml).
 */
data class ConnectionSettings(
    val serverBaseUrl: String = "",
    val apiToken: String = "",
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
        serverBaseUrl = prefs.getString(KEY_URL, "") ?: "",
        apiToken = prefs.getString(KEY_TOKEN, "") ?: "",
        pollIntervalSeconds = prefs.getInt(KEY_INTERVAL, 20),
        serviceEnabled = prefs.getBoolean(KEY_ENABLED, false)
    )

    fun update(settings: ConnectionSettings) {
        prefs.edit()
            .putString(KEY_URL, settings.serverBaseUrl)
            .putString(KEY_TOKEN, settings.apiToken)
            .putInt(KEY_INTERVAL, settings.pollIntervalSeconds)
            .putBoolean(KEY_ENABLED, settings.serviceEnabled)
            .apply()
        _settings.value = settings
    }

    fun setServiceEnabled(enabled: Boolean) {
        update(_settings.value.copy(serviceEnabled = enabled))
    }

    fun current(): ConnectionSettings = _settings.value

    companion object {
        private const val KEY_URL = "server_base_url"
        private const val KEY_TOKEN = "api_token"
        private const val KEY_INTERVAL = "poll_interval_seconds"
        private const val KEY_ENABLED = "service_enabled"

        @Volatile
        private var INSTANCE: SettingsStore? = null

        fun getInstance(context: Context): SettingsStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsStore(context.applicationContext).also { INSTANCE = it }
            }
    }
}
