package net.shadowxcraft.smartlights

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.GradientDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ColorSequence(val id: String, var name: String) {
    val colors = ArrayList<Color>()

    var sequenceType: Byte = 0

    var sustainTime = 0 // 60ths of a second.
    var transitionTime = 0 // 60ths of a second.

    // Transition type of 0 is linear from one color to the other
    // Other possible transition types include ease in, ease out, ease-in-out,
    // fade to black, fade to white.
    var transitionType: Byte = 0


    fun getDrawableRepresentation() : GradientDrawable {
        val gradient: GradientDrawable

        when {
            this.colors.isEmpty() -> {
                gradient = GradientDrawable()
                gradient.setColor(0)
            }
            this.colors.size == 1 -> {
                val singleColor = this.colors[0]
                gradient = GradientDrawable()
                gradient.setColor(singleColor.toArgb())
            }
            else -> {
                // A smooth transition since all time is spent in transition
                val animatedColors: ArrayList<Int> = ArrayList()
                for (color in this.colors) {
                    animatedColors.add(color.toArgb())
                }
                animatedColors.add(colors[0].toArgb())

                gradient = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    animatedColors.toIntArray())
            }
            /*else -> {
                val animatedColors: ArrayList<Int> = ArrayList()
                var positions: ArrayList<Float> = ArrayList()
                var totalTime: Float = ((sustainTime + transitionTime) * colors.size).toFloat()
                var currentPosition = 0.0f

                for (color in this.colors) {
                    // First the start of the color
                    animatedColors.add(color.toArgb())
                    positions.add(currentPosition)
                    // Now add the width of the first time
                    currentPosition += sustainTime / totalTime
                    animatedColors.add(color.toArgb())
                    positions.add(currentPosition)
                    // Now add the width of the transition time
                    currentPosition += transitionTime / totalTime
                }
                animatedColors.add(colors[0].toArgb())

                gradient = GradientDrawable()
                gradient.setColors(animatedColors.toIntArray(), positions.toFloatArray())
            }*/
        }


        return gradient
    }

    /**
     * Returns the duration in SECONDS
     */
    fun getDuration() : Double {
        return colors.size * (sustainTime + transitionTime) / 60.0
    }

    fun saveToDB(context: Context) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val database = DBHelper(context).writableDatabase
                val values = ContentValues()
                values.put("uuid", id)
                values.put(SQLTableData.ColorSequenceEntry.COLUMN_NAME_NAME, name)
                values.put(SQLTableData.ColorSequenceEntry.COLUMN_NAME_SEQUENCE_TYPE, sequenceType)
                values.put(SQLTableData.ColorSequenceEntry.COLUMN_NAME_SUSTAIN_TIME, sustainTime)
                values.put(SQLTableData.ColorSequenceEntry.COLUMN_NAME_TRANSITION_TIME, transitionTime)
                values.put(SQLTableData.ColorSequenceEntry.COLUMN_NAME_TRANSITION_TYPE, transitionType)

                database.insertWithOnConflict(SQLTableData.ColorSequenceEntry.TABLE_NAME,
                    null, values, SQLiteDatabase.CONFLICT_REPLACE)


                // Remove all old colors for the sequence
                database.delete(SQLTableData.ColorSequenceColorEntry.TABLE_NAME,
                    "${SQLTableData.ColorSequenceColorEntry.COLUMN_NAME_SEQUENCE_ID}=?",
                    arrayOf(id)
                )
                for (i in 0 until colors.size) {
                    val color = colors[i]

                    val componentValues = ContentValues()
                    componentValues.put(SQLTableData.ColorSequenceColorEntry.COLUMN_NAME_SEQUENCE_ID, id)
                    componentValues.put(SQLTableData.ColorSequenceColorEntry.COLUMN_NAME_ORDER_INDEX, i)
                    componentValues.put(SQLTableData.ColorSequenceColorEntry.COLUMN_NAME_COLOR_ARGB, color.toArgb())
                    database.insertWithOnConflict(SQLTableData.ColorSequenceColorEntry.TABLE_NAME,
                        null, componentValues, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
        }
    }
}