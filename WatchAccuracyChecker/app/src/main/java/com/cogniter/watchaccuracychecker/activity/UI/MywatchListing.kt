package com.cogniter.watchaccuracychecker.activity.UI

// MainActivity.kt


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.adapter.MyWatchesAdapter
import com.cogniter.watchaccuracychecker.database.DBHelper
import com.cogniter.watchaccuracychecker.databinding.HelpFragmentBinding
import com.cogniter.watchaccuracychecker.databinding.MywatchlistingBinding
import com.cogniter.watchaccuracychecker.model.ListItem
import com.cogniter.watchaccuracychecker.model.Subitem
import com.cogniter.watchaccuracychecker.service.TimerService
import com.cogniter.watchaccuracychecker.utills.GlobalVariables.COMMON_ID
import com.cogniter.watchaccuracychecker.utills.ImageUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random


class MywatchListing : Fragment(), MyWatchesAdapter.OnImageClickListener {

    private var _binding: MywatchlistingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MyWatchesAdapter
    private lateinit var dbHelper: DBHelper
    var itemList: List<ListItem>? = null
    private val REQUEST_IMAGE_CAPTURE = 1
    private var imageUri: Uri? = null
    var recyclerView: RecyclerView? = null
    var insertwatchimage: ImageView? = null
    var headervalue: RelativeLayout? = null
    var isServiceRunning:Boolean?=false
    // Define a unique notification channel ID

    lateinit var activity: Activity

    companion object {

        private const val REQUEST_CODE_PERMISSIONS = 20
        private lateinit var REQUIRED_PERMISSIONS: Array<String>
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.mywatchlisting, container, false)

        _binding = MywatchlistingBinding.inflate(inflater, container, false)


        activity = getActivity()!!
        dbHelper = DBHelper(activity)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {

            REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {

            REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA,Manifest.permission.POST_NOTIFICATIONS,Manifest.permission.READ_MEDIA_IMAGES)
        }



        (activity as? MainActivity)?.findViewById<TextView>(R.id.nameTextView)?.text = "MY WATCHES"
        (activity as? MainActivity)?.findViewById<ImageView>(R.id.backButton)?.visibility= View.GONE
        (activity as? MainActivity)?.findViewById<LinearLayout>(R.id.bottomNav)?.visibility = View.VISIBLE

        itemList = dbHelper.getAllItems()
        itemList= itemList!!.reversed()
        val addButton = view.findViewById<ImageView>(R.id.add_button)
        recyclerView = view.findViewById<RecyclerView>(R.id.name_list)
        headervalue = view.findViewById<RelativeLayout>(R.id.headervalue)
        if (itemList!!.size==0){
            headervalue!!.visibility= View.VISIBLE
        }else{
            headervalue!!.visibility= View.GONE
        }


        adapter = MyWatchesAdapter(itemList!!, activity)
        recyclerView!!.adapter = adapter
        recyclerView!!.layoutManager = LinearLayoutManager(activity)
        adapter.setOnImageClickListener(this)

        binding.addAWatchBtn.setOnClickListener {

            if (allPermissionsGranted()) {

                imageUri = null
                showAddNameTimeDialog()

            } else {
                permission()
            }

        }



