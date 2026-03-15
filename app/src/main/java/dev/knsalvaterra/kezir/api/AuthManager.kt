package dev.knsalvaterra.kezir.api

import android.content.Context
import android.util.Log
import dev.knsalvaterra.kezir.R

sealed class LoginResult {
    data class Success(val eventId: String, val pin: String) : LoginResult()
    data class Error(val message: Int) : LoginResult()
}

object AuthManager {
    private const val MOCK_SESSION_COOKIE = "m836v1d0grchu3mgu5v2e3ne91"
    private const val PREFS_NAME = "kezir_prefs"
    private const val KEY_EVENT_ID = "event_id"
    private const val KEY_PIN = "user_pin"

    private var tempEventId: String? = null
    private var tempPin: String? = null

    fun saveSessionInMemory(eventId: String, pin: String) {
        tempEventId = eventId
        tempPin = pin
    }

    fun getInMemorySession(): Pair<String, String>? {
        val id = tempEventId
        val pin = tempPin
        return if (id != null && pin != null) id to pin else null
    }

    fun clearSession() {
        tempEventId = null
        tempPin = null
    }

    fun saveSession(context: Context, eventId: String, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_EVENT_ID, eventId)
            .putString(KEY_PIN, pin)
            .apply()
    }

    fun getSavedSession(context: Context): Pair<String, String>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_EVENT_ID, null)
        val pin = prefs.getString(KEY_PIN, null)
        return if (id != null && pin != null) id to pin else null
    }

  // suspend fun login(sessionCookie: String): LoginResult {
  //     return LoginResult.Success("664544741697781760", sessionCookie)
  // }

  // suspend fun login(pin: String, eventId: String): LoginResult {
  //     return try {
  //         val request = PinRequest(pin, eventId)
  //         val response = ApiClient.api.verifyPin(request)

  //         if (response.isSuccessful && response.body()?.success == true) {
  //             val sessionCookie = response.headers()["Set-Cookie"]?.split(";")?.get(0)
  //             if (sessionCookie != null) {
  //                 LoginResult.Success(eventId, sessionCookie)
  //             } else {
  //                 Log.w("AuthManager", "Session cookie not found on login, using mock session")
  //                 LoginResult.Success(eventId, MOCK_SESSION_COOKIE)
  //             }
  //         } else {
  //             LoginResult.Error(R.string.invalid_pin_or_event_id)
  //         }
  //     } catch (e: Exception) {
  //         Log.e("AuthManager", "Login request failed", e)
  //         LoginResult.Error(R.string.login_failed_check_connection)
  //     }
  // }

    suspend fun login(pin: String, eventId: String): LoginResult {
        return try {
            val request = PinRequest(pin, eventId)
            val response = ApiClient.api.verifyPin(request)

            if (response.isSuccessful) {
                 val body = response.body()
                 if (body != null && body.success) {
                     LoginResult.Success(eventId, pin)
                 } else {
                     LoginResult.Error(R.string.invalid_pin_or_event_id)
                 }
            } else {
                LoginResult.Error(R.string.invalid_pin_or_event_id)
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Login request failed", e)
            LoginResult.Error(R.string.login_failed_check_connection)
        }
    }
}
