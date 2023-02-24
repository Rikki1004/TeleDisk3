package com.rikkimikki.teledisk.presentation.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.drinkless.td.libcore.telegram.TdApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class LoginViewModel : ViewModel() {

    var currentLocalPath = "/"
    var currentRemotePath = "/"


    val error = MutableLiveData<String>()

    val authState = TelegramRepository.authFlow.asLiveData()

    val newMessage = TelegramRepository.userInfoFlow.catch {
        error.postValue("message error ${it.message}")
    }.asLiveList()

    fun getReadyState():LiveData<Boolean>{
        return TelegramRepository.is_ready
    }

    init {
        TelegramRepository.api.attachClient()

    }

    fun getAllChats() :LiveData<List<TdApi.Chat>>{
        return TelegramRepository.allChats


    }


    fun <T> Flow<T>.asLiveList(
        context: CoroutineContext = EmptyCoroutineContext,
        timeoutInMs: Long = 5000L
    ): LiveData<List<T>> = liveData(context, timeoutInMs) {
        val arrayList = ArrayList<T>()
        collect { newItem ->
            arrayList.add(newItem)
            emit(
                arrayList.toList()
            )
        }
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