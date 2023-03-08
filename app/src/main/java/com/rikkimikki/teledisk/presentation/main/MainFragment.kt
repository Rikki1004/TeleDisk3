package com.rikkimikki.teledisk.presentation.main

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentMainBinding
import com.rikkimikki.teledisk.domain.FiltersFromType
import com.rikkimikki.teledisk.domain.PlaceItem
import com.rikkimikki.teledisk.domain.ScopeType
import com.rikkimikki.teledisk.domain.TdObject
import com.rikkimikki.teledisk.utils.GLOBAL_MAIN_STORAGE_PATH
import com.rikkimikki.teledisk.utils.GLOBAL_REMOTE_STORAGE_PATH
import com.rikkimikki.teledisk.utils.isNightModeEnabled
import com.rikkimikki.teledisk.utils.setIsNightModeEnabled


class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter : PlaceAdapter
    private lateinit var viewModel: ListFileViewModel
    private var chatsList = mutableListOf<Long>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ListFileViewModel::class.java]
        initClickListeners()

        val navView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
        val drawerSwitch: SwitchMaterial =
            navView.menu.findItem(R.id.dark_theme_switch).actionView as SwitchMaterial

        drawerSwitch.isChecked = isNightModeEnabled(requireActivity().application)
        drawerSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setIsNightModeEnabled(requireActivity().application, isChecked)
                requireActivity().recreate()
            }
        }

        navView.setNavigationItemSelectedListener {
            if (it.groupId == GROUP_ID) {
                viewModel.currentGroup = chatsList[it.itemId]
            }
            return@setNavigationItemSelectedListener true
        }

        viewModel.getChats().observe(viewLifecycleOwner) {
            val menu = navView.menu
            val submenu: Menu = menu.findItem(R.id.disk_container).subMenu
            submenu.clear()

            for (i in it.indices) {
                val a = submenu.add(GROUP_ID, i, Menu.NONE, "")
                chatsList.add(it[i].first)

                val s = SpannableString(it[i].second)
                s.setSpan(
                    ForegroundColorSpan(Color.parseColor(NEON_COLOR)),
                    0,
                    s.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                a.title = s

            }
            navView.invalidate()
        }

        adapter = PlaceAdapter(requireContext())

        adapter.onPlaceClickListener = object : PlaceAdapter.OnPlaceClickListener{
            override fun onPlaceClick(placeItem: PlaceItem) {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                placeItem.scopeType, FiltersFromType.DEFAULT,placeItem.path
                            )
                    )
            }
        }
        binding.horizontalRecycleView.layoutManager = LinearLayoutManager(requireActivity()).apply { orientation = LinearLayoutManager.HORIZONTAL }
        binding.horizontalRecycleView.adapter = adapter

        adapter.placeItemList = viewModel.getStorages()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun initClickListeners() {
        with(binding) {
            textViewPhoneSearch.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.Local, FiltersFromType.ALL_LOCAL,GLOBAL_MAIN_STORAGE_PATH
                            )
                    )
            }
            textViewTelediskSearch.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.TeleDisk, FiltersFromType.ALL_REMOTE, GLOBAL_REMOTE_STORAGE_PATH
                            )
                    )
            }
            textViewTopPanelApps.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.Local, FiltersFromType.APPS,GLOBAL_MAIN_STORAGE_PATH
                            )
                    )
            }
            textViewTopPanelDocs.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.Local, FiltersFromType.DOCUMENTS,GLOBAL_MAIN_STORAGE_PATH
                            )
                    )
            }
            textViewTopPanelImages.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.Local, FiltersFromType.PHOTO,GLOBAL_MAIN_STORAGE_PATH
                            )
                    )
            }
            textViewTopPanelMusic.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.Local, FiltersFromType.MUSIC,GLOBAL_MAIN_STORAGE_PATH
                            )
                    )
            }
            textViewTopPanelVideo.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.Local, FiltersFromType.VIDEO,GLOBAL_MAIN_STORAGE_PATH
                            )
                    )
            }


            /*constraintLayoutStorageMain.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.Local, FiltersFromType.DEFAULT
                            )
                    )
            }
            constraintLayoutStorageSd.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.Sd, FiltersFromType.DEFAULT
                            )
                    )
            }
            constraintLayoutStorageTd.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.TeleDisk, FiltersFromType.DEFAULT
                            )
                    )
            }*/
            /*constraintLayoutStorageVk.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections
                        .actionMainFragmentToListFilesFragment(
                            ScopeType.VkMsg,FiltersFromType.DEFAULT))
            }*/
        }
    }

    companion object {
        const val GROUP_ID = 10
        const val NEON_COLOR = "#03DAC5"
    }
}