package dev.knsalvaterra.kezir.api

import android.util.Log
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


@Suppress("all")
sealed class TicketResult {
    data class Success(val message: String, val order: Order?) : TicketResult()
    data class Error(val message: String) : TicketResult()
}

data class PinRequest(
    val pin: String,
    val event_id: String?
)

data class PinResponse(
    val success: Boolean,
    val message: String?
)

data class VerifyRequest( //evento e codigo de accesso
    val code: String,
    val event_id: String
)

/*

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
data class VerifyResponse(
    val success: Boolean,
    val message: String?,
    val order: Order?
)

data class Order(
    val buyer_name: String,
    val tickets: List<Ticket>
)

data class Ticket(
    val ticket_name: String,
    val quantity: String
)

// --- API Service ---

interface ApiService {
    /**
     * Corresponds to the Java declaration:
     * public Response<PinResponse> verifyPin(@Body PinRequest request);
     */
    @POST("api/box-office/verify-pin.php")
    suspend fun verifyPin(
        @Body request: PinRequest
    ): Response<PinResponse>

    /**
     * Corresponds to the Java declaration:
     * public Response<VerifyResponse> verifyCode(@Header("Cookie") String sessionCookie, @Body VerifyRequest request);
     */
    @POST("api/box-office/verify-code.php")
    suspend fun verifyCode(
        @Header("Cookie") sessionCookie: String,
        @Body request: VerifyRequest
    ): VerifyResponse
}

/**
 *  Lazy<ApiService> api = Retrofit.Builder().baseUrl("https://stage.kezir.st/").addConverterFactory(GsonConverterFactory.create()).build().create(ApiService.class);
 */
object ApiClient {

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://stage.kezir.st/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java) //JSON -> object
    }
}

// --- Ticket Operations ---

object TicketManager {
    /**

     * public static TicketResult evaluateTicket(String sessionCookie, String code, String eventId) {
     *

     */
    suspend fun evaluateTicket(sessionCookie: String, code: String, eventId: String?): TicketResult {
        if (eventId == null) {
            return TicketResult.Error("Event ID is missing.")
        }

        return try {

            val response = ApiClient.api.verifyCode(sessionCookie, VerifyRequest(code, eventId))

            if (response.success) {
                TicketResult.Success(response.message ?: "Success", response.order)
            } else {
                TicketResult.Error(response.message ?: "Invalid ticket.")
            }
        } catch (e: Exception) {
            Log.e("TicketManager", "Verification request failed", e)
            TicketResult.Error("Verification failed. Check network connection.")
        }
    }
}
