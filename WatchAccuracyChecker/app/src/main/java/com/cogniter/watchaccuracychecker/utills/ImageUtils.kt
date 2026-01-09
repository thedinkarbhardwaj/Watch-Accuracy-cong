package com.cogniter.watchaccuracychecker.utills

import android.app.ActivityManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.io.IOException


object ImageUtils {

    private const val PREF_NAME = "watch_prefs"
    private const val KEY_NOTIFICATION_INTERVAL = "notificationTime"
    private const val KEY_LAST_NOTIFICATION_TIME = "last_notification_time"

    // Get notification interval (in hours)
    fun getNotifcationTimeFromSharedPreferences(context: Context, key: String, defaultValue: Int): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(key, defaultValue)
    }

    fun saveNotifcationTimeToSharedPreferences(context: Context, key: String, value: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(key, value)
            .apply()
    }

    // 🔥 For preventing duplicate notifications
    fun getLastNotificationTime(context: Context): Long {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_NOTIFICATION_TIME, 0L)
    }

    fun saveLastNotificationTime(context: Context, timeMillis: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_NOTIFICATION_TIME, timeMillis)
            .apply()
    }

    fun clearLastNotificationTime(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LAST_NOTIFICATION_TIME)
            .apply()
    }

    fun getImageUriFromName(context: Context, imageName: String): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(imageName)
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID))
            cursor.close()
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        } else {
            // Image not found
            return null
        }
    }
}

