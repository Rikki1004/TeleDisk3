package com.rikkimikki.teledisk.presentation.main

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.input
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentBottomFileActionTransferBinding


class BottomFileActionTransferFragment : Fragment() {
    private var _binding: FragmentBottomFileActionTransferBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ListFileViewModel

    private var dialog: AlertDialog? = null
    private var editText : EditText? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomFileActionTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ListFileViewModel::class.java]

        viewModel.prepareToCopy()

        with(binding){
            textViewBottomPanelPaste.setOnClickListener {
                if (end()){
                    viewModel.is_copy_mode = false
                    val isCopy = requireArguments().getBoolean(EXTRA_COPY)
                    if (isCopy)
                        viewModel.copyFile()
                    else
                        viewModel.moveFile()
                    close()
                }
            }
            textViewBottomPanelCancel.setOnClickListener { viewModel.cancelCopy();viewModel.refresh(); close() }
            textViewBottomPanelCreate .setOnClickListener {if (end()) createFolderDialog()}
        }
    }


    private fun end():Boolean{
        if (viewModel.currentDirectory.path == "*"){
            Toast.makeText(requireContext(), getString(R.string.it_cant_be_done_here), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
    private fun close(){
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.bottom_view_container,BottomFileActionsFragment.newInstance(true))
            .commit()
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
        private const val EXTRA_COPY = "COPY"
        private const val EXTRA_TEXT_VIEW = "TEXT_VIEW"
        fun newInstance(is_copy:Boolean) = BottomFileActionTransferFragment().apply {
            arguments = Bundle().apply {
                putBoolean(EXTRA_COPY,is_copy)
            }
        }
    }
}