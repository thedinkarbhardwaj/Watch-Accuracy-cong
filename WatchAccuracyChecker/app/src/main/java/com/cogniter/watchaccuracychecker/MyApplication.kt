package com.cogniter.watchaccuracychecker


import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "App started, initializing WorkManager")

//        WorkManager.initialize(
//            this,
//            Configuration.Builder()
//                .setMinimumLoggingLevel(Log.DEBUG)
//                .build()
//        )
    }
}
