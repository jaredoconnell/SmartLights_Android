package net.shadowxcraft.smartlights

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LEDStripGroup(id: String, name: String, private val ledStrips: ArrayList<LEDStrip>,
                    controller: ESP32, initialBrightness: Int = 4095, initialOnState: Boolean = true,
                    initialColor: Color = Color(255, 0, 0),
                    initialSequence: ColorSequence? = null)
    : LEDStrip(id, name, controller)
{
    init {
        super.brightness = initialBrightness
        super.onState = initialOnState
        super.simpleColor = initialColor
        super.currentSeq = initialSequence
    }

    override var onState: Boolean
        get() = super.onState
        set(on) {
            super.onState = on
            for (i in ledStrips) {
                i.setOnState(on, false)
            }
        }
    override var brightness: Int
        get() = super.brightness
        set(brightness) {
            super.brightness = brightness
            for (i in ledStrips) {
                i.setBrightness(brightness, false)
            }
        }
    override var simpleColor: Color
        get() = super.simpleColor
        set(simpleColor) {
            super.simpleColor = simpleColor
            for (i in ledStrips) {
                i.setSimpleColor(simpleColor, false)
            }
        }
    override var currentSeq: ColorSequence?
        get() = super.currentSeq
        set(currentSeq) {
            super.currentSeq = currentSeq
            for (i in ledStrips) {
                i.setCurrentSeq(currentSeq, false)
            }
        }

    override fun savePropertiesToDBSync(values: ContentValues) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val database = DBHelper(controller.act).writableDatabase
                // Save all of them individually
                // The use of ?s/variable binding prevents sql injection
                val numRows = database.update(SQLTableData.LEDStripEntry.TABLE_NAME,
                    values,"uuid in (${getWhereClauseUnknowns()})", getIDs()
                )
                Log.println(Log.INFO, "LEDStripGroup", "Rows affected: $numRows")
            }
        }
    }

    override fun saveToDBFull() {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val database = DBHelper(controller.act).writableDatabase
                val groupValues = ContentValues()
                groupValues.put("uuid", id)
                groupValues.put(SQLTableData.LEDStripGroupEntry.COLUMN_NAME_NAME, name)
                if (currentSeq != null)
                    groupValues.put(SQLTableData.LEDStripGroupEntry.COLUMN_NAME_CUR_SEQ, currentSeq!!.id)
                val isOnAsInt = if (onState)
                    1
                else
                    0
                groupValues.put(SQLTableData.LEDStripGroupEntry.COLUMN_NAME_ON_STATE, isOnAsInt)
                groupValues.put(SQLTableData.LEDStripGroupEntry.COLUMN_NAME_BRIGHTNESS, brightness)
                groupValues.put(SQLTableData.LEDStripGroupEntry.COLUMN_NAME_RGB, simpleColor.toArgb())
                groupValues.put(SQLTableData.LEDStripGroupEntry.COLUMN_NAME_CONTROLLER, controller.dbId)

                database.insertWithOnConflict(SQLTableData.LEDStripGroupEntry.TABLE_NAME,
                    null, groupValues, SQLiteDatabase.CONFLICT_REPLACE)

                for (i in ledStrips) {
                    val ledStripValues = ContentValues()
                    ledStripValues.put(SQLTableData.LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP_GROUP, id)
                    ledStripValues.put(SQLTableData.LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP, i.id)
                    database.insertWithOnConflict(SQLTableData.LEDStripGroupItemEntry.TABLE_NAME,
                        null, ledStripValues, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
        }
    }

    private fun getWhereClauseUnknowns(): String {
        return if (ledStrips.isEmpty()) {
            ""
        } else {
            var returnVal = "?"
            for (i in 2..ledStrips.size) {
                returnVal += ", ?"
            }
            returnVal
        }
    }

    private fun getIDs(): Array<String> {
        val strArray = Array(ledStrips.size) { "" }
        for (i in 0 until ledStrips.size) {
            strArray[i] = ledStrips[i].id
        }
        return strArray
    }
}