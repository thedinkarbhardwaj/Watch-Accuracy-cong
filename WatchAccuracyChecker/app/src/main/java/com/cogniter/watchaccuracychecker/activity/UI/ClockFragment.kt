package com.cogniter.watchaccuracychecker.activity.UI

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
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
import com.cogniter.watchaccuracychecker.utills.GlobalVariables
import com.cogniter.watchaccuracychecker.utills.GlobalVariables.COMMON_ID
import kotlinx.coroutines.Runnable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Project name is Stopwatch
 * Created by Zukron Alviandy R on 9/2/2020
 * Contact me if any issues on zukronalviandy@gmail.com
 */
class ClockFragment : Fragment(), AnalogTimerView.TimerListener{


    lateinit var binding: ClockActivityBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    // Inside the activity class
    private lateinit var viewFinder: PreviewView
    var watchname = ""


    var imageBitmap:Bitmap? = null
    var elapsedTime:String? =""
    var itemID:Long? = 0
    var startBtn: Boolean =true
    lateinit var activity: Activity
    private lateinit var dbHelper: DBHelper
    var mediaPlayer: MediaPlayer? =null
    var iswatchRunning :Boolean =false


    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    override fun onDetach() {
        super.onDetach()

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.clock_activity, container, false)

        activity = getActivity()!!

        (activity as? MainActivity)?.findViewById<LinearLayout>(R.id.bottomNav)?.visibility = View.GONE
        LocalBroadcastManager.getInstance(activity).registerReceiver(timerUpdateReceiver, IntentFilter(TimerService.ACTION_TIMER_UPDATE))

        cameraShuttersoundInitalize()
        dbHelper = DBHelper(activity)
        viewFinder = view.findViewById(R.id.viewFinder) // Assuming the id of the PreviewView is viewFinder
        // Get a reference to the SurfaceView for the camera preview

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        //ISwatchRunning

        watchname = arguments?.getString("watchNAME")!!
        itemID= arguments?.getLong("itemID",0)!!
        iswatchRunning = arguments?.getBoolean("isrunning",false)!!

        (activity as? MainActivity)?.findViewById<TextView>(R.id.nameTextView)?.text = watchname
        (activity as? MainActivity)?.findViewById<ImageView>(R.id.backButton)?.visibility = View.VISIBLE
      //  watchName.text =watchname

        if (iswatchRunning) {
            startBtn = false
            binding.btnStart.text="Stop"
            binding.startStopImg.setImageResource(R.drawable.stop_button)
//            btn_start.setBackgroundResource(R.drawable.btn_stop)
            dbHelper.setStringValue(watchname)
            dbHelper.setLongValue(itemID!!)
            COMMON_ID = itemID!!.toInt()
        }