        binding.addButton.setOnClickListener {

            if (allPermissionsGranted()) {
                imageUri = null
                showAddNameTimeDialog()

            } else {
                permission()
            }


        }
        return binding.root


    }

    private  fun permission(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                activity,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            ActivityCompat.requestPermissions(
                activity,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }


    private fun updateNameDialog(item: ListItem) {
        val bottomSheetDialog = BottomSheetDialog(activity)
        val bottomSheetView: View = layoutInflater.inflate(R.layout.dialog_add_name_time, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        val nameTextView = bottomSheetDialog.findViewById<TextView>(R.id.nameTextView)
        val dialogNameInput = bottomSheetDialog.findViewById<EditText>(R.id.dialog_name_input)
        insertwatchimage = bottomSheetDialog.findViewById<ImageView>(R.id.insertwatchimage)
        val addWatchBtn = bottomSheetDialog.findViewById<TextView>(R.id.addWatchBtn)
        val cancelWatchBtn = bottomSheetDialog.findViewById<TextView>(R.id.cancelWatchBtn)
        nameTextView!!.text="Update Watch"
        addWatchBtn!!.text ="Update"
        try {


            if (item.watchimage.isNotEmpty()) {
                Glide.with(this)
                    .load(Uri.parse(item.watchimage))
                    .into(insertwatchimage!!)
                imageUri = Uri.parse(item.watchimage)
            }else{
                Glide.with(this)
                    .load(R.drawable.img_placeholder)
                    .into(insertwatchimage!!)
            }


        } catch (e: Exception) {

        }
        insertwatchimage!!.setOnClickListener { dispatchTakePictureIntent() }
        dialogNameInput!!.text = Editable.Factory.getInstance().newEditable(item.title)
        addWatchBtn!!.setOnClickListener {
            val name = dialogNameInput.text.toString()
            if (name.isNotEmpty()) {
                System.out.println("djijidji  " + item.id + "  " + name)
                if (imageUri == null) {
                    dbHelper.updateItemTitle(item.id, name, "")
                } else {
                    dbHelper.updateItemTitle(item.id, name, imageUri.toString())
                    imageUri = null

                }
                itemList = emptyList()
                itemList = dbHelper.getAllItems()
                itemList= itemList!!.reversed()
                adapter = MyWatchesAdapter(itemList!!, activity)
                recyclerView!!.adapter = adapter
                recyclerView!!.layoutManager = LinearLayoutManager(activity)
                adapter.setOnImageClickListener(this)
                Toast.makeText(activity, "Watch updated...", Toast.LENGTH_LONG).show()
            }
            bottomSheetDialog.dismiss()
        }
        cancelWatchBtn!!.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    private fun showAddNameTimeDialog() {

        val bottomSheetDialog = BottomSheetDialog(activity)
        val bottomSheetView: View = layoutInflater.inflate(R.layout.dialog_add_name_time, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        val dialogNameInput = bottomSheetDialog.findViewById<EditText>(R.id.dialog_name_input)
        insertwatchimage = bottomSheetDialog.findViewById<ImageView>(R.id.insertwatchimage)
        val addWatchBtn = bottomSheetDialog.findViewById<TextView>(R.id.addWatchBtn)
        val cancelWatchBtn = bottomSheetDialog.findViewById<TextView>(R.id.cancelWatchBtn)
        addWatchBtn!!.setOnClickListener {
            val name = dialogNameInput!!.text.toString()



            if (name.isEmpty()) {

                Toast.makeText(activity, "Please enter watch name.", Toast.LENGTH_SHORT).show()
            }else{
                itemList = emptyList()
                itemList = dbHelper.getAllItems()
                itemList?.forEach { listItem ->
                   if(listItem.title.toLowerCase().equals(name.toLowerCase()) ){

                       Toast.makeText(activity, "Duplicate watch name not allowed.", Toast.LENGTH_SHORT).show()
                       return@setOnClickListener
                   }
                }
                headervalue!!.visibility= View.GONE
                System.out.println("ddikidkjij   " + imageUri)
                // Add date string to the subitem
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

                //      dateFormat.dateFormatSymbols = DateFormatSymbols().apply { shortMonths = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec") }
                val currentDate = dateFormat.format(Date())
                val subItems = listOf(Subitem(generateRandomId(), "subitem1", "image1", "date"))
                if (imageUri == null) {
                    dbHelper.addItem(name, "",currentDate,false, subItems)
                } else {
                    dbHelper.addItem(name, imageUri.toString(),currentDate,false, subItems)
                }

                itemList = emptyList()
                itemList = dbHelper.getAllItems()

                itemList= itemList!!.reversed()
                adapter = MyWatchesAdapter(itemList!!, activity)
                recyclerView!!.adapter = adapter
                recyclerView!!.layoutManager = LinearLayoutManager(activity)
                adapter.setOnImageClickListener(this)

                bottomSheetDialog.dismiss()
            }
        }
        cancelWatchBtn!!.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        insertwatchimage!!.setOnClickListener { dispatchTakePictureIntent() }

        bottomSheetDialog.show()
    }

    fun generateRandomId(): Long {
        val random = Random()
        return random.nextLong()
    }

    override fun onImageClick(item: ListItem, pos: Int) {

        if (pos == 0) {
            val activity = requireActivity() as MainActivity
            activity.openFragmentWithBudelData(
                item.id,
                item.title,
                WatchDetailFragment(),
                "WatchDetail",
                item.isrunning
            )
        }
        else if (pos == 1) {
            imageUri = null
           updateNameDialog(item)
        } else if (pos == 2) {
            deleteItemDialog(item)
        } else {
            dbHelper.deleteStringValue()
            dbHelper.deleteLongValue()
            dbHelper.setStringValue(item.title)
            dbHelper.setLongValue(item.id!!)
            COMMON_ID =  item.id!!.toInt()
            System.out.println("  item.isrunningggg  "+item.isrunning)

                  val activity = requireActivity() as MainActivity
                  activity.openFragmentWithBudelData(item.id, item.title, ClockFragment(),"ClockActivity",item.isrunning)

        }

    }


    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(activity.packageManager)?.also {
                val imageFileName = "JPEG_${
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                        Date()
                    )
                }"
                val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

                imageUri = null
                imageUri = FileProvider.getUriForFile(
                    activity,
                    "com.cogniter.watchaccuracychecker.fileprovider",
                    imageFile
                )
                takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", CameraCharacteristics.LENS_FACING_BACK)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            Glide.with(this)
                .load(imageUri)
                .into(insertwatchimage!!)

        } else if (resultCode == Activity.RESULT_CANCELED) {
            imageUri = null
            // Handle the cancellation here
            //   Toast.makeText(this, "Picture taking cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteItemDialog(item: ListItem) {

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete " + item.title + " ?")
            .setPositiveButton("Yes") { _, _ ->
                dbHelper.deleteItem(item.id)
                itemList = emptyList()
                itemList = dbHelper.getAllItems()
                itemList= itemList!!.reversed()

                adapter = MyWatchesAdapter(itemList!!, activity)
                recyclerView!!.adapter = adapter
                recyclerView!!.layoutManager = LinearLayoutManager(activity)
                adapter.setOnImageClickListener(this)
                Toast.makeText(activity, item.title + " deleted successfully.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
        if(ImageUtils.isDarkModeEnabled(activity)){
            val buttonColor = ContextCompat.getColor(activity, R.color.white)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(buttonColor);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(buttonColor);
        }else{
            val buttonColor = ContextCompat.getColor(activity, R.color.black)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(buttonColor);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(buttonColor);
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }

    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (allPermissionsGranted()) {
                showAddNameTimeDialog()
            } else {
                // If permissions are not granted,
                // present a toast to notify the user that
                // the permissions were not granted.
                Toast.makeText(activity, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                activity.finish()
            }
        }
    }

    // Function to create and display a local notification




}
