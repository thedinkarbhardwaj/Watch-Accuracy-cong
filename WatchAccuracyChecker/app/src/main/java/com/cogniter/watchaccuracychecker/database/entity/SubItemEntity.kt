package com.cogniter.watchaccuracychecker.database.entity


import androidx.room.*

@Entity(
    tableName = "sub_items",
    foreignKeys = [
        ForeignKey(
            entity = WatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["watchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("watchId")]
)
data class SubItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val watchId: Long,
    val name: String,
    val image: String,
    val date: String
)

