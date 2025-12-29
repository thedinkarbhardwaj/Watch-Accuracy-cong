package com.cogniter.watchaccuracychecker.activity.UI

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.media.ExifInterface
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.AnalogTimerView
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.database.DBHelper
import com.cogniter.watchaccuracychecker.databinding.ClockActivityBinding
import com.cogniter.watchaccuracychecker.model.Subitem
import com.cogniter.watchaccuracychecker.service.TimerService
import com.cogniter.watchaccuracychecker.utills.GlobalVariables.COMMON_ID
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors



class ClockFragment : Fragment(), AnalogTimerView.TimerListener {

    var testElapsedTime = ""
    private lateinit var binding: ClockActivityBinding
    private lateinit var activityRef: Activity
    private lateinit var dbHelper: DBHelper

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: java.util.concurrent.ExecutorService
    private lateinit var outputDirectory: File
    private lateinit var viewFinder: PreviewView

    private var watchName = ""
    private var itemID: Long = 0
    private var elapsedTime = "00:00:00"
    private var imageBitmap: Bitmap? = null
    private var isWatchRunning = false

    private var mediaPlayer: MediaPlayer? = null

    // ----------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = ClockActivityBinding.inflate(inflater, container, false)
        activityRef = requireActivity()
        dbHelper = DBHelper(activityRef)

        (activityRef as MainActivity)
            .findViewById<LinearLayout>(R.id.bottomNav)
            ?.visibility = View.GONE

        LocalBroadcastManager.getInstance(activityRef)
            .registerReceiver(timerReceiver, IntentFilter(TimerService.ACTION_TIMER_UPDATE))

        initCameraSound()
        readArguments()
        setupUI()

        viewFinder = binding.viewFinder
        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = getOutputDirectory()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                activityRef,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {
            if (isWatchRunning) {
                showTrackingWarningDialog()
            } else {
                // Allow normal back navigation
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }

        return binding.root
    }

    private fun showTrackingWarningDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Tracking in progress")
            .setMessage("Tracking is currently running. Do you want to stop it and go back?")
            .setCancelable(false)
            .setPositiveButton("Stop & Exit") { _, _ ->
                stopTimer()
               // requireActivity().supportFragmentManager.popBackStack()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        cameraExecutor.shutdown()
        LocalBroadcastManager.getInstance(activityRef)
            .unregisterReceiver(timerReceiver)
    }

    // ----------------------------------------------------
    // ARGUMENTS & UI
    // ----------------------------------------------------

    private fun readArguments() {
        watchName = arguments?.getString("watchNAME") ?: ""
        itemID = arguments?.getLong("itemID", 0L) ?: 0L
        isWatchRunning = arguments?.getBoolean("isrunning", false) ?: false
        COMMON_ID = itemID.toInt()
    }

    private fun setupUI() {
        (activityRef as MainActivity).apply {
            findViewById<TextView>(R.id.nameTextView)?.text = watchName
            findViewById<ImageView>(R.id.backButton)?.visibility = View.VISIBLE
        }

        binding.myTimer.setTimerListener(this)

        binding.btnStartStop.setOnClickListener {
            if (isWatchRunning) stopTimer() else startTimer()
        }

        binding.btnrstart.setOnClickListener {
            resetTimer()
        }
    }

    private fun initCameraSound() {
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.camera_shutter)
        mediaPlayer?.setVolume(0.5f, 0.5f)
    }

    // ----------------------------------------------------
    // TIMER
    // ----------------------------------------------------

    private fun startTimer() {
        isWatchRunning = true
        binding.btnStart.text = "Stop"
        binding.startStopImg.setImageResource(R.drawable.stop_button)

        dbHelper.updateItemWatchRunning(itemID, true)

        val intent = Intent(activityRef, TimerService::class.java).apply {
            action = TimerService.ACTION_START_TIMER
            putExtra(TimerService.EXTRA_TIMER_ID, itemID.toInt())
        }
        ContextCompat.startForegroundService(activityRef, intent)
    }

    private fun stopTimer() {
        testElapsedTime = binding.elapsedTimeTxt.text.toString()


        isWatchRunning = false
        binding.btnStart.text = "Start"
        binding.startStopImg.setImageResource(R.drawable.start_button_icon)

        dbHelper.updateItemWatchRunning(itemID, false)

        val intent = Intent(activityRef, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_TIMER
            putExtra(TimerService.EXTRA_TIMER_ID, itemID.toInt())
        }
        activityRef.startService(intent)

        imageBitmap = takeScreenshot(binding.myTimer)
        takePhoto()

    }

