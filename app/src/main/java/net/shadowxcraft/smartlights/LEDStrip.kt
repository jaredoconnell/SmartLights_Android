package net.shadowxcraft.smartlights

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.shadowxcraft.smartlights.packets.SetBrightnessForLEDStrip
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.roundToInt

const val MAX_BRIGHTNESS = 4095

open class LEDStrip(val id: String, val name: String, val controller: ESP32)
{
    val components = ArrayList<LEDStripComponent>()

    val componentsToSave = ContentValues()

    open var currentSeq: ColorSequence? = null
        protected set
    fun setCurrentSeq(seq: ColorSequence?, save: Boolean) {
        this.currentSeq = seq
        if (save) {
            componentsToSave.put(SQLTableData.LEDStripEntry.COLUMN_NAME_CUR_SEQ, seq?.id)
        }
    }

    open var brightness = MAX_BRIGHTNESS
        protected set

    fun setBrightness(newBrightness: Int, save: Boolean) {
        brightness = newBrightness

        if (save) {
            componentsToSave.put(SQLTableData.LEDStripEntry.COLUMN_NAME_BRIGHTNESS, brightness)
        }
    }

    open var onState = true
        protected set
    fun setOnState(onState: Boolean, save: Boolean) {
        this.onState = onState
        if (save) {
            val value = if (onState) {
                1
            } else {
                0
            }
            componentsToSave.put(SQLTableData.LEDStripEntry.COLUMN_NAME_ON_STATE, value)
        }
    }
    open var simpleColor = Color(255, 0, 0)
        protected set
    fun setSimpleColor(color: Color, save: Boolean) {
        this.simpleColor = color

        if (save) {
            componentsToSave.put(SQLTableData.LEDStripEntry.COLUMN_NAME_RGB, color.toArgb())
        }
    }

    val scheduledChanges = TreeMap<String, ScheduledChange>()

    protected open fun savePropertiesToDBSync(values: ContentValues) {
        val database = DBHelper(controller.act).writableDatabase
        database.update(SQLTableData.LEDStripEntry.TABLE_NAME,
            values,"uuid=?", arrayOf(id))
        values.clear()
    }

    fun convertToLinear(exponentialInput: Int): Int {
        val originalPercentageDecimal = exponentialInput.toDouble() / MAX_BRIGHTNESS
        val adjustedPercentageDecimal = originalPercentageDecimal.pow(3)
        return (adjustedPercentageDecimal * MAX_BRIGHTNESS).roundToInt()
    }

    fun convertToExponential(linearInput: Int): Int {
        val linearPercentageDecimal = linearInput.toDouble() / MAX_BRIGHTNESS
        val adjustedPercentageDecimal = linearPercentageDecimal.pow(1.0/3.0)
        return (adjustedPercentageDecimal * MAX_BRIGHTNESS).roundToInt()
    }

    /**
     * Give it a value between 0 and MAX_BRIGHTNESS,
     * and it will interpret that as on an exponential scale,
     * then convert that to linear.
     */
    open fun setBrightnessExponential(exponentialInput: Int, save: Boolean) {
        setBrightness(convertToLinear(exponentialInput), save)
        Log.println(Log.INFO,"LEDStrip", "Set brightness to $brightness")
    }

    fun saveChangesSync() {
        if (componentsToSave.size() != 0) {
            savePropertiesToDBSync(componentsToSave)
        }
    }

    open fun saveToDBFull() {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val database = DBHelper(controller.act).writableDatabase
                val values = ContentValues()
                values.put("uuid", id)
                values.put(SQLTableData.LEDStripEntry.COLUMN_NAME_NAME, name)
                if (currentSeq != null)
                    values.put(SQLTableData.LEDStripEntry.COLUMN_NAME_CUR_SEQ, currentSeq!!.id)
                val isOnAsInt = if (onState)
                    1
                else
                    0
                values.put(SQLTableData.LEDStripEntry.COLUMN_NAME_ON_STATE, isOnAsInt)
                values.put(SQLTableData.LEDStripEntry.COLUMN_NAME_BRIGHTNESS, brightness)
                values.put(SQLTableData.LEDStripEntry.COLUMN_NAME_RGB, simpleColor.toArgb())
                values.put(SQLTableData.LEDStripEntry.COLUMN_NAME_CONTROLLER, controller.dbId)

                database.insertWithOnConflict(SQLTableData.LEDStripEntry.TABLE_NAME,
                    null, values, SQLiteDatabase.CONFLICT_REPLACE)

                for (component in components) {
                    val componentValues = ContentValues()
                    componentValues.put(SQLTableData.LEDStripComponentEntry.COLUMN_NAME_LED_STRIP_ID, id)
                    componentValues.put(SQLTableData.LEDStripComponentEntry.COLUMN_NAME_RGB, component.color.toArgb())
                    componentValues.put(SQLTableData.LEDStripComponentEntry.COLUMN_NAME_DRIVER_ID, component.driver.getAddress())
                    componentValues.put(SQLTableData.LEDStripComponentEntry.COLUMN_NAME_DRIVER_PIN, component.driverPin)
                    database.insertWithOnConflict(SQLTableData.LEDStripComponentEntry.TABLE_NAME,
                        null, componentValues, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
        }
    }

    /**
     * Gets the brightness in a way that matches the
     * exponential brightness.
     */
    fun getBrightnessExponential() : Int {
        return convertToExponential(brightness)
    }

    fun sendBrightnessPacket() {
        SetBrightnessForLEDStrip(this).queue()
    }
}