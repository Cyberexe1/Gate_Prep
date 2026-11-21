package com.gateprep.app.net

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Keeps the login (tokens + user) in encrypted storage, so the user stays signed in. */
class SessionStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences = try {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "gate_session", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // the Keystore can be unavailable on some devices; fall back to app-private storage
        context.getSharedPreferences("gate_session_plain", Context.MODE_PRIVATE)
    }

    @Volatile var accessToken: String? = prefs.getString("access", null)
        private set
    @Volatile var refreshToken: String? = prefs.getString("refresh", null)
        private set
    @Volatile var user: AuthUser? = prefs.getString("user", null)?.let { runCatching { json.decodeFromString<AuthUser>(it) }.getOrNull() }
        private set

    /** Called (on any thread) whenever the signed-in user changes, including null after logout or an expired session. */
    @Volatile var onUserChanged: ((AuthUser?) -> Unit)? = null

    val signedIn get() = user != null && refreshToken != null

    @Synchronized
    fun save(r: AuthResponse) {
        accessToken = r.accessToken
        refreshToken = r.refreshToken
        user = r.user
        prefs.edit().putString("access", r.accessToken).putString("refresh", r.refreshToken).putString("user", json.encodeToString(r.user)).apply()
        onUserChanged?.invoke(r.user)
    }

    @Synchronized
    fun clear() {
        val had = user != null
        accessToken = null
        refreshToken = null
        user = null
        prefs.edit().clear().apply()
        if (had) onUserChanged?.invoke(null)
    }
}
