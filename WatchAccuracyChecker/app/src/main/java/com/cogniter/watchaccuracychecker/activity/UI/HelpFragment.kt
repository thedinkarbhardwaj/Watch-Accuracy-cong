package com.cogniter.watchaccuracychecker.activity.UI

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.cogniter.watchaccuracychecker.R
import com.cogniter.watchaccuracychecker.activity.MainActivity
import com.cogniter.watchaccuracychecker.databinding.AboutusFragmentBinding
import com.cogniter.watchaccuracychecker.databinding.HelpFragmentBinding


class HelpFragment : Fragment() {

    private var _binding: HelpFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.help_fragment, container, false)

        _binding = HelpFragmentBinding.inflate(inflater, container, false)

        (activity as? MainActivity)?.findViewById<ImageView>(R.id.backButton)?.visibility = View.GONE
        (activity as? MainActivity)?.findViewById<LinearLayout>(R.id.bottomNav)?.visibility = View.VISIBLE

        return binding.root

    }
}
