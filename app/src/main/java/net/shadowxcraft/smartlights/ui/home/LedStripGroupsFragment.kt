package net.shadowxcraft.smartlights.ui.home

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.packets.SetColorForLEDStripPacket
import net.shadowxcraft.smartlights.ui.color_editor.ColorEditorDialog
import net.shadowxcraft.smartlights.ui.colors.ColorsFragment
import net.shadowxcraft.smartlights.ui.controllers.ControllersFragment
import net.shadowxcraft.smartlights.ui.schedules.SchedulesFragment


class LedStripGroupsFragment : Fragment(), ButtonClickListener, ColorEditorDialog.ColorSelectedListener {

    var adapter: LEDStripListAdapter? = null

    var dialog: ColorEditorDialog? = null

    private lateinit var rvControllers: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        OrderingManager.checkLedStripOrders(true)

        val root = inflater.inflate(R.layout.fragment_led_strips, container, false)

        setHasOptionsMenu(true)

        val floatingButton: View = root.findViewById(R.id.led_floating_action_button)
        floatingButton.setOnClickListener {
            // Show menu to add LED Strip
            Utils.replaceFragment(ControllersFragment(), parentFragmentManager)
        }

        // Lookup the recyclerview in activity layout
        rvControllers = root.findViewById(R.id.list_led_strip) as RecyclerView
        rvControllers.setHasFixedSize(true)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        rvControllers.addItemDecoration(itemDecoration)

        // Create adapter that uses the list of LED strips.
        adapter = LEDStripListAdapter(context, this, parentFragmentManager, true)
        //adapter = LEDStripGroupListAdapter(this, rvControllers)


        // Attach the adapter to the recyclerview to populate items
        rvControllers.adapter = adapter

        // Set layout manager to position the items
        rvControllers.layoutManager = LinearLayoutManager(context)

        SharedData.ledStripGroupsFragment = this

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SharedData.ledStripGroupsFragment = null
    }

    override fun onButtonClicked(position: Int, itemId: Int) {
        val ledStrip = adapter!!.getNthLEDStrip(position)
        when (itemId) {
            R.id.set_colors_button -> {
                Utils.replaceFragment(ColorsFragment(ledStrip), parentFragmentManager)
            }
            R.id.set_color_button -> {
                dialog = ColorEditorDialog(
                    BLEControllerManager.activity!!,
                    Color(255, 0, 0),
                    ledStrip
                )
                dialog!!.listener = this
                dialog!!.display()
            }
            R.id.edit_schedules_button -> {
                // TODO
                Utils.replaceFragment(SchedulesFragment(ledStrip), parentFragmentManager)
            }
        }
    }

    override fun onColorSelected(color: Color) {
        dialog!!.ledStrip!!.setSimpleColor(color, true)
        dialog!!.ledStrip!!.setCurrentSeq(null, true)
        // Clear the preview that likely built up.
        dialog!!.ledStrip!!.controller.clearQueueForPacketID(19)
        SetColorForLEDStripPacket(dialog!!.ledStrip!!, color, 0u).send()// indefinitely
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.led_strip_list_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.action_reorder) {
            toggleReorderMode()
            true
        } else super.onOptionsItemSelected(item)
    }

    private fun toggleReorderMode() {
        if (adapter == null)
            return
        adapter?.reorderMode = !adapter!!.reorderMode
        adapter?.notifyDataSetChanged()

        if (adapter!!.reorderMode) {
            itemTouchHelper.attachToRecyclerView(rvControllers)
            Toast.makeText(context, "Reordering mode enabled", Toast.LENGTH_SHORT).show()
        } else {
            itemTouchHelper.attachToRecyclerView(null)
            Toast.makeText(context, "Reordering mode disabled", Toast.LENGTH_SHORT).show()
        }
    }


    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    or ItemTouchHelper.START or ItemTouchHelper.END, 0)
        {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                if (!adapter!!.reorderMode)
                    return false
                val adapter = recyclerView.adapter as RecyclerView.Adapter<*>
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                OrderingManager.moveLEDStripOrder(from, to, true)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder?.itemView?.alpha = 1.0f
            }
        }

        ItemTouchHelper(simpleItemTouchCallback)
    }
}