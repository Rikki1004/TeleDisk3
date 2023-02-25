package com.rikkimikki.teledisk.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.rikkimikki.teledisk.data.local.TdRepositoryImpl
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.domain.GetAllChatsUseCase
import com.rikkimikki.teledisk.domain.GetLocalFilesUseCase
import com.rikkimikki.teledisk.domain.GetRemoteFilesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ListFileViewModel:ViewModel() {
    var currentLocalPath = "/"
    var currentRemotePath = "/"

    val repository = TelegramRepository

    private val getRemoteFilesUseCase = GetRemoteFilesUseCase(repository)
    private val getLocalFilesUseCase = GetLocalFilesUseCase(repository)
    private val getAllChatsUseCase = GetAllChatsUseCase(repository)
    val fileScope = repository.dataFromStore
    val chatScope = repository.allChats

    fun getRemoteFiles(id:Long,path:String){
        viewModelScope.launch { getRemoteFilesUseCase(id,path) }
    }

    fun getLocalFiles(path:String){
        viewModelScope.launch { getLocalFilesUseCase(path) }
    }

    fun getChats(){
        viewModelScope.launch { getAllChatsUseCase() }
    }

    //getDataFromLocal("/storage/emulated/0/Download")
}