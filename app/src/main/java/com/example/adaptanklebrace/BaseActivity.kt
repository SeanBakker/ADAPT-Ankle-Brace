package com.example.adaptanklebrace

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.adaptanklebrace.fragments.MoreOptionsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {

    @SuppressLint("InflateParams")
    override fun setContentView(layoutResID: Int) {
        val baseLayout = layoutInflater.inflate(R.layout.activity_base, null)
        val contentFrame = baseLayout.findViewById<FrameLayout>(R.id.content_frame)

        layoutInflater.inflate(layoutResID, contentFrame, true)
        super.setContentView(baseLayout)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_bar)

        bottomNavigationView.setOnItemSelectedListener { item ->
            val targetActivity = when (item.itemId) {
                R.id.nav_home -> MainActivity::class.java
                R.id.nav_instructions -> InstructionsActivity::class.java
                R.id.nav_recovery_plan -> RecoveryPlanActivity::class.java
                R.id.nav_recovery_data -> RecoveryDataActivity::class.java
                R.id.nav_more_options -> {
                    MoreOptionsFragment().show(supportFragmentManager, "more_options")
                    return@setOnItemSelectedListener true  // Do not finish() if it's a fragment
                }
                else -> null
            }

            targetActivity?.let {
                if (this::class.java != it) { // Prevent reloading the same activity
                    startActivity(Intent(this, it))
                    finish() // Close the current activity
                }
            }
            true
        }

        // Highlight the current selected item
        val currentId = when (this) {
            is MainActivity -> R.id.nav_home
            is InstructionsActivity -> R.id.nav_instructions
            is RecoveryPlanActivity -> R.id.nav_recovery_plan
            is RecoveryDataActivity -> R.id.nav_recovery_data
            is SettingsActivity -> R.id.nav_more_options
            is CommonExercisesActivity -> R.id.nav_more_options
            is NotificationsActivity -> R.id.nav_more_options
            else -> -1
        }

        // Remove highlight from all items first (resets the nav bar)
        for (i in 0 until bottomNavigationView.menu.size()) {
            bottomNavigationView.menu.getItem(i)?.isChecked = false
        }

        // Highlight only if the activity is listed in the navigation
        if (currentId != -1) {
            bottomNavigationView.menu.findItem(currentId)?.isChecked = true
        }
    }
}
