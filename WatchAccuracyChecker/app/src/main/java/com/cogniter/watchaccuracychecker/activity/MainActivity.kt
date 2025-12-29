package com.cogniter.watchaccuracychecker.activity


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.UI.AboutusFragment
import com.cogniter.watchaccuracychecker.activity.UI.AllHistoryFragment
import com.cogniter.watchaccuracychecker.activity.UI.ClockFragment
import com.cogniter.watchaccuracychecker.activity.UI.HelpFragment
import com.cogniter.watchaccuracychecker.activity.UI.MywatchListing
import com.cogniter.watchaccuracychecker.activity.UI.SettingsFragment
import com.cogniter.watchaccuracychecker.activity.UI.WatchDetailFragment
import com.cogniter.watchaccuracychecker.adapter.DrawerItemCustomAdapter
import com.cogniter.watchaccuracychecker.database.DBHelper
import com.cogniter.watchaccuracychecker.databinding.ActivityMainBinding
import com.cogniter.watchaccuracychecker.model.DataModel
import com.cogniter.watchaccuracychecker.service.TimerService
import com.cogniter.watchaccuracychecker.utills.GlobalVariables.COMMON_ID


class MainActivity : AppCompatActivity(), DrawerItemCustomAdapter.OnItemClickListener {


    private lateinit var adapter: DrawerItemCustomAdapter
    private lateinit var dbHelper: DBHelper

    var minutes = 0
    private lateinit var handler: Handler

    lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

         dbHelper = DBHelper(this)
        // alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        handler = Handler(Looper.getMainLooper())
        uiSetup()
        onClick()
        if (intent.getBooleanExtra("fromSplash", false)) {

            var itemList = dbHelper.getAllItems()
            if (itemList.isNotEmpty()) {
                itemList?.forEach { listItem ->
                    dbHelper.updateItemWatchRunning(listItem.id, false)

                }
            }


        } else if (intent.getBooleanExtra("fromNotification", false)) {
           Log.d("after touch timer id  ",intent.getIntExtra("timerId", -1).toString())
            Log.d("after touch watchname  ",intent.getStringExtra("watchname")!!)

//            dbHelper.setStringValue(intent.getStringExtra("watchname")!!)
//            dbHelper.setLongValue(intent.getIntExtra("timerId", -1).toLong())
            COMMON_ID = intent.getIntExtra("timerId", -1)
            openFragmentWithBudelData(
                intent.getIntExtra("timerId", -1).toLong(),
                intent.getStringExtra("watchname")!!,
                ClockFragment(),
                "ClockActivity",
                true
            )
        }


    }

    private fun uiSetup() {

        binding.bottomNav.visibility = View.VISIBLE
        binding.myWacthesIcon.setColorFilter(ContextCompat.getColor(this, R.color.darkyellow));
        binding.myWacthesText.setTextColor(ContextCompat.getColor(this, R.color.darkyellow))

        val drawerItem: Array<DataModel?> = arrayOfNulls(5)

        drawerItem[0] = DataModel(R.drawable.smart_watch_white, "Watches")
        drawerItem[1] = DataModel(R.drawable.history_white, "History")
        drawerItem[2] = DataModel(R.drawable.info_white, "About us")
        drawerItem[3] = DataModel(R.drawable.question, "Help")
        drawerItem[4] = DataModel(R.drawable.settings, "Settings")

        adapter = DrawerItemCustomAdapter(this, R.layout.list_view_item_row, drawerItem)
        binding.leftDrawer.adapter = adapter

        adapter.setOnItemClickListener(this)

        watchlistUI()


    }

    private fun onClick() {

        binding.layoutMywatches.setOnClickListener {

            watchlistUI()
        }
        binding.layoutTrackHistory.setOnClickListener {

            trackHistoryUI()
        }
        binding.layoutAboutUsHistory.setOnClickListener {
            aboutusUi()
        }
        binding.layouthelpUsHistory.setOnClickListener {
            helpusUI()
        }
        binding.layoutsettings.setOnClickListener {
            settingsUI()
        }

        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.leftDrawer.setOnItemClickListener { parent, view, position, id ->
            // Handle the item click event here
            if (position == 1) {
                watchlistUI()
            } else if (position == 2) {
                trackHistoryUI()
            } else if (position == 3) {
                aboutusUi()
            } else if (position == 4) {
                helpusUI()
            }else if (position == 5) {
                settingsUI()
            }
        }

        binding.drawerToggle.setOnClickListener {

            binding.drawerLayout.openDrawer(binding.leftDrawer)

        }

    }
    private fun settingsUI() {
        binding.bottomNav.visibility = View.VISIBLE

        binding.myWacthesIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.myWacthesText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.trackingHistoryIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.trackingHistoryText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.infoIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.infoText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.helpIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.helpText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.settingsIcon.setColorFilter(ContextCompat.getColor(this, R.color.darkyellow));
        binding.settingsText.setTextColor(ContextCompat.getColor(this, R.color.darkyellow))
        loadFragment(SettingsFragment())
        binding.nameTextView.text = "SETTINGS"
        adapter.setSelectedPosition(4)
    }

    private fun helpusUI() {
        binding.bottomNav.visibility = View.VISIBLE

        binding.myWacthesIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.myWacthesText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.trackingHistoryIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.trackingHistoryText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.infoIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.infoText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.helpIcon.setColorFilter(ContextCompat.getColor(this, R.color.darkyellow));
        binding.helpText.setTextColor(ContextCompat.getColor(this, R.color.darkyellow))
        binding.settingsIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.settingsText.setTextColor(ContextCompat.getColor(this, R.color.white))
        loadFragment(HelpFragment())
        binding.nameTextView.text = "HELP"
        adapter.setSelectedPosition(4)
    }

    private fun aboutusUi() {
        binding.bottomNav.visibility = View.VISIBLE

        binding.myWacthesIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.myWacthesText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.trackingHistoryIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.trackingHistoryText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding. infoIcon.setColorFilter(ContextCompat.getColor(this, R.color.darkyellow));
        binding. infoText.setTextColor(ContextCompat.getColor(this, R.color.darkyellow))
        binding.helpIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.helpText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.settingsIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.settingsText.setTextColor(ContextCompat.getColor(this, R.color.white))
        loadFragment(AboutusFragment())
        binding.nameTextView.text = "ABOUT US"
        adapter.setSelectedPosition(3)
    }

    private fun trackHistoryUI() {
        binding.bottomNav.visibility = View.VISIBLE
        binding.myWacthesIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding. myWacthesText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.trackingHistoryIcon.setColorFilter(ContextCompat.getColor(this, R.color.darkyellow));
        binding.trackingHistoryText.setTextColor(ContextCompat.getColor(this, R.color.darkyellow))
        binding.infoIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.infoText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.helpIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.helpText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.settingsIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.settingsText.setTextColor(ContextCompat.getColor(this, R.color.white))
        loadFragment(AllHistoryFragment())
        binding.nameTextView.text = "TRACK HISTORY"
        adapter.setSelectedPosition(2)
    }

    private fun watchlistUI() {
        binding.bottomNav.visibility = View.VISIBLE

        binding.myWacthesIcon.setColorFilter(ContextCompat.getColor(this, R.color.darkyellow));
        binding.myWacthesText.setTextColor(ContextCompat.getColor(this, R.color.darkyellow))
        binding.trackingHistoryIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.trackingHistoryText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.infoIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.infoText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding. helpIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.helpText.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.settingsIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        binding.settingsText.setTextColor(ContextCompat.getColor(this, R.color.white))
        loadFragment(MywatchListing())
        binding.nameTextView.text = "MY WATCHES"
        adapter.setSelectedPosition(1)
    }


    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.commit()
    }

    override fun onItemClick(position: Int) {
        binding.drawerLayout.closeDrawer(binding.leftDrawer)
        if (position == 1) {
            watchlistUI()
        } else if (position == 2) {
            trackHistoryUI()
        } else if (position == 3) {
            aboutusUi()
        } else if (position == 4) {
            helpusUI()
        }else if (position == 5) {
            settingsUI()
        }

    }

