package com.cogniter.watchaccuracychecker.utills

// TimerDataHolder.kt
object NotificationTimeHolder {
    val notificationtimers: MutableMap<Int, NotificationTimeData> = mutableMapOf()
}
// TimerData.kt
data class NotificationTimeData(
    var id: Int,
    var time: Int,
    var name: String,
)
