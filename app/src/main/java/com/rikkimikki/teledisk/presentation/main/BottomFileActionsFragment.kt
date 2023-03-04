package com.rikkimikki.teledisk.presentation.main

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentBottomFileActionsBinding

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

        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_TEXT_VIEW))
            createDialog(savedInstanceState.getString(EXTRA_TEXT_VIEW))

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
            textViewBottomPanelRename.setOnClickListener {
                //viewModel.renameItem("newFolder")
                createDialog()
            }
            textViewBottomPanelShare.setOnClickListener {
                //requireActivity().startActivity(viewModel.shareLocalFile())
                viewModel.shareItems()
            }
        }
    }

    private fun createDialog(et:String? = null ) {
        editText = EditText(requireContext())
        editText?.let { if (et != null) it.setText(et.toString()) else it.setText(viewModel.selectedItems[0].name) }
        dialog = AlertDialog.Builder(requireContext())
            .setTitle("Переименование файла")
            .setMessage("Введите новое имя")
            .setView(editText)
            .setPositiveButton("Переименовать") { _, _ -> viewModel.renameItem(editText?.text.toString())}
            .setNegativeButton("Отмена", null)
            .create()
        dialog?.show()
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