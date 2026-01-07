package com.cogniter.watchaccuracychecker.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey




//@Entity(tableName = "watch_items")
//data class WatchEntity(
//    @PrimaryKey(autoGenerate = true)
//    val id: Long = 0,
//
//    val title: String,
//    val watchImage: String,
//    val addedWatchTime: String,
//
//    val isWatchRunning: Boolean = false,
//
//    // Important for background-safe tracking
//    var startTimeMillis: Long? = null,
//    var elapsedTimeMillis: Long = 0
//)

@Entity(tableName = "watch_items")
data class WatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    var startTimeMillis: Long? = null,
    var elapsedTimeMillis: Long = 0,
    val isWatchRunning: Boolean = false,
    val watchImage: String,
    val addedWatchTime: String,
    val historyCount: Int = 0
)


