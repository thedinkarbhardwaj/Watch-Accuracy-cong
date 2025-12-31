package com.cogniter.watchaccuracychecker.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.database.AppDatabase
import com.cogniter.watchaccuracychecker.repository.WatchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SplashActivity : AppCompatActivity() {
    private val SPLASH_TIME_OUT: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val db = AppDatabase.getDatabase(this@SplashActivity)

        val repository = WatchRepository(db.watchDao())


//        lifecycleScope.launch(Dispatchers.IO) {
//            repository.stopAllRunningWatchesOnAppStart()
//        }
//
//
//
//        Handler().postDelayed({
//            // This method will be executed once the timer is over
//            // Start your app main activity
//            val i = Intent(this, MainActivity::class.java)
//            i.putExtra("fromSplash",true)
//            i.putExtra("fromNotification", false)
//            startActivity(i)
//
//            // close this activity
//            finish()
//        }, SPLASH_TIME_OUT)


//        lifecycleScope.launch {
//            repository.stopAllRunningWatchesOnAppStart() // suspend function
//            // Only start MainActivity AFTER this is done
//            kotlinx.coroutines.delay(3000) // splash delay
//            val i = Intent(this@SplashActivity, MainActivity::class.java)
//            i.putExtra("fromSplash", true)
//            i.putExtra("fromNotification", false)
//            startActivity(i)
//            finish()
//        }


        // SplashActivity
        runBlocking {
            repository.stopAllRunningWatchesOnAppStart()
        }

// then start MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()




    }
}
