package com.cogniter.watchaccuracychecker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.model.ListItem
import com.cogniter.watchaccuracychecker.model.Subitem

class AlhistoryAdapater(private val context: Context, private val itemList: List<ListItem>) : RecyclerView.Adapter<AlhistoryAdapater.ImageViewHolder>() ,CustomAdapater.OnDeleteClickListener{
    var adapter:CustomAdapater ? = null
    var subitemList: List<Subitem>? = null

    // Define the interface for click events
    interface OnallHistoryDeleteClickListener {
        fun OnallHistoryDelete(name: Subitem, i: Int)
    }

    // Add a property to hold the listener
    private var onallHistoryDeleteClickListener: OnallHistoryDeleteClickListener? = null

    // Method to set the listener from the activity
    fun setOnallHistoryDeleteClickListener(listener: OnallHistoryDeleteClickListener) {
        onallHistoryDeleteClickListener = listener
    }
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val historywatchName: TextView = itemView.findViewById(R.id.historywatchName)
        val subitemRecylerview: RecyclerView = itemView.findViewById(R.id.subitemRecylerview)


    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.all_history_adpater_items, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {


        holder.historywatchName.text = itemList[position].title


        subitemList=itemList[position].subItems!!
        subitemList?.let { list ->
            if (list.isNotEmpty()) {
                val mutableList = list.toMutableList()  // Convert to mutable list
                mutableList.removeAt(0)  // Remove the first item
                subitemList = mutableList.toList()  // Convert back to immutable list
            }
        }
        subitemList = subitemList?.reversed()

        if(subitemList!!.size==0){
            holder.historywatchName.visibility=View.GONE

        }else{
            holder.historywatchName.visibility=View.VISIBLE
        }
        if(position>0){
            val params = holder.historywatchName.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(0, 20, 0, 0) // Here, 50 is the margin from the top
            holder.historywatchName.layoutParams = params
        }

        adapter = CustomAdapater(
            true,
            itemList[position].title,
            context,
            subitemList!!
        )
        holder.subitemRecylerview!!.adapter = adapter
        holder.subitemRecylerview!!!!.layoutManager = LinearLayoutManager(context)

        holder.subitemRecylerview!!!!.adapter = adapter

        adapter!!.setOnDeleteClickListener(this)

    }
    override fun getItemCount() = itemList.size
    override fun OnDeleteClickListener(name: Subitem, i: Int) {
        onallHistoryDeleteClickListener!!.OnallHistoryDelete(name,i)
    }


}
