package com.cogniter.watchaccuracychecker.adapter

// NameAdapter.kt
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.model.ListItem


class MyWatchesAdapter(
    private var nameList: List<ListItem>,
    private val context: Context
) : RecyclerView.Adapter<MyWatchesAdapter.NameViewHolder>() {

    // Define the interface for click events
    interface OnImageClickListener {
        fun onImageClick(name: ListItem, i: Int)
    }

    // Add a property to hold the listener
    private var imageClickListener: OnImageClickListener? = null

    // Method to set the listener from the activity
    fun setOnImageClickListener(listener: OnImageClickListener) {
        imageClickListener = listener
    }

    class NameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val watchTimeText: TextView = itemView.findViewById(R.id.watchTimeText)
        val watchimage : ImageView = itemView.findViewById(R.id.watchimage)
        val deleteBtn: RelativeLayout = itemView.findViewById(R.id.deleteBtn)
        val editBtn: RelativeLayout = itemView.findViewById(R.id.editBtn)
        val historyBtn: TextView = itemView.findViewById(R.id.historyBtn)
        val trackBtn: TextView = itemView.findViewById(R.id.trackBtn)
        val trackingprogress :View =itemView.findViewById(R.id.trackingprogress)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.mywacthes_adapter, parent, false)
        return NameViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NameViewHolder, position: Int) {
        val currentItem = nameList[position]
        holder.nameTextView.text = currentItem.title

        if(currentItem.isrunning){
            holder.trackingprogress.visibility =View.VISIBLE
        }else{
            holder.trackingprogress.visibility =View.GONE
        }


        if(currentItem.subItems.size>1){
            holder.watchTimeText.text =currentItem.subItems.last().date
        }else{
            holder.watchTimeText.text =currentItem.addedWatchTime
        }

        try{

            if(currentItem.watchimage.isNotEmpty()){
                Glide.with(context)
                    .load(Uri.parse(currentItem.watchimage))
                    .into(holder.watchimage)
            }

                // Use Glide to load the image into the ImageView

        }catch ( e : Exception){

        }

        holder.watchimage.setOnClickListener {
            imageClickListener?.onImageClick(nameList[position],0)
        }
        holder.trackBtn.setOnClickListener(View.OnClickListener { view ->
            // Notify the activity when an image is clicked
            imageClickListener?.onImageClick(nameList[position],3)
        })
        holder.deleteBtn.setOnClickListener {
            imageClickListener?.onImageClick(nameList[position],2)
        }
        holder.editBtn.setOnClickListener {
            imageClickListener?.onImageClick(nameList[position],1)
        }
        holder.historyBtn.setOnClickListener(View.OnClickListener { view ->
            // Notify the activity when an image is clicked
            imageClickListener?.onImageClick(nameList[position],0)
        })



    }

    override fun getItemCount() = nameList.size


}
