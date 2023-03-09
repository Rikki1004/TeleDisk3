package com.rikkimikki.teledisk.presentation.main

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.local.FileBackgroundTransfer
import com.rikkimikki.teledisk.databinding.FragmentListFilesBinding
import com.rikkimikki.teledisk.domain.FiltersFromType
import com.rikkimikki.teledisk.domain.PlaceType
import com.rikkimikki.teledisk.domain.ScopeType
import com.rikkimikki.teledisk.domain.TdObject
import java.io.File

class ListFilesFragment : Fragment() {
    private var _binding: FragmentListFilesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter : ListFilesAdapter
    private lateinit var viewModel: ListFileViewModel
    private val args by navArgs<ListFilesFragmentArgs>()
    //private lateinit var actionsView = requireActivity().findViewById<FragmentContainerView>
    private val actionsView by lazy { requireActivity().findViewById<FragmentContainerView>(R.id.bottom_view_container) }

    private var filter: (list: List<TdObject>) -> List<TdObject> = {it}
    private var filterReversed: Boolean = false
    private var lastFilter: Int = -1
    private var selectMode : Boolean = false


    /*override fun onPause() {
        super.onPause()
        val mListState = binding.recycleViewListFiles.layoutManager?.onSaveInstanceState()
        mBundleRecyclerViewState.putParcelable("key", mListState)
    }

    override fun onResume() {
        super.onResume()
        if (mBundleRecyclerViewState != null) {
            Handler().postDelayed(Runnable {
                val mListState = mBundleRecyclerViewState.getParcelable<Parcelable>("key")
                binding.recycleViewListFiles.layoutManager?.onRestoreInstanceState(mListState)
            }, 50)
        }
        //binding.recycleViewListFiles.setLayoutManager(staggeredGridLayoutManager)
    }*/

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //val list : List<TdObject > = ArrayList<TdObject>()
        outState.putBoolean(SELECT_MODE,selectMode)
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

