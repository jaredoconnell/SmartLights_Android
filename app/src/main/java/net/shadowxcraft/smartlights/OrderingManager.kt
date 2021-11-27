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

    fun getLedStripPositions(isGroup: Boolean) : ArrayList<String> {
        return if (isGroup)
            ledStripGroupPositions
        else
            ledStripPositions
    }

    fun checkLedStripOrders(isGroup: Boolean) {
        val positions = getLedStripPositions(isGroup)
        Log.println(Log.DEBUG, "ControllerManager", "Num led strips in position list: " + positions.size + ". Group: " + isGroup)
        for (controller in ControllerManager.controllers) {
            val ledStrips = if (isGroup) {
                controller.ledStripGroups
            } else {
                controller.ledStrips
            }
            for (ledStrip in ledStrips) {
                if (ledStrip.value.id !in positions) {
                    Log.i("ControllerManager",
                        "Missing LED Strip position. Adding.... Group: $isGroup"
                    )
                    positions.add(ledStrip.value.id)
                    val position = positions.size - 1
                    GlobalScope.launch {
                        withContext(Dispatchers.IO) {
                            val database = DBHelper(controller.act).writableDatabase
                            val values = ContentValues()

                            val tableName = getTableName(isGroup)
                            val positionColName = getPositionColName(isGroup)
                            val ledStripIdColName = getIdColName(isGroup)
                            values.put(positionColName, position)
                            values.put(ledStripIdColName, ledStrip.key)
                            database.insertWithOnConflict(tableName,
                                null, values, SQLiteDatabase.CONFLICT_REPLACE)
                            database.close()

                        }
                    }
                }
            }
        }
    }

    fun moveLEDStripOrder(from: Int, to: Int, isGroup: Boolean) {
        Log.println(Log.DEBUG, "OrderingManager", "Moving LED position. Group: $isGroup")
        // Swap
        val positions = getLedStripPositions(isGroup)
        val ledStrip = positions.removeAt(from)
        positions.add(to, ledStrip)

        // Update all locations from the minimum of the old location to the end.
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val min = if (from < to) from else to

                val database = DBHelper(BLEControllerManager.activity!!.baseContext).writableDatabase
                val tableName = getTableName(isGroup)
                val positionColName = getPositionColName(isGroup)
                val ledStripIdColName = getIdColName(isGroup)

                for (i in min until positions.size) {
                    val curLedStrip = positions[i]
                    val values1 = ContentValues()
                    Log.println(
                        Log.DEBUG, "ControllerManager",
                        "Saving $curLedStrip to position $i"
                    )
                    values1.put(positionColName, i)
                    values1.put(ledStripIdColName, curLedStrip)
                    database.insertWithOnConflict(tableName,
                        null, values1, SQLiteDatabase.CONFLICT_REPLACE)
                }
                database.delete(tableName,
                    positionColName + " >= " + positions.size, null)
                database.close()
            }
        }
    }

    private fun getTableName(isGroup: Boolean) : String {
        return if (isGroup) {
            SQLTableData.LEDStripGroupDisplayOptionsEntry.TABLE_NAME
        } else {
            SQLTableData.LEDStripDisplayOptionsEntry.TABLE_NAME
        }
    }

    private fun getPositionColName(isGroup: Boolean) : String {
        return if (isGroup) {
            SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION
        } else {
            SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION
        }
    }

    private fun getIdColName(isGroup: Boolean) : String {
        return if (isGroup) {
            SQLTableData.LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID
        } else {
            SQLTableData.LEDStripDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID
        }
    }

}