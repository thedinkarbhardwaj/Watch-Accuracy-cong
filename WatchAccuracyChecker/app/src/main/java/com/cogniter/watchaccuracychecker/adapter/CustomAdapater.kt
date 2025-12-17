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
import androidx.viewpager.widget.ViewPager
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

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val elapsedTime: TextView = itemView.findViewById(R.id.elapsedTime)
        val historyTime: TextView = itemView.findViewById(R.id.historyTime)
        val deleteHistoryBtn: RelativeLayout = itemView.findViewById(R.id.deleteHistoryBtn)
    }

    // Define the interface for click events
    interface OnDeleteClickListener {
        fun OnDeleteClickListener(name: Subitem, i: Int)
    }
    // Add a property to hold the listener
    private var deleteClickListener: OnDeleteClickListener? = null

    // Method to set the listener from the activity
    fun setOnDeleteClickListener(listener: OnDeleteClickListener) {
        deleteClickListener = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {


        val imageUri = ImageUtils.getImageUriFromName(context, subitemList[position].image)
        if (imageUri != null) {
            // Use Glide to load the image into the ImageView
            Glide.with(context)
                .load(imageUri)
                .into(holder.imageView)
        } else {
            // Handle the case where the image is not found
        }
        holder.deleteHistoryBtn.setOnClickListener {
           deleteClickListener!!.OnDeleteClickListener(subitemList.get(position),position)
        }
     holder.itemView.setOnClickListener {
         showFullScreenPopup(position,subitemList[position].subitemId)

     }
        holder.historyTime.text ="Date & Time: "+subitemList[position].date
        holder.elapsedTime.text = "Elapsed time: "+subitemList[position].name
    }
    override fun getItemCount() = subitemList.size



    private fun showFullScreenPopup(position: Int, subitemId: Long) {
        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.fullscreen_popup)


        val viewPager = dialog.findViewById<ViewPager2>(R.id.viewPager)
        val nameTextView = dialog.findViewById<TextView>(R.id.nameTextView)
        nameTextView.text=watchName
        val backButtonPopup = dialog.findViewById<ImageView>(R.id.backButtonPopup)
        backButtonPopup.setOnClickListener {
            dialog.dismiss()
        }
        var mViewPagerAdapter: ImageSliderAdapter? =null

        var pos =0

        pos =position;


        if(isFromAll){
            //  var itemList: List<ListItem>? = null
            var  dbHelper = DBHelper(context)
            var itemList = dbHelper.getAllItems()
            itemList= itemList!!.reversed()

            val subitemList = mutableListOf<Subitem>() // Initialize the subitemList outside the loop
            subitemList.isEmpty()
            itemList?.forEach { listItem ->
                var subItemsForItem = dbHelper.getSubItemsForItem(listItem.id!!)
                // subItemsForItem.reversed()
                subitemList.addAll(subItemsForItem.reversed())


            }
            subitemList.removeAll { subItem -> subItem.name == "subitem1" }

            subitemList?.forEachIndexed { index, listItem ->
                if (listItem.subitemId == subitemId) {
                    pos=index;
                }
            }
            mViewPagerAdapter = ImageSliderAdapter(context  , subitemList)
            viewPager.adapter = mViewPagerAdapter
            viewPager.setCurrentItem(pos,false)

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    // This method will be invoked when the current page is scrolled
                }

                override fun onPageSelected(position: Int) {
               System.out.println("doklodkodk   "+dbHelper.getWatchNameForSubcategoryId(subitemList.get(position).subitemId))
                    nameTextView.text = dbHelper.getWatchNameForSubcategoryId(subitemList.get(position).subitemId)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    // This method will be invoked when the scroll state changes (scrolling started, ended, or idle)
                }
            })
        }else{
            mViewPagerAdapter = ImageSliderAdapter(context  , subitemList)
            viewPager.adapter = mViewPagerAdapter
            viewPager.setCurrentItem(pos,false)
        }

        dialog.show()
    }

}
