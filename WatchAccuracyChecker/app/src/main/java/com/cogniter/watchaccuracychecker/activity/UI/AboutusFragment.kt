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
import com.cogniter.watchaccuracychecker.databinding.ActivityMainBinding


class AboutusFragment : Fragment() {

    private var _binding: AboutusFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // ✅ Inflate ONLY using ViewBinding
        _binding = AboutusFragmentBinding.inflate(inflater, container, false)

        (activity as? MainActivity)
            ?.findViewById<ImageView>(R.id.backButton)
            ?.visibility = View.GONE

        (activity as? MainActivity)
            ?.findViewById<LinearLayout>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        binding.projectWebView.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.cogniter.com/requestaquote.aspx")
            )
            startActivity(browserIntent)
        }

        // ✅ IMPORTANT
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

