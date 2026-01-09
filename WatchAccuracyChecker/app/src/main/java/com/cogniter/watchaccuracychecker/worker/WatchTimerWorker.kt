package com.cogniter.watchaccuracychecker.worker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cogniter.watchaccuracychecker.Utila.NotificationHelper
import com.cogniter.watchaccuracychecker.Utila.TimeUtils
import com.cogniter.watchaccuracychecker.database.AppDatabase
import com.cogniter.watchaccuracychecker.utills.ImageUtils

class WatchTimerWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val context = applicationContext

        // ⚠️ Check Android 13+ permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("WatchWorker", "Notification permission not granted")
            return Result.success()
        }

        val db = AppDatabase.getDatabase(context)
        val watchDao = db.watchDao()

        val runningWatches = watchDao.getRunningWatches()
        if (runningWatches.isEmpty()) return Result.success()

        val now = System.currentTimeMillis()
        val intervalHours =
            ImageUtils.getNotifcationTimeFromSharedPreferences(context, "notificationTime", 2)
        val intervalMillis = intervalHours * 60 * 60 * 1000L

        val lastNotificationTime = ImageUtils.getLastNotificationTime(context)

        // 🔥 Only notify if interval passed
        if (lastNotificationTime == 0L || now - lastNotificationTime >= intervalMillis) {

            val watch = runningWatches.first() // you can loop if multiple watches

            val beginMillis = TimeUtils.beginTimeToMillis(watch.beginTime) ?: return Result.success()
            val elapsedHours = ((now - beginMillis) / (1000 * 60 * 60)).toInt()

            NotificationHelper(context).showNotification(
                watchId = watch.id.toInt(),
                watchName = watch.title,
                hours = elapsedHours
            )

            // Save last notification time
            ImageUtils.saveLastNotificationTime(context, now)

            Log.d("WatchWorker", "Notification sent for ${watch.title}")
        } else {
            // Interval not passed yet → do nothing
            Log.d("WatchWorker", "Interval not passed yet")
        }

        return Result.success()
    }
}
