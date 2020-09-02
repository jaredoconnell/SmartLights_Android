package net.shadowxcraft.smartlights.ui.edit_color_sequence

import android.app.Activity
import android.app.AlertDialog
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.madrapps.pikolo.HSLColorPicker
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.R.layout
import net.shadowxcraft.smartlights.packets.SetColorSequenceForLEDStripPacket


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ColorSequenceEditorFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ColorSequenceEditorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ColorSequenceEditorFragment(private val act: Activity, private val colorSequence: ColorSequence,
                                  private val controller: ESP32, private val ledstrip: LEDStrip?)
    : Fragment(), ButtonClickListener
{
    private var adapter: ColorSequenceEditorListAdapter? = null
    private var lastColor: Int = android.graphics.Color.RED

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val currentView: View = inflater.inflate(layout.fragment_edit_color_sequence, container, false)
        val sustainTimeMinutesView = currentView.findViewById<NumberPicker>(R.id.color_sequence_sustain_minutes)
        sustainTimeMinutesView.maxValue = 59
        sustainTimeMinutesView.minValue = 0
        sustainTimeMinutesView.value = colorSequence.sustainTime.div(60 * 60)
        val sustainTimeSecondsView = currentView.findViewById<NumberPicker>(R.id.color_sequence_sustain_seconds)
        sustainTimeSecondsView.maxValue = 59
        sustainTimeSecondsView.minValue = 0
        sustainTimeSecondsView.value = colorSequence.sustainTime.div(60).rem(60)
        val sustainTimeTicksView = currentView.findViewById<NumberPicker>(R.id.color_sequence_sustain_frames)
        sustainTimeTicksView.maxValue = 59
        sustainTimeTicksView.minValue = 0
        sustainTimeTicksView.value = colorSequence.sustainTime.rem(60)
        val transitionTimeMinutesView = currentView.findViewById<NumberPicker>(R.id.color_sequence_transition_minutes)
        transitionTimeMinutesView.maxValue = 59
        transitionTimeMinutesView.minValue = 0
        transitionTimeMinutesView.value = colorSequence.transitionTime.div(60 * 60)
        val transitionTimeSecondsView = currentView.findViewById<NumberPicker>(R.id.color_sequence_transition_seconds)
        transitionTimeSecondsView.maxValue = 59
        transitionTimeSecondsView.minValue = 0
        transitionTimeSecondsView.value = colorSequence.transitionTime.div(60).rem(60)
        val transitionTimeTicksView = currentView.findViewById<NumberPicker>(R.id.color_sequence_transition_frames)
        transitionTimeTicksView.maxValue = 59
        transitionTimeTicksView.minValue = 0
        transitionTimeTicksView.value = colorSequence.transitionTime.rem(60)
        val nameComponent: EditText = currentView.findViewById(R.id.edit_color_sequence_name)
        nameComponent.setText(colorSequence.name)


        val addButton: View = currentView.findViewById(R.id.add_color_sequence_color)
        addButton.setOnClickListener {
            displayColorEditor(-1)
        }
        val completeButton: View = currentView.findViewById(R.id.complete_color_sequence_edit)
        completeButton.setOnClickListener {
            val name = nameComponent.text.toString()
            when {
                colorSequence.colors.isEmpty() -> {
                    Toast.makeText(
                        BLEControllerManager.activity,
                        "Please add at least one color.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                name.isEmpty() -> {
                    Toast.makeText(
                        BLEControllerManager.activity,
                        "Please give the color sequence a name.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    // Update the values. The colors were already updated.
                    // TODO: Update these realtime
                    colorSequence.sustainTime = sustainTimeMinutesView.value * 60 * 60
                    colorSequence.sustainTime += sustainTimeSecondsView.value * 60
                    colorSequence.sustainTime +=  + sustainTimeTicksView.value
                    colorSequence.transitionTime = transitionTimeMinutesView.value * 60 * 60
                    colorSequence.transitionTime += transitionTimeSecondsView.value * 60
                    colorSequence.transitionTime += transitionTimeTicksView.value
                    colorSequence.name = name

                    if ((colorSequence.sustainTime + colorSequence.transitionTime <= 0)
                        && colorSequence.colors.size > 1) {
                        Toast.makeText(
                            BLEControllerManager.activity,
                            "Please add a transition or sustain time greater than 0.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {

                        controller.addColorSequence(colorSequence, true)

                        if (ledstrip != null) {
                            ledstrip.currentSeq = colorSequence
                            SetColorSequenceForLEDStripPacket(ledstrip).send()
                        }

                        (context as MainActivity).supportFragmentManager.popBackStack()
                    }
                }
            }
        }

        // Create adapter passing in the led strip components
        adapter = ColorSequenceEditorListAdapter(colorSequence.colors, this)

        // Lookup the recyclerview in activity layout
        val rvControllers = currentView.findViewById(R.id.color_sequence_colors) as RecyclerView
        rvControllers.setHasFixedSize(true)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        rvControllers.addItemDecoration(itemDecoration)


        // Attach the adapter to the recyclerview to populate items
        rvControllers.adapter = adapter

        // Set layout manager to position the items
        rvControllers.layoutManager = LinearLayoutManager(context)

        return currentView
    }

    override fun onButtonClicked(position: Int, itemId: Int) {
        displayColorEditor(position)
    }

    private fun displayColorEditor(colorIndex: Int) {
        val builder = AlertDialog.Builder(act)
        val inflater = act.layoutInflater;
        val view = inflater.inflate(R.layout.color_editor, null)
        val colorPicker: HSLColorPicker = view.findViewById(R.id.colorPicker)
        val backgroundImage: ImageView = view.findViewById(R.id.color_picker_preview_background)
        lastColor = if (colorIndex >= 0) {
            // Set to existing color
            colorSequence.colors[colorIndex].toArgb()
        } else {
            android.graphics.Color.RED
        }
        colorPicker.setColor(lastColor)
        backgroundImage.background.setColorFilter(lastColor, PorterDuff.Mode.MULTIPLY)


        colorPicker.setColorSelectionListener(object : SimpleColorSelectionListener() {
            override fun onColorSelected(color: Int) {
                // Do whatever you want with the color
                Log.println(Log.INFO, "ColorSeqEditorFragment", "Color: $color")
                lastColor = color
                backgroundImage.background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
            }
        })

        builder.setView(view)
            // Add action buttons
            .setPositiveButton(R.string.set) { _, _ ->
                val newColor = Color(lastColor)
                if (colorIndex >= 0) {
                    // Edit existing color
                    colorSequence.colors[colorIndex] = newColor
                } else {
                    colorSequence.colors.add(newColor)
                }
                adapter?.notifyDataSetChanged()
                Toast.makeText(
                    BLEControllerManager.activity,
                    "Applied",
                    Toast.LENGTH_SHORT
                ).show()
            }.show()
    }
}
