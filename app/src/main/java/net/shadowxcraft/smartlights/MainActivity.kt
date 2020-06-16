package net.shadowxcraft.smartlights

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import net.shadowxcraft.smartlights.ui.bluetooth.BluetoothFragment
import net.shadowxcraft.smartlights.ui.add_led_strip.LEDStripComponentFragment
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

const val REQUEST_LOCATION_PERMISSION = 100

class MainActivity : AppCompatActivity(), LEDStripComponentFragment.OnFragmentInteractionListener,
    BluetoothFragment.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        requestLocationPermission()
    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions,
            grantResults, this)
        startBluetooth()
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    fun requestLocationPermission() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            startBluetooth()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "Location perms are required to connect to" +
                "the light strip controllers via bluetooth!",
                REQUEST_LOCATION_PERMISSION,
                *perms
            )
        }
    }

    private fun startBluetooth() {
        Toast.makeText(this, "Starting bluetooth..", Toast.LENGTH_SHORT).show()

        BLEControllerManager.init(this)
    }
}