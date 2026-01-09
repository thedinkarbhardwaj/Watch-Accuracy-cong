package com.cogniter.watchaccuracychecker.adapter

import android.content.Context
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        private const val PAYLOAD_TIMER = "PAYLOAD_TIMER"
        private const val PAYLOAD_RUNNING = "PAYLOAD_RUNNING"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WatchEntity>() {
            override fun areItemsTheSame(oldItem: WatchEntity, newItem: WatchEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(old: WatchEntity, new: WatchEntity): Boolean {
                return old == new
            }

            override fun getChangePayload(old: WatchEntity, new: WatchEntity): Any? {
                val bundle = Bundle()
                if (old.elapsedTimeMillis != new.elapsedTimeMillis) {
                    bundle.putLong(PAYLOAD_TIMER, new.elapsedTimeMillis)
                }
                if (old.isWatchRunning != new.isWatchRunning) {
                    bundle.putBoolean(PAYLOAD_RUNNING, new.isWatchRunning)
                }
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
            historyBtn.text = "History (${item.historyCount})"

            updateRunningUI(item)

            if (item.watchImage.isNotEmpty()) {
                Glide.with(context)
                    .load(Uri.parse(item.watchImage))
                    .placeholder(R.drawable.app_icon)
                    .into(watchimage)
            } else {
                watchimage.setImageResource(R.drawable.app_icon)
            }

            watchimage.setOnClickListener { listener?.onImageClick(item, 0) }
            editBtn.setOnClickListener {
                if (item.isWatchRunning) {
                    Toast.makeText(context, "Please stop tracking before editing.", Toast.LENGTH_SHORT).show()
                } else {
                    listener?.onImageClick(item, 1)
                }
            }
            deleteBtn.setOnClickListener {
                if (item.isWatchRunning) {
                    Toast.makeText(context, "Please stop tracking before deleting.", Toast.LENGTH_SHORT).show()
                } else {
                    listener?.onImageClick(item, 2)
                }
            }
            trackBtn.setOnClickListener { listener?.onImageClick(item, 3) }
            historyBtn.setOnClickListener { listener?.onImageClick(item, 4) }
        }

        fun updateTimer(elapsedMillis: Long) {
            binding.tvTimerrrr.text = formatTime(elapsedMillis)
        }

        fun updateRunningUI(item: WatchEntity) = with(binding) {
            timerLay.visibility = if (item.isWatchRunning) View.VISIBLE else View.GONE

            if (item.isWatchRunning) {
                binding.tvTimerrrr.setText(timeDifference(item.beginTime) )

                trackBtn.text = "Tracking"
                trackBtn.setBackgroundResource(R.drawable.tracking_background)
                editBtn.isEnabled = false
                editBtn.setColorFilter(ContextCompat.getColor(context, R.color.grey), PorterDuff.Mode.SRC_IN)
                deleteBtn.isEnabled = false
                deleteBtn.setColorFilter(ContextCompat.getColor(context, R.color.grey), PorterDuff.Mode.SRC_IN)
            } else {
                trackBtn.text = "Track"
                trackBtn.setBackgroundResource(R.drawable.track_button_background)
                editBtn.isEnabled = true
                editBtn.setColorFilter(ContextCompat.getColor(context, R.color.white), PorterDuff.Mode.SRC_IN)
                deleteBtn.isEnabled = true
                deleteBtn.setColorFilter(ContextCompat.getColor(context, R.color.white), PorterDuff.Mode.SRC_IN)
            }
        }
    }

    fun timeDifference(
        beginDateStr: String?,
        formatter: SimpleDateFormat = SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        )
    ): String {

        if (beginDateStr.isNullOrEmpty()) return "00:00:00"

        val beginDate = try {
            formatter.parse(beginDateStr)
        } catch (e: Exception) {
            Log.e("TimeDiff", "Invalid begin date string", e)
            return "00:00:00"
        }

        val endDate = Date()
        val totalSeconds = ((endDate.time - beginDate.time) / 1000).coerceAtLeast(0)

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds / 60) % 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameViewHolder {
        val binding = MywacthesAdapterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NameViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: NameViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }

//        for (payload in payloads) {
//            if (payload is Bundle) {
//                if (payload.containsKey(PAYLOAD_TIMER)) {
//                    val elapsed = payload.getLong(PAYLOAD_TIMER)
//                    holder.updateTimer(elapsed)
//                }
//                if (payload.containsKey(PAYLOAD_RUNNING)) {
//                    val isRunning = payload.getBoolean(PAYLOAD_RUNNING)
//                    val item = getItem(position).copy(isWatchRunning = isRunning)
//                    holder.updateRunningUI(item)
//                }
//            }
//        }
    }

    private fun formatTime(ms: Long): String {
        val sec = (ms / 1000) % 60
        val min = (ms / (1000 * 60)) % 60
        val hr = ms / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hr, min, sec)
    }
}