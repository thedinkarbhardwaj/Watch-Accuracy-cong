package com.cogniter.watchaccuracychecker.activity.UI

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.adapter.CustomAdapater
import com.cogniter.watchaccuracychecker.database.DBHelper
import com.cogniter.watchaccuracychecker.databinding.SettingsFragmentBinding
import com.cogniter.watchaccuracychecker.databinding.WatchDetailActivityBinding
import com.cogniter.watchaccuracychecker.model.ListItem
import com.cogniter.watchaccuracychecker.model.Subitem
import com.cogniter.watchaccuracychecker.service.TimerService
import com.cogniter.watchaccuracychecker.utills.GlobalVariables
import com.cogniter.watchaccuracychecker.utills.ImageUtils


class WatchDetailFragment : Fragment() ,CustomAdapater.OnDeleteClickListener{

    private var _binding: WatchDetailActivityBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DBHelper
    var subitemList: List<Subitem>? = null
    lateinit var activity: Activity

    var imageRecyclerView: RecyclerView? = null
    var adapter:CustomAdapater ? = null
    var itemList: List<ListItem>? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.watch_detail_activity, container, false)
        activity = getActivity()!!

        dbHelper = DBHelper(activity)

        (activity as? MainActivity)?.findViewById<TextView>(R.id.nameTextView)?.text = "TRACK HISTORY"
        (activity as? MainActivity)?.findViewById<ImageView>(R.id.backButton)?.visibility = View.VISIBLE
        (activity as? MainActivity)?.findViewById<LinearLayout>(R.id.bottomNav)?.visibility = View.VISIBLE

        subitemList = dbHelper.getSubItemsForItem(arguments?.getLong("itemID",0)!!)
        binding.watchName.text = arguments?.getString("watchNAME")!!
        // Check if the subitemList is not null and not empty
        subitemList?.let { list ->
            if (list.isNotEmpty()) {
                val mutableList = list.toMutableList()  // Convert to mutable list
                mutableList.removeAt(0)  // Remove the first item
                subitemList = mutableList.toList()  // Convert back to immutable list
            }
        }
        subitemList = subitemList?.reversed()


        if(subitemList!!.size==0){
            binding.notrackingheader.visibility= View.VISIBLE

        }else{
            binding.notrackingheader.visibility= View.GONE


        }



        // Set the tooltip message


        binding.addTimer.setOnClickListener {
           val isServiceRunning = TimerService.isServiceRunning(activity, TimerService::class.java)

            itemList = dbHelper.getAllItems()
              itemList = itemList!!.reversed()

            dbHelper.deleteStringValue()
            dbHelper.deleteLongValue()
            dbHelper.setStringValue(arguments?.getString("watchNAME")!!)
            dbHelper.setLongValue(arguments?.getLong("itemID",0)!!)

            System.out.println(isServiceRunning!!.toString()+"  lpddlddddddddddddd  "+arguments?.getBoolean("isrunning")!!)

         //   if(arguments?.getBoolean("isrunning")!! && isServiceRunning!! || !arguments?.getBoolean("isrunning")!! && !isServiceRunning!!){
                val activity = requireActivity() as MainActivity
                activity.openFragmentWithBudelData(arguments?.getLong("itemID",0)!!, arguments?.getString("watchNAME")!!, ClockFragment(),"ClockActivity",arguments?.getBoolean("isrunning")!!)

//            }else  if(!arguments?.getBoolean("isrunning")!! && isServiceRunning!!){
//                var watchName=""
//                itemList?.forEach {
//                        item1 ->
//                    if (item1.isrunning) {
//                        // Found the running item
//                        watchName = item1.title
//                    }
//                }
//                Toast.makeText(activity,"Tracking for $watchName Watch is on.",Toast.LENGTH_SHORT).show()
//               // Toast.makeText(activity,watchName+" watch is running...",Toast.LENGTH_SHORT).show()
//
//            }
//                if(!arguments?.getBoolean("isrunning")!! && isServiceRunning!!){
//                    itemList?.forEach {
//                            item ->
//                        if (item.isrunning) {
//                            // Found the running item
//                            Toast.makeText(activity,item.title+" watch is running...",Toast.LENGTH_SHORT).show()
//                        }
//                    }
//
//                }else{
//                    val activity = requireActivity() as MainActivity
//                    activity.openFragmentWithBudelData(  arguments?.getLong("itemID",0)!!,
//                        arguments?.getString("watchNAME")!!, ClockFragment(),"ClockActivity",arguments?.getBoolean("isrunning")!!)
//                }

        }


         imageRecyclerView= view.findViewById(R.id.imageRecyclerView)
         adapter = CustomAdapater(false,arguments?.getString("watchNAME")!!,activity,subitemList!!)
        imageRecyclerView!!.adapter = adapter
        imageRecyclerView!!.layoutManager = LinearLayoutManager(activity)

        imageRecyclerView!!.adapter = adapter
        adapter!!.setOnDeleteClickListener(this)

        return binding.root

    }


    override fun OnDeleteClickListener(subitem: Subitem, i: Int) {

        deleteItemDialog(subitem)
    }
    fun deleteItemDialog(subitem: Subitem) {

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Delete")
            .setMessage("Are you sure to delete the record?")
            .setPositiveButton("Yes") { _, _ ->
                dbHelper.removeSubItem(subitem.subitemId)

                subitemList = emptyList()
                subitemList = dbHelper.getSubItemsForItem(arguments?.getLong("itemID",0)!!)

                // Check if the subitemList is not null and not empty
                subitemList?.let { list ->
                    if (list.isNotEmpty()) {
                        val mutableList = list.toMutableList()  // Convert to mutable list
                        mutableList.removeAt(0)  // Remove the first item
                        subitemList = mutableList.toList()  // Convert back to immutable list
                    }
                }
                subitemList = subitemList?.reversed()


//                if(subitemList!!.size==0){
//                    view.notrackingheader.visibility= View.VISIBLE
//                }else{
//                    notrackingheader.visibility= View.GONE
//                }


                 adapter = CustomAdapater(
                     false,
                     arguments?.getString("watchNAME")!!,
                     activity,
                     subitemList!!
                 )
                imageRecyclerView!!.adapter = adapter
                imageRecyclerView!!.layoutManager = LinearLayoutManager(activity)

                imageRecyclerView!!.adapter = adapter
                adapter!!.setOnDeleteClickListener(this)
                Toast.makeText(activity,"Record deleted successfully.", Toast.LENGTH_LONG).show()
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
}