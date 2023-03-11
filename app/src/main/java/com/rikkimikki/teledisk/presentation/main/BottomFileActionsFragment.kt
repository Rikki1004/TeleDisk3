package com.rikkimikki.teledisk.presentation.main

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.input
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentBottomFileActionsBinding
import com.rikkimikki.teledisk.domain.baseClasses.FileInfo


class BottomFileActionsFragment : Fragment() {
    private var _binding: FragmentBottomFileActionsBinding? = null
    private val binding get() = _binding!!
    private val actionsView by lazy { requireActivity().findViewById<FragmentContainerView>(R.id.bottom_view_container) }
    private lateinit var viewModel: ListFileViewModel
    private var dialog: AlertDialog? = null
    private var editText : EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomFileActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments?.getBoolean(EXTRA_CLOSE) == true)
            actionsView.visibility = View.GONE

        viewModel = ViewModelProvider(requireActivity())[ListFileViewModel::class.java]


        with(binding){
            textViewBottomPanelCopy.setOnClickListener {
                viewModel.refresh()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.bottom_view_container,BottomFileActionTransferFragment.newInstance(true))
                    .commit()
            }
            textViewBottomPanelMove.setOnClickListener {
                viewModel.refresh()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.bottom_view_container,BottomFileActionTransferFragment.newInstance(false))
                    .commit()
            }
            textViewBottomPanelDelete.setOnClickListener { viewModel.deleteItem() }
            textViewBottomPanelRename.setOnClickListener {renameFileDialog(viewModel.getRenamedItemName())}
            textViewBottomPanelMore.setOnClickListener {
                showPopupMenu(it)
            }
        }
    }

    private fun showPopupMenu(v: View) {
        val popupMenu = PopupMenu(requireContext(), v)
        popupMenu.inflate(R.menu.files_action_bottom)
        popupMenu
            .setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_share -> {
                        viewModel.shareItems()
                        true
                    }
                    R.id.action_info -> {
                        infoDialog(viewModel.getInfo())
                        true
                    }
                    else -> false
                }
            }

        popupMenu.show()
    }

    private fun infoDialog(info: FileInfo) {
        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.information)
            if (info.single){
                val dialog = customView(R.layout.info_view, scrollable = true, horizontalPadding = true)
                with(dialog.getCustomView()){
                    findViewById<TextView>(R.id.infoViewName).text = info.name
                    findViewById<TextView>(R.id.infoViewDate).text = info.date
                    findViewById<TextView>(R.id.infoViewPath).text = info.path
                    findViewById<TextView>(R.id.infoViewSize).text = info.size
                }
            }else{
                val dialog = customView(R.layout.info_view_multiple, scrollable = true, horizontalPadding = true)
                with(dialog.getCustomView()){
                    findViewById<TextView>(R.id.infoViewSize).text = info.size
                    findViewById<TextView>(R.id.infoViewContains).text = info.contains
                }
            }
        }
    }

    private fun renameFileDialog(oldItem:Pair<String,Boolean>) {
        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            if (oldItem.second)
                title(R.string.rename_file)
            else
                title(R.string.rename_folder)
            input(
                prefill = oldItem.first,
                hint = getString(R.string.rename_file),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            ) { _, text ->
                viewModel.renameItem(text.toString())
            }
            positiveButton(R.string.rename)
            negativeButton(R.string.cancel)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        editText?.let { outState.putString(EXTRA_TEXT_VIEW,it.text.toString()) }
    }

    override fun onDestroyView() {
        _binding = null
        dialog?.dismiss()
        super.onDestroyView()
    }

    companion object {
        private const val EXTRA_CLOSE = "CLOSE"
        private const val EXTRA_TEXT_VIEW = "TEXT_VIEW"
        fun newInstance(close:Boolean) = BottomFileActionsFragment().apply {
            arguments = Bundle().apply {
                putBoolean(EXTRA_CLOSE,close)
            }
        }
    }
}