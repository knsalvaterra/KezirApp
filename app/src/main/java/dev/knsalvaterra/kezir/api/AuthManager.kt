package dev.knsalvaterra.kezir.api

import android.util.Log

sealed class LoginResult { //
    data class Success(val eventId: String, val sessionCookie: String) : LoginResult()


    data class Error(val message: String) : LoginResult()


}

object AuthManager {
    suspend fun login(pin: String, eventId: String): LoginResult {
        return try {
            val request = PinRequest(pin, eventId)
            val response = ApiClient.api.verifyPin(request)


            if (response.isSuccessful && response.body()?.success == true) {
                val sessionCookie = response.headers()["Set-Cookie"]?.split(";")?.get(0) //headers()["Set-Cookie"]?.split(";")?.get(0)

                if (sessionCookie != null) {
                    LoginResult.Success(eventId, sessionCookie)
                } else {
                    LoginResult.Error("Session cookie not found in response.")
                }


            } else {
                LoginResult.Error("Invalid PIN or Event ID.")
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Login request failed", e)
            LoginResult.Error("Falha no login. Verifique a ligação à internet.")
        }
    }
}
