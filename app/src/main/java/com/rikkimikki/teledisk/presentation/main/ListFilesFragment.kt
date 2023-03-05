package com.rikkimikki.teledisk.presentation.main

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.local.FileBackgroundTransfer
import com.rikkimikki.teledisk.databinding.FragmentListFilesBinding
import com.rikkimikki.teledisk.domain.FiltersFromType
import com.rikkimikki.teledisk.domain.PlaceType
import com.rikkimikki.teledisk.domain.ScopeType
import com.rikkimikki.teledisk.domain.TdObject

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
        outState.putParcelableArray("list", adapter.currentList.toTypedArray())

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

        adapter = ListFilesAdapter(requireContext())


        adapter.onFileLongClickListener = object : ListFilesAdapter.OnFileLongClickListener{
            override fun onFileLongClick(tdObject: TdObject) {

                checkedItemsProcessing(adapter,tdObject)
            }
        }

        adapter.onFileClickListener = object : ListFilesAdapter.OnFileClickListener{
            override fun onFileClick(tdObject: TdObject) {
                if (tdObject.is_folder())
                    viewModel.changeDirectory(tdObject)
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
            if (it.isNotEmpty()){
                val count = it.size + if (it.any { it.name == ".."}) -1 else 0
                binding.toolBarTextViewCount.setText(requireActivity().getString(R.string.filter_menu_count_items,count.toString()))
                //binding.searchViewListFiles.setQuery("",false)
                //binding.searchViewListFiles.setFocusable(false)
                adapter.submitList(filter(it).toMutableList())
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
        if (savedInstanceState == null){
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

            when(args.filter){
                FiltersFromType.DEFAULT -> {
                    when(args.scopeType){
                        ScopeType.TeleDisk -> {viewModel.getRemoteFiles(-650777369,"/")}
                        ScopeType.Local -> {viewModel.getLocalFiles("/storage/emulated/0")}
                        ScopeType.VkMsg -> {}
                    }
                }
                else -> viewModel.getLocalFilesFiltered(args.filter)
                /*FiltersFromType.APPS -> FiltersFromType.APPS.ext
                FiltersFromType.MUSIC -> FiltersFromType.MUSIC.ext
                FiltersFromType.PHOTO -> FiltersFromType.PHOTO.ext
                FiltersFromType.DOCUMENTS -> FiltersFromType.DOCUMENTS.ext*/
            }
        }

        else{
            val li = savedInstanceState.getParcelableArray("list") as Array<TdObject>
            adapter.submitList(null)
            adapter.submitList(li.toMutableList())
        }

        //viewModel.getChats()
    }

    private fun toolBarSettings() {
        val toolbar = binding.toolbar
        val infoToolbar = binding.infoToolbar
        val pathToolbar = binding.pathToolbar
        val searchBar = toolbar.menu.findItem(R.id.action_search).actionView as SearchView
        //toolbar.inflateMenu(R.menu.files_action_menu)
        //toolbar.inflateMenu(R.menu.files_action_hidden_menu)
        infoToolbar.overflowIcon = requireActivity().getDrawable(R.drawable.arrow_down_drop_circle_outline_custom)
        //infoToolbar.setOve
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_18dp)
        toolbar.setNavigationOnClickListener { view ->
            viewModel.clickArrow()
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
            viewModel.refresh()
            true
        }

        toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.action_settings -> {}
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
                R.id.action_done -> {}
            }
            true
        }

    }

    private fun checkedItemsProcessing(adapter: ListFilesAdapter,tdObject: TdObject) {
        //tdObject.isChecked = !tdObject.isChecked

        val li = adapter.currentList.toMutableList()
        val el = li.indexOf(tdObject)
        li[el] = tdObject.copy(isChecked = !tdObject.isChecked)

        adapter.submitList(li)
        //adapter.notifyItemChanged(adapter.currentList.indexOf(tdObject))

        if (!tdObject.isChecked)
            viewModel.selectedItems.add(tdObject)
        else
            viewModel.selectedItems.remove(tdObject)

        if (li.any { it.isChecked })
            actionsView.visibility = View.VISIBLE
        else{
            actionsView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        _binding = null
        //viewModel.fileScope.removeObservers(viewLifecycleOwner)
        super.onDestroyView()
    }



    private fun getType(): ScopeType {
        return requireArguments().getSerializable(EXTRA_SCOPE_TYPE) as ScopeType
    }

    companion object {
        const val EXTRA_SCOPE_TYPE = "scopeType"
        const val EXTRA_FILTER = "filter"
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