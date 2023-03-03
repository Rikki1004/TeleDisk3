package com.rikkimikki.teledisk.presentation.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentBottomFileActionTransferBinding
import com.rikkimikki.teledisk.databinding.FragmentListFilesBinding


class BottomFileActionTransferFragment : Fragment() {
    private var _binding: FragmentBottomFileActionTransferBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ListFileViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomFileActionTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ListFileViewModel::class.java]

        with(binding){
            textViewBottomPanelPaste.setOnClickListener { viewModel.copyFile(); close() }
            textViewBottomPanelCancel.setOnClickListener { viewModel.refresh() ; close() }
            textViewBottomPanelCreate .setOnClickListener {  }
        }
    }
    private fun close(){
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.bottom_view_container,BottomFileActionsFragment.newInstance(true))
            .commit()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = BottomFileActionTransferFragment()
    }
}