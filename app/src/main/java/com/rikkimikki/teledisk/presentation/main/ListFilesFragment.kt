package com.rikkimikki.teledisk.presentation.main

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rikkimikki.teledisk.BuildConfig
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.local.FileBackgroundTransfer
import com.rikkimikki.teledisk.databinding.DialogInputTextBinding
import com.rikkimikki.teledisk.databinding.FragmentListFilesBinding
import com.rikkimikki.teledisk.domain.*
import java.io.File

class ListFilesFragment : Fragment() {
    private var _binding: FragmentListFilesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter : ListFilesAdapter
    private lateinit var viewModel: ListFileViewModel
    private val args by navArgs<ListFilesFragmentArgs>()
    //private lateinit var actionsView = requireActivity().findViewById<FragmentContainerView>
    private val actionsView by lazy { requireActivity().findViewById<FragmentContainerView>(R.id.bottom_view_container) }


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

        binding.recycleViewListFiles.adapter = adapter
        //adapter.submitList(null)
        viewModel = ViewModelProvider(requireActivity())[ListFileViewModel::class.java]
        //viewModel.fileScope.removeObservers(viewLifecycleOwner)

        viewModel.needLaunchIntent.observe(viewLifecycleOwner, Observer {
            startActivity(it)
        })

        viewModel.getNeedOpenLD().observe(viewLifecycleOwner, Observer {
            Toast.makeText(requireContext(), "операция успешно завершена: "+it.first, Toast.LENGTH_SHORT).show()
            if (it.second)
                //startActivity(viewModel.openLocalFile(it.first))
                viewModel.openLocalFile(it.first)
            else
                viewModel.refresh()
        })

        viewModel.fileScope.observe(viewLifecycleOwner, Observer {
            //adapter.submitList(null)
            if (it.isNotEmpty())
                adapter.submitList(it.toMutableList())
        })

        /*viewModel.chatScope.observe(viewLifecycleOwner, Observer {
            println(it)
            //viewModel.getRemoteFiles(-571102575,"/[R.G. Mechanics] Worms Revolution/Redist")
            //viewModel.getRemoteFiles(-571102575,"/[R.G. Mechanics] Worms Revolution")
            //viewModel.getRemoteFiles(-567578282,"/")
            //viewModel.getLocalFiles("/storage/emulated/0")
        })*/


        //when(requireArguments().getSerializable(EXTRA_SCOPE_TYPE) as ScopeType){
        if (savedInstanceState == null)
            when(args.scopeType){
                //ScopeType.TeleDisk -> viewModel.getChats()
                ScopeType.TeleDisk -> {
                    viewModel.refreshFileScope()
                    /*viewModel.chatScope.observe(viewLifecycleOwner, Observer {
                        viewModel.getRemoteFiles(-567578282,"/")
                    })*/
                    //viewModel.getRemoteFiles(-567578282,"/")
                    viewModel.getRemoteFiles(-650777369,"/")
                }
                ScopeType.Local -> {
                    viewModel.refreshFileScope()
                    viewModel.getLocalFiles("/storage/emulated/0")
                }
                ScopeType.VkMsg -> {}
            }
        else{
            val li = savedInstanceState.getParcelableArray("list") as Array<TdObject>
            adapter.submitList(null)
            adapter.submitList(li.toMutableList())
        }

        //viewModel.getChats()
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
        fun newInstance(scopeType: ScopeType): Fragment {
            return ListFilesFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(EXTRA_SCOPE_TYPE, scopeType)
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