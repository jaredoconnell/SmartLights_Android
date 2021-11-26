package net.shadowxcraft.smartlights

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object OrderingManager {

    val ledStripPositions = ArrayList<String>()
    val ledStripGroupPositions = ArrayList<String>()

    fun checkLedStripOrders() {
        Log.println(Log.DEBUG, "ControllerManager", "Num led strips in position list: " + ledStripPositions.size)
        for (controller in ControllerManager.controllers) {
            for (ledStrip in controller.ledStrips) {
                if (ledStrip.value.id !in ledStripPositions) {
                    Log.i("ControllerManager", "Missing LED Strip position. Adding...")
                    ledStripPositions.add(ledStrip.value.id)
                    val position = ledStripPositions.size - 1
                    GlobalScope.launch {
                        withContext(Dispatchers.IO) {
                            val database = DBHelper(controller.act).writableDatabase
                            val values = ContentValues()
                            values.put(SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION, position)
                            values.put(SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID, ledStrip.key)
                            database.insertWithOnConflict(SQLTableData.LEDStripDisplayOptionsEntry.TABLE_NAME,
                                null, values, SQLiteDatabase.CONFLICT_REPLACE)
                            database.close()

                        }
                    }
                }
            }
        }
    }

    fun moveLEDStripOrder(from: Int, to: Int) {
        // Swap
        val ledStrip = ledStripPositions.removeAt(from)
        ledStripPositions.add(to, ledStrip)

        // Update all locations from the minimum of the old location to the end.
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val min = if (from < to) from else to

                for (i in min until ledStripPositions.size) {
                    val curLedStrip = ledStripPositions[i]
                    val database = DBHelper(BLEControllerManager.activity!!.baseContext).writableDatabase
                    val values1 = ContentValues()
                    Log.println(
                        Log.DEBUG, "ControllerManager",
                        "Saving $curLedStrip to position $i"
                    )
                    values1.put(SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION, i)
                    values1.put(SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID, curLedStrip)
                    database.insertWithOnConflict(SQLTableData.LEDStripDisplayOptionsEntry.TABLE_NAME,
                        null, values1, SQLiteDatabase.CONFLICT_REPLACE)
                    database.delete(SQLTableData.LEDStripDisplayOptionsEntry.TABLE_NAME,
                        SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION + " >= " + ledStripPositions.size, null)
                    database.close()

                }
            }
        }
    }

    fun checkLedStripGroupOrders() {
        for (controller in ControllerManager.controllers) {
            for (ledStrip in controller.ledStripGroups) {
                if (ledStrip.key !in ledStripGroupPositions) {
                    Log.i("ControllerManager", "Missing LED Strip group position. Adding...")
                    ledStripGroupPositions.add(ledStrip.value.id)
                    val position = ledStripGroupPositions.size - 1
                    GlobalScope.launch {
                        withContext(Dispatchers.IO) {
                            val database = DBHelper(controller.act).writableDatabase
                            val values = ContentValues()
                            values.put(SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION, position)
                            values.put(SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID, ledStrip.key)
                            database.insertWithOnConflict(SQLTableData.LEDStripGroupDisplayOptionsEntry.TABLE_NAME,
                                null, values, SQLiteDatabase.CONFLICT_REPLACE)
                            database.close()

                        }
                    }
                }
            }
        }
    }

    fun moveLEDStripGroupOrder(from: Int, to: Int) {
        // Swap
        val ledStrip = ledStripGroupPositions.removeAt(from)
        ledStripGroupPositions.add(to, ledStrip)

        // Update all locations from the minimum of the old location to the end.
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val min = if (from < to) from else to

                for (i in min until ledStripGroupPositions.size) {
                    val curLedStrip = ledStripGroupPositions[i]
                    val database = DBHelper(BLEControllerManager.activity!!.baseContext).writableDatabase
                    val values1 = ContentValues()
                    values1.put(SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION, i)
                    values1.put(SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID, curLedStrip)
                    database.insertWithOnConflict(SQLTableData.LEDStripGroupDisplayOptionsEntry.TABLE_NAME,
                        null, values1, SQLiteDatabase.CONFLICT_REPLACE)

                    database.delete(SQLTableData.LEDStripGroupDisplayOptionsEntry.TABLE_NAME,
                        SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION + " >= " + ledStripGroupPositions.size, null)
                    database.close()

                }
            }
        }
    }

}