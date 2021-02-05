package net.shadowxcraft.smartlights.ui.edit_scheduled_change

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.R.layout
import net.shadowxcraft.smartlights.packets.ScheduleChangePacket
import net.shadowxcraft.smartlights.ui.color_editor.ColorEditorDialog
import net.shadowxcraft.smartlights.ui.colors.ColorsFragment
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

class ScheduledChangeEditorFragment(private val scheduledChange: ScheduledChange)
    : Fragment() {
    private lateinit var currentView: View
    private lateinit var colorFlipper: ViewFlipper
    private lateinit var timeTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var colorEditButton: Button
    private lateinit var colorSequenceEditButton: Button
    private lateinit var powerRadioGroup: RadioGroup
    private lateinit var colorRadioGroup: RadioGroup
    private lateinit var dateRadioGroup: RadioGroup
    private lateinit var brightnessChangeToggle: Switch
    private lateinit var brightnessChangeSlider: SeekBar
    private lateinit var dateFlipper: ViewFlipper
    private lateinit var turnOffToggleSwitch: Switch
    private lateinit var selectPowerOffSection: LinearLayout
    private lateinit var minutesRuntimeSection: LinearLayout
    private lateinit var nameTextEdit: EditText
    private var dialog: ColorEditorDialog? = null
    private val hourFormatter: DateTimeFormatter = DateTimeFormat.forPattern("hh:mm a")
    private val dateFormatter: DateTimeFormatter = DateTimeFormat.forPattern("EEE, MMMM dd, yyyy")
    private val daysToggles = arrayOf(R.id.sunday_toggle, R.id.monday_toggle, R.id.tuesday_toggle,
        R.id.wednesday_toggle, R.id.thursday_toggle, R.id.friday_toggle, R.id.saturday_toggle)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        if (scheduledChange.year <= 1970) { // Unchanged
            scheduledChange.setFromLocalDateTime(scheduledChange.getNowLocal().plusMinutes(30))
        }

        currentView = inflater.inflate(layout.fragment_edit_scheduled_change, container, false)
        timeTextView = currentView.findViewById(R.id.time_of_day_view)

        nameTextEdit = currentView.findViewById(R.id.edit_scheduled_change_name)
        nameTextEdit.setText(scheduledChange.name)

        initPowerSection()
        initColorSection()
        initBrightnessSection()
        initDateSection()

        val editTimeButton: View = currentView.findViewById(R.id.edit_time_button)
        editTimeButton.setOnClickListener {
            val startTime = scheduledChange.getTimeLocal()
            TimePickerDialog(context, { _: TimePicker, hour: Int, minute: Int ->
                scheduledChange.setFromLocalDateTime(scheduledChange.getTimeLocal()
                    .withHourOfDay(hour).withMinuteOfHour(minute))
                displayTime()
            }, startTime.hourOfDay, startTime.minuteOfHour,false).show()
        }

        val completeButton: View = currentView.findViewById(R.id.complete_scheduled_change_edit)
        completeButton.setOnClickListener {
            val name = nameTextEdit.text.toString()
            when {
                name.isEmpty() -> {
                    Toast.makeText(
                        BLEControllerManager.activity,
                        "Please give the scheduled change a name.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                colorRadioGroup.checkedRadioButtonId == R.id.item_color_sequence_color_id
                        && scheduledChange.newColorSequenceID == null -> {
                    Toast.makeText(
                        BLEControllerManager.activity,
                        "No color sequence is chosen.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    // For ASAP
                    scheduledChange.name = name
                    adjustTime()
                    ScheduleChangePacket(scheduledChange).send()
                    scheduledChange.ledStrip!!.scheduledChanges[scheduledChange.id] = scheduledChange
                    if (parentFragmentManager.backStackEntryCount > 0)
                        parentFragmentManager.popBackStackImmediate()
                    Toast.makeText(
                        BLEControllerManager.activity,
                        "Saved",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
        }

        displayTime()
        return currentView
    }

    private fun initPowerSection() {
        powerRadioGroup = currentView.findViewById(R.id.power_radio_group)
        turnOffToggleSwitch = currentView.findViewById(R.id.toggle_turn_off)
        minutesRuntimeSection = currentView.findViewById(R.id.select_runtime_section)
        selectPowerOffSection = currentView.findViewById(R.id.select_poweroff_section)
        checkValueOfPower()
        powerRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            scheduledChange.turnOn = checkedId == R.id.radio_power_turn_on
            checkValueOfPower()
        }
        turnOffToggleSwitch.setOnCheckedChangeListener { _, _/*isChecked*/ ->
            checkValueOfPower()
        }
    }

    private fun checkValueOfPower() {
        val correctId = if (scheduledChange.turnOn) {
            R.id.radio_power_turn_on
        } else {
            if (scheduledChange.turnsOff()) {
                R.id.radio_power_turn_off
            } else {
                R.id.radio_power_no_change
            }
        }

        if (powerRadioGroup.checkedRadioButtonId != correctId) {
            powerRadioGroup.check(correctId)
        }

        selectPowerOffSection.visibility = if (scheduledChange.turnOn) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (scheduledChange.turnOn) {
            minutesRuntimeSection.visibility = if (scheduledChange.secondsUntilOff == 0
                && !turnOffToggleSwitch.isChecked) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    private fun initColorSection() {
        colorEditButton = currentView.findViewById(R.id.select_color)
        colorEditButton.setOnClickListener {
            dialog = ColorEditorDialog(BLEControllerManager.activity!!,
                scheduledChange.newColor ?: Color(255, 0, 0),
                scheduledChange.ledStrip)
            dialog!!.listener =  object : ColorEditorDialog.ColorSelectedListener {
                override fun onColorSelected(color: Color) {
                    scheduledChange.newColor = color
                    updateColorDisplay()
                }
            }
            dialog!!.display()
        }

        colorSequenceEditButton = currentView.findViewById(R.id.select_color_sequence)
        colorSequenceEditButton.setOnClickListener {
            Utils.replaceFragment(
                ColorsFragment(scheduledChange.ledStrip, scheduledChange),
                parentFragmentManager)
        }
        colorFlipper = currentView.findViewById(R.id.color_flipper)
        colorRadioGroup = currentView.findViewById(R.id.color_radio_group)
        colorRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_color_color -> {
                    if (scheduledChange.newColor == null) {
                        scheduledChange.newColor = Color(255, 0, 0)
                    }
                    colorFlipper.visibility = View.VISIBLE
                    colorFlipper.displayedChild = 0
                }
                R.id.radio_color_sequence -> {
                    colorFlipper.visibility = View.VISIBLE
                    colorFlipper.displayedChild = 1
                    scheduledChange.newColor = null
                    if (scheduledChange.newColorSequenceID == null) {
                        // It is null, so the user needs to pick a color sequence.
                        Utils.replaceFragment(
                            ColorsFragment(scheduledChange.ledStrip, scheduledChange),
                            parentFragmentManager)
                    }
                    updateColorSequenceDisplay()
                }
                R.id.radio_color_unchanged -> {
                    colorFlipper.visibility = View.GONE
                    scheduledChange.newColor = null
                    scheduledChange.newColorSequenceID = null
                }
            }
            checkValueOfPower()
            updateColorDisplay()
        }
        when {
            scheduledChange.newColor != null -> {
                colorRadioGroup.check(R.id.radio_color_color)
            }
            scheduledChange.newColorSequenceID != null -> {
                colorRadioGroup.check(R.id.radio_color_sequence)
            }
            else -> {
                colorRadioGroup.check(R.id.radio_color_unchanged)
            }
        }
    }

    private fun updateColorSequenceDisplay() {
        if (scheduledChange.newColorSequenceID != null) {
            val colorSequence = SharedData.colorsSequences[scheduledChange.newColorSequenceID!!]

            val colorSequencePreview: ImageView =
                currentView.findViewById(R.id.item_color_sequence_preview)
            val colorSequenceName: TextView = currentView.findViewById(R.id.color_sequence_name)
            colorSequencePreview.setImageDrawable(colorSequence!!.getDrawableRepresentation())
            colorSequenceName.text = colorSequence.name
        }
    }
    private fun updateColorDisplay() {
        val colorDisplay: View = currentView.findViewById(R.id.color_preview)
        val color = if (scheduledChange.newColor == null) {
            android.graphics.Color.TRANSPARENT
        } else {
            scheduledChange.newColor!!.toArgb()
        }
        colorDisplay.setBackgroundColor(color)
    }

    private fun initBrightnessSection() {
        brightnessChangeToggle = currentView.findViewById(R.id.toggle_brightness_change)
        brightnessChangeSlider = currentView.findViewById(R.id.brightness_bar)

        brightnessChangeToggle.isChecked = scheduledChange.newBrightness >= 0
        brightnessChangeToggle.setOnCheckedChangeListener { _, isChecked ->
            scheduledChange.newBrightness = if (isChecked) {
                4095
            } else {
                -1
            }
            onUpdateBrightnessToggleChange()
        }
        brightnessChangeSlider.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean)
            {
                scheduledChange.newBrightness = scheduledChange.ledStrip!!.convertToLinear(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        onUpdateBrightnessToggleChange()
    }

    private fun onUpdateBrightnessToggleChange() {
        brightnessChangeSlider.isEnabled = scheduledChange.newBrightness != -1
        if (brightnessChangeSlider.isEnabled)
            brightnessChangeSlider.progress = scheduledChange.ledStrip!!.convertToExponential(scheduledChange.newBrightness)
        checkValueOfPower() // Since this could change it from turn off to no change
    }

    private fun initDateSection() {
        dateTextView = currentView.findViewById(R.id.date_view)
        dateFlipper = currentView.findViewById(R.id.date_flipper)
        dateRadioGroup = currentView.findViewById(R.id.date_radio_group)
        dateRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_date_days_of_week -> {
                    dateFlipper.visibility = View.VISIBLE
                    dateFlipper.displayedChild = 0
                    scheduledChange.isSpecificDate = false
                }
                R.id.radio_date_scheduled -> {
                    dateFlipper.visibility = View.VISIBLE
                    dateFlipper.displayedChild = 1
                    displayDate()
                    scheduledChange.isSpecificDate = true
                }
                R.id.radio_date_today -> {
                    adjustTime()
                    scheduledChange.isSpecificDate = true
                    dateFlipper.visibility = View.GONE
                }
                R.id.radio_date_daily-> {
                    scheduledChange.isSpecificDate = false
                    for (i in 0..6) {
                        val day = currentView.findViewById<Switch>(daysToggles[i])
                        day.isChecked = true
                    }
                    scheduledChange.makeDaily()
                    dateFlipper.visibility = View.GONE
                }
            }
        }
        val editDateButton: View = currentView.findViewById(R.id.edit_date_button)
        editDateButton.setOnClickListener {
            val startTime = scheduledChange.getTimeLocal()
            // Note: The date dialog uses zero-indexed months.
            DatePickerDialog(requireContext(), { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                scheduledChange.setFromLocalDateTime(scheduledChange.getTimeLocal()
                    .withYear(year).withMonthOfYear(month + 1).withDayOfMonth(dayOfMonth))
                displayDate()
            }, startTime.year, startTime.monthOfYear - 1,startTime.dayOfMonth).show()
        }

        for (i in 0..6) {
            val day = currentView.findViewById<Switch>(daysToggles[i])
            day.isChecked = scheduledChange.getDayStatus(i)
            day.setOnCheckedChangeListener { _, isChecked ->
                scheduledChange.setDayStatus(i, isChecked)
            }
        }

        if (scheduledChange.isSpecificDate) {
            if (scheduledChange.isWithin24Hours()) {
                dateRadioGroup.check(R.id.radio_date_today)
            } else {
                dateRadioGroup.check(R.id.radio_date_today)
            }
        } else {
            if (scheduledChange.isEveryDay()) {
                dateRadioGroup.check(R.id.radio_date_today)
            } else {
                dateRadioGroup.check(R.id.radio_date_days_of_week)
            }
        }
    }

    /**
     * Determine the day that makes it as soon as possible.
     * If the time is later than now, do today
     * If the time is earlier than now in the day, do tomorrow.
     */
    private fun adjustTime() {
        if (dateRadioGroup.checkedRadioButtonId == R.id.radio_date_today) {
            // If the current time is after the scheduled minute, set it to tomorrow.
            // else today
            if (scheduledChange.getNowLocal().secondOfDay().get() > scheduledChange.getTimeLocal().secondOfDay().get()) {
                scheduledChange.setFromLocalDateTime(
                    scheduledChange.getTimeLocal()
                        .withDate(scheduledChange.getNowLocal().toLocalDate().plusDays(1)))
            } else {
                scheduledChange.setFromLocalDateTime(
                    scheduledChange.getTimeLocal()
                        .withDate(scheduledChange.getNowLocal().toLocalDate()))
            }
            displayDate()
            scheduledChange.isSpecificDate = true
        }
    }

    private fun displayTime() {
        timeTextView.text = scheduledChange.getTimeLocal().toString(hourFormatter)
    }
    private fun displayDate() {
        dateTextView.text = scheduledChange.getTimeLocal().toString(dateFormatter)
    }
}
