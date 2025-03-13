package com.asdevs.kinematix

import PostureAIFragment
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.asdevs.kinematix.fragments.HomeFragment
import com.asdevs.kinematix.fragments.NutritionPlannerFragment
import com.asdevs.kinematix.fragments.ProfileFragment
import com.asdevs.kinematix.fragments.WorkoutPlannerFragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabPostureAI: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkGooglePlayServices()
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fabPostureAI = findViewById(R.id.fab_posture_ai)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val anim = AnimationUtils.loadAnimation(this, R.anim.scale_animation)
            bottomNavigationView.findViewById<View>(item.itemId)?.startAnimation(anim)

            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_workout -> loadFragment(WorkoutPlannerFragment())
                R.id.nav_nutrition -> loadFragment(NutritionPlannerFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }


        fabPostureAI.setOnClickListener {
            val anim = AnimationUtils.loadAnimation(this, R.anim.scale_animation)
            fabPostureAI.startAnimation(anim)
            loadFragment(PostureAIFragment())
        }

    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun loadFragmentWithBackStack(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)  // Add to back stack for PostureAI
            .commit()
    }

    override fun onBackPressed() {
        // Dismiss any dialogs in the current fragment
        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { fragment ->
            if (fragment is PostureAIFragment) {
                fragment.dismissDialogs()
            }
        }

        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 1)?.show()
            } else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { fragment ->
            if (fragment is PostureAIFragment) {
                fragment.dismissDialogs()
            }
        }
        super.onDestroy()
    }
}

