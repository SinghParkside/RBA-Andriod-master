package edu.uwp.appfactory.wishope.views.landing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import edu.uwp.appfactory.wishope.R
import edu.uwp.appfactory.wishope.utils.UserConstants
import edu.uwp.appfactory.wishope.views.map.MapFragment

/**
 * This class allows the coach to see BottomNavigationView on each fragment.
 * It allows the coach to transition between their home screen, messaging view, and
 * the map view.
 * @author Allen Rocha
 * @version 1.9.5
 * @since 30-11-2020
 */
class CoachLandingActivity : AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    var lasti: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coach_landing)
        Log.d("LandingActivity", "onCreate() called.")
        if (savedInstanceState == null) supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.nav_host_fragment,
                CoachHomeFragment(),
                "CoachHomeFragment"
            )
            .addToBackStack(null)
            .commit()
        if (UserConstants.UID.isEmpty() || UserConstants.UID == null) UserConstants.UID =
            FirebaseAuth.getInstance().uid
        val bottomNavView =
            findViewById<BottomNavigationView>(R.id.coach_bottom_navigation)
        bottomNavView.setOnNavigationItemSelectedListener(this)
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onBackPressed() {
        // super.onBackPressed();
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val i = item.itemId

        val fragmentTransaction =
            supportFragmentManager.beginTransaction()


        when (i) {
            R.id.fragment_home -> if (lasti != i) {
                fragmentTransaction
                    .replace(
                        R.id.nav_host_fragment,
                        CoachHomeFragment(),
                        "HomeFragment"
                    )
                    .addToBackStack(null)
                    .commit()
                lasti = i
            } else {
                null
            }
            R.id.fragment_communication -> startActivity(
                Intent(
                    this,
                    CommunicationActivity::class.java
                )
            )
            R.id.fragment_map -> if (lasti != i) {
                fragmentTransaction
                    .replace(
                        R.id.nav_host_fragment,
                        MapFragment(),
                        "MapFragment"
                    )
                    .addToBackStack(null)
                    .commit()
                lasti = i
            }
            else -> {
                null
            }
        }
        return false
    }

    companion object {
        private const val TAG = "CoachLandingActivity"
    }
}