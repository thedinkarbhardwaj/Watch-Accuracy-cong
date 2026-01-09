package com.cogniter.watchaccuracychecker.activity.UI

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
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
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.Utila.NotificationHelper
import com.cogniter.watchaccuracychecker.activity.AnalogTimerView
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.database.AppDatabase
import com.cogniter.watchaccuracychecker.database.entity.SubItemEntity
import com.cogniter.watchaccuracychecker.databinding.ClockActivityBinding
import com.cogniter.watchaccuracychecker.repository.WatchRepository
import com.cogniter.watchaccuracychecker.viewmodel.WatchViewModel
import com.cogniter.watchaccuracychecker.viewmodel.WatchViewModelFactory
import com.cogniter.watchaccuracychecker.worker.WatchTimerWorker
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ClockFragment : Fragment(), AnalogTimerView.TimerListener {

    private lateinit var binding: ClockActivityBinding
    private lateinit var activityRef: Activity
    private lateinit var watchViewModel: WatchViewModel

    // Camera
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: java.util.concurrent.ExecutorService
    private lateinit var outputDirectory: File
    private lateinit var viewFinder: PreviewView
    private var mediaPlayer: MediaPlayer? = null

    // Timer
    private var watchName = ""
    private var itemID: Long = 0L
    private var isWatchRunning = false
    private var timerJob: Job? = null
    private var testElapsedTime = ""
    private var imageBitmap: Bitmap? = null

    // ----------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = ClockActivityBinding.inflate(inflater, container, false)
        activityRef = requireActivity()

        val db = AppDatabase.getDatabase(requireContext())
        val repository = WatchRepository(db.watchDao())
        watchViewModel = ViewModelProvider(
            this,
            WatchViewModelFactory(repository)
        )[WatchViewModel::class.java]

        readArguments()
        setupUI()
        restoreTimerState()

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
    }

    private fun setupUI() {
        (activityRef as MainActivity).apply {
            findViewById<TextView>(R.id.nameTextView)?.text = watchName
            findViewById<ImageView>(R.id.backButton)?.visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.bottomNav)?.visibility = View.GONE
        }

        binding.myTimer.setTimerListener(this)

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
    // TIMER LOGIC (NO SERVICE)
    // ----------------------------------------------------

    private fun startTimer() {
        val startMillis = System.currentTimeMillis()
        isWatchRunning = true
        updateStartStopUI()

        val beginTimeStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        watchViewModel.updateRunningState(
            watchId = itemID,
            isRunning = true,
            startTime = startMillis
        )

        watchViewModel.beginTimeAdded(
            watchId = itemID,
            beginTimeAdd = beginTimeStr
        )

        // Use applicationContext to be safe


        startUITimer(startMillis)

        startWatchTimerWorker(this.requireActivity().applicationContext)
    }

    private fun stopTimer() {
        testElapsedTime = binding.elapsedTimeTxt.text.toString()
        isWatchRunning = false
        updateStartStopUI()
        timerJob?.cancel()

        val elapsedMillis = parseTimeToMillis(testElapsedTime)

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

        imageBitmap = takeScreenshot(binding.myTimer)
        takePhoto()
    }

    fun startWatchTimerWorker(context: Context) {

        val workRequest =
            PeriodicWorkRequestBuilder<WatchTimerWorker>(
                15, TimeUnit.MINUTES // minimum allowed
            )
                .addTag("WATCH_TIMER_WORK")
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "WATCH_TIMER_WORK",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

//
//        val workRequest = OneTimeWorkRequestBuilder<WatchTimerWorker>()
//            .setInitialDelay(5, TimeUnit.SECONDS) // 30 sec
//            .build()
//
//        WorkManager.getInstance(context.applicationContext).enqueue(workRequest)
    }


    private fun takeScreenshot(view: View): Bitmap {
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            view.draw(canvas)
        }
    }

    private fun parseTimeToMillis(time: String): Long {
        val parts = time.split(":").map { it.toLong() }
        val hr = parts.getOrNull(0) ?: 0
        val min = parts.getOrNull(1) ?: 0
        val sec = parts.getOrNull(2) ?: 0
        return (hr * 3600 + min * 60 + sec) * 1000
    }

    private fun resetTimer() {
        timerJob?.cancel()
        isWatchRunning = false
        updateStartStopUI()
        binding.elapsedTimeTxt.text = "00:00:00"

        watchViewModel.updateRunningState(
            watchId = itemID,
            isRunning = false,
            startTime = null
        )

        binding.myTimer.updateTimerView(0L)

    }

    // ----------------------------------------------------
    // UI TIMER
    // ----------------------------------------------------

    private fun startUITimer(startMillis: Long) {
        timerJob?.cancel()

        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startMillis
                binding.elapsedTimeTxt.text = formatMillis(elapsed)
                binding.myTimer.updateTimerView(elapsed)
                delay(1000)
            }
        }
    }

    private fun formatMillis(millis: Long): String {
        val sec = (millis / 1000) % 60
        val min = (millis / (1000 * 60)) % 60
        val hr = millis / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hr, min, sec)
    }

    private fun restoreTimerState() {
        CoroutineScope(Dispatchers.IO).launch {
            val watch = watchViewModel.getWatchById(itemID) ?: return@launch

            withContext(Dispatchers.Main) {
                if (watch.isWatchRunning && watch.beginTime != null) {
                    isWatchRunning = true
                    updateStartStopUI()
                    startUITimer(watch.startTimeMillis!!)
                } else {
                    binding.elapsedTimeTxt.text = "00:00:00"
                }
            }


        }
    }

    // ----------------------------------------------------
    // CAMERA (UNCHANGED)
    // ----------------------------------------------------

    private fun initCamera() {
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.camera_shutter)
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewFinder = binding.viewFinder
        outputDirectory = getOutputDirectory()

        if (allPermissionsGranted()) startCamera()
        else requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
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
                    Log.e("Camera", "Photo failed", exc)
                }
            }
        )
    }

    // ----------------------------------------------------
    // IMAGE SAVE
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
            watchViewModel.updateHistoryCount(itemID)

            withContext(Dispatchers.Main) {
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
            matrix.postScale(1.8f, 1.8f)
            matrix.postTranslate(900f, 1100f) // Adjust position/size as needed
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
        val uri = activityRef.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        uri?.let {
            activityRef.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
        }
        return filename
    }

    private fun getOutputDirectory(): File =
        requireContext().externalMediaDirs.firstOrNull()?.let {
            File(it, getString(R.string.app_name)).apply { mkdirs() }
        } ?: requireContext().filesDir

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun showTrackingWarningDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Tracking running")
            .setMessage("Stop tracking and exit?")
            .setPositiveButton("Stop") { _, _ -> stopTimer() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerJob?.cancel()
        cameraExecutor.shutdown()
        mediaPlayer?.release()
    }

    override fun onTimeUpdated(remainingTime: Long) {}

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
