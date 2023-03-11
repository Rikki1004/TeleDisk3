package com.rikkimikki.teledisk.presentation.login

import androidx.lifecycle.*
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class LoginViewModel : ViewModel() {
    val error = MutableLiveData<String>()
    val authState = TelegramRepository.authFlow.asLiveData()

    fun getReadyState():LiveData<Boolean>{
        return TelegramRepository.is_ready
    }
    init {
        TelegramRepository.api.attachClient()
    }

    private val scope = viewModelScope + CoroutineExceptionHandler { _, throwable ->
        error.postValue(throwable.message)
    }

    fun sendPhone(phone: String) = scope.launch {
        TelegramRepository.sendPhone(phone)
    }

    fun sendCode(code: String) = scope.launch {
        TelegramRepository.sendCode(code)
    }

    fun sendPassword(password: String) = scope.launch {
        TelegramRepository.sendPassword(password)
    }
}