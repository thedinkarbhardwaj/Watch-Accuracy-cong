package com.cogniter.watchaccuracychecker.adapter

import android.content.Context
import android.net.Uri
import android.os.Bundle
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

        private const val PAYLOAD_TITLE = "PAYLOAD_TITLE"
        private const val PAYLOAD_TIMER = "PAYLOAD_TIMER"
        private const val PAYLOAD_RUNNING = "PAYLOAD_RUNNING"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WatchEntity>() {

            override fun areItemsTheSame(oldItem: WatchEntity, newItem: WatchEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(old: WatchEntity, new: WatchEntity): Boolean {
                return old.title == new.title &&
                        old.elapsedTimeMillis == new.elapsedTimeMillis &&
                        old.isWatchRunning == new.isWatchRunning &&
                        old.watchImage == new.watchImage &&
                        old.addedWatchTime == new.addedWatchTime
            }

            override fun getChangePayload(old: WatchEntity, new: WatchEntity): Any? {
                val bundle = Bundle()

                if (old.title != new.title) {
                    bundle.putBoolean(PAYLOAD_TITLE, true)
                }

                if (old.elapsedTimeMillis != new.elapsedTimeMillis) {
                    bundle.putBoolean(PAYLOAD_TIMER, true)
                }

                if (old.isWatchRunning != new.isWatchRunning) {
                    bundle.putBoolean(PAYLOAD_RUNNING, true)
                }

                return if (bundle.isEmpty) null else bundle
            }
        }
    }

    inner class NameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvTimer: TextView = itemView.findViewById(R.id.tvTimerrrr)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val watchTimeText: TextView = itemView.findViewById(R.id.watchTimeText)
        val watchImage: ImageView = itemView.findViewById(R.id.watchimage)
        val deleteBtn: RelativeLayout = itemView.findViewById(R.id.deleteBtn)
        val editBtn: RelativeLayout = itemView.findViewById(R.id.editBtn)
        val historyBtn: TextView = itemView.findViewById(R.id.historyBtn)
        val trackBtn: TextView = itemView.findViewById(R.id.trackBtn)
        val trackingProgress: View = itemView.findViewById(R.id.trackingprogress)

        fun bind(item: WatchEntity) {
            nameTextView.text = item.title
            watchTimeText.text = item.addedWatchTime
            tvTimer.text = formatTime(item.elapsedTimeMillis)

            updateRunningUI(item)

            if (item.watchImage.isNotEmpty()) {
                Glide.with(context)
                    .load(Uri.parse(item.watchImage))
                    .into(watchImage)
            } else {
                watchImage.setImageResource(R.drawable.app_icon)
            }

            watchImage.setOnClickListener { listener?.onImageClick(item, 0) }
            editBtn.setOnClickListener { listener?.onImageClick(item, 1) }
            deleteBtn.setOnClickListener { listener?.onImageClick(item, 2) }
            trackBtn.setOnClickListener { listener?.onImageClick(item, 3) }
            historyBtn.setOnClickListener { listener?.onImageClick(item, 4) }
        }

        fun updateTitle(item: WatchEntity) {
            nameTextView.text = item.title
        }

        fun updateTimer(item: WatchEntity) {
            tvTimer.text = formatTime(item.elapsedTimeMillis)
        }

        fun updateRunningUI(item: WatchEntity) {
            trackingProgress.visibility =
                if (item.isWatchRunning) View.VISIBLE else View.GONE

            tvTimer.visibility =
                if (item.isWatchRunning) View.VISIBLE else View.GONE

            if (item.isWatchRunning) {
                trackBtn.text = "Tracking"
                trackBtn.setBackgroundResource(R.drawable.tracking_background)
            } else {
                trackBtn.text = "Track"
                trackBtn.setBackgroundResource(R.drawable.track_button_background)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.mywacthes_adapter, parent, false)
        return NameViewHolder(view)
    }

    override fun onBindViewHolder(holder: NameViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: NameViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        val item = getItem(position)
        val bundle = payloads[0] as Bundle

        if (bundle.getBoolean(PAYLOAD_TITLE)) {
            holder.updateTitle(item)
        }

        if (bundle.getBoolean(PAYLOAD_TIMER)) {
            holder.updateTimer(item)
        }

        if (bundle.getBoolean(PAYLOAD_RUNNING)) {
            holder.updateRunningUI(item)
        }
    }

    private fun formatTime(ms: Long): String {
        val sec = (ms / 1000) % 60
        val min = (ms / (1000 * 60)) % 60
        val hr = ms / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hr, min, sec)
    }
}
