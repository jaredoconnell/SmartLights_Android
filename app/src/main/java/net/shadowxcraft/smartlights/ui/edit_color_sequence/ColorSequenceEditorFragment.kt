package net.shadowxcraft.smartlights.ui.edit_color_sequence

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.R.layout
import net.shadowxcraft.smartlights.packets.SetColorSequenceForLEDStripPacket
import net.shadowxcraft.smartlights.ui.color_editor.ColorEditorDialog


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ScheduledChangeEditorFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ScheduledChangeEditorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ColorSequenceEditorFragment(private val act: FragmentActivity, private val colorSequence: ColorSequence,
                                  private val controller: ESP32, private val ledstrip: LEDStrip?,
                                  private val scheduledChange: ScheduledChange? = null)
    : Fragment(), ButtonClickListener, ColorEditorDialog.ColorSelectedListener,
    TabLayout.OnTabSelectedListener {
    private lateinit var tabLayout: TabLayout
    private lateinit var flipper: ViewFlipper
    private var adapter: ColorSequenceEditorListAdapter? = null
    private var editedColorIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val currentView: View = inflater.inflate(layout.fragment_edit_color_sequence, container, false)

        tabLayout = currentView.findViewById(R.id.tabs)
        flipper = currentView.findViewById(R.id.flipper)
        tabLayout.addOnTabSelectedListener(this)
        tabLayout.addTab(tabLayout.newTab().setText("Colors").setIcon(R.drawable.ic_baseline_color_lens_24))
        tabLayout.addTab(tabLayout.newTab().setText("Properties").setIcon(R.drawable.ic_baseline_edit_24))

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
                    tabLayout.getTabAt(0)?.select()
                    Toast.makeText(
                        act,
                        "Please add at least one color.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                name.isEmpty() -> {
                    tabLayout.getTabAt(1)?.select()
                    Toast.makeText(
                        act,
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
                            act,
                            "Please add a transition or sustain time greater than 0.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {

                        controller.addColorSequence(colorSequence, true)

                        if (ledstrip != null && scheduledChange == null) {
                            ledstrip.setCurrentSeq(colorSequence, true)
                            SetColorSequenceForLEDStripPacket(ledstrip).send()
                        } else if (scheduledChange != null) {
                            scheduledChange.newColorSequenceID = colorSequence.id
                        }

                        act.supportFragmentManager.popBackStack()
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
        editedColorIndex = colorIndex
        val initialColor = if (colorIndex >= 0) {
            // Set to existing color
            colorSequence.colors[colorIndex]
        } else {
            Color(255, 0, 0)
        }
        val dialog = ColorEditorDialog(act, initialColor, ledstrip)
        dialog.listener = this
        dialog.display()

    }

    override fun onColorSelected(newColor: Color) {
        if (editedColorIndex >= 0) {
            // Edit existing color
            colorSequence.colors[editedColorIndex] = newColor
        } else {
            colorSequence.colors.add(newColor)
        }
        adapter?.notifyDataSetChanged()
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        flipper.displayedChild = tab!!.position
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
    }
}
