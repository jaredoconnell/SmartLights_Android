package net.shadowxcraft.smartlights.ui.edit_color_sequence

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.ButtonClickListener
import net.shadowxcraft.smartlights.Color
import net.shadowxcraft.smartlights.R


// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class ColorSequenceEditorListAdapter(private val colorsList: ArrayList<Color>,
                                     private val editButtonClickListener: ButtonClickListener
)
    : RecyclerView.Adapter<ColorSequenceEditorListAdapter.ViewHolder?>()
{

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        var colorView: View = itemView.findViewById(R.id.color_preview)
        var colorID: TextView = itemView.findViewById(R.id.item_color_sequence_color_id)
        var editButton: Button = itemView.findViewById(R.id.color_edit_button)
        override fun onClick(v: View?) {
            //val component = componentList[adapterPosition]

        }

        init {
            itemView.setOnClickListener(this)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context: Context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val deviceView: View = inflater.inflate(R.layout.item_color_sequence_color, parent, false)

        // Return a new holder instance
        return ViewHolder(deviceView)
    }

    override fun getItemCount(): Int {
        return colorsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val color: Color = colorsList[position]

        // Set item views based on your views and data model
        holder.colorView.setBackgroundColor(color.toArgb())
        holder.colorID.text = color.toString()
        holder.editButton.setOnClickListener {
            editButtonClickListener.onButtonClicked(position, R.id.color_edit_button)
        }
    }
}