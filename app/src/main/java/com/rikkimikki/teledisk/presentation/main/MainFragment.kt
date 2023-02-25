package com.rikkimikki.teledisk.presentation.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentMainBinding
import com.rikkimikki.teledisk.domain.ScopeType
import com.rikkimikki.teledisk.presentation.login.LoginViewModel
import com.rikkimikki.teledisk.presentation.login.MainLoginFragment

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        initClickListeners()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    fun initClickListeners(){
        with(binding){
            textViewTopPanelApps.setOnClickListener {  }
            textViewTopPanelDocs.setOnClickListener {  }
            textViewTopPanelImages.setOnClickListener {  }
            textViewTopPanelMusic.setOnClickListener {  }
            textViewTopPanelVideo.setOnClickListener {  }

            constraintLayoutStorageMain.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.main_view_container,ListFilesFragment.newInstance(ScopeType.Local))
                .commit()
            }
            constraintLayoutStorageSd.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.main_view_container,ListFilesFragment.newInstance(ScopeType.TeleDisk))
                    .commit()
            }
            constraintLayoutStorageTd.setOnClickListener { requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.main_view_container,ListFilesFragment.newInstance(ScopeType.VkMsg))
                .commit()
            }

        }
    }

    companion object {
        fun newInstance() = MainLoginFragment()
    }
}