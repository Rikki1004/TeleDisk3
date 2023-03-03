package com.rikkimikki.teledisk.presentation.main

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.rikkimikki.teledisk.data.local.FileBackgroundTransfer
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository.downloadLD
import com.rikkimikki.teledisk.domain.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi

class ListFileViewModel(application: Application):AndroidViewModel(application) {
    var currentLocalPath = "/"
    var currentRemotePath = "/"

    val repository = TelegramRepository

    private val getRemoteFilesUseCase = GetRemoteFilesUseCase(repository)
    private val getLocalFilesUseCase = GetLocalFilesUseCase(repository)
    private val getAllChatsUseCase = GetAllChatsUseCase(repository)
    private val fileOperationComplete = FileOperationCompleteUseCase(repository)
    val fileScope = repository.dataFromStore
    val chatScope = repository.allChats

    val selectedItems = mutableListOf<TdObject>()
    private lateinit var currentDirectory : TdObject

init {
    //repository.reload()
}
    val isRemoteDownloadComplete = MutableLiveData<String>()

    fun refresh(){
        changeDirectory(currentDirectory)
    }
    fun refreshFileScope(){
        fileScope.value = listOf()
    }
    fun refreshSelectedItems(){
        selectedItems.clear()
    }

    fun copyFile(){
        val startIntent = FileBackgroundTransfer.getIntent(
            getApplication(),
            selectedItems[0],
            currentDirectory
            )
        ContextCompat.startForegroundService(getApplication(), startIntent)
        refreshSelectedItems()
    }

    fun getRemoteFiles(id:Long,path:String){
        currentDirectory = TdObject("currentDir",PlaceType.TeleDisk,FileType.Folder,path, groupID = id)
        //if (path == "/") fileScope.value = listOf()
        var a = viewModelScope.launch { getRemoteFilesUseCase(id,path) }
    }

    fun getLocalFiles(path:String){
        currentDirectory = TdObject("currentDir",PlaceType.Local,FileType.Folder,path)
        //if (path == "/storage/emulated/0") fileScope.value = listOf()
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
    fun getNeedOpenLD(): LiveData<Pair<String, Boolean>> {
        return fileOperationComplete()
    }
    //getDataFromLocal("/storage/emulated/0/Download")
}