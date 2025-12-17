package com.cogniter.watchaccuracychecker.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.cogniter.watchaccuracychecker.R

class SplashActivity : AppCompatActivity() {
    private val SPLASH_TIME_OUT: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            // This method will be executed once the timer is over
            // Start your app main activity
            val i = Intent(this, MainActivity::class.java)
            i.putExtra("fromSplash",true)
            i.putExtra("fromNotification", false)
            startActivity(i)

            // close this activity
            finish()
        }, SPLASH_TIME_OUT)
    }
}
