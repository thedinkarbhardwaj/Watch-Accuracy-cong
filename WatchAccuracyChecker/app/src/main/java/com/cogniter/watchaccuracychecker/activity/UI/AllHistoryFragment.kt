package com.cogniter.watchaccuracychecker.activity.UI

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.adapter.AlhistoryAdapater

import com.cogniter.watchaccuracychecker.database.DBHelper
import com.cogniter.watchaccuracychecker.model.ListItem
import com.cogniter.watchaccuracychecker.model.Subitem
import com.cogniter.watchaccuracychecker.utills.ImageUtils


class AllHistoryFragment : Fragment(), AlhistoryAdapater.OnallHistoryDeleteClickListener{
    private lateinit var dbHelper: DBHelper
    var itemList: List<ListItem>? = null
    lateinit var activity: Activity

    var imageRecyclerView: RecyclerView? = null
    var adapter:AlhistoryAdapater ? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.qll_history_detail_activity, container, false)
        activity = getActivity()!!
        (activity as? MainActivity)?.findViewById<ImageView>(R.id.backButton)?.visibility = View.GONE
        (activity as? MainActivity)?.findViewById<LinearLayout>(R.id.bottomNav)?.visibility = View.VISIBLE
        dbHelper = DBHelper(activity)


        itemList = dbHelper.getAllItems()
        itemList = itemList?.reversed()


//        if(itemList!!.size==0){
//            view.notrackingheader.visibility= View.VISIBLE
//
//        }else{
//
//            if(itemList!!.size>0 ){
//
//            }
//
//            view.notrackingheader.visibility= View.GONE
//
//        }


         imageRecyclerView= view.findViewById(R.id.imageRecyclerView)
         adapter = AlhistoryAdapater(activity,itemList!!)
        imageRecyclerView!!.adapter = adapter
        imageRecyclerView!!.layoutManager = LinearLayoutManager(activity)

        imageRecyclerView!!.adapter = adapter
        adapter!!.setOnallHistoryDeleteClickListener(this)

        return view
    }

    override fun OnallHistoryDelete(name: Subitem, i: Int) {
        deleteItemDialog(name)
    }

    fun deleteItemDialog(subitem: Subitem) {

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Delete")
            .setMessage("Are you sure to delete the record?")
            .setPositiveButton("Yes") { _, _ ->
                dbHelper.removeSubItem(subitem.subitemId)

                itemList = emptyList()
                itemList = dbHelper.getAllItems()
                itemList = itemList?.reversed()


//                if(itemList!!.size==0){
//                    view!!.notrackingheader.visibility= View.VISIBLE
//
//                }else{
//                    view!!.notrackingheader.visibility= View.GONE
//
//
//                }
                System.out.println("derrpdlpdld "+itemList!!.size)

                imageRecyclerView= view!!.findViewById(R.id.imageRecyclerView)
                adapter = AlhistoryAdapater(activity,itemList!!)
                imageRecyclerView!!.adapter = adapter
                imageRecyclerView!!.layoutManager = LinearLayoutManager(activity)

                imageRecyclerView!!.adapter = adapter
                adapter!!.setOnallHistoryDeleteClickListener(this)
                Toast.makeText(activity,"Record deleted successfully.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()

        if(ImageUtils.isDarkModeEnabled(activity)){
//            val buttonColor = ContextCompat.getColor(activity, R.color.white)
//            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(buttonColor);
//            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(buttonColor);
        }else{
//            val buttonColor = ContextCompat.getColor(activity, R.color.black)
//            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(buttonColor);
//            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(buttonColor);
        }

    }
}