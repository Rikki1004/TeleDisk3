package com.rikkimikki.teledisk.presentation.main

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.input
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.local.FileBackgroundTransfer
import com.rikkimikki.teledisk.databinding.FragmentListFilesBinding
import com.rikkimikki.teledisk.domain.baseClasses.*
import com.rikkimikki.teledisk.presentation.adapters.ListFilesAdapter
import com.rikkimikki.teledisk.utils.findIndex
import com.rikkimikki.teledisk.utils.saveCount

class ListFilesFragment : Fragment() {
    private var _binding: FragmentListFilesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ListFilesAdapter
    private lateinit var viewModel: ListFileViewModel
    private val args by navArgs<ListFilesFragmentArgs>()

    private var filter: (list: List<TdObject>) -> List<TdObject> = { it } //initially, the filter returns the list without changing it
    private var filterReversed: Boolean = false
    private var lastFilter: Int = -1
    private var selectMode: Boolean = false
    private val actionsView by lazy { requireActivity().findViewById<FragmentContainerView>(R.id.bottom_view_container) }

    private val bp1 by lazy { actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelCopy)}
    private val bp2 by lazy { actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelMove)}
    private val bp3 by lazy { actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelDelete)}
    private val bp4 by lazy { actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelRename)}
    private val bp5 by lazy { actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelMore)}

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SELECT_MODE, selectMode)
        if (adapter.currentList.size < 1000)
            outState.putParcelableArray(SAVE_LIST, adapter.currentList.toTypedArray())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ListFileViewModel::class.java]
        initBackPressed()
        initClickListeners()
        initAdapter()
        initObservers()
        toolBarSettings()

        if (savedInstanceState == null || !savedInstanceState.containsKey(SAVE_LIST)) {
            init()
        } else {
            val li = savedInstanceState.getParcelableArray(SAVE_LIST) as Array<TdObject>
            adapter.submitList(null)
            adapter.submitList(li.toMutableList())
            binding.loadDataProgressBar.visibility = View.GONE

            if (savedInstanceState.getBoolean(SELECT_MODE)){
                changeToolbarSelectMode(true)
            }
        }
    }

    private fun init() {
        viewModel.setLocalPath(args.path)

        when (args.filter) {
            FiltersFromType.DEFAULT -> {
                when (args.scopeType) {
                    ScopeType.TeleDisk -> viewModel.getRemoteFiles(args.path)
                    ScopeType.Local -> viewModel.getLocalFiles(args.path)
                    ScopeType.Sd -> viewModel.getLocalFiles(args.path)
                    ScopeType.VkMsg -> {}
                }
            }
            FiltersFromType.ALL_REMOTE -> {
                viewModel.getRemoteFilesFiltered(args.filter, args.path)
            }
            FiltersFromType.ALL_LOCAL -> {
                viewModel.getLocalFilesFiltered(args.filter, args.path)
            }
            else -> {
                viewModel.getLocalFilesFiltered(args.filter, args.path)
            }
        }
    }

    private fun initObservers() {
        viewModel.needHideSelect.observe(viewLifecycleOwner) {
            deselect()
        }
        viewModel.needCancelSelect.observe(viewLifecycleOwner) {
            changeToolbarSelectMode(false)
        }
        viewModel.needLaunchIntent.observe(viewLifecycleOwner) {
            startActivity(it)
        }
        viewModel.needPressBackButton.observe(viewLifecycleOwner) {
            requireActivity().onBackPressed()
        }


        viewModel.getNeedOpenLD().observe(viewLifecycleOwner) {
            Toast.makeText(
                requireContext(),
                getString(R.string.operation_completed_successfully) + it.first,
                Toast.LENGTH_SHORT
            ).show()
            if (it.second)
                viewModel.openLocalFile(it.first)
            else
                viewModel.refresh()
        }

        viewModel.fileScope.observe(viewLifecycleOwner) {
            initTopToolbar(it)
            binding.loadDataProgressBar.visibility = View.GONE
            val li = filter(it).toMutableList()
            adapter.submitList(li)
            saveCount(requireContext(),args.filter,li.size)
        }
    }

    private fun initAdapter() {
        adapter = ListFilesAdapter(requireContext())
        adapter.onFileLongClickListener = object : ListFilesAdapter.OnFileLongClickListener {
            override fun onFileLongClick(tdObject: TdObject) {
                if (!viewModel.is_copy_mode)
                    checkedItemsProcessing(tdObject)
            }
        }

        adapter.onFileClickListener = object : ListFilesAdapter.OnFileClickListener {
            override fun onFileClick(tdObject: TdObject) {
                if (selectMode) {
                    checkedItemsProcessing(tdObject)
                    return
                }
                if (tdObject.is_folder()) {
                    viewModel.changeDirectory(tdObject)
                }

                if (tdObject.is_file()) {
                    if (tdObject.placeType == PlaceType.TeleDisk) {
                        val startIntent =
                            FileBackgroundTransfer.getIntent(requireActivity(), tdObject)
                        ContextCompat.startForegroundService(requireActivity(), startIntent)
                    }
                    if (tdObject.placeType == PlaceType.Local) {
                        viewModel.openLocalFile(tdObject.path)
                    }
                }
            }
        }
        binding.recycleViewListFiles.layoutManager = LinearLayoutManager(requireActivity()).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        binding.recycleViewListFiles.adapter = adapter
    }

    private fun initClickListeners() {
        binding.buttomDeselect.setOnClickListener {
            changeToolbarSelectMode(false)
        }

        binding.toolBarAllChecked.setOnClickListener {
            val li = adapter.currentList.toList()

            if (li.size == 1) {
                if (!li[0].isChecked)
                    checkedItemsProcessing(li[0])
                return@setOnClickListener
            }

            val li2 = mutableListOf<TdObject>()
            for (i in li) {
                li2.add(i.copy(isChecked = true))
            }

            adapter.submitList(li2)

            viewModel.selectedItems.removeAll { true }
            viewModel.selectedItems.addAll(li2)

            configureBottomNavigation(li2)
        }
    }

    private fun initBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this.isEnabled = false

                viewModel.currentDirectory = TdObject("noDir", PlaceType.Local,
                    FileType.Folder,"*")

                if (selectMode){
                    actionsView.visibility = View.GONE
                    viewModel.selectedItems.clear()
                }

                requireActivity().onBackPressed()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (selectMode) {
            configureBottomNavigation()
        }
        if (viewModel.is_copy_mode){
            actionsView.visibility = View.VISIBLE
        }
        notifyCounter()
        initTopToolbar()
    }

    private fun initTopToolbar(list: List<TdObject> = adapter.currentList.toList()){
        binding.pathTextView.text = viewModel.currentDirectory.path
        val count = list.size
        binding.toolBarTextViewCount.text = requireActivity().getString(
            R.string.filter_menu_count_items,
            count.toString()
        )
    }

    private fun notifyCounter() {
        binding.toolBarTextViewCountChecked.text = if (viewModel.selectedItems.isEmpty()) {
            getString(R.string.no_items_selected)
        } else {
            getString(R.string.items_selected) + viewModel.selectedItems.size
        }
    }

    private fun toolBarSettings() {
        val toolbar = binding.toolbar
        val infoToolbar = binding.infoToolbar
        val searchBar = toolbar.menu.findItem(R.id.action_search).actionView as SearchView

        infoToolbar.overflowIcon =
            AppCompatResources.getDrawable(requireContext(),R.drawable.arrow_down_drop_circle_outline_custom)

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_18dp)
        toolbar.setNavigationOnClickListener {
            viewModel.clickArrow(args.path)
        }


        searchBar.setOnCloseListener {
            if (adapter.layoutManagerType == adapter.MANAGER_GRID)
                toolbar.menu.findItem(R.id.action_layout_linear).isVisible = true
            else
                toolbar.menu.findItem(R.id.action_layout_grid).isVisible = true
            return@setOnCloseListener false
        }
        searchBar.setOnSearchClickListener {
            toolbar.menu.findItem(R.id.action_layout_grid).isVisible = false
            toolbar.menu.findItem(R.id.action_layout_linear).isVisible = false
        }

        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                adapter.filter1(p0)
                return false
            }
        })


        infoToolbar.setOnMenuItemClickListener {
            //zeroing the name (it can be with an arrow)
            when (lastFilter) {
                R.id.action_filter_size -> {
                    infoToolbar.menu.findItem(lastFilter).title =
                        getString(R.string.filter_menu_size)
                }
                R.id.action_filter_name -> {
                    infoToolbar.menu.findItem(lastFilter).title =
                        getString(R.string.filter_menu_name)
                }
                R.id.action_filter_type -> {
                    infoToolbar.menu.findItem(lastFilter).title =
                        getString(R.string.filter_menu_type)
                }
                R.id.action_filter_time -> {
                    infoToolbar.menu.findItem(lastFilter).title =
                        getString(R.string.filter_menu_time)
                }
            }
            when (it.itemId) {
                R.id.action_filter_size -> {

                    filterReversed = if (lastFilter == it.itemId) !filterReversed else false

                    val newTitle = if (filterReversed)
                        getString(R.string.filter_menu_arrow_up) + getString(R.string.filter_menu_size)
                    else
                        getString(R.string.filter_menu_arrow_down) + getString(R.string.filter_menu_size)

                    val s = SpannableString(newTitle)
                    s.setSpan(
                        ForegroundColorSpan(Color.parseColor(SELECTED_COLOR)),
                        0,
                        s.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    it.title = s
                    binding.toolBarTextViewFilter.text = newTitle

                    filter =
                        { items ->
                            val files = items.filter {item -> item.is_file() }
                            val folders = items.filter {item -> !item.is_file() }

                            val filteredFolders = folders.sortedBy { item -> item.size }
                            val filteredFiles = if (filterReversed)
                                files.sortedByDescending { item -> item.size }
                            else
                                files.sortedBy { item -> item.size }

                            filteredFolders + filteredFiles
                        }
                }

                R.id.action_filter_name -> {

                    filterReversed = if (lastFilter == it.itemId) !filterReversed else false

                    val newTitle = if (filterReversed)
                        getString(R.string.filter_menu_arrow_up) + getString(R.string.filter_menu_name)
                    else
                        getString(R.string.filter_menu_arrow_down) + getString(R.string.filter_menu_name)

                    val s = SpannableString(newTitle)
                    s.setSpan(
                        ForegroundColorSpan(Color.parseColor(SELECTED_COLOR)),
                        0,
                        s.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    it.title = s
                    binding.toolBarTextViewFilter.text = newTitle

                    filter =
                        { items ->
                            val files = items.filter { item -> item.is_file() }
                            val folders = items.filter { item -> !item.is_file() }

                            val filteredFolders = folders.sortedBy { item -> item.name }
                            val filteredFiles = if (filterReversed)
                                files.sortedByDescending { item -> item.name }
                            else
                                files.sortedBy { item -> item.name }

                            filteredFolders + filteredFiles
                        }
                }

                R.id.action_filter_time -> {

                    filterReversed = if (lastFilter == it.itemId) !filterReversed else false

                    val newTitle = if (filterReversed)
                        getString(R.string.filter_menu_arrow_up) + getString(R.string.filter_menu_time)
                    else
                        getString(R.string.filter_menu_arrow_down) + getString(R.string.filter_menu_time)

                    val s = SpannableString(newTitle)
                    s.setSpan(
                        ForegroundColorSpan(Color.parseColor(SELECTED_COLOR)),
                        0,
                        s.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    it.title = s
                    binding.toolBarTextViewFilter.text = newTitle

                    filter =
                        { items ->
                            val files = items.filter { item -> item.is_file() }
                            val folders = items.filter { item -> !item.is_file() }

                            val filteredFolders = folders.sortedBy { item -> item.unixTimeDate }
                            val filteredFiles = if (filterReversed)
                                files.sortedByDescending { item -> item.unixTimeDate }
                            else
                                files.sortedBy { item -> item.unixTimeDate }

                            filteredFolders + filteredFiles
                        }
                }

                R.id.action_filter_type -> {

                    filterReversed = if (lastFilter == it.itemId) !filterReversed else false

                    val newTitle = if (filterReversed)
                        getString(R.string.filter_menu_arrow_up) + getString(R.string.filter_menu_type)
                    else
                        getString(R.string.filter_menu_arrow_down) + getString(R.string.filter_menu_type)

                    val s = SpannableString(newTitle)
                    s.setSpan(
                        ForegroundColorSpan(Color.parseColor(SELECTED_COLOR)),
                        0,
                        s.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    it.title = s
                    binding.toolBarTextViewFilter.text = newTitle

                    filter =
                        { items ->
                            val files = items.filter { item -> item.is_file() }
                            val folders = items.filter { item -> !item.is_file() }

                            val filteredFolders = folders.sortedBy { item -> item.name }
                            val filteredFiles = if (filterReversed)
                                files.sortedByDescending { item -> item.name.substringAfterLast(".") }
                            else
                                files.sortedBy { item -> item.name.substringAfterLast(".") }

                            filteredFolders + filteredFiles
                        }
                }
            }
            lastFilter = it.itemId
            if (args.filter == FiltersFromType.DEFAULT)
                viewModel.refresh()
            else
                init()
            true
        }

        //changing the items display type
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_layout_grid -> {
                    adapter.layoutManagerType = adapter.MANAGER_GRID
                    binding.recycleViewListFiles.layoutManager =
                        GridLayoutManager(requireActivity(), 6)
                    toolbar.menu.findItem(R.id.action_layout_linear).isVisible = true
                    it.isVisible = false
                }
                R.id.action_layout_linear -> {
                    adapter.layoutManagerType = adapter.MANAGER_LINEAR
                    binding.recycleViewListFiles.layoutManager =
                        LinearLayoutManager(requireActivity())
                    toolbar.menu.findItem(R.id.action_layout_grid).isVisible = true
                    it.isVisible = false
                }
                R.id.action_new_folder -> {
                    createFolderDialog()
                }
            }
            true
        }
    }


    private fun checkedItemsProcessing(tdObject: TdObject) {
        //turn on the selection mode if it is not enabled and create a new reversible TdObject
        if (!selectMode)
            changeToolbarSelectMode(true)
        val newObj = tdObject.copy(isChecked = !tdObject.isChecked)

        //add the selected elements to the ViewModel or delete them if they are already there
        with(viewModel.selectedItems) {
            if (!tdObject.isChecked && findIndex(tdObject, this) == null)
                add(newObj)
            else
                findIndex(tdObject, this)?.let { removeAt(it) }
        }

        //we replace them in the adapter
        val li = adapter.currentList.toMutableList()
        findIndex(tdObject, li)?.let {
            li[it] = newObj
            adapter.submitList(li)
        }
        configureBottomNavigation(li)
        //we adjust the upper and lower panels according to the choice
    }
    private fun configureBottomNavigation(li:List<TdObject> = adapter.currentList.toMutableList()){
        val oneChecked = li.filter { it.isChecked }.size == 1
        val zeroChecked = li.all { !it.isChecked }
        if (oneChecked || zeroChecked) {
            val color = AppCompatResources.getDrawable(
                requireContext(),
                if (oneChecked) R.color.colorMainBackground
                else R.color.colorStoreBackgroundOther
            )
            bp1.isClickable = oneChecked; bp1.background = color
            bp2.isClickable = oneChecked; bp2.background = color
            bp3.isClickable = oneChecked; bp3.background = color
            bp4.isClickable = oneChecked; bp4.background = color
            bp5.isClickable = oneChecked; bp5.background = color
        } else {
            bp4.isClickable =
                false
            bp4.background =
                AppCompatResources.getDrawable(
                    requireContext(),
                    R.color.colorStoreBackgroundOther
                )
        }
        notifyCounter()
    }

    private fun changeToolbarSelectMode(selectModeOn: Boolean) {
        if (selectModeOn) {
            selectMode = true
            actionsView.visibility = View.VISIBLE
            binding.selectToolbar.visibility = View.VISIBLE
            binding.toolbar.visibility = View.GONE
        } else {
            actionsView.visibility = View.GONE
            viewModel.selectedItems.clear()
            deselect()
        }
    }

    private fun deselect() {
        selectMode = false
        binding.selectToolbar.visibility = View.GONE
        binding.toolbar.visibility = View.VISIBLE

        val li = adapter.currentList
        val li2 = li.map { it.copy(isChecked = false) }
        adapter.submitList(li2.toMutableList())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    @SuppressLint("CheckResult")
    private fun createFolderDialog() {
        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.new_folder)
            positiveButton(R.string.create)
            negativeButton(R.string.cancel)
            input(
                hint = getString(R.string.new_folder_hint),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                waitForPositiveButton = true
            ) { _, text -> viewModel.createFolder(text.toString()) }
        }
    }

    companion object {
        const val SELECT_MODE = "selectMode"
        const val SAVE_LIST = "list"
        const val SELECTED_COLOR = "#ABCABC"
    }
}