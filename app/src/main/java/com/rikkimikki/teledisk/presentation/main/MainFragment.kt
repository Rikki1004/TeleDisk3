package com.rikkimikki.teledisk.presentation.main

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.mlkit.common.sdkinternal.CommonUtils
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentMainBinding
import com.rikkimikki.teledisk.domain.FiltersFromType
import com.rikkimikki.teledisk.domain.ScopeType
import com.rikkimikki.teledisk.utils.isNightModeEnabled
import com.rikkimikki.teledisk.utils.isToogleEnabled
import com.rikkimikki.teledisk.utils.setIsNightModeEnabled
import com.rikkimikki.teledisk.utils.setIsToogleEnabled
import java.lang.ref.WeakReference


class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ListFileViewModel
    private lateinit var navView : NavigationView
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


        /*mySwitch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(p0: CompoundButton?, isChecked: Boolean) {
                Toast.makeText(requireContext(), "onCheck", Toast.LENGTH_SHORT).show()
            }
        })*/

        /*switch_btn.setOnClickListener(View.OnClickListener {
            if(isNightModeOn){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                switch_btn.text = "Enable Dark Mode"
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                switch_btn.text = "Disable Dark Mode"
            }
        })*/

        val drawerSwitch : SwitchMaterial = navView.getMenu().findItem(R.id.dark_theme_switch).getActionView() as SwitchMaterial

        //drawerSwitch.solidColor(resources.getColor(R.color.md_red))

        drawerSwitch.isChecked = isNightModeEnabled(requireActivity().application)

        drawerSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed){
                setIsNightModeEnabled(requireActivity().application, isChecked)
                requireActivity().recreate()
            }
        }

        navView.setNavigationItemSelectedListener {
            if (it.groupId == GROUP_ID){
                viewModel.currentGroup = chatsList[it.itemId]
            }
            return@setNavigationItemSelectedListener true
        }

        viewModel.getChats().observe(viewLifecycleOwner) {
            val menu = navView.menu
            val submenu: Menu = menu.findItem (R.id.disk_container).subMenu //menu.addSubMenu("Удаленные диски")
            submenu.clear()

            for(i in 0 until it.size){
                val a = submenu.add(GROUP_ID,i,Menu.NONE,"")
                chatsList.add(it[i].first)

                val s = SpannableString(it[i].second)
                s.setSpan(
                    ForegroundColorSpan(Color.parseColor("#03DAC5")),
                    0,
                    s.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                a.title = s

            }
            navView.invalidate()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    fun initClickListeners(){
        with(binding){
            textViewPhoneSearch.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections
                        .actionMainFragmentToListFilesFragment(
                            ScopeType.Local,FiltersFromType.ALL_LOCAL))
            }
            textViewTelediskSearch.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections
                        .actionMainFragmentToListFilesFragment(
                            ScopeType.Local,FiltersFromType.ALL_REMOTE))
            }
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
            constraintLayoutStorageVk.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections.actionMainFragmentToListFilesFragment(ScopeType.VkMsg,FiltersFromType.DEFAULT))
                /*requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.main_view_container,ListFilesFragment.newInstance(ScopeType.VkMsg))
                    .addToBackStack(null)
                    .commit()*/
            }
            constraintLayoutStorageSd.setOnClickListener {
                findNavController()
                    .navigate(MainFragmentDirections.actionMainFragmentToListFilesFragment(ScopeType.Sd,FiltersFromType.DEFAULT))
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
        const val GROUP_ID = 10
        fun newInstance() = MainFragment()
    }
}