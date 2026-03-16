package dev.knsalvaterra.kezir.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

//instructions to interact with the database
@Dao
interface TicketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: StoredTicket)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<StoredTicket>)

    @Query("SELECT * FROM offline_tickets WHERE code = :code")
    suspend fun getTicketByCode(code: String): StoredTicket?

    @Query("UPDATE offline_tickets SET isUsed = 1, usedAt = :timestamp, syncStatus = 'DIRTY' WHERE code = :code")
    suspend fun markAsUsedOffline(code: String, timestamp: Long)

    @Query("SELECT * FROM offline_tickets WHERE syncStatus = 'DIRTY'")
    suspend fun getUnsyncedTickets(): List<StoredTicket>
}