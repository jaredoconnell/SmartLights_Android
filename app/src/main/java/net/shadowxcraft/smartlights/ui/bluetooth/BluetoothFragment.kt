package net.shadowxcraft.smartlights.ui.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import net.shadowxcraft.smartlights.BLEControllerManager
import net.shadowxcraft.smartlights.ClickListener
import net.shadowxcraft.smartlights.MainActivity
import net.shadowxcraft.smartlights.R


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [BluetoothFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [BluetoothFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BluetoothFragment : Fragment(), ClickListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallbackImpl = ScanCallbackImpl()
    private var foundBluetoothDevices: ArrayList<BluetoothDevice> = ArrayList()
    private var adapter: BluetoothAdapter? = null

    inner class ScanCallbackImpl : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult ) {
            Log.println(Log.INFO, "BluetoothFragment", "Got result! " + result.device.name)
            val device = result.device
            if(!foundBluetoothDevices.contains(device)) { // new
                foundBluetoothDevices.add(device)
                adapter!!.notifyItemInserted(foundBluetoothDevices.size - 1);
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.println(Log.WARN, "BluetoothFragment", "Failed.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        if(BLEControllerManager.supportsBluetooth()) {
            scanner = BLEControllerManager.getBLEScanner()
            Log.println(Log.INFO, "BluetoothFragment", "Starting bluetooth scan.")
            scanner!!.startScan(scanCallback)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_bluetooth, container, false)

        // Lookup the recyclerview in activity layout
        val rvDevices = view.findViewById(R.id.list_bluetooth_devices) as RecyclerView
        rvDevices.setHasFixedSize(true)
        val itemDecoration: ItemDecoration =
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        rvDevices.addItemDecoration(itemDecoration)

        // Create adapter passing in the sample user data
        adapter = BluetoothAdapter(foundBluetoothDevices, this)

        // Attach the adapter to the recyclerview to populate items
        rvDevices.adapter = adapter

        // Set layout manager to position the items
        rvDevices.layoutManager = LinearLayoutManager(context)

        if(!BLEControllerManager.supportsBluetooth()) {
            view.findViewById<TextView>(R.id.bluetooth_textview).text = "Bluetooth Not Supported"
        }

        return view
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scanner?.stopScan(scanCallback)
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
         * @return A new instance of fragment BluetoothFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BluetoothFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onPositionClicked(position: Int) {
        scanner?.stopScan(scanCallback)
        Log.println(Log.INFO, "BluetoothFragment", "Selected: " + foundBluetoothDevices[position])
        BLEControllerManager.connectTo(foundBluetoothDevices[position])
        (context as MainActivity).supportFragmentManager.popBackStackImmediate()
    }


}
