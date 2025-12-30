package com.cogniter.watchaccuracychecker.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WatchWithSubItems(
    @Embedded val watch: WatchEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "watchId"
    )
    val subItems: List<SubItemEntity>
)
