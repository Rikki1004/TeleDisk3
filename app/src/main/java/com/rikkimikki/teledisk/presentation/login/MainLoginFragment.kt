package com.rikkimikki.teledisk.presentation.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FragmentMainLoginBinding
import com.rikkimikki.teledisk.presentation.main.MainActivity


class MainLoginFragment : Fragment() {
    private var _binding: FragmentMainLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        startObservers()

        binding.buttonStartLogin.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.login_view_container, InputLoginFragment.newInstance())
                .commit()
        }
    }

    private fun startObservers() {
        viewModel.authState.observe(viewLifecycleOwner){}
        viewModel.getReadyState().observe(viewLifecycleOwner) {
            if (it){
                startActivity(MainActivity.getInstance(requireContext()))
            }
            else
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.login_view_container, InputLoginFragment.newInstance())
                    .commit()
        }
    }
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}