package com.cogniter.watchaccuracychecker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.database.entity.SubItemEntity
import com.cogniter.watchaccuracychecker.database.entity.WatchWithSubItems
import com.cogniter.watchaccuracychecker.adapter.CustomAdapater

class AlHistoryAdapter(
    private val context: Context,
    private var watchList: List<WatchWithSubItems>
) : RecyclerView.Adapter<AlHistoryAdapter.WatchViewHolder>(), CustomAdapater.OnDeleteClickListener {

    private var onAllHistoryDeleteClickListener: OnAllHistoryDeleteClickListener? = null

    interface OnAllHistoryDeleteClickListener {
        fun onAllHistoryDelete(subItem: SubItemEntity)
    }

    fun setOnAllHistoryDeleteClickListener(listener: OnAllHistoryDeleteClickListener) {
        onAllHistoryDeleteClickListener = listener
    }

    inner class WatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val watchNameTextView: TextView = itemView.findViewById(R.id.historywatchName)
        val subItemRecyclerView: RecyclerView = itemView.findViewById(R.id.subitemRecylerview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.all_history_adpater_items, parent, false)
        return WatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: WatchViewHolder, position: Int) {
        val currentWatch = watchList[position]
        holder.watchNameTextView.text = currentWatch.watch.title

        // Get subitems from Room entity
        var subItemList: List<SubItemEntity> = currentWatch.subItems ?: emptyList()

        // Remove first placeholder item if needed
        if (subItemList.isNotEmpty()) {
            subItemList = subItemList.drop(1).reversed()
        }

        holder.watchNameTextView.visibility = if (subItemList.isEmpty()) View.GONE else View.VISIBLE

        // Add top margin for items after first
        if (position > 0) {
            val params = holder.watchNameTextView.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(0, 20, 0, 0)
            holder.watchNameTextView.layoutParams = params
        }

        // Convert SubItemEntity to Subitem model for CustomAdapter
        val subItemModelList = subItemList.map { entity ->
            com.cogniter.watchaccuracychecker.model.Subitem(
                subitemId = entity.id ?: 0L,
                name = entity.name ?: "",
                image = entity.image ?: "",
                date = entity.date ?: ""
            )
        }

        // Setup nested RecyclerView for subitems
        val subAdapter = CustomAdapater(true, currentWatch.watch.title, context, subItemModelList)
        holder.subItemRecyclerView.layoutManager = LinearLayoutManager(context)
        holder.subItemRecyclerView.adapter = subAdapter

        // Handle delete click from subAdapter
        subAdapter.setOnDeleteClickListener(object : CustomAdapater.OnDeleteClickListener {
            override fun OnDeleteClickListener(name: com.cogniter.watchaccuracychecker.model.Subitem, i: Int) {
                // Find corresponding SubItemEntity
                val entity = subItemList[i]
                onAllHistoryDeleteClickListener?.onAllHistoryDelete(entity)
            }
        })
    }

    override fun getItemCount(): Int = watchList.size

    fun updateList(newList: List<WatchWithSubItems>) {
        watchList = newList
        notifyDataSetChanged()
    }

    override fun OnDeleteClickListener(name: com.cogniter.watchaccuracychecker.model.Subitem, i: Int) {
        // Not used here; handled in nested adapter
    }
}