//    private fun resetTimer() {
//        binding.elapsedTimeTxt.text = "00:00:00"
//    }

    private fun resetTimer() {

        isWatchRunning = false
        binding.btnStart.text = "Start"
        binding.startStopImg.setImageResource(R.drawable.start_button_icon)

        dbHelper.updateItemWatchRunning(itemID, false)

        val intent = Intent(activityRef, TimerService::class.java).apply {
            action = TimerService.ACTION_RESET_TIMER
            putExtra(TimerService.EXTRA_TIMER_ID, itemID.toInt())
        }
        activityRef.startService(intent)
    }


    // ----------------------------------------------------
    // TIMER RECEIVER
    // ----------------------------------------------------

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != TimerService.ACTION_TIMER_UPDATE) return

            val id = intent.getIntExtra(TimerService.EXTRA_TIMER_ID, 0)
            val time = intent.getLongExtra(TimerService.EXTRA_TIMER_VALUE, 0)

            if (COMMON_ID == id) {
                val sec = (time / 1000 % 60).toInt()
                val min = (time / (1000 * 60) % 60).toInt()
                val hr = (time / (1000 * 60 * 60)).toInt()

                elapsedTime = String.format("%02d:%02d:%02d", hr, min, sec)
                binding.elapsedTimeTxt.text = elapsedTime
            }
        }
    }

    // ----------------------------------------------------
    // CAMERA
    // ----------------------------------------------------

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(activityRef)

        providerFuture.addListener({
            val cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(viewFinder.surfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(activityRef))
    }

    private fun takePhoto() {
        mediaPlayer?.start()
        val capture = imageCapture ?: return

        val file = File(
            outputDirectory,
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(activityRef),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    processCapturedImage(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", exception.message ?: "Capture failed")
                }
            }
        )
    }

    private fun processCapturedImage(photoFile: File) {
        Executors.newSingleThreadExecutor().execute {

            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            val rotated = rotateBitmapIfRequired(bitmap, photoFile)
            val finalBitmap = addOverlay(rotated)

            val imageName = saveBitmap(finalBitmap)
            photoFile.delete()

            val date = SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

          //  elapsedTime =  binding.elapsedTimeTxt.text.toString()

            dbHelper.addSubItem(
                itemID,
                System.currentTimeMillis(),
                Subitem(
                    System.currentTimeMillis(),
                    testElapsedTime,
//                    elapsedTime,
                    imageName ?: "",
                    date
                )
            )

            requireActivity().runOnUiThread {
                Toast.makeText(activityRef, "Record saved successfully", Toast.LENGTH_SHORT).show()
                (activityRef as MainActivity).openFragmentWithBudelData(
                    itemID,
                    watchName,
                    WatchDetailFragment(),
                    "WatchDetail",
                    false
                )
            }
        }

    }

    // ----------------------------------------------------
    // IMAGE HELPERS
    // ----------------------------------------------------

    private fun rotateBitmapIfRequired(bitmap: Bitmap, file: File): Bitmap {
        val exif = ExifInterface(file.absolutePath)
        return when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotate(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotate(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotate(bitmap: Bitmap, degree: Float): Bitmap =
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(degree) }, true)

    private fun addOverlay(cameraBitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(
            cameraBitmap.width,
            cameraBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        canvas.drawBitmap(cameraBitmap, 0f, 0f, null)

        imageBitmap?.let {
            val matrix = Matrix()
//            matrix.postScale(3f, 3f)
//            matrix.postTranslate(100f, 100f)
            matrix.postScale(2f, 2f)
            matrix.postTranslate(500f, 900f)
            canvas.drawBitmap(it, matrix, null)
        }
        return result
    }

    private fun saveBitmap(bitmap: Bitmap): String? {
        val filename = "${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        }
        val uri = activityRef.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        uri?.let {
            activityRef.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            return filename
        }
        return null
    }

    private fun takeScreenshot(view: View): Bitmap =
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            view.draw(canvas)
        }

    // ----------------------------------------------------

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(activityRef, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

    private fun getOutputDirectory(): File =
        activityRef.externalMediaDirs.firstOrNull()?.let {
            File(it, getString(R.string.app_name)).apply { mkdirs() }
        } ?: activityRef.filesDir

    override fun onTimeUpdated(remainingTime: Long) {}

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}


