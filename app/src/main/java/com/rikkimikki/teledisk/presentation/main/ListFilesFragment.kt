package com.rikkimikki.teledisk.presentation.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rikkimikki.teledisk.BuildConfig
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.local.FileBackgroundTransfer
import com.rikkimikki.teledisk.databinding.FragmentListFilesBinding
import com.rikkimikki.teledisk.domain.*
import java.io.File

class ListFilesFragment : Fragment() {
    private var _binding: FragmentListFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ListFileViewModel
    private val args by navArgs<ListFilesFragmentArgs>()

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

        val adapter = ListFilesAdapter(requireContext())

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
                    if(tdObject.placeType == PlaceType.Local)
                        openLocalFile(tdObject.path)
                }

            }
        }
        //binding.recycleViewListFiles.layoutManager = GridLayoutManager(requireContext(),4)
        binding.recycleViewListFiles.layoutManager = LinearLayoutManager(context).apply { orientation = LinearLayoutManager.VERTICAL }

        binding.recycleViewListFiles.adapter = adapter
        viewModel = ViewModelProvider(this)[ListFileViewModel::class.java]
        //viewModel.fileScope.removeObservers(viewLifecycleOwner)


        viewModel.getNeedOpenLD().observe(viewLifecycleOwner, Observer {
            //println(""+it.local.downloadedPrefixSize+"/"+it.size)
            if (it.local.isDownloadingCompleted)
                openLocalFile(it.local.path)
        })

        viewModel.fileScope.observe(viewLifecycleOwner, Observer {
            adapter.submitList(null)
            adapter.submitList(it?.toMutableList())
        })

        /*viewModel.chatScope.observe(viewLifecycleOwner, Observer {
            println(it)
            //viewModel.getRemoteFiles(-571102575,"/[R.G. Mechanics] Worms Revolution/Redist")
            //viewModel.getRemoteFiles(-571102575,"/[R.G. Mechanics] Worms Revolution")
            //viewModel.getRemoteFiles(-567578282,"/")
            //viewModel.getLocalFiles("/storage/emulated/0")
        })*/


        //when(requireArguments().getSerializable(EXTRA_SCOPE_TYPE) as ScopeType){
        when(args.scopeType){
            //ScopeType.TeleDisk -> viewModel.getChats()
            ScopeType.TeleDisk -> {
                /*viewModel.chatScope.observe(viewLifecycleOwner, Observer {
                    viewModel.getRemoteFiles(-567578282,"/")
                })*/
                viewModel.getRemoteFiles(-567578282,"/")
                //viewModel.getChats()
            }
            ScopeType.Local -> viewModel.getLocalFiles("/storage/emulated/0")
            ScopeType.VkMsg -> {}
        }

        //viewModel.getChats()
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

    private fun openLocalFile(path:String){
        val uri = FileProvider.getUriForFile(requireActivity(),
            BuildConfig.APPLICATION_ID + ".provider", File(path))
        val intent = Intent(Intent.ACTION_VIEW)
        val type = requireActivity().contentResolver.getType(uri)
        intent.setDataAndType(uri,type)
        intent.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        ContextCompat.startActivity(requireActivity(),intent,null)
    }
}