        binding.myTimer.setTimerListener(this);
        binding.btnStartStop.setOnClickListener {

            if(startBtn){
                mediaPlayer!!.start()
                startBtn = false
                binding.btnStart.text="Stop"
                dbHelper.updateItemWatchRunning(itemID!!,true)
                binding.startStopImg.setImageResource(R.drawable.stop_button)
//                btn_start.setBackgroundResource(R.drawable.btn_stop)
                dbHelper.setStringValue(watchname)
                dbHelper.setLongValue(itemID!!)
                // Start the MyForegroundService


                val intent = Intent(activity, TimerService::class.java)
                intent.action = TimerService.ACTION_START_TIMER
                intent.putExtra(TimerService.EXTRA_TIMER_ID, itemID!!.toInt()) // Set the timer ID
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(activity, intent)
                } else {
                    activity.startService(intent)
                }

//                val stopServiceIntent = Intent(activity, TimerService::class.java)
//                stopServiceIntent.action = TimerService.ACTION_STOP_SERVICE
//                stopServiceIntent.putExtra(TimerService.EXTRA_TIMER_ID, itemID!!.toInt()) // Set the timer ID
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    ContextCompat.startForegroundService(activity, stopServiceIntent)
//                } else {
//                    activity.startService(stopServiceIntent)
//                }


            }else{
                startBtn = true
                binding.btnStart.text="Start"
                dbHelper.updateItemWatchRunning(itemID!!,false)
                binding.startStopImg.setImageResource(R.drawable.start_button_icon)
//                btn_start.setBackgroundResource(R.drawable.btn_start)
                dbHelper.deleteStringValue()
                dbHelper.deleteLongValue()
                val stopServiceIntent = Intent(activity, TimerService::class.java)
                stopServiceIntent.action = TimerService.ACTION_STOP_TIMER
                stopServiceIntent.putExtra(TimerService.EXTRA_TIMER_ID, itemID!!.toInt()) // Set the timer ID
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(activity, stopServiceIntent)
                } else {
                    activity.startService(stopServiceIntent)
                }
//                val stopService= Intent(activity, TimerService::class.java)
//                stopService.action = TimerService.ACTION_STOP_SERVICE
//                stopService.putExtra(TimerService.EXTRA_TIMER_ID, itemID!!.toInt()) // Set the timer ID
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    ContextCompat.startForegroundService(activity, stopService)
//                } else {
//                    activity.startService(stopService)
//                }
                imageBitmap = takeScreenshot(binding.myTimer)
                takePhoto()



            }

        }

        binding.btnrstart.setOnClickListener {
            startBtn = true
            binding.btnStart.text="Start"

            dbHelper.updateItemWatchRunning(itemID!!,false)
            binding.startStopImg.setImageResource(R.drawable.start_button_icon)
            //       btn_start.setBackgroundResource(R.drawable.btn_start)

            val intent = Intent(activity, TimerService::class.java)
            intent.action = TimerService.ACTION_RESET_TIMER
            intent.putExtra(TimerService.EXTRA_TIMER_ID, itemID!!.toInt()) // Set the timer ID
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(activity, intent)
            } else {
                activity.startService(intent)
            }


            val stopServiceIntent = Intent(activity, TimerService::class.java)
            stopServiceIntent.action = TimerService.ACTION_STOP_SERVICE
            stopServiceIntent.putExtra(TimerService.EXTRA_TIMER_ID, itemID!!.toInt()) // Set the timer ID
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(activity, stopServiceIntent)
            } else {
                activity.startService(stopServiceIntent)
            }


            timeChange()

        }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        return view
    }

    fun timeChange() {
        val timer = object: CountDownTimer(100, 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                binding.elapsedTimeTxt.text= "00:00:00"
            }
        }
        timer.start()
    }




    fun cameraShuttersoundInitalize(){
         mediaPlayer = MediaPlayer.create(context, R.raw.camera_shutter)
        val volume = 0.5f // Set the volume to 50% of maximum volume
        mediaPlayer!!.setVolume(volume, volume)

        
    }

    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TimerService.ACTION_TIMER_UPDATE) {

                val id = intent.getIntExtra(TimerService.EXTRA_TIMER_ID, 0)
                val eTime = intent.getLongExtra(TimerService.EXTRA_TIMER_VALUE, 0)
                val isRuning = intent.getBooleanExtra(TimerService.IS_RUNNING, false)


                if (COMMON_ID == id) {
                    if (isRuning) {

                        try{
                            val seconds: Int = (eTime / 1000 % 60).toInt()
                            val minutes: Int = (eTime / (1000 * 60) % 60).toInt()
                            val hours: Int = (eTime / (1000 * 60 * 60)).toInt()

                            val timeTaken = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                            elapsedTime =timeTaken.toString()
                            binding.elapsedTimeTxt.text= elapsedTime
                        }catch (e:Exception){

                        }


                    }
                }

//                // Update the AnalogTimerView with the new timer value
//               // val milliseconds: Int = (eTime % 1000).toInt()
//                try{
//                    val seconds: Int = (eTime / 1000 % 60).toInt()
//                    val minutes: Int = (eTime / (1000 * 60) % 60).toInt()
//                    val hours: Int = (eTime / (1000 * 60 * 60)).toInt()
//
//                    val timeTaken = String.format("%02d:%02d:%02d", hours, minutes, seconds)
//                    elapsedTime =timeTaken.toString()
//                    elapsedTimeTxt.text= elapsedTime
//                }catch (e: Exception){
//
//                }


            }
        }
    }



    private fun takePhoto() {
        // Get a stable reference of the
        // modifiable image capture use case
        // Play camera shutter sound when a photo is clicked
        mediaPlayer!!.start()

        val imageCapture = imageCapture ?: return
        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener,
        // which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)


                    val msg = "Photo capture succeeded: $savedUri"

                  captureCameraSurfaceScreenshot(savedUri,photoFile)
                    Log.d(TAG, msg)
                }
            })
    }
    private fun captureCameraSurfaceScreenshot(savedUri: Uri, photoFile: File) {
        AsyncTask.execute {
            // Get the drawing cache of the surface view
            val bitmap = uriToBitmap(activity, savedUri)
            val rotatedBitmap = rotateBitmapIfRequired(activity, bitmap, savedUri)
            val resultBitmap= addedBackgroundBitmap(activity,rotatedBitmap!!)

            val savedFilename = saveBitmapToGallery(resultBitmap!!,activity)

            photoFile.delete()

            var subitem1 =dbHelper.getSubItemsForItem(arguments?.getLong("itemID",0)!!)
            System.out.println(subitem1)

            // Add date string to the subitem
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

            // dateFormat.dateFormatSymbols = DateFormatSymbols().apply { shortMonths = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec") }
            val currentDate = dateFormat.format(Date())
            dbHelper.addSubItem(arguments?.getLong("itemID",0)!!,generateRandomId(), Subitem(generateRandomId(),elapsedTime!!, savedFilename!!,currentDate))

            var subitem =dbHelper.getSubItemsForItem(arguments?.getLong("itemID",0)!!)
            System.out.println(subitem)

            activity.runOnUiThread {

//                val intent = Intent(activity, MainActivity::class.java)
//
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                startActivity(intent)

               val activity = requireActivity() as MainActivity
//                activity.onBackPressed()
                activity.openFragmentWithBudelData(
                    itemID!!,
                    watchname,
                    WatchDetailFragment(),
                    "WatchDetail",
                    false
                )

                Toast.makeText(activity, "Record saved successfully." + "", Toast.LENGTH_LONG).show()
            }
        }
    }
    fun addedBackgroundBitmap(context: Context, cameraImage: Bitmap): Bitmap? {
        // Load the original image and the new background photo


// Create a new Bitmap with the same dimensions as the original image
        val resultBitmap: Bitmap = Bitmap.createBitmap(cameraImage.width, cameraImage.height, cameraImage.config)

// Create a Canvas object and draw the original image onto it
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(cameraImage, 0f, 0f, null)

// Calculate the position for drawing the image in the center
        val centerX = (canvas.width - (imageBitmap!!.width * 3)) / 2
//        val centerY = (canvas.height - (imageBitmap!!.height * 3)) / 2-200
        val centerY = (canvas.height - (imageBitmap!!.height * 3)) / 2-100

// Create a Matrix to scale the bitmap
        val matrix = Matrix()
        matrix.postScale(3F,3F)

// Apply the scale to the original bitmap without creating a new bitmap
        matrix.postTranslate(centerX.toFloat(), centerY.toFloat()) // Translate to the center
        canvas.drawBitmap(imageBitmap!!, matrix, null)


        return resultBitmap

    }
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateRandomId(): Long {
        val random = Random()
        return random.nextLong()
    }
    private fun saveBitmapToGallery(bitmap: Bitmap, context: Context): String? {
        val filename = "${System.currentTimeMillis()}.jpg" // Create a unique filename
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream) // Compress and save the bitmap to the output stream
            }

            // Notify the media scanner about the new image
            MediaScannerConnection.scanFile(
                context,
                arrayOf(imageUri.path),
                arrayOf("image/jpeg"),
                null
            )

            return filename // Return the filename of the saved image
        }

        return null // Return null if the image was not saved
    }

    // Add a new function to rotate the bitmap if required
    private fun rotateBitmapIfRequired(context: Context, bitmap: Bitmap?, uri: Uri): Bitmap? {
        val ei = ExifInterface(uri.path!!)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap?, degrees: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }



    fun takeScreenshot(view: View): Bitmap {
        // Create a bitmap with the same dimensions as the view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        // Create a canvas with the bitmap
        val canvas = Canvas(bitmap)

        // Draw the view onto the canvas
        view.draw(canvas)

        return bitmap
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)


        cameraProviderFuture.addListener(Runnable {

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val scale = camera!!.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                        camera!!.cameraControl.setZoomRatio(scale)
                        return true
                    }
                }

                val scaleGestureDetector = ScaleGestureDetector(activity, listener)

                viewFinder.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    return@setOnTouchListener true
                }


            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }


        }, ContextCompat.getMainExecutor(activity))


    }



    private fun stopCamera() {
        cameraExecutor.shutdown()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            try {
                // Unbind all use cases from camera provider
                cameraProvider.unbindAll()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case unbinding failed", exc)
            }
        }, ContextCompat.getMainExecutor(activity))
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }

    // creates a folder inside internal storage
    private fun getOutputDirectory(): File {
        val mediaDir = activity.externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else activity.filesDir
    }

    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // If permissions are not granted,
                // present a toast to notify the user that
                // the permissions were not granted.
                Toast.makeText(activity, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()

            }
        }
    }

    companion object {
        private const val TAG = "CameraXGFG"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()


        mediaPlayer!!.release()
        cameraExecutor.shutdown()
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(timerUpdateReceiver)
    }

//    override fun onBackPressed() {
//        stopCamera()
//        val stopServiceIntent = Intent(activity, TimerService::class.java)
//        stopServiceIntent.action = TimerService.ACTION_STOP_SERVICE
//        activity.startService(stopServiceIntent)
//        val intent = Intent(activity, MainActivity::class.java)
//        intent.putExtra("itemID", itemID) // Add the ListItem object to the intent
//        intent.putExtra("watchNAME", watchname) // Add the ListItem object to the intent
//        intent.putExtra("fromClockActivity", true) // Add the ListItem object to the intent
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        startActivity(intent)
//        System.out.println("dlpoldpol   "+"stop hua")
//        super.onBackPressed()
//    }

    override fun onTimeUpdated(remainingTime: Long) {
//        System.out.println("dlpdlplp    "+remainingTime)
//        System.out.println("dkodko  "+Lap.convertToDuration(remainingTime.toInt()))
//        val minutes: Long = (remainingTime / 60).toLong()
//        val seconds: Long = (remainingTime % 60).toLong()
//        val milliseconds: Long = 0 // Assuming no milliseconds in this example


//        System.out.println("dkodko  "+Lap.convertToDuration(remainingTime.toInt()))

    }


}
