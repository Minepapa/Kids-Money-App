package com.minepapa.kidsmoneyapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.sb_dark_green)
        setContentView(R.layout.activity_splash)
        val title = findViewById<TextView>(R.id.tvSplashTitle)
        title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)
    }
}
