package com.cogniter.watchaccuracychecker.activity.UI

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.CustomCameraActivity
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.adapter.MyWatchesAdapter
import com.cogniter.watchaccuracychecker.database.AppDatabase
import com.cogniter.watchaccuracychecker.database.entity.WatchEntity
import com.cogniter.watchaccuracychecker.databinding.MywatchlistingBinding
import com.cogniter.watchaccuracychecker.repository.WatchRepository
import com.cogniter.watchaccuracychecker.service.TimerService
import com.cogniter.watchaccuracychecker.viewmodel.WatchViewModel
import com.cogniter.watchaccuracychecker.viewmodel.WatchViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog

class MywatchListing : Fragment(), MyWatchesAdapter.OnImageClickListener {


    private var shouldReloadRecycler = false
    private val handler = Handler(Looper.getMainLooper())

    private var _binding: MywatchlistingBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityRef: Activity


    private lateinit var watchViewModel: WatchViewModel
    private lateinit var adapter: MyWatchesAdapter
    private var imageUri: Uri? = null
    private var insertWatchImage: ImageView? = null

    // Camera launcher for CustomCameraActivity
    private lateinit var cameraLauncher: androidx.activity.result.ActivityResultLauncher<Intent>


    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getIntExtra(TimerService.EXTRA_TIMER_ID, -1) ?: return
            val elapsed = intent.getLongExtra(TimerService.EXTRA_TIMER_VALUE, 0L)
            val isRunning = intent.getBooleanExtra(TimerService.IS_RUNNING, false)

            watchViewModel.updateElapsedTime(id, elapsed, isRunning)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = MywatchlistingBinding.inflate(inflater, container, false)

        val db = AppDatabase.getDatabase(requireContext())
        val repository = WatchRepository(db.watchDao())

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(
                timerReceiver,
                IntentFilter(TimerService.ACTION_TIMER_UPDATE)
            )

        watchViewModel = ViewModelProvider(
            this,
            WatchViewModelFactory(repository)
        )[WatchViewModel::class.java]

        activityRef = requireActivity()

        (activityRef as MainActivity).apply {
            findViewById<TextView>(R.id.nameTextView)?.text = "MY WATCHES"
            findViewById<ImageView>(R.id.backButton)?.visibility = View.GONE
        }

        setupCameraLauncher()
        setupRecycler()
        observeData()
        setupClicks()
        startRecyclerReloadRecursively()



        return binding.root
    }


    fun startRecyclerReloadRecursively() {
        shouldReloadRecycler = true
        reloadRecyclerRecursively()
    }


    fun stopRecyclerReload() {
        shouldReloadRecycler = false
        handler.removeCallbacksAndMessages(null) // prevent memory leaks
    }


    private fun reloadRecyclerRecursively() {
        if (!shouldReloadRecycler) return

        handler.postDelayed({
            if (!shouldReloadRecycler) return@postDelayed

            adapter?.notifyDataSetChanged()
            reloadRecyclerRecursively() // 🔁 recursion
        }, 1000) // 1 second delay
    }


    // ---------------- CAMERA LAUNCHER ----------------
    private fun setupCameraLauncher() {
        cameraLauncher =
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    imageUri = result.data?.data
                    insertWatchImage?.let {
                        Glide.with(this)
                            .load(imageUri)
                            .into(it)
                    }
                }
            }
    }

    private fun openCustomCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCustomCameraInternal()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }


    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCustomCameraInternal()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required to continue",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun openCustomCameraInternal() {
        val intent = Intent(requireContext(), CustomCameraActivity::class.java)
        cameraLauncher.launch(intent)
    }


    // ---------------- RECYCLER ----------------
    private fun setupRecycler() {
        adapter = MyWatchesAdapter(requireContext())
        adapter.setOnImageClickListener(this)

        binding.nameList.layoutManager = LinearLayoutManager(requireContext())
        binding.nameList.adapter = adapter
    }

    private fun observeData() {
        watchViewModel.allWatches.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.headervalue.visibility =
                if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // ---------------- CLICKS ----------------
    private fun setupClicks() {
        binding.addAWatchBtn.setOnClickListener { showAddDialog() }
        binding.addButton.setOnClickListener { showAddDialog() }
    }

    private fun showAddDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_add_name_time, null)
        dialog.setContentView(view)

        val nameInput = view.findViewById<EditText>(R.id.dialog_name_input)
        insertWatchImage = view.findViewById(R.id.insertwatchimage)

        insertWatchImage!!.setOnClickListener {
            openCustomCamera()
        }

        view.findViewById<TextView>(R.id.addWatchBtn).setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Enter watch name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val date = java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date())

            watchViewModel.insertWatch(
                WatchEntity(
                    title = name,
                    watchImage = imageUri?.toString() ?: "",
                    addedWatchTime = date
                )
            )

            imageUri = null
            dialog.dismiss()
        }

        view.findViewById<TextView>(R.id.cancelWatchBtn)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onImageClick(item: WatchEntity, action: Int) {
        when (action) {
            0 -> {
                // Click on watch image → Open Watch Detail Fragment
                (activity as? MainActivity)?.openFragmentWithBudelData(
                    item.id,
                    item.title,
                    WatchDetailFragment(),
                    "WatchDetail",
                    item.isWatchRunning
                )
            }
            1 -> {
                // Edit watch → Show update dialog
                showUpdateDialog(item)
            }
            2 -> {
                // Delete watch
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Watch")
                    .setMessage("Are you sure you want to delete this watch?")
                    .setPositiveButton("Yes") { _, _ ->
                        watchViewModel.deleteWatch(item.id)
                        Toast.makeText(requireContext(), "Deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            3 -> {
                // Track watch → Open ClockFragment or tracking activity
                (activity as? MainActivity)?.openFragmentWithBudelData(
                    item.id,
                    item.title,
                    ClockFragment(),
                    "ClockActivity",
                    item.isWatchRunning
                )
            }
            4 -> {
                // History → Open WatchDetailFragment showing history
                (activity as? MainActivity)?.openFragmentWithBudelData(
                    item.id,
                    item.title,
                    WatchDetailFragment(),
                    "WatchHistory",
                    item.isWatchRunning
                )
            }
        }
    }

    // ---------------- Update Watch Dialog ----------------
    private fun showUpdateDialog(item: WatchEntity) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_add_name_time, null)
        dialog.setContentView(view)

        val nameInput = view.findViewById<EditText>(R.id.dialog_name_input)
        insertWatchImage = view.findViewById<ImageView>(R.id.insertwatchimage)

        nameInput.setText(item.title)

        if (item.watchImage.isNotEmpty()) {
            imageUri = Uri.parse(item.watchImage)
            Glide.with(this).load(imageUri).into(insertWatchImage!!)
        }

        insertWatchImage?.setOnClickListener {
            openCustomCamera()
        }

        view.findViewById<TextView>(R.id.addWatchBtn).apply {
            text = "Update"
            setOnClickListener {
                val updatedName = nameInput.text.toString().trim()
                if (updatedName.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter watch name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                watchViewModel.updateWatch(
                    item.copy(
                        title = updatedName,
                        watchImage = imageUri?.toString() ?: item.watchImage
                    )
                )

                imageUri = null
                dialog.dismiss()
            }
        }

        view.findViewById<TextView>(R.id.cancelWatchBtn)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

}
