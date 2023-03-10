import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.rikkimikki.teledisk.data.tdLib.AuthState
import com.rikkimikki.teledisk.databinding.FragmentInputLoginBinding
import com.rikkimikki.teledisk.presentation.login.LoginViewModel
import com.rikkimikki.teledisk.presentation.main.MainActivity

class InputLoginFragment : Fragment() {
    private var _binding: FragmentInputLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LoginViewModel
    private var currentState = STATE_SEND_PHONE

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        startObservers()
    }

    private fun startObservers() {
        viewModel.getReadyState().observe(viewLifecycleOwner, Observer {
            if (it) startActivity(MainActivity.getInstance(requireContext()))
        })

        binding.buttonStartLogin.setOnClickListener {
            with(viewModel){
                when(currentState){
                    STATE_SEND_CODE -> sendCode(binding.textEditStartLogin.text.toString())
                    STATE_SEND_PASSWORD -> sendPassword(binding.textEditStartLogin.text.toString())
                    STATE_SEND_PHONE -> sendPhone(binding.textEditStartLogin.text.toString())
                    else -> null
                }
            }
            binding.textEditStartLogin.setText("")
        }

        viewModel.authState.observe(viewLifecycleOwner, Observer { state ->
            if (state == null) return@Observer //skip

                if (state !is AuthState.LoggedIn) {
                    binding.textViewStartLogin.text = state.dialogHint
                    when (state) {
                        AuthState.EnterPhone ->  currentState = STATE_SEND_PHONE
                        AuthState.EnterCode -> currentState = STATE_SEND_CODE
                        is AuthState.EnterPassword -> currentState = STATE_SEND_PASSWORD
                        else -> {}
                    }
                }

            if (state is AuthState.LoggedIn)
                startActivity(MainActivity.getInstance(requireContext()))
        })
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = InputLoginFragment()
        const val STATE_SEND_PHONE = 1
        const val STATE_SEND_CODE = 2
        const val STATE_SEND_PASSWORD = 3
    }
}