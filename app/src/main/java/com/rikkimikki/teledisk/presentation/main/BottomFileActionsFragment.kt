package com.rikkimikki.teledisk.presentation.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentBottomFileActionTransferBinding
import com.rikkimikki.teledisk.databinding.FragmentBottomFileActionsBinding

class BottomFileActionsFragment : Fragment() {
    private var _binding: FragmentBottomFileActionsBinding? = null
    private val binding get() = _binding!!
    private val actionsView by lazy { requireActivity().findViewById<FragmentContainerView>(R.id.bottom_view_container) }
    private lateinit var viewModel: ListFileViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomFileActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments?.getBoolean(EXTRA_CLOSE) == true)
            actionsView.visibility = View.GONE

        viewModel = ViewModelProvider(requireActivity())[ListFileViewModel::class.java]

        with(binding){
            textViewBottomPanelCopy.setOnClickListener {
                viewModel.refresh()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.bottom_view_container,BottomFileActionTransferFragment.newInstance(true))
                    .commit()
            }
            textViewBottomPanelMove.setOnClickListener {
                viewModel.refresh()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.bottom_view_container,BottomFileActionTransferFragment.newInstance(false))
                    .commit()
            }
            textViewBottomPanelDelete.setOnClickListener { viewModel.deleteItem() }
            textViewBottomPanelRename.setOnClickListener { viewModel.renameItem("newFolder")}//"newName.txt"
            textViewBottomPanelShare.setOnClickListener {
                //requireActivity().startActivity(viewModel.shareLocalFile())
                viewModel.shareItems()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val EXTRA_CLOSE = "CLOSE"
        fun newInstance(close:Boolean) = BottomFileActionsFragment().apply {
            arguments = Bundle().apply {
                putBoolean(EXTRA_CLOSE,close)
            }
        }
    }
}