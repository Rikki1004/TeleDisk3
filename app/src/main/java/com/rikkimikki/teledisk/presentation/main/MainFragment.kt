package com.rikkimikki.teledisk.presentation.main

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.input
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentMainBinding
import com.rikkimikki.teledisk.domain.baseClasses.FiltersFromType
import com.rikkimikki.teledisk.domain.baseClasses.PlaceItem
import com.rikkimikki.teledisk.domain.baseClasses.ScopeType
import com.rikkimikki.teledisk.utils.*


class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter : PlaceAdapter
    private lateinit var viewModel: ListFileViewModel
    private var chatsList = mutableListOf<Long>()
    private val actionsView by lazy { requireActivity().findViewById<FragmentContainerView>(R.id.bottom_view_container) }
    private lateinit var navView : NavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ListFileViewModel::class.java]
        navView = requireActivity().findViewById(R.id.nav_view)

        if (viewModel.is_copy_mode){
            actionsView.visibility = View.VISIBLE
        }

        setCount()
        adapterSettings()
        initSwitch()
        initClickListeners()
        initObservers()
    }

    private fun adapterSettings() {
        adapter = PlaceAdapter(requireContext())
        binding.horizontalRecycleView.layoutManager = LinearLayoutManager(requireActivity()).apply { orientation = LinearLayoutManager.HORIZONTAL }
        binding.horizontalRecycleView.adapter = adapter
        adapter.placeItemList = viewModel.getStorages()
    }

    private fun initSwitch() {
        val drawerSwitch: SwitchMaterial =
            navView.menu.findItem(R.id.dark_theme_switch).actionView as SwitchMaterial

        drawerSwitch.isChecked = isNightModeEnabled(requireActivity().application)
        drawerSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setIsNightModeEnabled(requireActivity().application, isChecked)
                requireActivity().recreate()
            }
        }
    }

    private fun setCount() {
        binding.textViewTopPanelDocsCount.text = getCount(requireContext(), FiltersFromType.DOCUMENTS)
        binding.textViewTopPanelAppsCount.text = getCount(requireContext(), FiltersFromType.APPS)
        binding.textViewTopPanelArchivesCount.text = getCount(requireContext(), FiltersFromType.ARCHIVES)
        binding.textViewTopPanelImagesCount.text = getCount(requireContext(), FiltersFromType.PHOTO)
        binding.textViewTopPanelMusicCount.text = getCount(requireContext(), FiltersFromType.MUSIC)
        binding.textViewTopPanelVideoCount.text = getCount(requireContext(), FiltersFromType.VIDEO)
    }

    private fun initObservers() {
        navView.setNavigationItemSelectedListener {
            if (it.groupId == GROUP_ID) {
                viewModel.currentGroup = chatsList[it.itemId]
            }
            if (it.groupId == GROUP_ID_ADD_GROUP) {
                createFolderDialog()
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
            submenu.add(GROUP_ID_ADD_GROUP, 0, Menu.NONE, getString(R.string.create_group_menu))
            navView.invalidate()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun checkGroup():Boolean{
        if (viewModel.currentGroup == ListFileViewModel.NO_GROUP){
            Toast.makeText(requireContext(), getString(R.string.first_select_a_group), Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }

    private fun initClickListeners() {

        adapter.onPlaceClickListener = object : PlaceAdapter.OnPlaceClickListener{
            override fun onPlaceClick(placeItem: PlaceItem) {
                if (checkGroup()) return
                if (placeItem.scopeType == ScopeType.VkMsg){
                    Toast.makeText(requireContext(), getString(R.string.not_available_yet), Toast.LENGTH_SHORT).show()
                    return
                }
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                placeItem.scopeType, FiltersFromType.DEFAULT,placeItem.path
                            )
                    )
            }
        }
        with(binding) {

            imageViewOpenDrawer.setOnClickListener {
                requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout).openDrawer(
                    GravityCompat.START)
            }
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
                if (checkGroup()) return@setOnClickListener
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
            textViewTopPanelArchives.setOnClickListener {
                findNavController()
                    .navigate(
                        MainFragmentDirections
                            .actionMainFragmentToListFilesFragment(
                                ScopeType.Local, FiltersFromType.ARCHIVES,GLOBAL_MAIN_STORAGE_PATH
                            )
                    )
            }
        }
    }

    private fun createFolderDialog() {
        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.new_folder)
            positiveButton(R.string.create)
            negativeButton(R.string.cancel)
            input(
                hint = getString(R.string.new_group_creating_title),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                waitForPositiveButton = true
            ) { _, text -> viewModel.createGroup(text.toString())}
        }
    }

    companion object {
        const val GROUP_ID = 10
        const val GROUP_ID_ADD_GROUP = 11
        const val NEON_COLOR = "#03DAC5"
    }
}