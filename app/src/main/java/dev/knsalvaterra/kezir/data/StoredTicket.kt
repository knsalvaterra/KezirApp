package dev.knsalvaterra.kezir.data

import androidx.room.Entity
import androidx.room.PrimaryKey
//todo
@Entity(tableName = "offline_tickets")
data class StoredTicket(
    @PrimaryKey val code: String,
    val ticketType: String,
    val holderName: String,

    val eventId: String,
    val buyerName: String,
    val buyerPhone: String,
    val isUsed: Boolean = false,
    val usedAt: Long? = null,
    // track if this change has been sent to the server
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

enum class SyncStatus {
    SYNCED,     // Matches the server
    DIRTY       // Changed offline, needs uploading Uploading
}