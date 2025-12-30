package com.cogniter.watchaccuracychecker.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.database.entity.WatchEntity

class MyWatchesAdapter(
    private val context: Context
) : ListAdapter<WatchEntity, MyWatchesAdapter.NameViewHolder>(DIFF_CALLBACK) {

    interface OnImageClickListener {
        fun onImageClick(item: WatchEntity, action: Int)
    }

    private var listener: OnImageClickListener? = null

    fun setOnImageClickListener(l: OnImageClickListener) {
        listener = l
    }

    companion object {
        private val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<WatchEntity>() {
                override fun areItemsTheSame(
                    oldItem: WatchEntity,
                    newItem: WatchEntity
                ): Boolean = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: WatchEntity,
                    newItem: WatchEntity
                ): Boolean = oldItem == newItem
            }
    }

    inner class NameViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvTimer: TextView = itemView.findViewById(R.id.tvTimerrrr)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val watchTimeText: TextView = itemView.findViewById(R.id.watchTimeText)
        val watchImage: ImageView = itemView.findViewById(R.id.watchimage)
        val deleteBtn: RelativeLayout = itemView.findViewById(R.id.deleteBtn)
        val editBtn: RelativeLayout = itemView.findViewById(R.id.editBtn)
        val historyBtn: TextView = itemView.findViewById(R.id.historyBtn)
        val trackBtn: TextView = itemView.findViewById(R.id.trackBtn)
        val trackingProgress: View = itemView.findViewById(R.id.trackingprogress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.mywacthes_adapter, parent, false)
        return NameViewHolder(view)
    }

    override fun onBindViewHolder(holder: NameViewHolder, position: Int) {
        val item = getItem(position)

        holder.nameTextView.text = item.title
        holder.watchTimeText.text = item.addedWatchTime

        holder.trackingProgress.visibility =
            if (item.isWatchRunning) View.VISIBLE else View.GONE

        holder.tvTimer.visibility =
            if (item.isWatchRunning) View.VISIBLE else View.GONE

        if (item.isWatchRunning) {
            holder.trackBtn.setText("Tracking")
            holder.trackBtn.setBackgroundResource(R.drawable.tracking_background)
        }else{
            holder.trackBtn.setText("Track")

            holder.trackBtn.setBackgroundResource(R.drawable.track_button_background)

        }


        if (item.watchImage.isNotEmpty()) {
            Glide.with(context)
                .load(Uri.parse(item.watchImage))
                .into(holder.watchImage)
        } else {
            holder.watchImage.setImageResource(R.drawable.app_icon) // optional
        }

        holder.watchImage.setOnClickListener {
            listener?.onImageClick(item, 0)
        }

        holder.editBtn.setOnClickListener {
            listener?.onImageClick(item, 1)
        }

        holder.deleteBtn.setOnClickListener {
            listener?.onImageClick(item, 2)
        }

        holder.trackBtn.setOnClickListener {
            listener?.onImageClick(item, 3)
        }

        holder.historyBtn.setOnClickListener {
            listener?.onImageClick(item, 4)
        }
    }
}
