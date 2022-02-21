package edu.uwp.appfactory.wishope.views.landing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import edu.uwp.appfactory.wishope.R
import edu.uwp.appfactory.wishope.utils.UserConstants
import edu.uwp.appfactory.wishope.views.map.MapFragment

/**
 * <h1>.</h1>
 * <p>
 * </p>
 * This class updates the UI with the "RecovereeHomeFragment" to display the recoveree's
 * home screen. It creates the view that allows the recoveree to call online coaches.
 * Also, displays the bottom navigation bar for each fragment which allows the recoveree
 * to transition between their home screen, messaging view, and map view.
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
class RecovereeLandingActivity : AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener {
    lateinit var toolbar: ActionBar
    var lasti: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recoveree_landing)

        enableMyLocation()

        Log.d("LandingActivity", "onCreate() called.")

        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.nav_host_fragment,
                    RecovereeHomeFragment(),
                    "HomeFragment"
                )
                .addToBackStack(null)
                .commit()

        if (UserConstants.UID.isNullOrEmpty())
            UserConstants.UID = FirebaseAuth.getInstance().uid

        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_navigation)
//        toolbar = supportActionBar!!
        bottomNavView.setOnNavigationItemSelectedListener(this)
    }

    /**
     * The method determines which activity or fragment to display depending on
     * which tab the recoveree chooses to tap
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val i: Int = item.itemId

        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()

        when (i) {
            R.id.fragment_home -> {
                if (lasti != i) {
                    fragmentTransaction
                        .replace(
                            R.id.nav_host_fragment,
                            RecovereeHomeFragment(),
                            "HomeFragment"
                        )
                        .addToBackStack(null)
                        .commit()
                    lasti = i
                } else {
                    null
                }
            }
            R.id.fragment_communication -> startActivity(
                Intent(
                    this,
                    CommunicationActivity::class.java
                )
            )
            R.id.fragment_map -> {
                if (lasti != i) {
                    fragmentTransaction
                        .replace(
                            R.id.nav_host_fragment,
                            MapFragment(),
                            "MapFragment"
                        )
                        .addToBackStack(null)
                        .commit()
                    lasti = i
                } else {
                    null
                }
            }
        }
        return true
    }

    override fun onBackPressed() {
        return
    }

    /**
     * Allows the app to determine a rough estimate of the user's location
     */
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
    }

    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

}

