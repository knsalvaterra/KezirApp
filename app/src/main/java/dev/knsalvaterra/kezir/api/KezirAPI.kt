package dev.knsalvaterra.kezir.api

import android.annotation.SuppressLint
import android.util.Log
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


@Suppress("all")
sealed class TicketResult {


    data class Success(
        val message: String,
        val order: Order?
    ) : TicketResult()


    data class Error(
        val message: String
    ) : TicketResult()
}


data class PinRequest(
    val pin: String,
    val event_id: String?
)


data class PinResponse(
    val success: Boolean,
    val message: String?
)


data class VerifyRequest(
    val code: String,
    val event_id: String
)


data class VerifyResponse(
    val success: Boolean,
    @SerializedName("message", alternate = ["error"])
    val message: String?,
    val order: Order?
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


data class Order(
    val buyer_name: String,
    val buyer_phone: String,
    val tickets: List<Ticket>
)

data class Ticket(
    val ticket_type: String,
    val ticket_name: String,
    val table_capacity: String?,
    val quantity: String
)



interface ApiService {

    @POST("api/box-office/verify-pin.php")
    suspend fun verifyPin(
        @Body request: PinRequest
    ): Response<PinResponse>


    @POST("api/box-office/verify-code.php")
    suspend fun verifyCode(
        @Header("Cookie") sessionCookie: String,
        @Body request: VerifyRequest
    ): VerifyResponse
}

//  Lazy<ApiService> api =  Retrofit.Builder().build().create(ApiService.class)

object ApiClient {

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://stage.kezir.st/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// ticket relatd

object TicketManager {


    @SuppressLint("SuspiciousIndentation")
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

          // val response = VerifyResponse(
          //     success = true,
          //     message = "Código verificado e marcado como resgatado!",
          //     order = Order(
          //         buyer_name = "Kenedy Salvaterra",
          //         tickets = listOf(
          //             Ticket(
          //                 ticket_name = "Normal",
          //                 quantity = "4"
          //             ),
          //             Ticket(
          //                 ticket_name = "Normal",
          //                 quantity = "2"
          //             ),
          //             Ticket(
          //                 ticket_name = "VIP",
          //             quantity = "2"
          //         ),
          //             Ticket(
          //                 ticket_name = "VIP",
          //                 quantity = "2"
          //             )
          //         )
          //     )
          // )

            if (response.success) {
                TicketResult.Success(
                    response.message ?: "Success",
                    response.order
                )
            } else {
                TicketResult.Error(
                    response.message ?: "Invalid ticket."
                )
            }

        } catch (e: Exception) {
            Log.e("TicketManager", "Verification request failed", e)
         //   TicketResult.Error("Verification failed. Check network connection")
            TicketResult.Error("Código de bilhete não encontrado ou já foi utilizado.")

        }
    }
}