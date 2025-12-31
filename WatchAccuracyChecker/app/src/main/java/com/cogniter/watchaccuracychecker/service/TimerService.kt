package com.cogniter.watchaccuracychecker.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.database.AppDatabase
import com.cogniter.watchaccuracychecker.database.WatchDao
import com.cogniter.watchaccuracychecker.repository.WatchRepository
import com.cogniter.watchaccuracychecker.utills.NotificationTimeData
import com.cogniter.watchaccuracychecker.utills.NotificationTimeHolder
import com.cogniter.watchaccuracychecker.utills.TimerData
import com.cogniter.watchaccuracychecker.utills.TimerDataHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val timers = TimerDataHolder.timers
    private val notificationTimers = NotificationTimeHolder.notificationtimers
    private val handler = Handler()
    private lateinit var watchDao: WatchDao


    private lateinit var watchRepository: WatchRepository

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        watchRepository = WatchRepository(db.watchDao())

        watchDao = db.watchDao()
        watchRepository = WatchRepository(watchDao)


    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------------
    // TIMER CONTROL
    // ------------------------------------

    private fun startTimer(timerId: Int) {
        val existingTimer = timers[timerId]

        if (existingTimer == null) {
            CoroutineScope(Dispatchers.IO).launch {
                val watchName =
                    watchRepository.getWatchTitleById(timerId.toLong()) ?: "Unknown"

                val timer = TimerData(
                    startTime = System.currentTimeMillis(),
                    elapsedTime = 0L,
                    isRunning = true
                )

                timers[timerId] = timer
                notificationTimers[timerId] =
                    NotificationTimeData(timerId, 0, watchName)

                startRunnable(timerId)
            }
        } else {
            // Resume timer (DO NOT RESET)
            existingTimer.startTime =
                System.currentTimeMillis() - existingTimer.elapsedTime
            existingTimer.isRunning = true
            startRunnable(timerId)
        }
    }

    private fun startRunnable2(timerId: Int) {
        val timer = timers[timerId] ?: return

        val runnable = object : Runnable {
            override fun run() {
                if (!timer.isRunning) return

                timer.elapsedTime =
                    System.currentTimeMillis() - timer.startTime

                sendTimerUpdate(timerId, timer.elapsedTime, true)

                handler.postDelayed(this, 1000)
            }
        }

        timer.runnable = runnable
        handler.post(runnable)
    }

    private fun startRunnable(timerId: Int) {
        val timer = timers[timerId] ?: return

        val runnable = object : Runnable {
            override fun run() {
                if (!timer.isRunning) return

                timer.elapsedTime =
                    System.currentTimeMillis() - timer.startTime

                // ✅ UPDATE ROOM (SINGLE SOURCE OF TRUTH)
                CoroutineScope(Dispatchers.IO).launch {
                    watchDao.updateElapsedTime(
                        watchId = timerId.toLong(),
                        elapsed = timer.elapsedTime,
                        isRunning = true
                    )
                }

                // UI broadcast (ClockFragment only)
                sendTimerUpdate(timerId, timer.elapsedTime, true)

                handler.postDelayed(this, 1000)
            }
        }

        timer.runnable = runnable
        handler.post(runnable)
    }


    private fun stopTimer2(timerId: Int) {
        val timer = timers[timerId] ?: return

        timer.isRunning = false
        timer.runnable?.let { handler.removeCallbacks(it) }

        sendTimerUpdate(timerId, timer.elapsedTime, false)

        timers.remove(timerId)
        notificationTimers.remove(timerId)
    }

    private fun stopTimer(timerId: Int) {
        val timer = timers[timerId] ?: return

        timer.isRunning = false
        timer.runnable?.let { handler.removeCallbacks(it) }

        CoroutineScope(Dispatchers.IO).launch {
            watchDao.updateElapsedTime(
                watchId = timerId.toLong(),
                elapsed = timer.elapsedTime,
                isRunning = false
            )
        }

        sendTimerUpdate(timerId, timer.elapsedTime, false)

        timers.remove(timerId)
        notificationTimers.remove(timerId)
    }


    private fun resetTimer2(timerId: Int) {
        val timer = timers[timerId] ?: return

        timer.isRunning = false
        timer.elapsedTime = 0L
        timer.runnable?.let { handler.removeCallbacks(it) }

        sendTimerUpdate(timerId, 0L, false)
    }

    private fun resetTimer(timerId: Int) {
        val timer = timers[timerId] ?: return

        timer.isRunning = false
        timer.elapsedTime = 0L
        timer.runnable?.let { handler.removeCallbacks(it) }

        CoroutineScope(Dispatchers.IO).launch {
            watchDao.updateElapsedTime(
                watchId = timerId.toLong(),
                elapsed = 0L,
                isRunning = false
            )
        }

        sendTimerUpdate(timerId, 0L, false)
    }


    // ------------------------------------
    // BROADCAST UPDATE
    // ------------------------------------

    private fun sendTimerUpdate(
        timerId: Int,
        elapsedTime: Long,
        isRunning: Boolean
    ) {
        val intent = Intent(ACTION_TIMER_UPDATE).apply {
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_TIMER_VALUE, elapsedTime)
            putExtra(IS_RUNNING, isRunning)
        }

        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)

        val hours = (elapsedTime / (1000 * 60 * 60)).toInt()

        notificationTimers[timerId]?.let { notif ->
            if (hours > 0 && notif.time != hours) {
                notif.time = hours
                if (hours % 2 == 0) {
                    showNotification(timerId, notif.name, hours)
                }
            }
        }
    }

    // ------------------------------------
    // SERVICE COMMANDS
    // ------------------------------------

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        intent ?: return START_STICKY

        val timerId = intent.getIntExtra(EXTRA_TIMER_ID, -1)

        when (intent.action) {
            ACTION_START_TIMER -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForeground(
                        NOTIFICATION_ID,
                        createForegroundNotification()
                    )
                }
                startTimer(timerId)
            }

            ACTION_STOP_TIMER -> stopTimer(timerId)
            ACTION_RESET_TIMER -> resetTimer(timerId)
            ACTION_STOP_SERVICE -> stopService()
        }

        return START_STICKY
    }

    private fun stopService() {
        stopForeground(true)
        stopSelf()
    }

    // ------------------------------------
    // NOTIFICATIONS
    // ------------------------------------

    private fun createForegroundNotification(): Notification {
        val channelId = "timer_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentText("Watch accuracy service running check")
            .setSound(Uri.EMPTY)
            .build()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(
        timerId: Int,
        watchName: String,
        hours: Int
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("timerId", timerId)
            putExtra("watchName", watchName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            timerId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "watch_notification_channel"
        val manager = NotificationManagerCompat.from(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Watch Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentText(
                "Tracking $watchName watch for $hours hours"
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(timerId, notification)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("TimerService", "App cleared from Recents, stopping running watches")

        // Ensure all running watches are stopped in DB
        CoroutineScope(Dispatchers.IO).launch {
            try {
                watchRepository.stopAllRunningWatchesOnAppStart()
            } catch (e: Exception) {
                Log.e("TimerService", "Error stopping watches on task removed", e)
            }
        }

        // Stop the service
        stopForeground(true)
        stopSelf()

        super.onTaskRemoved(rootIntent)
    }



    override fun onDestroy() {
        timers.forEach { (_, timer) ->
            timer.runnable?.let { handler.removeCallbacks(it) }
        }
        timers.clear()
        notificationTimers.clear()
        super.onDestroy()
    }




    // ------------------------------------
    // CONSTANTS
    // ------------------------------------

    companion object {
        const val NOTIFICATION_ID = 1

        const val ACTION_START_TIMER = "START_TIMER"
        const val ACTION_STOP_TIMER = "STOP_TIMER"
        const val ACTION_RESET_TIMER = "RESET_TIMER"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"

        const val ACTION_TIMER_UPDATE = "TIMER_UPDATE"
        const val EXTRA_TIMER_ID = "EXTRA_TIMER_ID"
        const val EXTRA_TIMER_VALUE = "EXTRA_TIMER_VALUE"
        const val IS_RUNNING = "IS_RUNNING"

        fun isServiceRunning(
            context: Context,
            serviceClass: Class<*>
        ): Boolean {
            val manager =
                context.getSystemService(Context.ACTIVITY_SERVICE)
                        as ActivityManager

            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }
}
