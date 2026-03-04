package dev.knsalvaterra.kezir.api

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.annotations.SerializedName
import dev.knsalvaterra.kezir.data.AppDatabase
import dev.knsalvaterra.kezir.data.StoredTicket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> true
            else -> false
        }
    }

    private suspend fun verifyTicketOffline(context: Context, code: String): TicketResult {
        val ticketDao = AppDatabase.getDatabase(context).ticketDao()
        val tkt = withContext(Dispatchers.IO) {
            ticketDao.getTicketByCode(code)
        }

        return if (tkt != null) {
            val mockOrder = Order(
                buyer_name = tkt.buyerName,
                buyer_phone = tkt.buyerPhone,
                tickets = listOf(
                    Ticket(
                        ticket_type = tkt.ticketType,
                        ticket_name = tkt.ticketName,
                        table_capacity = tkt.tableCapacity,
                        quantity = tkt.quantity
                    )
                )
            )
            TicketResult.Success(
                "Código verificado offline!",
                mockOrder
            )
        } else {
            TicketResult.Error("Código de bilhete offline inválido")
        }
    }

    @SuppressLint("SuspiciousIndentation")
    suspend fun evaluateTicket(
        context: Context,
        sessionCookie: String,
        code: String,
        eventId: String?
    ): TicketResult {
        if (eventId == null) return TicketResult.Error("Event ID missing")

        return try {//online
            val response = ApiClient.api.verifyCode(sessionCookie, VerifyRequest(code, eventId))

            if (response.success && response.order != null) {
                //  cache this ticket in Room for future offline use
                cacheTicketLocally(context, code, eventId, response.order)

                TicketResult.Success(response.message ?: "Sucesso", response.order)
            } else {
                TicketResult.Error(response.message ?: "Bilhete inválido")
            }
        } catch (e: Exception) {
            // offline  network fails, check local database
            Log.e("TicketManager", "Network failed, checking offline DB")
            verifyTicketOffline(context, code)
        }
    }

    private suspend fun cacheTicketLocally(context: Context, code: String, eventId: String, order: Order) {
        val ticketDao = AppDatabase.getDatabase(context).ticketDao()
        val stored = StoredTicket(
            code = code,
            eventId = eventId,
            buyerName = order.buyer_name,
            buyerPhone = order.buyer_phone,
            ticketType = order.tickets.firstOrNull()?.ticket_type ?: "",
            ticketName = order.tickets.firstOrNull()?.ticket_name ?: "",
            quantity = order.tickets.firstOrNull()?.quantity ?: "1"
            ,
            tableCapacity = null
        )
        withContext(Dispatchers.IO) { ticketDao.insertTicket(stored) }
    }
}