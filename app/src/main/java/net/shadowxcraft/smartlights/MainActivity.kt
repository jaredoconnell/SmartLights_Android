package net.shadowxcraft.smartlights

import android.Manifest
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.database.getStringOrNull
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import net.shadowxcraft.smartlights.ui.add_led_strip.LEDStripComponentFragment
import net.shadowxcraft.smartlights.ui.bluetooth.BluetoothFragment
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

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

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        SharedData.navFragment = navHostFragment
        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_groups
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.selectedItemId = getPreferences(Context.MODE_PRIVATE)
            .getInt("last_page", R.id.navigation_home)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            getPreferences(Context.MODE_PRIVATE).edit().putInt("last_page", destination.id).apply()
        }
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
                loadColorSequences(db)
                loadColorSequenceColors(db)
                loadControllers(db)
                loadPWMDrivers(db)
                loadLEDStripOrders(db)
                loadLEDStripGroupOrders(db)
                loadColorSequenceOrders(db)
                loadLEDStrips(db)
                loadLEDStripGroups(db)
                db.close()
                SharedData.loaded = true
            } catch (any: Exception) {
                Log.e( "MainActivity", "Exception in loadFromDB", any)
            }
        }
    }

    private fun loadColorSequences(db: SQLiteDatabase) {
        val selectedCols = arrayOf(
            "uuid",
            SQLTableData.ColorSequenceEntry.COLUMN_NAME_NAME,
            SQLTableData.ColorSequenceEntry.COLUMN_NAME_SEQUENCE_TYPE,
            SQLTableData.ColorSequenceEntry.COLUMN_NAME_SUSTAIN_TIME,
            SQLTableData.ColorSequenceEntry.COLUMN_NAME_TRANSITION_TIME,
            SQLTableData.ColorSequenceEntry.COLUMN_NAME_TRANSITION_TYPE)
        val cursor = db.query(
            SQLTableData.ColorSequenceEntry.TABLE_NAME,
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
            val sequenceType = cursor.getInt(2)
            val sequenceSustainTime = cursor.getInt(3)
            val sequenceTransitionTime = cursor.getInt(4)
            val sequenceTransitionType = cursor.getInt(5)

            if (!SharedData.colorsSequences.containsKey(uuid)) {
                val sequence = ColorSequence(uuid, name)
                sequence.sequenceType = sequenceType.toByte()
                sequence.sustainTime = sequenceSustainTime
                sequence.transitionTime = sequenceTransitionTime
                sequence.transitionType = sequenceTransitionType.toByte()
                SharedData.colorsSequences[uuid] = sequence
            }
        }
        cursor.close()
    }

    private fun loadColorSequenceColors(db: SQLiteDatabase) {
        val selectedCols = arrayOf(
            SQLTableData.ColorSequenceColorEntry.COLUMN_NAME_SEQUENCE_ID,
            SQLTableData.ColorSequenceColorEntry.COLUMN_NAME_ORDER_INDEX,
            SQLTableData.ColorSequenceColorEntry.COLUMN_NAME_COLOR_ARGB)
        val cursor = db.query(
            SQLTableData.ColorSequenceColorEntry.TABLE_NAME,
            selectedCols,
            null,
            null,
            null,
            null,
            "`${SQLTableData.ColorSequenceColorEntry.COLUMN_NAME_ORDER_INDEX}` ASC"
        )
        while (cursor.moveToNext()) {
            val sequenceID = cursor.getString(0)
            val orderIndex = cursor.getInt(1)
            val color = Color(cursor.getInt(2))

            val sequence = SharedData.colorsSequences[sequenceID]
            if (sequence == null) {
                Log.println(Log.WARN, "MainActivity", "Could not find sequence ID while" +
                        " loading colors")
                continue
            }
            if (sequence.colors.size == orderIndex) { // depends on the ascending order.
                sequence.colors.add(color)
            } else {
                Log.println(Log.WARN, "MainActivity", "Order mismatch while" +
                        " loading color (index: $orderIndex, size: ${sequence.colors.size})")
            }
        }
        cursor.close()
    }

    private fun loadControllers(db: SQLiteDatabase) {
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

    private fun loadPWMDrivers(db: SQLiteDatabase) {
        val selectedCols = arrayOf(
            SQLTableData.PWMDriverEntry.COLUMN_NAME_CONTROLLER_ID,
            SQLTableData.PWMDriverEntry.COLUMN_NAME_ADDRESS)
        val cursor = db.query(
            SQLTableData.PWMDriverEntry.TABLE_NAME,
            selectedCols,
            null,
            null,
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val controllerID = cursor.getInt(0)
            val addr = cursor.getInt(1)

            if (addr < 64) {
                Log.println(Log.WARN, "MainActivity", "Database has a driver with address" +
                        " less than 64, which is invalid.")
                continue
            }

            val controller = ControllerManager.controllerIDMap[controllerID]
            if (controller == null) {
                Log.println(Log.WARN, "MainActivity", "Could not find controller with" +
                        " ID $controllerID.")
                continue
            }
            val pwmDriver = PWMDriver(addr)
            controller.addPWMDriver(pwmDriver, false)
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
                    SharedData.colorsSequences[curSeqID]
                }

                val newStrip = LEDStrip( uuid, name, controller)
                newStrip.setCurrentSeq(currSeq, false)
                newStrip.setOnState(onState == 1, false)
                newStrip.setBrightness(brightness, false)
                newStrip.setSimpleColor(Color(colorArgb), false)
                loadLEDStripComponents(newStrip, db)
                controller.addLEDStrip(newStrip, sendPacket=false, save=false)
            }
        }
        cursor.close()
    }

    private fun loadLEDStripComponents(strip: LEDStrip, db: SQLiteDatabase) {
        val selectedCols = arrayOf(
            SQLTableData.LEDStripComponentEntry.COLUMN_NAME_RGB,
            SQLTableData.LEDStripComponentEntry.COLUMN_NAME_DRIVER_ID,
            SQLTableData.LEDStripComponentEntry.COLUMN_NAME_DRIVER_PIN,
        )
        val cursor = db.query(
            SQLTableData.LEDStripComponentEntry.TABLE_NAME,
            selectedCols,
            "${SQLTableData.LEDStripComponentEntry.COLUMN_NAME_LED_STRIP_ID}=?",
            arrayOf(strip.id),
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val color = Color(cursor.getInt(0))
            val driverID = cursor.getInt(1)
            val driverPin = cursor.getInt(2)
            val driver = strip.controller.getPWMDriverByAddress(driverID)
            if (driver != null)
                strip.components.add(LEDStripComponent(color, driver, driverPin))
            else
                Log.println(Log.WARN, "MainActivity", "Could not add LED Strip" +
                        " component due to missing driver by address.")
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
                    SharedData.colorsSequences[curSeqID]
                }

                val ledStrips = getLEDStripsForGroup(controller, uuid, db)

                val newStrip = LEDStripGroup( uuid, name, ledStrips, controller, brightness,
                    onState == 1, Color(colorArgb), currSeq)
                controller.addLEDStripGroup(newStrip, sendPacket=false, save=false)
            }
        }
        cursor.close()
    }

    private fun loadLEDStripOrders(db: SQLiteDatabase) {
        Log.println(Log.DEBUG, "MainActivity", "Loading positions for LED strips")
        OrderingManager.ledStripPositions.clear()
        val selectedCols = arrayOf(
            SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID,
            SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION
        )
        val cursor = db.query(
            SQLTableData.LEDStripDisplayOptionsEntry.TABLE_NAME,
            selectedCols,
            null,
            null,
            null,
            null,
            SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION
        )
        var lastPosition = -1
        while (cursor.moveToNext()) {
            val uuid = cursor.getString(0)
            val position = cursor.getInt(1)

            if (position != lastPosition + 1) {
                Log.println(Log.WARN, "MainActivity", "Invalid position $position while loading led strip positions. lastPosition: $lastPosition")
            }
            lastPosition = position

            Log.println(Log.DEBUG, "MainActivity", "$uuid at position $position")
            OrderingManager.ledStripPositions.add(uuid)

        }
        cursor.close()
    }

    private fun loadLEDStripGroupOrders(db: SQLiteDatabase) {
        OrderingManager.ledStripGroupPositions.clear()
        val selectedCols = arrayOf(
            SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID,
            SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION
        )
        val cursor = db.query(
            SQLTableData.LEDStripGroupDisplayOptionsEntry.TABLE_NAME,
            selectedCols,
            null,
            null,
            null,
            null,
            SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION
        )
        var lastPosition = -1
        while (cursor.moveToNext()) {
            val uuid = cursor.getString(0)
            val position = cursor.getInt(1)

            if (position != lastPosition + 1) {
                Log.println(Log.WARN, "MainActivity", "Invalid position $position while loading led strip group positions. lastPosition: $lastPosition")
            }
            lastPosition = position

            OrderingManager.ledStripGroupPositions.add(uuid)

        }
        cursor.close()
    }
    private fun loadColorSequenceOrders(db: SQLiteDatabase) {
        OrderingManager.colorSequencePositions.clear()
        val selectedCols = arrayOf(
            SQLTableData.ColorSequenceDisplayOptionsEntry.COLUMN_NAME_COLOR_SEQUENCE_ID,
            SQLTableData.ColorSequenceDisplayOptionsEntry.COLUMN_NAME_POSITION
        )
        val cursor = db.query(
            SQLTableData.ColorSequenceDisplayOptionsEntry.TABLE_NAME,
            selectedCols,
            null,
            null,
            null,
            null,
            SQLTableData.ColorSequenceDisplayOptionsEntry.COLUMN_NAME_POSITION
        )
        var lastPosition = -1
        while (cursor.moveToNext()) {
            val uuid = cursor.getString(0)
            val position = cursor.getInt(1)

            if (position != lastPosition + 1) {
                Log.println(Log.WARN, "MainActivity", "Invalid position $position while loading led strip group positions. lastPosition: $lastPosition")
            }
            lastPosition = position

            OrderingManager.colorSequencePositions.add(uuid)

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
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (EasyPermissions.hasPermissions(this, *perms)) {
            startBluetooth()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "Location and bluetooth perms are required to connect to" +
                        "the light strip controllers via bluetooth!",
                REQUEST_LOCATION_PERMISSION,
                *perms
            )
        }
    }

    private fun startBluetooth() {
        BLEControllerManager.init(this)

        Handler().postDelayed({
            ControllerManager.connectAll()
        }, 1500)
    }

    override fun onResume() {
        // Register a listener for the sensor.
        super.onResume()
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        saveAllLEDStripChanges()
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        saveAllLEDStripChanges()
        super.onDestroy()
    }

    private fun saveAllLEDStripChanges() {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                for (controller in ControllerManager.controllers) {
                    for (ledStrip in controller.ledStrips.values) {
                        ledStrip.saveChangesSync()
                    }
                    for (ledStripGroup in controller.ledStripGroups.values) {
                        ledStripGroup.saveChangesSync()
                    }
                }
            }
        }
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