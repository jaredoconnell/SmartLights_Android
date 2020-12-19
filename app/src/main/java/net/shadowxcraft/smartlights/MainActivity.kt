package net.shadowxcraft.smartlights

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
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
import net.shadowxcraft.smartlights.ui.home.LedStripsFragment
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.lang.Exception

const val REQUEST_LOCATION_PERMISSION = 100

class MainActivity : AppCompatActivity(), LEDStripComponentFragment.OnFragmentInteractionListener,
    BluetoothFragment.OnFragmentInteractionListener, SensorEventListener {

    var ledStripsFragment: LedStripsFragment? = null
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var lastLuxVal = 0.0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home
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

        Handler().postDelayed({
            connectToDevices()
        }, 2000)
    }

    private fun connectToDevices() {
        val db = SQLiteDB(this)
        try {
            db.writableDatabase
            val database = db.readableDatabase
            val selectedCols = arrayOf(CONTROLLER_COLUMN_ADDRESS, CONTROLLER_COLUMN_NAME)
            val cursor = database.query(
                CONTROLLER_TABLE_NAME,
                selectedCols,
                null,
                null,
                null,
                null,
                null
            )
            while (cursor.moveToNext()) {
                BLEControllerManager.attemptToConnectToAddr(cursor.getString(0))
            }
        } catch (any: Exception) {

        }
    }

    override fun onResume() {
        // Register a listener for the sensor.
        super.onResume()
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause()
        sensorManager.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            lastLuxVal = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    fun getLuxVal(): Float {
        return lastLuxVal;
    }
}