package com.cogniter.watchaccuracychecker.adapter

import android.content.Context
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.database.entity.WatchEntity
import com.cogniter.watchaccuracychecker.databinding.MywacthesAdapterBinding

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

                if (old.title != new.title) bundle.putBoolean(PAYLOAD_TITLE, true)
                if (old.elapsedTimeMillis != new.elapsedTimeMillis) bundle.putBoolean(PAYLOAD_TIMER, true)
                if (old.isWatchRunning != new.isWatchRunning) bundle.putBoolean(PAYLOAD_RUNNING, true)

                return if (bundle.isEmpty) null else bundle
            }
        }
    }

    inner class NameViewHolder(
        private val binding: MywacthesAdapterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WatchEntity) = with(binding) {

            nameTextView.text = item.title
            watchTimeText.text = item.addedWatchTime
            tvTimerrrr.text = formatTime(item.elapsedTimeMillis)

            historyBtn.setText("History (" + item.historyCount + ")")

            updateRunningUI(item)

            if (item.watchImage.isNotEmpty()) {
                Glide.with(context)
                    .load(Uri.parse(item.watchImage))
                    .into(watchimage)
            } else {
                watchimage.setImageResource(R.drawable.app_icon)
            }

            watchimage.setOnClickListener { listener?.onImageClick(item, 0) }
            editBtn.setOnClickListener {

                if(item.isWatchRunning){
                    Toast.makeText(context,"Please stop tracking before edit the watch.", Toast.LENGTH_SHORT).show()
                }else {
                    listener?.onImageClick(item, 1)
                }
            }
            deleteBtn.setOnClickListener {
                if(item.isWatchRunning){
                    Toast.makeText(context,"Please stop tracking before deleting the watch.", Toast.LENGTH_SHORT).show()
                }else {
                    listener?.onImageClick(item, 2)
                }

            }
            trackBtn.setOnClickListener { listener?.onImageClick(item, 3) }
            historyBtn.setOnClickListener { listener?.onImageClick(item, 4) }
        }

        fun updateTitle(item: WatchEntity) {
            binding.nameTextView.text = item.title
        }

        fun updateTimer(item: WatchEntity) {
            binding.tvTimerrrr.text = formatTime(item.elapsedTimeMillis)
        }

        fun updateRunningUI(item: WatchEntity) = with(binding) {

//            trackingprogress.visibility =
//                if (item.isWatchRunning) View.VISIBLE else View.GONE

            timerLay.visibility =
                if (item.isWatchRunning) View.VISIBLE else View.GONE

            if (item.isWatchRunning) {
                trackBtn.text = "Tracking"
                trackBtn.setBackgroundResource(R.drawable.tracking_background)

                editBtn.isClickable = false
                editBtn.isEnabled = false
                editBtn.setColorFilter(
                    ContextCompat.getColor(context, R.color.grey),
                    PorterDuff.Mode.SRC_IN
                )

                deleteBtn.isClickable = false
                deleteBtn.isEnabled = false
                deleteBtn.setColorFilter(
                    ContextCompat.getColor(context, R.color.grey),
                    PorterDuff.Mode.SRC_IN
                )

            } else {
                trackBtn.text = "Track"
                trackBtn.setBackgroundResource(R.drawable.track_button_background)

                editBtn.isClickable = true
                editBtn.isEnabled = true
                editBtn.setColorFilter(
                    ContextCompat.getColor(context, R.color.white),
                    PorterDuff.Mode.SRC_IN
                )

                deleteBtn.isClickable = true
                deleteBtn.isEnabled = true
                deleteBtn.setColorFilter(
                    ContextCompat.getColor(context, R.color.white),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameViewHolder {
        val binding = MywacthesAdapterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NameViewHolder(binding)
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

        if (bundle.getBoolean(PAYLOAD_TITLE)) holder.updateTitle(item)
        if (bundle.getBoolean(PAYLOAD_TIMER)) holder.updateTimer(item)
        if (bundle.getBoolean(PAYLOAD_RUNNING)) holder.updateRunningUI(item)
    }

    private fun formatTime(ms: Long): String {
        val sec = (ms / 1000) % 60
        val min = (ms / (1000 * 60)) % 60
        val hr = ms / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hr, min, sec)
    }
}
