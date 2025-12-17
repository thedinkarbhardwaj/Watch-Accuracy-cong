package com.cogniter.watchaccuracychecker.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.database.DBHelper
import com.cogniter.watchaccuracychecker.utills.ImageUtils
import com.cogniter.watchaccuracychecker.utills.ImageUtils.isAppInForeground
import com.cogniter.watchaccuracychecker.utills.NotificationTimeData
import com.cogniter.watchaccuracychecker.utills.NotificationTimeHolder
import com.cogniter.watchaccuracychecker.utills.TimerData
import com.cogniter.watchaccuracychecker.utills.TimerDataHolder


class TimerService : Service() {

    private val timers = TimerDataHolder.timers
    private val notificationtimers = NotificationTimeHolder.notificationtimers
    private val handler = Handler()

    private fun sendTimerUpdate(timerId: Int,timeTaken: Long,running: Boolean) {
        val intent = Intent(ACTION_TIMER_UPDATE)
        intent.putExtra(EXTRA_TIMER_VALUE, timeTaken)
        intent.putExtra(EXTRA_TIMER_ID, timerId)
        intent.putExtra(IS_RUNNING,running)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

//
       val hours: Int = (timeTaken / (1000 * 60 * 60)).toInt()


        if(notificationtimers[timerId]!!.time==hours){

        }else{
            if(hours>0){
                notificationtimers[timerId]!!.time =hours

                if (notificationtimers[timerId]!!.time % ImageUtils.getNotifcationTimeFromSharedPreferences(this,"notificationTime",2) == 0) {
                     if(!isAppInForeground(this)){
                    System.out.println("kfiofkokfofkokfofkks     "+"show notifi")
                    showNotification(this,notificationtimers[timerId]!!.time,notificationtimers[timerId]!!.name,timerId)
                      }

                }
            }



        }




    }


    companion object {

        fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        private const val NOTIFICATION_ID = -1
         const val ACTION_TIMER_UPDATE = "com.example.timerapp.TIMER_UPDATE"
         const val EXTRA_TIMER_VALUE = "timerValue"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"

        const val ACTION_START_TIMER = "START_TIMER"
        const val ACTION_STOP_TIMER = "STOP_TIMER"
        const val ACTION_RESET_TIMER = "RESET_TIMER"

        const val EXTRA_TIMER_ID = "EXTRA_TIMER_ID"
        const val IS_RUNNING = "IS_RUNNING"


    }

    private fun startTimer(timerId: Int) {

        if (!timers.containsKey(timerId)) {
            var dbHelper = DBHelper(this!!)
            var watchname= dbHelper.getTitleFromItemId(timerId.toLong())
            timers[timerId] = TimerData(System.currentTimeMillis(), 0L, true)
            notificationtimers[timerId]= NotificationTimeData(timerId,0,watchname!!)
            handler.postDelayed({ updateTimer(timerId) }, 0)
        }else{
            // Restart the existing timer
            val existingTimer = timers[timerId]
            existingTimer!!.startTime = System.currentTimeMillis()
            existingTimer!!.elapsedTime = 0L
            existingTimer!!.isRunning = true
            //val restartDelay = 1000L // Specify the restart delay in milliseconds
            handler.postDelayed({ updateTimer(timerId) }, 0)
            Log.d("TimerService", "Restarted timer for timerId: $timerId")
        }
    }

    private fun updateTimer(timerId: Int) {

        val timerData = timers[timerId]
        if (timerData != null && timerData.isRunning) {
            timerData.elapsedTime = System.currentTimeMillis() - timerData.startTime
            sendTimerUpdate(timerId, timerData.elapsedTime,timerData.isRunning)
            handler.postDelayed({ updateTimer(timerId) }, 0)
        }else{
            Log.d("TimerService", "Timer $timerId is stopped")
        }
    }

    private fun stopTimer(timerId: Int) {

        val timerData = timers[timerId]

        if (timerData != null && timerData.isRunning) {
            timerData.isRunning = false
            handler.removeCallbacksAndMessages(timerData.isRunning ) // Remove any pending callbacks and messages
            sendTimerUpdate(timerId, 0L,timerData.isRunning)
            timers.remove(timerId)
            notificationtimers.remove(timerId)
        }
    }

    private fun resetTimer(timerId: Int) {

        val timerData = timers[timerId]
        if (timerData != null) {
            timerData.isRunning = false
            timerData.elapsedTime = 0L
            handler.removeCallbacksAndMessages(timerData.isRunning ) // Remove any pending callbacks and messages
            sendTimerUpdate(timerId, 0L,timerData.isRunning)
            //  timers.remove(timerId)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopService()
        }else if (intent?.action == ACTION_RESET_TIMER) {
            resetTimer(intent.getIntExtra(EXTRA_TIMER_ID, -1))
        } else if (intent?.action == ACTION_START_TIMER) {
           startForeground(NOTIFICATION_ID, createNotification())
           startTimer(intent.getIntExtra(EXTRA_TIMER_ID, -1))
        }else if (intent?.action == ACTION_STOP_TIMER) {
            stopTimer(intent.getIntExtra(EXTRA_TIMER_ID, -1))
        }
        return START_STICKY
    }



    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
    private fun createNotification(): Notification {
        val channelId = "timer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Timer Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentText("Watch accuracy service is running...")
            .setSmallIcon(R.drawable.app_icon)
            .setSound(Uri.EMPTY) // Set the notification sound to null
           // .setOngoing(true) // Make the notification ongoing
            .build()
    }

//    private fun createNotification(): Notification {
//        System.out.println("flrlfolfl   "+"forground notifi")
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel("123456", "Timer Service", NotificationManager.IMPORTANCE_DEFAULT)
//            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
//        }
//
//        return NotificationCompat.Builder(this, "123456")
//            .setContentText("Watch accuracy service is running...")
//            .setSmallIcon(R.drawable.app_icon)
//            .build()
//    }
    // Method to stop the service
    private fun stopService() {

        stopForeground(true)  // Remove the service from the foreground state
        stopSelf()  // Stop the service
    handler.removeCallbacksAndMessages(null ) // Remove any pending callbacks and messages
    }

    private  fun showNotification(
        context: Context?,
        timeTaken: Int,
        watchname: String,
        timerId: Int
    ) {

        Log.e("AlarmReceive ", "testing called broadcast called")

        Log.d("before touch timer id  ",timerId.toString())
        Log.d("before touch watchname  ",watchname)
          val intent = Intent(this, MainActivity::class.java)
          intent.putExtra("fromSplash",false)
          intent.putExtra("fromNotification", true)
          intent.putExtra("watchname", watchname)
          intent.putExtra("timerId", timerId)
         val pendingIntent = PendingIntent.getActivity(this, timerId, intent,
            PendingIntent.FLAG_IMMUTABLE)


        // Build the notification
        val notification = NotificationCompat.Builder(context!!, "1234")
            .setSmallIcon(R.drawable.app_icon)
            .setContentText("Tracking for $watchname watch has been on for $timeTaken hours.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)  // Dismiss the notification when clicked

        // Get the NotificationManager
        val notificationManager = NotificationManagerCompat.from(context!!)

        // Create a notification channel for devices running API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "1234",
                "1234",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Display the notification
        notificationManager.notify(timerId, notification.build())
    }

}
