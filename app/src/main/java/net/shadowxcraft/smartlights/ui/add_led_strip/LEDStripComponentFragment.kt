package net.shadowxcraft.smartlights.ui.add_led_strip

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.R.layout
import java.util.*
import kotlin.collections.ArrayList


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LEDStripComponentFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LEDStripComponentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LEDStripComponentFragment(val controller: ESP32) : Fragment() {
    //private var param1: String? = null
    private var listener: OnFragmentInteractionListener? = null
    var adapter: LEDStripComponentListAdapter? = null

    var components: ArrayList<LEDStripComponent> = ArrayList()
    var colors: TreeMap<String, Color> = TreeMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //param1 = it.getString(ARG_PARAM1)
        }
        colors["Red"] = Color(255, 0, 0)
        colors["Green"] = Color(0, 255, 0)
        colors["Blue"] = Color(0, 0, 255)
        colors["3200k"] = Color(3200, 4095)
        colors["4000k"] = Color(4000, 4095)
        colors["5000k"] = Color(5000, 4095)
        colors["6500k"] = Color(6500, 4095)
    }

    /**
     * Null driver means the ESP32.
     */
    fun pinAlreadyInUse(pin: Int, driver: PinDriver, ignoredComponent: LEDStripComponent?) : Boolean {
        for (component in components) {
            if (component != ignoredComponent
                && component.driver == driver && component.driverPin == pin)
                return true
        }
        for (strip in controller.ledStrips.values) {
             for (component in strip.components) {
                 if (component != ignoredComponent && component.driver == driver && component.driverPin == pin)
                     return true
             }
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val currentView: View = inflater.inflate(layout.fragment_led_strip_components, container, false)
        val addButton: View = currentView.findViewById(R.id.add_led_strip_component_floating_action_button)
        addButton.setOnClickListener {
            EditComponentDialog(this, this.requireActivity())
        }
        val completeButton: View = currentView.findViewById(R.id.complete_led_strip_components)
        completeButton.setOnClickListener {
            val nameComponent: EditText = currentView.findViewById(R.id.new_led_strip_name)
            val name = nameComponent.text.toString()
            when {
                components.isEmpty() -> {
                    Toast.makeText(
                        BLEControllerManager.activity,
                        "Please add components.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                name.isEmpty() -> {
                    Toast.makeText(
                        BLEControllerManager.activity,
                        "Please give the LED strip a name.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    val nextID = UUID.randomUUID().toString()
                    val newStrip = LEDStrip(nextID, name, controller)
                    newStrip.components.addAll(components)
                    controller.addLEDStrip(newStrip, sendPacket=true, save=true)
                    (context as MainActivity).supportFragmentManager.popBackStack()
                }
            }
        }

        // Create adapter passing in the led strip components
        adapter = LEDStripComponentListAdapter(this, components)

        // Lookup the recyclerview in activity layout
        val rvControllers = currentView.findViewById(R.id.list_controllers) as RecyclerView
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }
}
