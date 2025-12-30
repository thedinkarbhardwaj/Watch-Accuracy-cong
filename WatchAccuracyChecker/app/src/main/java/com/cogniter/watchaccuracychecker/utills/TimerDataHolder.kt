package com.cogniter.watchaccuracychecker.utills

// TimerDataHolder.kt
object TimerDataHolder {
    val timers: MutableMap<Int, TimerData> = mutableMapOf()
}
// TimerData.kt
//data class TimerData(
//    var startTime: Long,
//    var elapsedTime: Long,
//    var isRunning: Boolean
//)


data class TimerData(
    var startTime: Long,
    var elapsedTime: Long,
    var isRunning: Boolean,
    var runnable: Runnable? = null
)