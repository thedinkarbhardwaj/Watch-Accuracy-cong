package com.cogniter.watchaccuracychecker.activity.UI

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.adapter.AlHistoryAdapter
import com.cogniter.watchaccuracychecker.database.AppDatabase
import com.cogniter.watchaccuracychecker.database.entity.SubItemEntity
import com.cogniter.watchaccuracychecker.database.entity.WatchWithSubItems
import com.cogniter.watchaccuracychecker.utills.ImageUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class AllHistoryFragment : Fragment(), AlHistoryAdapter.OnAllHistoryDeleteClickListener {

    private lateinit var activityRef: Activity
    private var watchList: List<WatchWithSubItems> = emptyList()
    private var adapter: AlHistoryAdapter? = null
    private var recyclerView: RecyclerView? = null
    private lateinit var database: AppDatabase

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadWatchHistoryFromRoom()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Storage permission is required to load history",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.qll_history_detail_activity, container, false)
        activityRef = requireActivity()
        database = AppDatabase.getDatabase(activityRef)

        // Hide back button and show bottom nav
        (activityRef as? MainActivity)?.findViewById<ImageView>(R.id.backButton)?.visibility =
            View.GONE
        (activityRef as? MainActivity)?.findViewById<LinearLayout>(R.id.bottomNav)?.visibility =
            View.VISIBLE

        setupRecyclerView(view)
//        loadWatchHistoryFromRoom()

        checkStoragePermissionAndLoad()


        return view
    }

    private fun checkStoragePermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadWatchHistoryFromRoom()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                showStoragePermissionDialog(permission)
            }

            else -> {
                storagePermissionLauncher.launch(permission)
            }
        }
    }

    private fun showStoragePermissionDialog(permission: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Storage Permission Required")
            .setMessage("We need storage access to load watch history images.")
            .setCancelable(false)
            .setPositiveButton("Allow") { _, _ ->
                storagePermissionLauncher.launch(permission)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.imageRecyclerView)
        adapter = AlHistoryAdapter(activityRef, watchList)
        recyclerView?.layoutManager = LinearLayoutManager(activityRef)
        recyclerView?.adapter = adapter
        adapter?.setOnAllHistoryDeleteClickListener(this)
    }

    private fun loadWatchHistoryFromRoom() {
        // Using coroutine to collect Flow from Room
//        lifecycleScope.launch {
//            database.watchDao().getWatchesWithSubItems().collect { watches ->
//            database.watchDao().getOnlyWatchesWithHistoryOnce().collect { watches ->
//                watchList = watches.reversed() // Latest first
//                adapter?.updateList(watchList)
//            }


//        }

        lifecycleScope.launch {
            val watches = withContext(Dispatchers.IO) {
                database
                    .watchDao()
                    .getWatchesWithSubItems()
//                    .getOnlyWatchesWithHistory()
            }

            watchList = watches.reversed()
            adapter?.updateList(watchList)
        }
    }

    override fun onAllHistoryDelete(subItem: SubItemEntity) {
        deleteItemDialog(subItem)
    }

    private fun deleteItemDialog(subItem: SubItemEntity) {
        AlertDialog.Builder(activityRef)
            .setTitle("Delete")
            .setMessage("Are you sure to delete the record?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                   // val watchId = database.watchDao().getWatchIdBySubItemId(subItem.id)

                  //  database.watchDao().deleteSubItem(subItem.id)
               //     database.watchDao().decrementHistoryCount(watchId)
                    database.watchDao().deleteHistoryAndUpdateCount(subItem.id)

                    watchList = emptyList()
                    loadWatchHistoryFromRoom()
                   // adapter?.notifyDataSetChanged()


                    Toast.makeText(activityRef, "Record deleted successfully.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()

        if (ImageUtils.isDarkModeEnabled(activityRef)) {
            // Optional: customize dialog button colors for dark mode
        }
    }


}
