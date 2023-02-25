package com.rikkimikki.teledisk.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.rikkimikki.teledisk.databinding.FragmentListFilesBinding
import com.rikkimikki.teledisk.domain.ScopeType
import com.rikkimikki.teledisk.presentation.login.LoginViewModel
import com.rikkimikki.teledisk.presentation.login.MainLoginFragment

class ListFilesFragment : Fragment() {
    private var _binding: FragmentListFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ListFileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ListFilesAdapter(requireContext())
        binding.recycleViewListFiles.layoutManager = GridLayoutManager(requireContext(),4)// LinearLayoutManager(context)
        binding.recycleViewListFiles.adapter = adapter
        viewModel = ViewModelProvider(this)[ListFileViewModel::class.java]

        viewModel.fileScope.observe(viewLifecycleOwner, Observer {
            adapter.submitList(null)
            adapter.submitList(it?.toMutableList())
        })

        viewModel.chatScope.observe(viewLifecycleOwner, Observer {
            println(it)
            viewModel.getRemoteFiles(-567578282,"/")
        })

        viewModel.getChats()

    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun getType(): ScopeType {
        return requireArguments().getSerializable(EXTRA_SCOPE_TYPE) as ScopeType
    }

    companion object {
        const val EXTRA_SCOPE_TYPE = "scopeType"
        fun newInstance(scopeType: ScopeType): Fragment {
            return ListFilesFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(EXTRA_SCOPE_TYPE, scopeType)
                }
            }
        }
    }
}