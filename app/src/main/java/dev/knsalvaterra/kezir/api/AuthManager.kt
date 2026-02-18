package dev.knsalvaterra.kezir.api

import android.util.Log
import dev.knsalvaterra.kezir.R

sealed class LoginResult { //
    data class Success(val eventId: String, val sessionCookie: String) : LoginResult()


    data class Error(val message: Int) : LoginResult()


}

object AuthManager {
    // muckup login with this cookie m836v1d0grchu3mgu5v2e3ne91
    private const val MOCK_SESSION_COOKIE = "m836v1d0grchu3mgu5v2e3ne91"

    suspend fun login(sessionCookie: String): LoginResult {
        return LoginResult.Success("664544741697781760", sessionCookie)
        }

    suspend fun login(pin: String, eventId: String): LoginResult {
        return try {
            val request = PinRequest(pin, eventId)
            val response = ApiClient.api.verifyPin(request)


            if (response.isSuccessful && response.body()?.success == true) {
                val sessionCookie = response.headers()["Set-Cookie"]?.split(";")?.get(0)

                if (sessionCookie != null) {
                    LoginResult.Success(eventId, sessionCookie)
                } else {
                    Log.w("AuthManager", "Session cookie not found on login, using mock session")
                    LoginResult.Success(eventId, MOCK_SESSION_COOKIE)
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
