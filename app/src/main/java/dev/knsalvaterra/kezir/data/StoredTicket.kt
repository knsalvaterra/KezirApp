package dev.knsalvaterra.kezir.data

import androidx.room.Entity
import androidx.room.PrimaryKey
//todo
@Entity(tableName = "offline_tickets")
data class StoredTicket(
    @PrimaryKey
    val code: String,

    val buyerName: String,
    val buyerPhone: String,
    val ticketType: String,
    val ticketName: String,
    val tableCapacity: String?,
    val quantity: String,
    val eventId: String
)