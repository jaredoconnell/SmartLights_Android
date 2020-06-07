package net.shadowxcraft.smartlights.ui.controllers

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import net.shadowxcraft.smartlights.BLEControllerManager
import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.R
import net.shadowxcraft.smartlights.R.*
import net.shadowxcraft.smartlights.Utils
import net.shadowxcraft.smartlights.ui.bluetooth.BluetoothFragment
import net.shadowxcraft.smartlights.ui.bluetooth.BluetoothListAdapter


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ControllersFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ControllersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ControllersFragment : Fragment(), BLEControllerManager.BluetoothConnectionListener {
    //private var param1: String? = null
    private var listener: OnFragmentInteractionListener? = null
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
        val fab: View = currentView.findViewById(R.id.controllers_floating_action_button)
        fab.setOnClickListener {
            Utils.replaceFragment(BluetoothFragment(), parentFragmentManager)
        }

        // Create adapter passing in the sample user data
        adapter = ControllerListAdapter(BLEControllerManager.controllers)

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

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
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
        Log.println(Log.INFO, "ControllersFragment", "onConnect called " + BLEControllerManager.controllers )
        adapter?.notifyDataSetChanged()
    }
}
