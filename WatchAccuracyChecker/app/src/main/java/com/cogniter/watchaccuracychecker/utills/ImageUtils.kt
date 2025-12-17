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

    fun isDarkModeEnabled(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
    fun saveNotifcationTimeToSharedPreferences(context: Context, key: String, value: Int) {
        val sharedPreferences = context.getSharedPreferences("my_shared_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }
    fun getNotifcationTimeFromSharedPreferences(context: Context, key: String, defaultValue: Int): Int {
        val sharedPreferences = context.getSharedPreferences("my_shared_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getInt(key, defaultValue)
    }
    fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = context.packageName

        val runningAppProcesses = activityManager.runningAppProcesses

        runningAppProcesses?.let {
            for (processInfo in it) {
                if (processInfo.processName == packageName && processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return true
                }
            }
        }
        return false
    }

}
