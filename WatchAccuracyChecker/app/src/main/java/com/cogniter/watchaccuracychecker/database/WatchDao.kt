package com.cogniter.watchaccuracychecker.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cogniter.watchaccuracychecker.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchDao {

    /* ---------------- WATCH ---------------- */

    @Query("SELECT COUNT(*) FROM watch_items WHERE id = :watchId")
    suspend fun watchExists(watchId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatch(watch: WatchEntity): Long

    @Update
    suspend fun updateWatch(watch: WatchEntity)

    @Query("DELETE FROM watch_items WHERE id = :watchId")
    suspend fun deleteWatch(watchId: Long)

    @Query("SELECT * FROM watch_items")
    fun getAllWatches(): Flow<List<WatchEntity>>

    @Query("SELECT * FROM watch_items WHERE id = :watchId LIMIT 1")
    suspend fun getWatchById(watchId: Long): WatchEntity?

    @Query("""
UPDATE watch_items 
SET historyCount = historyCount + 1 
WHERE id = :watchId
""")
    suspend fun incrementHistoryCount(watchId: Long)

    @Query("""
UPDATE watch_items 
SET historyCount = 
    CASE 
        WHEN historyCount > 0 THEN historyCount - 1 
        ELSE 0 
    END
WHERE id = :watchId
""")
    suspend fun decrementHistoryCount(watchId: Long)


    @Query("SELECT watchId FROM sub_items WHERE id = :subItemId")
    suspend fun getWatchIdBySubItemId(subItemId: Long): Long

    @Transaction
    suspend fun deleteHistoryAndUpdateCount(subItemId: Long) {
        val watchId = getWatchIdBySubItemId(subItemId)
        deleteSubItem(subItemId)
        decrementHistoryCount(watchId)
    }



    /* ---------------- SUB ITEMS ---------------- */

    @Insert
    suspend fun insertSubItem(subItem: SubItemEntity): Long

    @Query("DELETE FROM sub_items WHERE id = :subItemId")
    suspend fun deleteSubItem(subItemId: Long)

    @Query("SELECT * FROM sub_items WHERE watchId = :watchId")
    suspend fun getSubItemsByWatchId(watchId: Long): List<SubItemEntity>


    /* ---------------- WATCH + SUBITEMS ---------------- */

//    @Transaction
//    @Query("SELECT * FROM watch_items")
//    fun getWatchesWithSubItems(): Flow<List<WatchWithSubItems>>


    @Query("SELECT * FROM watch_items")
    fun getWatchesWithSubItems(): List<WatchWithSubItems>

    @Transaction
    @Query("SELECT * FROM watch_items WHERE id = :watchId")
    suspend fun getWatchWithSubItemsById(
        watchId: Long
    ): WatchWithSubItems?


//    @Transaction
//    @Query("""
//SELECT * FROM watch_items
//WHERE id IN (SELECT DISTINCT watchId FROM sub_items)
//""")
//    fun getOnlyWatchesWithHistory(): List<WatchWithSubItems>



    /* ---------------- RUN STATE ---------------- */

    @Query("""
        UPDATE watch_items
        SET isWatchRunning = :isRunning,
            startTimeMillis = :startTime
        WHERE id = :watchId
    """)
    suspend fun updateRunningState(
        watchId: Long,
        isRunning: Boolean,
        startTime: Long?
    )

    @Query("SELECT isWatchRunning FROM watch_items WHERE id = :watchId")
    fun isWatchRunning(watchId: Long): LiveData<Boolean>

    @Query("""
    UPDATE watch_items 
    SET elapsedTimeMillis = :elapsed,
        isWatchRunning = :isRunning
    WHERE id = :watchId
""")
    suspend fun updateElapsedTime(
        watchId: Long,
        elapsed: Long,
        isRunning: Boolean
    )

    @Query("""
    UPDATE watch_items
    SET elapsedTimeMillis = :elapsed,
        isWatchRunning = :running
    WHERE id = :watchId
""")
    suspend fun updateTimer(
        watchId: Long,
        elapsed: Long,
        running: Boolean
    )


    @Query("""
UPDATE watch_items
SET 
    elapsedTimeMillis = elapsedTimeMillis + 
        CASE 
            WHEN startTimeMillis > 0 THEN (:now - startTimeMillis)
            ELSE 0
        END,
    isWatchRunning = 0,        -- ✅ sets running state to false
    startTimeMillis = 220        -- resets start time
WHERE isWatchRunning = 1       -- only affects currently running watches
""")
    suspend fun stopAllRunningWatches(now: Long)



}
