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


    /* ---------------- SUB ITEMS ---------------- */

    @Insert
    suspend fun insertSubItem(subItem: SubItemEntity): Long

    @Query("DELETE FROM sub_items WHERE id = :subItemId")
    suspend fun deleteSubItem(subItemId: Long)

    @Query("SELECT * FROM sub_items WHERE watchId = :watchId")
    suspend fun getSubItemsByWatchId(watchId: Long): List<SubItemEntity>


    /* ---------------- WATCH + SUBITEMS ---------------- */

    @Transaction
    @Query("SELECT * FROM watch_items")
    fun getWatchesWithSubItems(): Flow<List<WatchWithSubItems>>

    @Transaction
    @Query("SELECT * FROM watch_items WHERE id = :watchId")
    suspend fun getWatchWithSubItemsById(
        watchId: Long
    ): WatchWithSubItems?


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

}
