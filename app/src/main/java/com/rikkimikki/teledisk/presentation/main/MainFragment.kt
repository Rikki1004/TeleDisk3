package com.rikkimikki.teledisk.presentation.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.databinding.FragmentMainBinding
import com.rikkimikki.teledisk.domain.FiltersFromType
import com.rikkimikki.teledisk.domain.ScopeType
import com.rikkimikki.teledisk.presentation.login.LoginViewModel
import com.rikkimikki.teledisk.presentation.login.MainLoginFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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

        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            TelegramRepository.getAllChats()
        }
        //view.findNavController().navigate(R.id.viewTransactionsAction)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    fun initClickListeners(){
        with(binding){
            textViewTopPanelApps.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections
                        .actionMainFragmentToListFilesFragment(
                            ScopeType.Local,FiltersFromType.APPS))
            }
            textViewTopPanelDocs.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections
                        .actionMainFragmentToListFilesFragment(
                            ScopeType.Local,FiltersFromType.DOCUMENTS))
            }
            textViewTopPanelImages.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections
                        .actionMainFragmentToListFilesFragment(
                            ScopeType.Local,FiltersFromType.PHOTO))
            }
            textViewTopPanelMusic.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections
                        .actionMainFragmentToListFilesFragment(
                            ScopeType.Local,FiltersFromType.MUSIC))
            }
            textViewTopPanelVideo.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections
                        .actionMainFragmentToListFilesFragment(
                            ScopeType.Local,FiltersFromType.VIDEO))
            }

            //val navHostFragment = requireActivity().supportFragmentManager.primaryNavigationFragment //findFragmentById(R.id.main_view_container) as MainFragment

            constraintLayoutStorageMain.setOnClickListener {

                findNavController()
                    //.navigate(R.id.action_mainFragment_to_listFilesFragment)
                    .navigate(MainFragmentDirections.actionMainFragmentToListFilesFragment(ScopeType.Local,FiltersFromType.DEFAULT))
                /*requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.main_view_container,ListFilesFragment.newInstance(ScopeType.Local))
                    .addToBackStack(null)
                .commit()*/
            }
            constraintLayoutStorageSd.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections.actionMainFragmentToListFilesFragment(ScopeType.VkMsg,FiltersFromType.DEFAULT))
                /*requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.main_view_container,ListFilesFragment.newInstance(ScopeType.VkMsg))
                    .addToBackStack(null)
                    .commit()*/
            }
            constraintLayoutStorageTd.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections.actionMainFragmentToListFilesFragment(ScopeType.TeleDisk,FiltersFromType.DEFAULT))
                /*requireActivity().supportFragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.main_view_container,ListFilesFragment.newInstance(ScopeType.TeleDisk))
                .commit()*/
            }

        }
    }

    companion object {
        fun newInstance() = MainFragment()
    }
}