package dev.knsalvaterra.kezir.api

import android.util.Log
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Result wrapper for ticket verification operations.
 *
 * We use a sealed class here because ticket verification can either work or fail,
 * and we want to handle both cases cleanly without nullable mess everywhere.
 */
@Suppress("all")
sealed class TicketResult {

    /**
     * Ticket is valid and we got the order info back
     */
    data class Success(
        val message: String,
        val order: Order?
    ) : TicketResult()

    /**
     * Something went wrong. Could be an invalid code, network issue,
     * or the backend rejecting the request for some reason.
     */
    data class Error(
        val message: String
    ) : TicketResult()
}

/**
 * What we send when a staff member tries to log in with their PIN.
 */
data class PinRequest(
    val pin: String,
    val event_id: String?
)

/**
 * Backend's answer to a PIN verification attempt.
 */
data class PinResponse(
    val success: Boolean,
    val message: String?
)

/**
 * Request payload for checking if a ticket code is legit.
 */
data class VerifyRequest(
    val code: String,
    val event_id: String
)

/**
 * What comes back when we try to verify a ticket.
 * If it's valid, we also get order details.
 */
data class VerifyResponse(
    val success: Boolean,

    @SerializedName("message", alternate = ["error"])
    val message: String?,
    val order: Order?
)

/*
 Example of what a successful response looks like:

 {
    "success": true,
    "message": "Código verificado e marcado como resgatado!",
    "order": {
        "buyer_name": "Dalila Rita",
        "buyer_phone": "+2399974821",
        "tickets": [
            {
                "ticket_type": "vip",
                "ticket_name": "Normal",
                "table_capacity": null,
                "quantity": "4"
            }
        ]
    }
 }
*/

/**
 * Order information tied to a verified ticket.
 * Tells us who bought it and what they're getting.
 */
data class Order(
    val buyer_name: String,
    val tickets: List<Ticket>
)

/**
 * Individual ticket within an order.
 * The quantity is a string because that's how the API sends it (yeah, I know).
 */
data class Ticket(
    val ticket_name: String,
    val quantity: String
)

// --- API Service ---

/**
 * Defines all the network calls to the backend.
 */
interface ApiService {

    /**
     * Checks if a staff PIN is valid.
     */
    @POST("api/box-office/verify-pin.php")
    suspend fun verifyPin(
        @Body request: PinRequest
    ): Response<PinResponse>

    /**
     * Verifies a ticket code and marks it as redeemed if valid.
     *
     * Important: This endpoint requires authentication via session cookie
     * since the backend uses PHP sessions. Make sure you pass the cookie
     * you got after PIN verification.
     */
    @POST("api/box-office/verify-code.php")
    suspend fun verifyCode(
        @Header("Cookie") sessionCookie: String,
        @Body request: VerifyRequest
    ): Response<VerifyResponse>
}

/**
 * Single source of truth for our Retrofit setup.
 * Built lazily so we don't waste resources if the app never needs the API.
 */
object ApiClient {

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://stage.kezir.st/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// --- Ticket Operations ---

/**
 * Higher-level ticket operations that the rest of the app can use
 * without worrying about HTTP details or error handling.
 */
object TicketManager {

    /**
     * Verifies a ticket code and gives back a clean result.
     *
     * This handles all the messy network stuff internally - successful responses,
     * failed requests, missing data, etc. - and converts everything into either
     * a Success or Error result that's easy to work with in the UI.
     *
     * @param sessionCookie The PHP session cookie from PIN verification
     * @param code The ticket code we're checking (scanned or typed)
     * @param eventId Which event this ticket should be valid for
     * @return Success with order info if valid, Error with message otherwise
     */
    suspend fun evaluateTicket(
        sessionCookie: String,
        code: String,
        eventId: String?
    ): TicketResult {


        if (eventId == null) {
            return TicketResult.Error("Event ID is missing.")
        }

        return try {
            val response = ApiClient.api.verifyCode(
                sessionCookie,
                VerifyRequest(code, eventId)
            )

            // HTTP request itself succeeded
            if (!response.isSuccessful) {
                // HTTP error (400, 401, 500, etc.)
                return TicketResult.Error(
                    "Server error: ${response.code()} - ${response.message()}"
                )
            }


            val body = response.body()
            if (body == null) {
                return TicketResult.Error("Empty response from server")
            }

            if (body.success) {
                TicketResult.Success(
                    body.message ?: "Success",
                    body.order
                )
            } else {
                TicketResult.Error(
                    body.message ?: "Invalid ticket."
                )
            }

        } catch (e: Exception) {
            Log.e("TicketManager", "Verification request failed", e)
            TicketResult.Error("Network error: ${e.message}")
        }
    }
}