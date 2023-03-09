package com.rikkimikki.teledisk.presentation.main

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.input
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentBottomFileActionTransferBinding
import com.rikkimikki.teledisk.databinding.FragmentListFilesBinding


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

        //if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_TEXT_VIEW))
        //    createDialog(savedInstanceState.getString(EXTRA_TEXT_VIEW))

        with(binding){
            textViewBottomPanelPaste.setOnClickListener {
                val isCopy = requireArguments().getBoolean(EXTRA_COPY)
                if (isCopy)
                    viewModel.copyFile()
                else
                    viewModel.moveFile()
                close()
            }
            textViewBottomPanelCancel.setOnClickListener { viewModel.refresh(); close() }
            textViewBottomPanelCreate .setOnClickListener {createFolderDialog()}
        }
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

    /*private fun createDialog(et:String? = null ) {
        editText = EditText(requireContext())
        editText?.let { if (et != null) it.setText(et.toString()) }
        dialog = AlertDialog.Builder(requireContext())
            .setTitle("Создание папки")
            .setMessage("Введите имя новой папки")
            .setView(editText)
            .setPositiveButton("Создать") { _, _ ->
                viewModel.createFolder(editText?.text.toString())
                viewModel.refresh()
            }
            .setNegativeButton("Отмена", null)
            .create()
        dialog?.show()
    }*/

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