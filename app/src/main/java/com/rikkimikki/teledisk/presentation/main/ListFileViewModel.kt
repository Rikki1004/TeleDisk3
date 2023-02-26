package com.rikkimikki.teledisk.presentation.main

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.*
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository.downloadLD
import com.rikkimikki.teledisk.domain.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import java.net.URI

class ListFileViewModel:ViewModel() {
    var currentLocalPath = "/"
    var currentRemotePath = "/"

    val repository = TelegramRepository

    private val getRemoteFilesUseCase = GetRemoteFilesUseCase(repository)
    private val getLocalFilesUseCase = GetLocalFilesUseCase(repository)
    private val getAllChatsUseCase = GetAllChatsUseCase(repository)
    val fileScope = repository.dataFromStore
    val chatScope = repository.allChats

init {
    repository.reload()
}
    val isRemoteDownloadComplete = MutableLiveData<String>()

    fun getRemoteFiles(id:Long,path:String){
        var a = viewModelScope.launch { getRemoteFilesUseCase(id,path) }
    }

    fun getLocalFiles(path:String){
        viewModelScope.launch { getLocalFilesUseCase(path) }
    }

    fun getChats(){
        viewModelScope.launch { getAllChatsUseCase() }
    }

    fun changeDirectory(directory:Tfolder) {
        if (directory.type == FolderType.TeleDiskFolder)
            getRemoteFiles(directory.groupID,directory.path)
        if (directory.type == FolderType.LocalFolder)
            getLocalFiles(directory.path)
    }

    fun openFile(file: Tfile) {
        if (file.type == FileType.LocalFile){
            TODO()
        }
        if (file.type == FileType.TeleDiskFile)
            viewModelScope.launch {
                TelegramRepository.loadFile(file.fileID.toInt())
            }
    }
    fun getDwndLD():LiveData<TdApi.File>{
        //val medLD = MediatorLiveData<TdApi.File>()
        //medLD.addSource(downloadLD, Observer { if (it.local.isDownloadingCompleted) isRemoteDownloadComplete.value = "" })
        //return medLD
        return downloadLD
    }
    //getDataFromLocal("/storage/emulated/0/Download")
}