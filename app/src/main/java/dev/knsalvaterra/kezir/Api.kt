import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class VerifyRequest(
    val code: String,
    val event_id: String
)


//{
//    "success": true,
//    "message": "Código verificado e marcado como resgatado!",
//    "order": {
//        "buyer_name": "Dalila Rita",
//        "buyer_phone": "+2399974821",
//        "tickets": [
//            {
//                "ticket_type": "vip",
//                "ticket_name": "Normal",
//                "table_capacity": null,
//                "quantity": "4"
//            }
//        ]
//    }
//}
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

data class PinRequest(
    val pin: String,
    val event_id: String
)

data class PinResponse(
    val success: Boolean,
    val message: String?
)
interface ApiService {

    //  Get Authorized
    @POST("api/box-office/verify-pin.php")
    suspend fun verifyPin(
        @Body request: PinRequest
    ): retrofit2.Response<PinResponse> // We use 'Response' wrapper to read headers

    //Verify the Ticket (now with a Cookie header!)
    @POST("api/box-office/verify-code.php")
    suspend fun verifyCode(
        @Header("Cookie") sessionCookie: String, // This is the "Passport"
        @Body request: VerifyRequest
    ): VerifyResponse
}

object ApiClient {
   //data recebida JSON -> Objecto
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://stage.kezir.st/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}