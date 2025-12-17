package com.cogniter.watchaccuracychecker.adapter


import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.model.DataModel


class DrawerItemCustomAdapter(
    var mContext: Context,
    var layoutResourceId: Int,
    data: Array<DataModel?>
) : ArrayAdapter<DataModel?>(mContext, layoutResourceId, data) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }
    // Add a variable to keep track of the selected position
    private var selectedPosition: Int = -1
    private var itemClickListener: OnItemClickListener? = null
    var data: Array<DataModel?> = data // Change the type to nullable DataModel

    // Add a method to update the selected position
    fun setSelectedPosition(position: Int) {
        System.out.println("dkijdiidjijdi  position "+position)
        selectedPosition = position
        notifyDataSetChanged() // Notify the adapter that the data set has changed
    }
    // Define the interface for the item click listener
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }


    // Method to set the listener from the activity
    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewType = getItemViewType(position)
        return if (viewType == VIEW_TYPE_HEADER) {
            val headerView = LayoutInflater.from(mContext).inflate(R.layout.header_layout, parent, false)

            headerView
        } else {
            var listItem = convertView
            val inflater = (mContext as Activity).layoutInflater
            listItem = inflater.inflate(R.layout.list_view_item_row, parent, false)
            val imageViewIcon = listItem.findViewById<View>(R.id.imageViewIcon) as ImageView
            val rightArrow = listItem.findViewById<View>(R.id.rightArrow) as ImageView
            val textViewName = listItem.findViewById<View>(R.id.textViewName) as TextView
            val folder: DataModel? = data[position - 1] // Adjust position for header
            folder?.let {
                imageViewIcon.setImageResource(it.icon)
                textViewName.text = it.name
            }
            listItem.setOnClickListener {

                itemClickListener!!.onItemClick(position)
            }
            // Update the color based on the selected position


            if (position == selectedPosition) {
                imageViewIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.darkyellow))
                textViewName.setTextColor(ContextCompat.getColor(mContext, R.color.darkyellow))
                rightArrow.setColorFilter(ContextCompat.getColor(mContext, R.color.darkyellow))
            } else {
                imageViewIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.white))
                textViewName.setTextColor(ContextCompat.getColor(mContext, R.color.white))
                rightArrow.setColorFilter(ContextCompat.getColor(mContext, R.color.white))
            }



            listItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun getCount(): Int {
        return data.size + 1 // Add 1 for the header
    }

    override fun getItem(position: Int): DataModel? {
        return if (position == 0) {
            null
        } else {
            data[position - 1] // Adjust position for header
        }
    }

    override fun getViewTypeCount(): Int {
        return 2 // Header view and item view
    }
}
