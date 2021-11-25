package net.shadowxcraft.smartlights

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ControllerManager {
    val controllerAddrMap = HashMap<String, ESP32>() // map addr to controller
    val controllerIDMap = HashMap<Int, ESP32>() // map addr to controller
    val controllers = ArrayList<ESP32>()

    val ledStripOrders = ArrayList<LEDStrip>()
    val ledStripGroupOrders = ArrayList<LEDStripGroup>()

    fun addController(controller: ESP32) {
        if (controllerAddrMap.containsKey(controller.addr))
            throw IllegalStateException("Added controller that already exists")
        controllers.add(controller)
        controllerAddrMap[controller.addr] = controller
        controllerIDMap[controller.dbId] = controller
    }

    fun connectAll() {
        for (i in controllers) {
            i.checkConnection()
        }
    }

    fun getLEDStripByID(uuid: String) : LEDStrip? {
        for (controller in controllers) {
            val strip = controller.getLEDStrip(uuid)
            if (strip != null)
                return strip
        }
        return null
    }

    fun getLEDStripGroupByID(uuid: String) : LEDStrip? {
        for (controller in controllers) {
            val strip = controller.getLEDStrip(uuid)
            if (strip != null)
                return strip
        }
        return null
    }

    fun checkLedStripOrders() {
        for (controller in controllers) {
            for (ledStrip in controller.ledStrips) {
                if (!containsLedStrip(ledStrip.value)) {
                    Log.i("ControllerManager", "Missing LED Strip order. Adding...")
                    ledStripOrders.add(ledStrip.value)
                    val position = ledStripOrders.size - 1
                    GlobalScope.launch {
                        withContext(Dispatchers.IO) {
                            val database = DBHelper(controller.act).writableDatabase
                            val values = ContentValues()
                            values.put(SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION, position)
                            values.put(SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID, ledStrip.key)
                            database.insertWithOnConflict(SQLTableData.LEDStripDisplayOptionsEntry.TABLE_NAME,
                                null, values, SQLiteDatabase.CONFLICT_REPLACE)

                        }
                    }
                }
            }
        }
    }

    private fun containsLedStrip(ledStrip: LEDStrip) : Boolean {
        for (i in ledStripOrders) {
            if (i.id == ledStrip.id) {
                return true
            }
        }
        return false
    }

    fun moveLEDStripOrder(from: Int, to: Int) {
        // Swap
        val ledStrip = ledStripOrders.removeAt(from)
        ledStripOrders.add(to, ledStrip)

        // Update all locations from the minimum of the old location to the end.
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val min = if (from < to) from else to

                for (i in min until ledStripOrders.size) {
                    val curLedStrip = ledStripOrders[i]
                    val database = DBHelper(curLedStrip.controller.act).writableDatabase
                    val values1 = ContentValues()
                    values1.put(SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION, i)
                    values1.put(SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID, curLedStrip.id)
                    database.insertWithOnConflict(SQLTableData.LEDStripDisplayOptionsEntry.TABLE_NAME,
                        null, values1, SQLiteDatabase.CONFLICT_REPLACE)
                    database.close()

                }
            }
        }
    }

    fun checkLedStripGroupOrders() {
        for (controller in controllers) {
            for (ledStrip in controller.ledStripGroups) {
                if (!containsLedStripGroup(ledStrip.value)) {
                    Log.i("ControllerManager", "Missing LED Strip order. Adding...")
                    ledStripOrders.add(ledStrip.value)
                    val position = ledStripOrders.size - 1
                    GlobalScope.launch {
                        withContext(Dispatchers.IO) {
                            val database = DBHelper(controller.act).writableDatabase
                            val values = ContentValues()
                            values.put(SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION, position)
                            values.put(SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID, ledStrip.key)
                            database.insertWithOnConflict(SQLTableData.LEDStripGroupDisplayOptionsEntry.TABLE_NAME,
                                null, values, SQLiteDatabase.CONFLICT_REPLACE)

                        }
                    }
                }
            }
        }
    }

    private fun containsLedStripGroup(ledStrip: LEDStrip) : Boolean {
        for (i in ledStripGroupOrders) {
            if (i.id == ledStrip.id) {
                return true
            }
        }
        return false
    }

    fun moveLEDStripGroupOrder(from: Int, to: Int) {
        // Swap
        val ledStrip = ledStripGroupOrders.removeAt(from)
        ledStripGroupOrders.add(to, ledStrip)

        // Update all locations from the minimum of the old location to the end.
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val min = if (from < to) from else to

                for (i in min until ledStripGroupOrders.size) {
                    val curLedStrip = ledStripGroupOrders[i]
                    val database = DBHelper(curLedStrip.controller.act).writableDatabase
                    val values1 = ContentValues()
                    values1.put(SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION, i)
                    values1.put(SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID, curLedStrip.id)
                    database.insertWithOnConflict(SQLTableData.LEDStripGroupDisplayOptionsEntry.TABLE_NAME,
                        null, values1, SQLiteDatabase.CONFLICT_REPLACE)
                    database.close()

                }
            }
        }
    }
}