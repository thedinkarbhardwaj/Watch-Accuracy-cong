package com.cogniter.watchaccuracychecker.activity.UI

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.adapter.CustomAdapater
import com.cogniter.watchaccuracychecker.database.AppDatabase
import com.cogniter.watchaccuracychecker.databinding.WatchDetailActivityBinding
import com.cogniter.watchaccuracychecker.model.Subitem
import com.cogniter.watchaccuracychecker.service.TimerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchDetailFragment : Fragment(), CustomAdapater.OnDeleteClickListener
{

    private var _binding: WatchDetailActivityBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityRef: Activity

    private var subitemList: MutableList<Subitem> = mutableListOf()
    private lateinit var adapter: CustomAdapater

    private val database by lazy {
        AppDatabase.getDatabase(requireContext())
    }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadSubItems()
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
        _binding = WatchDetailActivityBinding.inflate(inflater, container, false)
        activityRef = requireActivity()

        setupUI()
        setupRecyclerView()

        setupAddTimerButton()

        checkStoragePermissionAndLoad()

        return binding.root
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
                loadSubItems()
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

    private fun setupUI() {
        (activityRef as MainActivity).findViewById<TextView>(R.id.nameTextView).text =
            "TRACK HISTORY"
        (activityRef as MainActivity).findViewById<ImageView>(R.id.backButton).visibility =
            View.VISIBLE
        (activityRef as MainActivity).findViewById<LinearLayout>(R.id.bottomNav).visibility =
            View.VISIBLE

        binding.watchName.text = arguments?.getString("watchNAME") ?: "Unknown Watch"
    }

    private fun setupRecyclerView() {
        adapter = CustomAdapater(
            false,
            arguments?.getString("watchNAME") ?: "",
            activityRef,
            subitemList
        )
        binding.imageRecyclerView.layoutManager = LinearLayoutManager(activityRef)
        binding.imageRecyclerView.adapter = adapter
        adapter.setOnDeleteClickListener(this)
    }

    private fun loadSubItems() {
        val watchId = arguments?.getLong("itemID") ?: return

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                database.watchDao().getWatchWithSubItemsById(watchId)
            }

            subitemList.clear()

            result?.subItems
                ?.drop(0) // remove first dummy item
                ?.reversed()
                ?.forEach {
                    subitemList.add(
                        Subitem(
                            subitemId = it.id,
                            image = it.image,
                            name = it.name,
                            date = it.date
                        )
                    )
                }

            binding.notrackingheader.visibility =
                if (subitemList.isEmpty()) View.VISIBLE else View.GONE

            adapter.notifyDataSetChanged()
        }
    }

    private fun setupAddTimerButton() {
        binding.addTimer.setOnClickListener {
            val watchId = arguments?.getLong("itemID") ?: return@setOnClickListener
            val watchName = arguments?.getString("watchNAME") ?: ""
            val isRunning = arguments?.getBoolean("isrunning") ?: false

            if (isRunning){
                Toast.makeText(this.requireActivity(),"Your watch is already in tracking mode, so you can’t add a new one at this time.",
                    Toast.LENGTH_SHORT).show()

                return@setOnClickListener
            }


            (activityRef as MainActivity).openFragmentWithBudelData2(
                watchId,
                watchName,
                ClockFragment(),
                "ClockActivity",
                isRunning
            )

        }
    }

    override fun OnDeleteClickListener(subitem: Subitem, position: Int) {
        AlertDialog.Builder(activityRef)
            .setTitle("Delete")
            .setMessage("Are you sure to delete the record?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
//                    database.watchDao().deleteSubItem(subitem.subitemId)
                    database.watchDao().deleteHistoryAndUpdateCount(subitem.subitemId)
                }
                subitemList.removeAt(position)
                adapter.notifyItemRemoved(position)
                Toast.makeText(activityRef, "Deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
