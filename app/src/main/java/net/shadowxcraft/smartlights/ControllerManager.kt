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

    val ledStripOrders = ArrayList<String>()
    val ledStripGroupOrders = ArrayList<String>()

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

    fun checkLedStripOrders() {
        Log.println(Log.DEBUG, "ControllerManager", "Num led strips in position list: " + ledStripOrders.size)
        for (controller in controllers) {
            for (ledStrip in controller.ledStrips) {
                if (ledStrip.value.id !in ControllerManager.ledStripOrders) {
                    Log.i("ControllerManager", "Missing LED Strip position. Adding...")
                    ledStripOrders.add(ledStrip.value.id)
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
                    val database = DBHelper(BLEControllerManager.activity!!.baseContext).writableDatabase
                    val values1 = ContentValues()
                    Log.println(Log.DEBUG, "ControllerManager",
                        "Saving $curLedStrip to position $i"
                    )
                    values1.put(SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION, i)
                    values1.put(SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID, curLedStrip)
                    database.insertWithOnConflict(SQLTableData.LEDStripDisplayOptionsEntry.TABLE_NAME,
                        null, values1, SQLiteDatabase.CONFLICT_REPLACE)
                    database.delete(SQLTableData.LEDStripDisplayOptionsEntry.TABLE_NAME,
                        SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION + " >= " + ledStripOrders.size, null)
                    database.close()

                }
            }
        }
    }

    fun checkLedStripGroupOrders() {
        for (controller in controllers) {
            for (ledStrip in controller.ledStripGroups) {
                if (ledStrip.key !in ledStripGroupOrders) {
                    Log.i("ControllerManager", "Missing LED Strip group position. Adding...")
                    ledStripGroupOrders.add(ledStrip.value.id)
                    val position = ledStripGroupOrders.size - 1
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
                    val database = DBHelper(BLEControllerManager.activity!!.baseContext).writableDatabase
                    val values1 = ContentValues()
                    values1.put(SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION, i)
                    values1.put(SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID, curLedStrip)
                    database.insertWithOnConflict(SQLTableData.LEDStripGroupDisplayOptionsEntry.TABLE_NAME,
                        null, values1, SQLiteDatabase.CONFLICT_REPLACE)

                    database.delete(SQLTableData.LEDStripGroupDisplayOptionsEntry.TABLE_NAME,
                        SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION + " >= " + ledStripGroupOrders.size, null)
                    database.close()

                }
            }
        }
    }
}