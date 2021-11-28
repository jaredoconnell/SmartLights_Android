package net.shadowxcraft.smartlights.ui.controllers

import android.app.AlertDialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.R.*
import net.shadowxcraft.smartlights.ui.add_led_strip.LEDStripComponentFragment
import net.shadowxcraft.smartlights.ui.bluetooth.BluetoothFragment


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ControllersFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ControllersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ControllersFragment : Fragment(), BLEControllerManager.BluetoothConnectionListener,
    ButtonClickListener
{
    //private var param1: String? = null
    private var adapter: ControllerListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //param1 = it.getString(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val currentView: View = inflater.inflate(layout.fragment_controllers, container, false)
        val floatingButton: View = currentView.findViewById(R.id.controllers_floating_action_button)
        floatingButton.setOnClickListener {
            Utils.replaceFragment(BluetoothFragment(), parentFragmentManager)
        }

        // Create adapter passing in the sample user data
        adapter = ControllerListAdapter(ControllerManager.controllers, this)

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

        BLEControllerManager.setConnectionListener(this)

        return currentView
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ControllersFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(/*param1: String*/) =
            ControllersFragment().apply {
                arguments = Bundle().apply {
                    //putString(ARG_PARAM1, param1)
                }
            }
    }

    override fun onControllerChange(device: ESP32) {
        Log.println(Log.INFO, "ControllersFragment",
            "onConnect called " + ControllerManager.controllers )
        adapter?.notifyDataSetChanged()
    }

    override fun onButtonClicked(position: Int, itemId: Int) {
        val device = ControllerManager.controllers[position]

        when (itemId) {
            R.id.add_pwm_driver_button -> {
                val view = requireActivity().layoutInflater.inflate(layout.new_item_pwm_driver, null)
                val addressSelector: NumberPicker = view.findViewById(R.id.pwm_driver_id_picker)
                addressSelector.minValue = 64
                addressSelector.maxValue = 120

                AlertDialog.Builder(this.activity)
                    .setView(view).setNegativeButton("Cancel", null)
                    .setPositiveButton("Add") { _: DialogInterface?, _: Int ->
                        val driverAddr: Int = addressSelector.value

                        device.addPWMDriver(PWMDriver(driverAddr), true)
                    }.show()
            }
            R.id.add_led_strip_button -> {
                Utils.replaceFragment(LEDStripComponentFragment(device), parentFragmentManager)
            }
            R.id.rename_controller_button -> {
                device.renameController(this)
            }
            else -> {
                Toast.makeText(
                    BLEControllerManager.activity,
                    "Unknown button.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    }
}
