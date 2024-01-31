package com.cdac.rrrlf.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.cdac.rrrlf.Fragments.HomeFragment
import com.cdac.rrrlf.Fragments.ProfileFragment
import com.cdac.rrrlf.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class Dashboard : AppCompatActivity() {
    var bottom_navigation: BottomNavigationView? = null
    var HomeFragment = HomeFragment()
    var ProfileFragment = ProfileFragment()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        bottom_navigation = findViewById(R.id.bottom_navigation)
        supportFragmentManager.beginTransaction().replace(R.id.container, HomeFragment).commit()
        bottom_navigation!!.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    supportFragmentManager.beginTransaction().replace(R.id.container, HomeFragment)
                        .commit()
                    return@OnNavigationItemSelectedListener true
                }

                R.id.profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, ProfileFragment).commit()


                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        })
    }

    override fun onBackPressed() {
        val currentFragment =
            this.supportFragmentManager.findFragmentById(R.id.container)
        if (currentFragment is ProfileFragment) {
            supportFragmentManager.beginTransaction()
                .apply { replace(R.id.container, HomeFragment) .commit() }
        } else {
            super.onBackPressed()
        }
    }


}