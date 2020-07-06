package net.shadowxcraft.smartlights

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_VERSION = 3
const val DATABASE_NAME = "led_strip_data"
const val CONTROLLER_TABLE_NAME = "known_controllers"
const val CONTROLLER_COLUMN_ADDRESS = "ble_address"
const val CONTROLLER_COLUMN_NAME = "name"

class SQLiteDB(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL("CREATE TABLE " + CONTROLLER_TABLE_NAME
            + " (" + CONTROLLER_COLUMN_ADDRESS + " TEXT PRIMARY KEY, "
            + CONTROLLER_COLUMN_NAME + " TEXT);")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }
}