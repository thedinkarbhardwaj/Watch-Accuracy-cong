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
            showFullScreenPopup(position, subItem.subitemId)
        }
    }

    override fun getItemCount(): Int = subitemList.size

    private fun showFullScreenPopup(position: Int, subitemId: Long) {
        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.fullscreen_popup)

        val viewPager = dialog.findViewById<ViewPager2>(R.id.viewPager)
        val nameTextView = dialog.findViewById<TextView>(R.id.nameTextView)
        nameTextView.text = watchName
        val backButtonPopup = dialog.findViewById<ImageView>(R.id.backButtonPopup)
        backButtonPopup.setOnClickListener { dialog.dismiss() }

        var currentPosition = position
        val displayList: List<Subitem> = if (isFromAll) {
            val dbHelper = DBHelper(context)
            var allItems = dbHelper.getAllItems()?.reversed() ?: emptyList()
            val allSubItems = mutableListOf<Subitem>()
            allItems.forEach { item ->
                val subItems = dbHelper.getSubItemsForItem(item.id ?: 0)
                allSubItems.addAll(subItems.reversed())
            }
            allSubItems.removeAll { it.name == "subitem1" }

            // Find the correct position for the clicked subitem
            currentPosition = allSubItems.indexOfFirst { it.subitemId == subitemId }.takeIf { it >= 0 } ?: 0
            allSubItems
        } else {
            subitemList
        }

        val mViewPagerAdapter = ImageSliderAdapter(context, displayList)
        viewPager.adapter = mViewPagerAdapter
        viewPager.setCurrentItem(currentPosition, false)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (isFromAll) {
                    val dbHelper = DBHelper(context)
                    val watchNameForSubItem = dbHelper.getWatchNameForSubcategoryId(displayList[position].subitemId)
                    nameTextView.text = watchNameForSubItem ?: watchName
                }
            }
        })

        dialog.show()
    }
}
