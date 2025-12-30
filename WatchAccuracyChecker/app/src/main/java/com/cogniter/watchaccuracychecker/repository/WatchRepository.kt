package com.cogniter.watchaccuracychecker.repository

import androidx.lifecycle.LiveData
import com.cogniter.watchaccuracychecker.database.WatchDao
import com.cogniter.watchaccuracychecker.database.entity.*
import kotlinx.coroutines.flow.Flow

class WatchRepository(
    private val dao: WatchDao
) {

    fun isWatchRunning(watchId: Long): LiveData<Boolean> {
        return dao.isWatchRunning(watchId)
    }


    suspend fun getWatchTitleById(watchId: Long): String? =
        dao.getWatchById(watchId)?.title

    fun getAllWatches(): Flow<List<WatchEntity>> =
        dao.getAllWatches()

    fun getWatchesWithSubItems(): Flow<List<WatchWithSubItems>> =
        dao.getWatchesWithSubItems()

    suspend fun insertWatch(watch: WatchEntity): Long =
        dao.insertWatch(watch)

    suspend fun updateWatch(watch: WatchEntity) =
        dao.updateWatch(watch)

    suspend fun deleteWatch(watchId: Long) =
        dao.deleteWatch(watchId)

    suspend fun insertSubItem(subItem: SubItemEntity) =
        dao.insertSubItem(subItem)

    suspend fun deleteSubItem(subItemId: Long) =
        dao.deleteSubItem(subItemId)

    suspend fun updateRunningState(
        watchId: Long,
        isRunning: Boolean,
        startTime: Long?
    ) {
        dao.updateRunningState(watchId, isRunning, startTime)
    }

    suspend fun getWatchById(watchId: Long): WatchEntity? =
        dao.getWatchById(watchId)

    /**
     * 🔒 CRITICAL METHOD
     * Ensures Watch row exists before inserting SubItem
     * Prevents FOREIGN KEY crash
     */
//    suspend fun ensureWatchExists(
//        watchId: Long,
//        name: String
//    ) {
//        val exists = dao.watchExists(watchId)
//        if (exists == 0) {
//            dao.insertWatch(
//                WatchEntity(
//                    id = watchId,
//                    title = name,
//                    isWatchRunning = false
//                )
//            )
//        }
//    }
}
