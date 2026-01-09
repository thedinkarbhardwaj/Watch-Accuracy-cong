package com.cogniter.watchaccuracychecker.Utila

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.MainActivity
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "watch_notification_channel"
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Watch Tracking",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setSound(Uri.EMPTY, null)

            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    fun showNotification(watchId: Int, watchName: String, hours: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("watchId", watchId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            watchId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // use system icon for testing
            .setContentTitle("Watch Tracking")
            .setContentText("$watchName tracking active")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(watchId, notification)
    }
}

object TimeUtils {

    private const val FORMAT = "dd/MM/yyyy HH:mm:ss"  // match your beginTime

    fun beginTimeToMillis(beginTime: String?): Long? {
        if (beginTime.isNullOrEmpty()) return null

        return try {
            val sdf = java.text.SimpleDateFormat(FORMAT, java.util.Locale.getDefault())
            sdf.parse(beginTime)?.time
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

