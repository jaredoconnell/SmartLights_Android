package net.shadowxcraft.smartlights

import android.Manifest
import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
import androidx.core.database.getStringOrNull
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import net.shadowxcraft.smartlights.ui.bluetooth.BluetoothFragment
import net.shadowxcraft.smartlights.ui.add_led_strip.LEDStripComponentFragment
import net.shadowxcraft.smartlights.ui.home.LedStripsFragment
import net.shadowxcraft.smartlights.ui.led_strip_groups.LedStripGroupsFragment
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.lang.Exception

const val REQUEST_LOCATION_PERMISSION = 100

class MainActivity : AppCompatActivity(), LEDStripComponentFragment.OnFragmentInteractionListener,
    BluetoothFragment.OnFragmentInteractionListener, SensorEventListener {

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
                R.id.navigation_home, R.id.navigation_groups
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        requestLocationPermission()

        if (!SharedData.loaded) {
            initLoadFromDB()
        }
        Log.println(Log.INFO, "MainActivity", "Activity created.")
    }

    private fun initLoadFromDB() {
        GlobalScope.launch {
            loadFromDB()
        }
    }

    private suspend fun loadFromDB() {
        withContext(Dispatchers.IO) {
            try {
                val dbHelper = DBHelper(this@MainActivity)
                // in thread pool
                val db = dbHelper.readableDatabase
                loadDevices(db)
                loadLEDStrips(db)
                loadLEDStripGroups(db)
                //val cursor = db.query(SQLTableData.ControllerEntry.TABLE_NAME,
                //    null, null, null, null, null, null)

                //Log.println(Log.INFO, "MainActivity", "There are " + cursor.count + " columns")

                //cursor.close()
                SharedData.loaded = true
            } catch (any: Exception) {
                Log.e( "MainActivity", "Exception in loadFromDB", any)
            }
        }
    }

    private fun loadDevices(db: SQLiteDatabase) {
        val selectedCols = arrayOf(
            "id",
            SQLTableData.ControllerEntry.COLUMN_NAME_BLE_ADDR,
            SQLTableData.ControllerEntry.COLUMN_NAME_NAME)
        val cursor = db.query(
            SQLTableData.ControllerEntry.TABLE_NAME,
            selectedCols,
            null,
            null,
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val addr = cursor.getString(1)
            val name = cursor.getString(2)

            if (!ControllerManager.controllerAddrMap.containsKey(addr)) {
                val newController = ESP32(this, addr, name)
                newController.dbId = id
                ControllerManager.addController(newController)
            }
        }
        cursor.close()
    }

    private fun loadLEDStrips(db: SQLiteDatabase) {
        val selectedCols = arrayOf(
            "uuid",
            SQLTableData.LEDStripEntry.COLUMN_NAME_NAME,
            SQLTableData.LEDStripEntry.COLUMN_NAME_CUR_SEQ,
            SQLTableData.LEDStripEntry.COLUMN_NAME_ON_STATE,
            SQLTableData.LEDStripEntry.COLUMN_NAME_BRIGHTNESS,
            SQLTableData.LEDStripEntry.COLUMN_NAME_RGB,
            SQLTableData.LEDStripEntry.COLUMN_NAME_CONTROLLER
        )
        val cursor = db.query(
            SQLTableData.LEDStripEntry.TABLE_NAME,
            selectedCols,
            null,
            null,
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val uuid = cursor.getString(0)
            val name = cursor.getString(1)
            val curSeqID = cursor.getStringOrNull(2)
            val onState = cursor.getInt(3)
            val brightness = cursor.getInt(4)
            val colorArgb = cursor.getInt(5)
            val controllerID = cursor.getInt(6)

            val controller = ControllerManager.controllerIDMap[controllerID]
            if (controller == null) {
                Log.println(Log.WARN, "MainActivity",
                    "Unknown controller ID while loading LED Strip")
                continue
            }

            if (!controller.ledStrips.containsKey(uuid)) {
                val currSeq = if (curSeqID == null) {
                    null
                } else {
                    controller.colorsSequences[curSeqID]
                }

                val newStrip = LEDStrip( uuid, name, controller)
                newStrip.setCurrentSeq(currSeq, false)
                newStrip.setOnState(onState == 1, false)
                newStrip.setBrightness(brightness, false)
                newStrip.setSimpleColor(Color(colorArgb), false)
                controller.addLEDStrip(newStrip, sendPacket=false, save=false)
            }
        }
        cursor.close()
    }
    private fun loadLEDStripGroups(db: SQLiteDatabase) {
        val selectedCols = arrayOf(
            "uuid",
            SQLTableData.LEDStripGroupEntry.COLUMN_NAME_NAME,
            SQLTableData.LEDStripGroupEntry.COLUMN_NAME_CUR_SEQ,
            SQLTableData.LEDStripGroupEntry.COLUMN_NAME_ON_STATE,
            SQLTableData.LEDStripGroupEntry.COLUMN_NAME_BRIGHTNESS,
            SQLTableData.LEDStripGroupEntry.COLUMN_NAME_RGB,
            SQLTableData.LEDStripGroupEntry.COLUMN_NAME_CONTROLLER
        )
        val cursor = db.query(
            SQLTableData.LEDStripGroupEntry.TABLE_NAME,
            selectedCols,
            null,
            null,
            null,
            null,
            null
        )
        Log.println(Log.INFO, "MainActivity", "Loading ${cursor.count} groups")
        while (cursor.moveToNext()) {
            val uuid = cursor.getString(0)
            val name = cursor.getString(1)
            val curSeqID = cursor.getStringOrNull(2)
            val onState = cursor.getInt(3)
            val brightness = cursor.getInt(4)
            val colorArgb = cursor.getInt(5)
            val controllerID = cursor.getInt(6)

            val controller = ControllerManager.controllerIDMap[controllerID]
            if (controller == null) {
                Log.println(Log.WARN, "MainActivity",
                    "Unknown controller ID while loading LED Strip")
                continue
            }

            if (!controller.ledStripGroups.containsKey(uuid)) {
                val currSeq = if (curSeqID == null) {
                    null
                } else {
                    controller.colorsSequences[curSeqID]
                }

                val ledStrips = getLEDStripsForGroup(controller, uuid, db)

                val newStrip = LEDStripGroup( uuid, name, ledStrips, controller)
                newStrip.setCurrentSeq(currSeq, false)
                newStrip.setOnState(onState == 1, false)
                newStrip.setBrightness(brightness, false)
                newStrip.setSimpleColor(Color(colorArgb), false)
                controller.addLEDStripGroup(newStrip, sendPacket=false, save=false)
            }
        }
        cursor.close()
    }

    private fun getLEDStripsForGroup(controller: ESP32, groupID: String, db: SQLiteDatabase) : ArrayList<LEDStrip> {
        val ledStrips = ArrayList<LEDStrip>()

        val selectedCols = arrayOf(
            SQLTableData.LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP,
            SQLTableData.LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP_GROUP,
        )
        val cursor = db.query(
            SQLTableData.LEDStripGroupItemEntry.TABLE_NAME,
            selectedCols,
            "${SQLTableData.LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP_GROUP}=?",
            arrayOf(groupID),
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val stripID = cursor.getString(0)
            val strip = controller.ledStrips[stripID]
            if (strip == null) {
                Log.println(Log.WARN, "MainActivity", "Could not find strip in" +
                        "getLEDStripsForGroup with GroupID $groupID")
                break // Just stop here.
            } else {
                ledStrips.add(strip)
            }
        }
        cursor.close()
        return ledStrips
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
            //ControllerManager.connectAll()
        }, 2000)
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