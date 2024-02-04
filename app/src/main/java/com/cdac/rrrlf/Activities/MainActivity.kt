package com.cdac.rrrlf.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.cdac.rrrlf.R
import android.os.Handler
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val window: Window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)

        Handler().postDelayed({
            val intent = Intent(this@MainActivity, Dashboard::class.java)
            startActivity(intent)
            finish() // close the splash activity to prevent it from showing when pressing the back button
        }, 1000) // 2000 milliseconds (adjust as needed)

    }
}