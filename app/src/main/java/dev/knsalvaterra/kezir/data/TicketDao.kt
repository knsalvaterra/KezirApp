package dev.knsalvaterra.kezir.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TicketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: StoredTicket)

    @Query("SELECT * FROM offline_tickets WHERE code = :code")
    suspend fun getTicketByCode(code: String): StoredTicket?
}