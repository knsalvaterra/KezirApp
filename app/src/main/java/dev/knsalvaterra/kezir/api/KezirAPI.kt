package dev.knsalvaterra.kezir.api

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dev.knsalvaterra.kezir.R
import dev.knsalvaterra.kezir.data.AppDatabase
import dev.knsalvaterra.kezir.data.StoredTicket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


@Suppress("all")
sealed class TicketResult {

    data class Success(
        val message: String,
        val order: Order?
    ) : TicketResult()


    data class Error(
        val message: String,
        val order: Order? = null
    ) : TicketResult()
}

data class Event(
    val id: String,
    val title: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("end_time") val endTime: String,
    val location: String,
    @SerializedName("cover_image") val coverImage: String?
)

data class SearchEventsResponse(
    val success: Boolean,
    val events: List<Event>
)

data class PinRequest(
    val pin: String,
    val event_id: String?
)


data class PinResponse(
    val success: Boolean,
    val message: String?
)


data class VerifyRequest(
    val event_id: String,
    val code: String,
    val pin: String
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
        @Body request: VerifyRequest
    ): Response<VerifyResponse>

    @GET("api/box-office/events.php")
    suspend fun searchEvents(
        @Query("query") query: String
    ): Response<SearchEventsResponse>
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

// ticket related

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

  //  private suspend fun verifyTicketOffline(context: Context, code: String): TicketResult {
  //      val ticketDao = AppDatabase.getDatabase(context).ticketDao()
  //      val tkt = withContext(Dispatchers.IO) {
  //          ticketDao.getTicketByCode(code)
  //      }
//
  //      return if (tkt != null) {
  //          val mockOrder = Order(
  //              buyer_name = tkt.buyerName,
  //              buyer_phone = tkt.buyerPhone,
  //              tickets = listOf(
  //                  Ticket(
  //                      ticket_type = tkt.ticketType,
  //                      ticket_name = tkt.ticketName,
  //                      table_capacity = tkt.tableCapacity,
  //                      quantity = tkt.quantity
  //                  )
  //              )
  //          )
  //          TicketResult.Success(
  //              context.getString(R.string.ticket_verified_offline),
  //              mockOrder
  //          )
  //      } else {
  //          TicketResult.Error(context.getString(R.string.ticket_error_offline_invalid))
  //      }
  //  }

    @SuppressLint("SuspiciousIndentation")
    suspend fun evaluateTicket(
        context: Context,
        pin: String,
        code: String,
        eventId: String?
    ): TicketResult {
        if (eventId == null) {
            return TicketResult.Error(context.getString(R.string.ticket_error_no_id))
        }

        if (!isNetworkAvailable(context)) {
            return TicketResult.Error(context.getString(R.string.login_failed_check_connection))
        }

        return try {
            val response = ApiClient.api.verifyCode(
                VerifyRequest(eventId, code, pin)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {


                    TicketResult.Success(
                        body.message ?: context.getString(R.string.status_subtitle_success),
                        body.order
                    )
                } else {
                    // backend given ticket is invalid or already used
                    TicketResult.Error(
                        message = body?.message ?: context.getString(R.string.ticket_error_default),
                        order = body?.order
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val parsedError = try { Gson().fromJson(errorBody, VerifyResponse::class.java) } catch (e: Exception) { null }

                val message = when {
                    parsedError?.message != null -> parsedError.message
                    else -> context.getString(R.string.ticket_error_default)
                }
                
                TicketResult.Error(
                    message = message,
                    order = parsedError?.order
                )
            }
        } catch (e: Exception) {
            Log.e("TicketManager", "Verification request failed", e)
            
            val errorMessage = if (!isNetworkAvailable(context)) {
                context.getString(R.string.login_failed_check_connection)
            } else {
                "Erro de comunicação com o servidor. Tente novamente."
            }
            TicketResult.Error(errorMessage)
        }
    }

  //  private suspend fun cacheTicketLocally(context: Context, code: String, eventId: String, order: Order) {
  //      val ticketDao = AppDatabase.getDatabase(context).ticketDao()
  //      val stored = StoredTicket(
  //          code = code,
  //          eventId = eventId,
  //          buyerName = order.buyer_name,
  //          buyerPhone = order.buyer_phone,
  //          ticketType = order.tickets.firstOrNull()?.ticket_type ?: "",
  //          ticketName = order.tickets.firstOrNull()?.ticket_name ?: "",
  //          quantity = order.tickets.firstOrNull()?.quantity ?: "1"
  //          ,
  //          tableCapacity = null
  //      )
  //      withContext(Dispatchers.IO) { ticketDao.insertTicket(stored) }
  //  }
}
