package com.cogniter.watchaccuracychecker.activity.UI

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.databinding.MywatchlistingBinding
import com.cogniter.watchaccuracychecker.databinding.SettingsFragmentBinding
import com.cogniter.watchaccuracychecker.utills.ImageUtils


class SettingsFragment : Fragment() {

    private var _binding: SettingsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.settings_fragment, container, false)

        _binding = SettingsFragmentBinding.inflate(inflater, container, false)


        if(ImageUtils.getNotifcationTimeFromSharedPreferences(activity!!,"notificationTime",2)==2){
            binding.speedText.text="2 hrs"
        }else if(ImageUtils.getNotifcationTimeFromSharedPreferences(activity!!,"notificationTime",2)==4){
            binding.speedText.text="4 hrs"
        }else if(ImageUtils.getNotifcationTimeFromSharedPreferences(activity!!,"notificationTime",2)==6){
            binding.speedText.text="6 hrs"
        }else if(ImageUtils.getNotifcationTimeFromSharedPreferences(activity!!,"notificationTime",2)==8){
            binding.speedText.text="8 hrs"
        }

        binding.speedText.setOnClickListener {
            speedSelectedDialog(view)
        }
        return binding.root
    }
    fun speedSelectedDialog(view: View) {
        val dialog = Dialog(activity!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_select_speed)
        val interval1 = dialog.findViewById<View>(R.id.interval1) as TextView
        val interval2 = dialog.findViewById<View>(R.id.interval2) as TextView
        val interval3 = dialog.findViewById<View>(R.id.interval3) as TextView
        val interval4 = dialog.findViewById<View>(R.id.interval4) as TextView


        interval1.setOnClickListener(View.OnClickListener {
            ImageUtils.saveNotifcationTimeToSharedPreferences(activity!!,"notificationTime",2)
            binding.speedText.text="2 hrs"
            dialog.dismiss()
        })
        interval2.setOnClickListener(View.OnClickListener {
            ImageUtils.saveNotifcationTimeToSharedPreferences(activity!!,"notificationTime",4)
            binding.speedText.text="4 hrs"
            dialog.dismiss()
        })
        interval3.setOnClickListener(View.OnClickListener {
            ImageUtils.saveNotifcationTimeToSharedPreferences(activity!!,"notificationTime",6)
            binding.speedText.text="6 hrs"
            dialog.dismiss()
        })
        interval4.setOnClickListener(View.OnClickListener {
            ImageUtils.saveNotifcationTimeToSharedPreferences(activity!!,"notificationTime",8)
            binding.speedText.text="8 hrs"
            dialog.dismiss()
        })

        dialog.show()
    }
}
