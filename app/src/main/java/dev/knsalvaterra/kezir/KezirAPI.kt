package dev.knsalvaterra.kezir

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


//{
//    "success": true,
//    "message": "Código verificado e marcado como resgatado!",
//    "order": {
//    "buyer_name": "Dalila Rita",
//    "buyer_phone": "+2399974821",
//    "tickets": [
//    {
//        "ticket_type": "vip",
//        "ticket_name": "Normal",
//        "table_capacity": null,
//        "quantity": "4"
//    }
//    ]
//}
//}
data class VerifyRequest(
    val code: String,
    val event_id: String
)

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
    val event_id: String?
)

data class PinResponse(
    val success: Boolean,
    val message: String?
)

interface ApiService {

    @POST("api/box-office/verify-pin.php")
    suspend fun verifyPin( //public Response<PinResponse> verifyPin(@Body PinRequest request);
        @Body request: PinRequest
    ): retrofit2.Response<PinResponse>

    @POST("api/box-office/verify-code.php")
    suspend fun verifyCode( //public Response<VerifyResponse> verifyCode(@Header("Cookie") String sessionCookie, @Body VerifyRequest request);
        @Header("Cookie") sessionCookie: String,
        @Body request: VerifyRequest
    ): VerifyResponse
}
//singleton instance, Lazy<ApiService> api = Retrofit.Builder().baseUrl("https://stage.kezir.st/").addConverterFactory(GsonConverterFactory.create()).build().create(ApiService.class);
object ApiClient {

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://stage.kezir.st/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java) //JSON -> object
    }
}