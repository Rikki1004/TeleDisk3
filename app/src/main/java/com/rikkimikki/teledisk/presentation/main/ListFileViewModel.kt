package com.rikkimikki.teledisk.presentation.main

import androidx.lifecycle.*
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository.downloadLD
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository.needOpen
import com.rikkimikki.teledisk.domain.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi

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

    fun changeDirectory(directory:TdObject) {
        if (directory.is_folder() && directory.placeType == PlaceType.TeleDisk)
            getRemoteFiles(directory.groupID,directory.path)
        if (directory.is_folder() && directory.placeType == PlaceType.Local)
            getLocalFiles(directory.path)
    }

    fun openFile(file: TdObject) {
        if (file.is_file() && file.placeType == PlaceType.Local){
            TODO()
        }
        if (file.is_file() && file.placeType == PlaceType.TeleDisk){
            viewModelScope.launch {
                TelegramRepository.loadFile(file.fileID.toInt())
            }
        }

    }
    fun getDwndLD():LiveData<TdApi.File>{
        //val medLD = MediatorLiveData<TdApi.File>()
        //medLD.addSource(downloadLD, Observer { if (it.local.isDownloadingCompleted) isRemoteDownloadComplete.value = "" })
        //return medLD
        return downloadLD
    }
    fun getNeedOpenLD():LiveData<TdApi.File>{
        return needOpen
    }
    //getDataFromLocal("/storage/emulated/0/Download")
}