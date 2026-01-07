package com.cogniter.watchaccuracychecker.adapter

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.database.DBHelper
import com.cogniter.watchaccuracychecker.database.entity.SubItemEntity
import com.cogniter.watchaccuracychecker.model.Subitem
import com.cogniter.watchaccuracychecker.utills.ImageUtils

class CustomAdapater(
    private val isFromAll: Boolean,
    private val watchName: String,
    private val context: Context,
    private var subitemList: List<Subitem>
) : RecyclerView.Adapter<CustomAdapater.ImageViewHolder>() {

    private var deleteClickListener: OnDeleteClickListener? = null

    interface OnDeleteClickListener {
        fun OnDeleteClickListener(subItem: Subitem, position: Int)
    }

    fun setOnDeleteClickListener(listener: OnDeleteClickListener) {
        deleteClickListener = listener
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val elapsedTime: TextView = itemView.findViewById(R.id.elapsedTime)
        val historyTime: TextView = itemView.findViewById(R.id.historyTime)
        val deleteHistoryBtn: RelativeLayout = itemView.findViewById(R.id.deleteHistoryBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val subItem = subitemList[position]

        // Load image safely
        val imageUri = ImageUtils.getImageUriFromName(context, subItem.image)
        if (imageUri != null) {
            Glide.with(context).load(imageUri).into(holder.imageView)
        }

        holder.historyTime.text = "Date & Time: ${subItem.date}"
        holder.elapsedTime.text = "Elapsed time: ${subItem.name}"

        holder.deleteHistoryBtn.setOnClickListener {
            deleteClickListener?.OnDeleteClickListener(subItem, position)
        }

        holder.itemView.setOnClickListener {
//            showFullScreenPopup(position, subItem.subitemId)

            showFullScreenPopup(subItem)
        }
    }

    override fun getItemCount(): Int = subitemList.size


    private fun showFullScreenPopup(subItem: Subitem) {

        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.fullscreen_popup)

        val imageView = dialog.findViewById<ImageView>(R.id.historyImage)
        val nameTextView = dialog.findViewById<TextView>(R.id.nameTextView)
        val dateText = dialog.findViewById<TextView>(R.id.dateText)
        val elapsedText = dialog.findViewById<TextView>(R.id.elapsedText)
        val backBtn = dialog.findViewById<ImageView>(R.id.backButtonPopup)

        backBtn.setOnClickListener { dialog.dismiss() }

        // Watch name
        nameTextView.text = watchName

        // Load image safely
        val imageUri = ImageUtils.getImageUriFromName(context, subItem.image)
        if (imageUri != null) {
            Glide.with(context)
                .load(imageUri)
                .into(imageView)
        }

        // Date & time
        dateText.text = "Date: ${subItem.date}"

        // Elapsed time
        elapsedText.text = "Elapsed: ${subItem.name}"

        dialog.show()
    }


}
