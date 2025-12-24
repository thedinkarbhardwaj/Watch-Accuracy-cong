package com.cogniter.watchaccuracychecker.activity.UI

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.CustomCameraActivity
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.adapter.MyWatchesAdapter
import com.cogniter.watchaccuracychecker.database.DBHelper
import com.cogniter.watchaccuracychecker.databinding.MywatchlistingBinding
import com.cogniter.watchaccuracychecker.model.ListItem
import com.cogniter.watchaccuracychecker.model.Subitem
import com.cogniter.watchaccuracychecker.utills.GlobalVariables.COMMON_ID
import com.cogniter.watchaccuracychecker.utills.ImageUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MywatchListing : Fragment(), MyWatchesAdapter.OnImageClickListener {

    private var _binding: MywatchlistingBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>


    private lateinit var activityRef: Activity
    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: MyWatchesAdapter

    private var itemList: List<ListItem> = emptyList()
    private var imageUri: Uri? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var headerValue: RelativeLayout
    private var insertWatchImage: ImageView? = null

    // Camera launchers
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    // ---------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = MywatchlistingBinding.inflate(inflater, container, false)
        activityRef = requireActivity()
        dbHelper = DBHelper(activityRef)

        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    imageUri = result.data?.data
                    Glide.with(this)
                        .load(imageUri)
                        .into(insertWatchImage!!)
                }
            }


        setupActivityUI()
        setupCameraLaunchers()
        setupRecycler()
        setupClicks()

        return binding.root
    }

    // ---------------- CAMERA ----------------

    private fun setupCameraLaunchers() {

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && imageUri != null) {
                    Glide.with(this)
                        .load(imageUri)
                        .into(insertWatchImage!!)
                } else {
                    imageUri = null
                }
            }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) openCamera()
                else Toast.makeText(activityRef, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
//            openCameraInternal()

            openCamera2()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCameraInternal() {

        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val storageDir =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return

        val imageFile = File.createTempFile(
            "JPEG_$timeStamp",
            ".jpg",
            storageDir
        )

        val uri = FileProvider.getUriForFile(
            requireContext(),
            "com.cogniter.watchaccuracychecker.fileprovider",
            imageFile
        )

        imageUri = uri        // store for later use
        takePictureLauncher.launch(uri)  // ✅ non-null
    }


    private fun openCamera2() {
        val intent = Intent(requireContext(), CustomCameraActivity::class.java)
        cameraLauncher.launch(intent)
    }


    // ---------------- UI ----------------

    private fun setupActivityUI() {
        (activityRef as? MainActivity)?.apply {
            findViewById<TextView>(R.id.nameTextView)?.text = "MY WATCHES"
            findViewById<ImageView>(R.id.backButton)?.visibility = View.GONE
            findViewById<LinearLayout>(R.id.bottomNav)?.visibility = View.VISIBLE
        }
    }

    private fun setupRecycler() {

        recyclerView = binding.nameList
        headerValue = binding.headervalue

        itemList = dbHelper.getAllItems().reversed()
        toggleHeader()

        adapter = MyWatchesAdapter(itemList, activityRef)
        adapter.setOnImageClickListener(this)

        recyclerView.layoutManager = LinearLayoutManager(activityRef)
        recyclerView.adapter = adapter
    }

    private fun refreshList() {
        itemList = dbHelper.getAllItems().reversed()
        toggleHeader()

        adapter = MyWatchesAdapter(itemList, activityRef)
        adapter.setOnImageClickListener(this)
        recyclerView.adapter = adapter
    }

    private fun toggleHeader() {
        headerValue.visibility =
            if (itemList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupClicks() {
        binding.addAWatchBtn.setOnClickListener { showAddDialog() }
        binding.addButton.setOnClickListener { showAddDialog() }
    }

    // ---------------- DIALOG ----------------

    private fun showAddDialog() {

        val dialog = BottomSheetDialog(activityRef)
        val view = layoutInflater.inflate(R.layout.dialog_add_name_time, null)
        dialog.setContentView(view)

        val nameInput = view.findViewById<EditText>(R.id.dialog_name_input)
        insertWatchImage = view.findViewById(R.id.insertwatchimage)

        insertWatchImage!!.setOnClickListener { openCamera() }

        view.findViewById<TextView>(R.id.addWatchBtn).setOnClickListener {

            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(activityRef, "Enter watch name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val date =
                SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

            val subItems = listOf(Subitem(Random().nextLong(), "sub", "img", "date"))

            dbHelper.addItem(
                name,
                imageUri?.toString() ?: "",
                date,
                false,
                subItems
            )

            imageUri = null
            refreshList()
            dialog.dismiss()
        }

        view.findViewById<TextView>(R.id.cancelWatchBtn)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // ---------------- ADAPTER CALLBACK ----------------

    override fun onImageClick(item: ListItem, pos: Int) {
        when (pos) {
            0 -> (activityRef as MainActivity).openFragmentWithBudelData(
                item.id, item.title, WatchDetailFragment(), "WatchDetail", item.isrunning
            )
            1 -> updateDialog(item)
            2 -> deleteDialog(item)
            else -> {
                COMMON_ID = item.id!!.toInt()
                (activityRef as MainActivity).openFragmentWithBudelData(
                    item.id, item.title, ClockFragment(), "ClockActivity", item.isrunning
                )
            }
        }
    }

    private fun updateDialog(item: ListItem) {

        val dialog = BottomSheetDialog(activityRef)
        val view = layoutInflater.inflate(R.layout.dialog_add_name_time, null)
        dialog.setContentView(view)

        insertWatchImage = view.findViewById(R.id.insertwatchimage)
        val nameInput = view.findViewById<EditText>(R.id.dialog_name_input)
        nameInput.text = Editable.Factory.getInstance().newEditable(item.title)

        if (item.watchimage.isNotEmpty()) {
            imageUri = Uri.parse(item.watchimage)
            Glide.with(this).load(imageUri).into(insertWatchImage!!)
        }

        view.findViewById<TextView>(R.id.addWatchBtn).apply {
            text = "Update"
            setOnClickListener {
                val name = nameInput.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(activityRef, "Enter watch name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                dbHelper.updateItemTitle(
                    item.id,
                    nameInput.text.toString(),
                    imageUri?.toString() ?: ""
                )
                imageUri = null
                refreshList()
                dialog.dismiss()
            }
        }

        view.findViewById<TextView>(R.id.cancelWatchBtn)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun deleteDialog(item: ListItem) {

        val dialog = AlertDialog.Builder(activityRef)
            .setTitle("Delete")
            .setMessage("Delete ${item.title}?")
            .setPositiveButton("Yes") { _, _ ->
                dbHelper.deleteItem(item.id)
                refreshList()
            }
            .setNegativeButton("No", null)
            .create()

        dialog.show()

        val color = if (ImageUtils.isDarkModeEnabled(activityRef))
            ContextCompat.getColor(activityRef, R.color.white)
        else
            ContextCompat.getColor(activityRef, R.color.black)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
