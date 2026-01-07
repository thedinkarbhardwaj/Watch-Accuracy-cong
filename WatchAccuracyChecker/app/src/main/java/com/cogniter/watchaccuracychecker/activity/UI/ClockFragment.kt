package com.cogniter.watchaccuracychecker.activity.UI

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.media.ExifInterface
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.AnalogTimerView
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.database.AppDatabase
import com.cogniter.watchaccuracychecker.database.entity.SubItemEntity
import com.cogniter.watchaccuracychecker.databinding.ClockActivityBinding
import com.cogniter.watchaccuracychecker.repository.WatchRepository
import com.cogniter.watchaccuracychecker.service.TimerService
import com.cogniter.watchaccuracychecker.utills.GlobalVariables.COMMON_ID
import com.cogniter.watchaccuracychecker.viewmodel.WatchViewModel
import com.cogniter.watchaccuracychecker.viewmodel.WatchViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class ClockFragment : Fragment(), AnalogTimerView.TimerListener {

    private lateinit var binding: ClockActivityBinding
    private lateinit var activityRef: Activity
    private lateinit var watchViewModel: WatchViewModel

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: java.util.concurrent.ExecutorService
    private lateinit var outputDirectory: File
    private lateinit var viewFinder: PreviewView

    private var watchName = ""
    private var itemID: Long = 0
    private var isWatchRunning = false
    private var testElapsedTime = ""
    private var imageBitmap: Bitmap? = null
    private var mediaPlayer: MediaPlayer? = null

    // ----------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = ClockActivityBinding.inflate(inflater, container, false)
        activityRef = requireActivity()

        // ROOM + VIEWMODEL
        val db = AppDatabase.getDatabase(requireContext())
        val repository = WatchRepository(db.watchDao())
        watchViewModel = ViewModelProvider(
            this,
            WatchViewModelFactory(repository)
        )[WatchViewModel::class.java]

        readArguments()
        setupUI()
//        initCamera()

        LocalBroadcastManager.getInstance(activityRef)
            .registerReceiver(timerReceiver, IntentFilter(TimerService.ACTION_TIMER_UPDATE))

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (isWatchRunning) showTrackingWarningDialog()
            else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCamera()

    }

    // ----------------------------------------------------
    // ARGUMENTS & UI
    // ----------------------------------------------------

    private fun readArguments() {
        watchName = arguments?.getString("watchNAME") ?: ""
        itemID = arguments?.getLong("itemID") ?: 0L
        isWatchRunning = arguments?.getBoolean("isrunning") ?: false
        COMMON_ID = itemID.toInt()
    }

    private fun setupUI() {
        (activityRef as MainActivity).apply {
            findViewById<TextView>(R.id.nameTextView)?.text = watchName
            findViewById<ImageView>(R.id.backButton)?.visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.bottomNav)?.visibility = View.GONE
        }

        updateStartStopUI()
        binding.myTimer.setTimerListener(this)

        // No storage permission check needed anymore
        binding.btnStartStop.setOnClickListener {
            if (isWatchRunning) stopTimer() else startTimer()
        }

        binding.btnrstart.setOnClickListener {
            resetTimer()
        }
    }

    private fun updateStartStopUI() {
        if (isWatchRunning) {
            binding.startStopImg.setImageResource(R.drawable.stop_button)
            binding.btnStart.text = "Stop"
        } else {
            binding.startStopImg.setImageResource(R.drawable.start_button_icon)
            binding.btnStart.text = "Start"
        }
    }

    // ----------------------------------------------------
    // TIMER
    // ----------------------------------------------------

    private fun startTimer() {
        isWatchRunning = true
        updateStartStopUI()

        // Save running state + start time
        watchViewModel.updateRunningState(
            watchId = itemID,
            isRunning = true,
            startTime = System.currentTimeMillis()
        )

        val intent = Intent(activityRef, TimerService::class.java).apply {
            action = TimerService.ACTION_START_TIMER
            putExtra(TimerService.EXTRA_TIMER_ID, itemID.toInt())
        }
        ContextCompat.startForegroundService(activityRef, intent)
    }

    private fun stopTimer() {
        testElapsedTime = binding.elapsedTimeTxt.text.toString()
        isWatchRunning = false
        updateStartStopUI()

        // Calculate final elapsed time
        val elapsedMillis = parseTimeToMillis(testElapsedTime)

        // Save final time + stop state
        watchViewModel.updateElapsedTime(
            id = itemID.toInt(),
            elapsed = elapsedMillis,
            running = false
        )

        watchViewModel.updateRunningState(
            watchId = itemID,
            isRunning = false,
            startTime = null
        )

        val intent = Intent(activityRef, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_TIMER
            putExtra(TimerService.EXTRA_TIMER_ID, itemID.toInt())
        }
        activityRef.startService(intent)

        imageBitmap = takeScreenshot(binding.myTimer)
        takePhoto()
    }

    private fun parseTimeToMillis(time: String): Long {
        val parts = time.split(":").map { it.toLong() }
        val hr = parts.getOrNull(0) ?: 0
        val min = parts.getOrNull(1) ?: 0
        val sec = parts.getOrNull(2) ?: 0
        return (hr * 3600 + min * 60 + sec) * 1000
    }

    private fun resetTimer() {
        isWatchRunning = false
        updateStartStopUI()

        watchViewModel.stopWatch(itemID)

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
                binding.elapsedTimeTxt.text = String.format("%02d:%02d:%02d", hr, min, sec)
            }
        }
    }

    // ----------------------------------------------------
    // CAMERA
    // ----------------------------------------------------

    private fun initCamera() {
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.camera_shutter)
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewFinder = binding.viewFinder
        outputDirectory = getOutputDirectory()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activityRef)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

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
        val capture = imageCapture ?: return
        mediaPlayer?.start()

        val photoFile = File(outputDirectory, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activityRef),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    processCapturedImage(photoFile)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("Camera", "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    // ----------------------------------------------------
    // IMAGE PROCESSING + SAVE
    // ----------------------------------------------------

    private fun processCapturedImage(photoFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            val rotatedBitmap = rotateBitmapIfRequired(originalBitmap, photoFile)
            val finalBitmap = addOverlay(rotatedBitmap)

            val imageName = saveBitmap(finalBitmap)
            photoFile.delete() // Clean up temporary file

            val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

            val subItem = SubItemEntity(
                watchId = itemID,
                name = testElapsedTime,
                image = imageName,
                date = date
            )

            watchViewModel.addSubItem(subItem)
            watchViewModel.updateHistoryCount(subItem.watchId)

            requireActivity().runOnUiThread {
                Toast.makeText(activityRef, "Record saved", Toast.LENGTH_SHORT).show()
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

    private fun rotateBitmapIfRequired(bitmap: Bitmap, file: File): Bitmap {
        val exif = ExifInterface(file.absolutePath)
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotate(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotate(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotate(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degree) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun addOverlay(cameraBitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(cameraBitmap.width, cameraBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(cameraBitmap, 0f, 0f, null)

        imageBitmap?.let { overlay ->
            val matrix = Matrix()
            matrix.postScale(1.6f, 1.6f)
            matrix.postTranslate(700f, 800f) // Adjust position/size as needed
            canvas.drawBitmap(overlay, matrix, null)
        }
        return result
    }

    private fun saveBitmap(bitmap: Bitmap): String {
        val filename = "${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val uri = activityRef.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let { imageUri ->
            activityRef.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
        return filename
    }

    private fun takeScreenshot(view: View): Bitmap {
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            view.draw(canvas)
        }
    }

    // ----------------------------------------------------

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        return requireContext().externalMediaDirs.firstOrNull()?.let {
            File(it, getString(R.string.app_name)).apply { mkdirs() }
        } ?: requireContext().filesDir
    }

    override fun onTimeUpdated(remainingTime: Long) {}

    private fun showTrackingWarningDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Tracking in progress")
            .setMessage("Tracking is currently running. Do you want to stop it and go back?")
            .setCancelable(false)
            .setPositiveButton("Stop & Exit") { _, _ -> stopTimer() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(activityRef).unregisterReceiver(timerReceiver)
        cameraExecutor.shutdown()
        mediaPlayer?.release()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}