        /*requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,object :OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                *//*requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.main_view_container,MainFragment.newInstance())
                    .commit()*//*
                //requireActivity().startActivity(MainActivity.getInstance(requireContext()))
                requireActivity().supportFragmentManager.popBackStackImmediate()
            }
        })*/
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,object :
            OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                this.isEnabled = false
                //changeToolbarSelectMode(false)
                if (selectMode)
                    actionsView.visibility = View.GONE
                requireActivity().onBackPressed()
            }
        })


        adapter = ListFilesAdapter(requireContext())

        binding.buttomDeselect.setOnClickListener {
            changeToolbarSelectMode(false)
        }


        adapter.onFileLongClickListener = object : ListFilesAdapter.OnFileLongClickListener{
            override fun onFileLongClick(tdObject: TdObject) {

                checkedItemsProcessing(tdObject)
            }
        }

        adapter.onFileClickListener = object : ListFilesAdapter.OnFileClickListener{
            override fun onFileClick(tdObject: TdObject) {
                if (selectMode){
                    checkedItemsProcessing(tdObject)
                    return
                }
                if (tdObject.is_folder()){
                    viewModel.changeDirectory(tdObject)
                }

                if(tdObject.is_file()){
                    if (tdObject.placeType == PlaceType.TeleDisk){
                        val startIntent = FileBackgroundTransfer.getIntent(requireActivity(),tdObject)
                        /*val startIntent = FileBackgroundTransfer.getIntent(
                            requireActivity(),
                            tdObject,
                            TdObject("Downloads",PlaceType.Local,FileType.Folder,"/storage/emulated/0/Download/1"),

                        )*/
                        ContextCompat.startForegroundService(requireActivity(), startIntent)
                    }
                    if(tdObject.placeType == PlaceType.Local){
                        //startActivity(viewModel.openLocalFile(tdObject.path))
                        viewModel.openLocalFile(tdObject.path)


                        /*val startIntent = FileBackgroundTransfer.getIntent(
                            requireActivity(),
                            tdObject,
                            TdObject("Downloads",PlaceType.TeleDisk,FileType.Folder,"/", groupID = -567578282L),

                            )
                        ContextCompat.startForegroundService(requireActivity(), startIntent)*/
                    }

                }

            }
        }
        //binding.recycleViewListFiles.layoutManager = GridLayoutManager(requireContext(),4)

        binding.recycleViewListFiles.layoutManager = LinearLayoutManager(requireActivity()).apply { orientation = LinearLayoutManager.VERTICAL }

        /*binding.searchViewListFiles.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                adapter.filter1(p0)
                return false
            }
        })*/

        binding.recycleViewListFiles.adapter = adapter
        //adapter.submitList(null)
        viewModel = ViewModelProvider(requireActivity())[ListFileViewModel::class.java]
        //viewModel.fileScope.removeObservers(viewLifecycleOwner)

        viewModel.needCancelSelect.observe(viewLifecycleOwner, Observer {
            deselect()
        })

        viewModel.needLaunchIntent.observe(viewLifecycleOwner, Observer {
            startActivity(it)
        })

        viewModel.needPressBackButton.observe(viewLifecycleOwner, Observer {
            requireActivity().onBackPressed()
        })


        viewModel.getNeedOpenLD().observe(viewLifecycleOwner, Observer {
            Toast.makeText(requireContext(), "операция успешно завершена: "+it.first, Toast.LENGTH_SHORT).show()
            if (it.second)
                viewModel.openLocalFile(it.first)
            else
                viewModel.refresh()
        })

        viewModel.fileScope.observe(viewLifecycleOwner, Observer {
            //adapter.submitList(null)
            binding.pathTextView.setText(viewModel.currentDirectory.path)
            binding.loadDataProgressBar.visibility = View.GONE
            if (it.isNotEmpty()){
                val count = it.size + if (it.any { it.name == ".."}) -1 else 0
                binding.toolBarTextViewCount.setText(requireActivity().getString(R.string.filter_menu_count_items,count.toString()))
                //binding.searchViewListFiles.setQuery("",false)
                //binding.searchViewListFiles.setFocusable(false)
                val a = filter(it).toMutableList()
                adapter.submitList(a)
            }
        })


        toolBarSettings()



        /*viewModel.chatScope.observe(viewLifecycleOwner, Observer {
            println(it)
            //viewModel.getRemoteFiles(-571102575,"/[R.G. Mechanics] Worms Revolution/Redist")
            //viewModel.getRemoteFiles(-571102575,"/[R.G. Mechanics] Worms Revolution")
            //viewModel.getRemoteFiles(-567578282,"/")
            //viewModel.getLocalFiles("/storage/emulated/0")
        })*/


        //when(requireArguments().getSerializable(EXTRA_SCOPE_TYPE) as ScopeType){
        if (savedInstanceState == null || !savedInstanceState.containsKey(SAVE_LIST)){

            /*when(args.scopeType){
                //ScopeType.TeleDisk -> viewModel.getChats()
                ScopeType.TeleDisk -> {
                    viewModel.refreshFileScope()
                    *//*viewModel.chatScope.observe(viewLifecycleOwner, Observer {
                        viewModel.getRemoteFiles(-567578282,"/")
                    })*//*
                    //viewModel.getRemoteFiles(-567578282,"/")
                    viewModel.getRemoteFiles(-650777369,"/")
                }
                ScopeType.Local -> {
                    viewModel.refreshFileScope()
                    viewModel.getLocalFiles("/storage/emulated/0")
                }
                ScopeType.VkMsg -> {}
            }*/

            init()
        }

        else{
            val li = savedInstanceState.getParcelableArray(SAVE_LIST) as Array<TdObject>
            adapter.submitList(null)
            adapter.submitList(li.toMutableList())
            binding.loadDataProgressBar.visibility = View.GONE

            if (savedInstanceState.getBoolean(SELECT_MODE))
                changeToolbarSelectMode(true)
        }

        //viewModel.getChats()
    }

    private fun init(){
        viewModel.setLocalPath(args.path)
        when(args.filter){
            FiltersFromType.DEFAULT -> {
                when(args.scopeType){
                    //ScopeType.TeleDisk -> {viewModel.getRemoteFiles(-650777369,"/")}
                    ScopeType.TeleDisk -> {viewModel.getRemoteFiles(viewModel.currentGroup,args.path)}
                    ScopeType.Local -> {viewModel.getLocalFiles(args.path)}
                    ScopeType.Sd -> {viewModel.getLocalFiles(args.path)}
                    ScopeType.VkMsg -> {}
                }
            }
            FiltersFromType.ALL_REMOTE -> {viewModel.getRemoteFilesFiltered(args.filter,args.path)}
            FiltersFromType.ALL_LOCAL -> {viewModel.getLocalFilesFiltered(args.filter,args.path)}

            else -> {viewModel.getLocalFilesFiltered(args.filter,args.path)}
            /*FiltersFromType.APPS -> FiltersFromType.APPS.ext
            FiltersFromType.MUSIC -> FiltersFromType.MUSIC.ext
            FiltersFromType.PHOTO -> FiltersFromType.PHOTO.ext
            FiltersFromType.DOCUMENTS -> FiltersFromType.DOCUMENTS.ext*/
        }
    }


    private fun toolBarSettings() {
        val toolbar = binding.toolbar
        val infoToolbar = binding.infoToolbar
        val pathToolbar = binding.pathToolbar
        val searchBar = toolbar.menu.findItem(R.id.action_search).actionView as SearchView



        //searchBar.res //setBackgroundColor(resources.getColor(R.color.colorMainText))
        //searchBar.setTe (resources.getColor(R.color.colorMainText))
        //toolbar.inflateMenu(R.menu.files_action_menu)
        //toolbar.inflateMenu(R.menu.files_action_hidden_menu)
        infoToolbar.overflowIcon = requireActivity().getDrawable(R.drawable.arrow_down_drop_circle_outline_custom)
        //infoToolbar.setOve
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_18dp)
        toolbar.setNavigationOnClickListener { view ->
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
            when (lastFilter){
                R.id.action_filter_size -> {infoToolbar.menu.findItem(lastFilter).title = getString(R.string.filter_menu_size)}
                R.id.action_filter_name -> {infoToolbar.menu.findItem(lastFilter).title = getString(R.string.filter_menu_name)}
                R.id.action_filter_type -> {infoToolbar.menu.findItem(lastFilter).title = getString(R.string.filter_menu_type)}
                R.id.action_filter_time -> {infoToolbar.menu.findItem(lastFilter).title = getString(R.string.filter_menu_time)}
            }
            when(it.itemId){
                R.id.action_filter_size -> {

                    filterReversed = if (lastFilter == it.itemId) !filterReversed else false

                    val newTitle = if (filterReversed)
                        getString(R.string.filter_menu_arrow_up) + getString(R.string.filter_menu_size)
                    else
                        getString(R.string.filter_menu_arrow_down) + getString(R.string.filter_menu_size)

                    val s = SpannableString(newTitle)
                    s.setSpan(
                        ForegroundColorSpan(Color.parseColor("#ABCABC")),
                        0,
                        s.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    it.setTitle(s)
                    binding.toolBarTextViewFilter.text = newTitle

                    filter =
                        { items ->
                            val files = items.filter { it.is_file() }
                            val folders = items.filter { !it.is_file() }

                            val filteredFolders = folders.sortedBy { item -> item.size}
                            val filteredFiles = if (filterReversed)
                                files.sortedByDescending { item -> item.size}
                            else
                                files.sortedBy { item -> item.size}

                            filteredFolders+filteredFiles
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
                        ForegroundColorSpan(Color.parseColor("#ABCABC")),
                        0,
                        s.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    it.setTitle(s)
                    binding.toolBarTextViewFilter.text = newTitle

                    filter =
                        { items ->
                            val files = items.filter { it.is_file() }
                            val folders = items.filter { !it.is_file() }

                            val filteredFolders = folders.sortedBy { item -> item.name}
                            val filteredFiles = if (filterReversed)
                                files.sortedByDescending { item -> item.name}
                            else
                                files.sortedBy { item -> item.name}

                            filteredFolders+filteredFiles
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
                        ForegroundColorSpan(Color.parseColor("#ABCABC")),
                        0,
                        s.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    it.setTitle(s)
                    binding.toolBarTextViewFilter.text = newTitle

                    filter =
                        { items ->
                            val files = items.filter { it.is_file() }
                            val folders = items.filter { !it.is_file() }

                            val filteredFolders = folders.sortedBy { item -> item.unixTimeDate}
                            val filteredFiles = if (filterReversed)
                                files.sortedByDescending { item -> item.unixTimeDate}
                            else
                                files.sortedBy { item -> item.unixTimeDate}

                            filteredFolders+filteredFiles
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
                        ForegroundColorSpan(Color.parseColor("#ABCABC")),
                        0,
                        s.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    it.setTitle(s)
                    binding.toolBarTextViewFilter.text = newTitle

                    filter =
                        { items ->
                            val files = items.filter { it.is_file() }
                            val folders = items.filter { !it.is_file() }

                            val filteredFolders = folders.sortedBy { item -> item.name}
                            val filteredFiles = if (filterReversed)
                                files.sortedByDescending { item -> item.name.substringAfterLast(".")}
                            else
                                files.sortedBy { item -> item.name.substringAfterLast(".")}

                            filteredFolders+filteredFiles
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

        toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                //R.id.action_share -> {}//{viewModel.shareItems()}
                R.id.action_search -> {
                    /*findNavController()
                        .navigate(MainFragmentDirections
                            .actionMainFragmentToSearchFragment ())*/
                }
                R.id.action_layout_grid -> {
                    adapter.layoutManagerType = adapter.MANAGER_GRID
                    binding.recycleViewListFiles.layoutManager = GridLayoutManager(requireActivity(),4)
                    //binding.recycleViewListFiles.invalidateItemDecorations()
                    toolbar.menu.findItem(R.id.action_layout_linear).isVisible = true
                    it.isVisible =false
                }
                R.id.action_layout_linear -> {
                    adapter.layoutManagerType = adapter.MANAGER_LINEAR
                    binding.recycleViewListFiles.layoutManager = LinearLayoutManager(requireActivity())
                    toolbar.menu.findItem(R.id.action_layout_grid).isVisible = true
                    it.isVisible =false
                }
                R.id.action_new_folder -> {createFolderDialog()}
            }
            true
        }

    }

    private fun checkedItemsProcessing(tdObject: TdObject) {
        //tdObject.isChecked = !tdObject.isChecked
        if (!selectMode)
            changeToolbarSelectMode(true)


        val li = adapter.currentList.toMutableList()
        val el = li.indexOf(tdObject)
        val el2 = tdObject.copy(isChecked = !tdObject.isChecked)
        li[el] = el2

        adapter.submitList(li)
        //adapter.notifyItemChanged(adapter.currentList.indexOf(tdObject))

        if (!tdObject.isChecked)
            viewModel.selectedItems.add(el2)
        else{
            viewModel.selectedItems.remove(tdObject)
        }

        val oneChecked = li.filter { it.isChecked }.size == 1
        val zeroChecked = li.all { !it.isChecked }
        if (oneChecked || zeroChecked){
            val color = AppCompatResources.getDrawable(
                requireContext(),
                if (oneChecked) R.color.colorMainBackground
                else R.color.colorStoreBackgroundOther
            )
            val bp1 = actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelCopy)
            val bp2 = actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelMove)
            val bp3 = actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelDelete)
            val bp4 = actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelRename)
            val bp5 = actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelMore)

            bp1.isClickable = oneChecked; bp1.background = color
            bp2.isClickable = oneChecked; bp2.background = color
            bp3.isClickable = oneChecked; bp3.background = color
            bp4.isClickable = oneChecked; bp4.background = color
            bp5.isClickable = oneChecked; bp5.background = color

            }else{
            actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelRename).isClickable = false
            actionsView.findViewById<LinearLayout>(R.id.textViewBottomPanelRename).background = AppCompatResources.getDrawable(requireContext(),R.color.colorStoreBackgroundOther)
        }

        binding.toolBarTextViewCountChecked.text = if (viewModel.selectedItems.isEmpty()){
            "Элементы не выбраны"
        }else{
            "Выбрано: " + viewModel.selectedItems.size
        }
    }

    private fun changeToolbarSelectMode(selectModeOn:Boolean){
        if(selectModeOn){
            selectMode = true
            actionsView.visibility = View.VISIBLE
            binding.selectToolbar.visibility = View.VISIBLE
            binding.toolbar.visibility = View.GONE
        } else{
            actionsView.visibility = View.GONE
            deselect()

        }
        viewModel.selectedItems.clear()
    }
    private fun deselect(){
        selectMode = false
        binding.selectToolbar.visibility = View.GONE
        binding.toolbar.visibility = View.VISIBLE

        val li = adapter.currentList
        val li2 = li.map { it.copy(isChecked = false) }// copy(isChecked = !tdObject.isChecked)
        adapter.submitList(li2.toMutableList())
    }

    override fun onDestroyView() {
        _binding = null
        //viewModel.fileScope.removeObservers(viewLifecycleOwner)
        super.onDestroyView()
    }


    private fun createFolderDialog() {
        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.new_folder)
            positiveButton(R.string.create)
            negativeButton(R.string.cancel)
            input(
                hint = getString(R.string.new_folder_hint),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                waitForPositiveButton = true
            ) { _, text -> viewModel.createFolder(text.toString())}
        }
    }

    companion object {
        const val EXTRA_SCOPE_TYPE = "scopeType"
        const val EXTRA_FILTER = "filter"

        const val SELECT_MODE = "selectMode"
        const val SAVE_LIST = "list"
        fun newInstance(scopeType: ScopeType,filter:FiltersFromType? = null): Fragment {
            return ListFilesFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(EXTRA_SCOPE_TYPE, scopeType)
                    putSerializable(EXTRA_FILTER,filter)
                }
            }
        }
    }


    /*requireContext().showDialog(
    title = "Your Title",
    inputField = "Your Description",
    titleOfPositiveButton = "yes",
    titleOfNegativeButton = "No",
    positiveButtonFunction = { },
    negativeButtonFunction = { }
    )*/


}