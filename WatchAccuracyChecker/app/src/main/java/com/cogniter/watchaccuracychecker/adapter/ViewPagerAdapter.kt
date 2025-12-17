package com.cogniter.watchaccuracychecker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

import com.cogniter.watchaccuracychecker.R

import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.cogniter.watchaccuracychecker.model.Subitem
import com.cogniter.watchaccuracychecker.utills.ImageUtils
import com.github.chrisbanes.photoview.PhotoView


class ImageSliderAdapter(private val mContext: Context,private val  itemList: List<Subitem>) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_pageradapter, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {


        System.out.println(position.toString()+ "   dlldoodkosubitemList   "+itemList.size)

        val imageUri = ImageUtils.getImageUriFromName(mContext, itemList[position].image)
        //holder.fullscreenImage.setImageURI(imageUri) // Set the image resource
        Glide.with(mContext)
            .load(imageUri)
            .into(  holder.fullscreenImage)
//        holder.titleText.text = itemList[position].name  // Set the title
//        holder.titleDateTime.text=itemList[position].date

        holder.titleDateTime.text ="Date & Time: "+itemList[position].date
        holder.titleText.text = "Elapsed time: "+itemList[position].name

    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fullscreenImage: PhotoView = itemView.findViewById(R.id.fullscreenImage)
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val titleDateTime :TextView = itemView.findViewById(R.id.titleTime)

    }
}