//    override fun onBackPressed() {
//        binding.bottomNav.visibility = View.VISIBLE
//        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
//        val fragmentName = currentFragment?.javaClass?.simpleName
//
//
//        println("Current fragment name: $fragmentName")
//        if (fragmentName.equals("WatchDetailFragment")) {
//            val fragmentManager = supportFragmentManager
//            val count = fragmentManager.backStackEntryCount
//            for (i in 0 until count) {
//                fragmentManager.popBackStack()
//            }
//            watchlistUI()
//            return
//        }
//
//        if (fragmentName.equals("MywatchListing") || fragmentName.equals("AboutusFragment") || fragmentName.equals(
//                "AllHistoryFragment"
//            )
//        ) {
//
//            return
//        }
//
//
//        super.onBackPressed()
//
//
//    }

    private var doubleBackToExitPressedOnce = false
    private val doubleBackHandler = Handler(Looper.getMainLooper())


    override fun onBackPressed() {

        binding.bottomNav.visibility = View.VISIBLE

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.container)

        when (currentFragment) {

            is WatchDetailFragment -> {
                supportFragmentManager.popBackStack(
                    null,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                watchlistUI()
            }

            is MywatchListing -> {
                handleDoubleBackExit()
            }


            is AboutusFragment,
            is AllHistoryFragment -> {
//                super.onBackPressed()
                watchlistUI()

            }

            else -> {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
//                    super.onBackPressed()
                    watchlistUI()

                }
            }
        }
    }

    private fun handleDoubleBackExit() {
        if (doubleBackToExitPressedOnce) {
            finish()
            return
        }

        doubleBackToExitPressedOnce = true
        Toast.makeText(
            this,
            "Press back again to exit",
            Toast.LENGTH_SHORT
        ).show()

        doubleBackHandler.postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }


    fun openFragmentWithBudelData(
        itemId: Long,
        watchName: String,
        fragment: Fragment,
        fragmentName: String,
        isrunning: Boolean
    ) {


        val bundle = Bundle()
        bundle.putLong("itemID", itemId)
        bundle.putString("watchNAME", watchName)
        bundle.putBoolean("isrunning", isrunning)
        fragment.arguments = bundle
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null)

        if (!supportFragmentManager.isStateSaved) {
            transaction.commit()
        } else {
            transaction.commitAllowingStateLoss()
        }

        // Remove the last back stack entry

    }

    override fun onDestroy() {
        val isServiceRunning = TimerService.isServiceRunning(this, TimerService::class.java)
        System.out.println("lpplflfplfss    "+isServiceRunning)
        if(isServiceRunning){

            val stopServiceIntent = Intent(this, TimerService::class.java)
            stopServiceIntent.action = TimerService.ACTION_STOP_SERVICE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, stopServiceIntent)
            } else {
               startService(stopServiceIntent)
            }
        }


        super.onDestroy()
    }


}
