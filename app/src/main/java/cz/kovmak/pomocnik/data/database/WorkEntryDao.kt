package cz.kovmak.pomocnik.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkEntryDao {

    @Query("SELECT * FROM work_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<WorkEntry>>

    @Query("SELECT * FROM work_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): WorkEntry?

    @Query("SELECT * FROM work_entries WHERE order_id LIKE '%' || :query || '%' OR description_cz LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchEntries(query: String): Flow<List<WorkEntry>>

    @Query("SELECT COUNT(*) FROM work_entries")
    fun getEntryCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: WorkEntry): Long

    @Update
    suspend fun updateEntry(entry: WorkEntry)

    @Delete
    suspend fun deleteEntry(entry: WorkEntry)

    @Query("DELETE FROM work_entries")
    suspend fun deleteAllEntries()

    @Query("SELECT * FROM work_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntries(limit: Int = 20): Flow<List<WorkEntry>>

    @Query("SELECT * FROM work_entries WHERE is_sent = 0 ORDER BY timestamp DESC")
    fun getUnsentEntries(): Flow<List<WorkEntry>>
}
