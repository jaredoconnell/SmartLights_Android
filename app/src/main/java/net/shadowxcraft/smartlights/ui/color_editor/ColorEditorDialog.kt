package net.shadowxcraft.smartlights.ui.color_editor

import android.app.Activity
import android.app.AlertDialog
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.madrapps.pikolo.HSLColorPicker
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener
import net.shadowxcraft.smartlights.Color
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.R
import net.shadowxcraft.smartlights.packets.SetColorForLEDStripPacket

class ColorEditorDialog(private val act: Activity, initialColor: Color, val ledStrip: LEDStrip?)
    : SimpleColorSelectionListener(), SeekBar.OnSeekBarChangeListener
{
    private var lastColor = initialColor.toArgb()

    private lateinit var backgroundImage: ImageView
    private lateinit var colorTempIndicator: TextView
    private lateinit var colorTempSeekBar: SeekBar
    private lateinit var colorPicker: HSLColorPicker

    var listener: ColorSelectedListener? = null

    fun display() {
        val builder = AlertDialog.Builder(act)

        val inflater = act.layoutInflater
        val view = inflater.inflate(R.layout.color_editor, null)
        colorTempSeekBar = view.findViewById(R.id.color_temp_bar)
        colorTempIndicator = view.findViewById(R.id.color_id_indicator)
        val colorTempBackground: ImageView = view.findViewById(R.id.color_temp_preview)
        colorPicker = view.findViewById(R.id.colorPicker)
        backgroundImage = view.findViewById(R.id.color_picker_preview_background)
        colorPicker.setColor(lastColor)
        backgroundImage.background.setColorFilter(lastColor, PorterDuff.Mode.MULTIPLY)
        // Hide the seekbar until touched
        colorTempSeekBar.thumb.mutate().alpha = 0
        // Show background
        val color = Color(0)
        val colors: ArrayList<Int> = ArrayList()
        for (i in 1..80) {
            color.setRGBFromTemp(i * 100 + 1000)
            colors.add(color.toArgb())
        }
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            colors.toIntArray())
        colorTempBackground.setImageDrawable(gradient)

        colorPicker.setColorSelectionListener(this)
        colorTempSeekBar.setOnSeekBarChangeListener(this)

        builder.setView(view)
            // Add action buttons
            .setPositiveButton(R.string.set) { _, _ ->
                val newColor = Color(lastColor)
                listener?.onColorSelected(newColor)
            }.show()
            .setCancelable(true)
    }

    override fun onColorSelected(color: Int) {
        // Do whatever you want with the color
        val colorAsObj = Color(color)
        onSelectedColor(colorAsObj)
        colorTempIndicator.text = colorAsObj.toString()

        // Hide color temp selection since they selected a color
        colorTempSeekBar.thumb.mutate().alpha = 0
        colorTempSeekBar.progress = 0
    }

    override fun onProgressChanged(seek: SeekBar,
                                   progress: Int, fromUser: Boolean)
    {
        // Show the seekbar
        colorTempSeekBar.thumb.mutate().alpha = 255
        val colorTemp = progress * 200 + 1000;
        val color = Color(colorTemp, 255)
        onSelectedColor(color)
        colorPicker.setColor(lastColor)
        colorTempIndicator.text = "${colorTemp}k"

    }

    private fun onSelectedColor(color: Color) {
        lastColor = color.toArgb()
        backgroundImage.background.setColorFilter(lastColor, PorterDuff.Mode.MULTIPLY)
        if (ledStrip != null) {
            SetColorForLEDStripPacket(ledStrip, color, 5).queue()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    interface ColorSelectedListener {
        fun onColorSelected(color: Color)
    }
}