package com.rikkimikki.teledisk.presentation.main

import android.app.Application
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rikkimikki.teledisk.BuildConfig
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.local.FileBackgroundTransfer
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository.downloadLD
import com.rikkimikki.teledisk.databinding.DialogInputTextBinding
import com.rikkimikki.teledisk.domain.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File


class ListFileViewModel(application: Application):AndroidViewModel(application) {
    var currentLocalPath = "/"
    var currentRemotePath = "/"

    val repository = TelegramRepository

    private val getRemoteFilesUseCase = GetRemoteFilesUseCase(repository)
    private val getLocalFilesUseCase = GetLocalFilesUseCase(repository)
    private val getAllChatsUseCase = GetAllChatsUseCase(repository)
    private val fileOperationComplete = FileOperationCompleteUseCase(repository)
    private val createFolderUseCase = CreateFolderUseCase(repository)
    private val renameFileUseCase = RenameFileUseCase(repository)
    private val renameFolderUseCase = RenameFolderUseCase(repository)
    private val deleteFileUseCase = DeleteFileUseCase(repository)
    private val deleteFolderUseCase = DeleteFolderUseCase(repository)
    private val tempPathsForSendUseCase = TempPathsForSendUseCase(repository)
    private val getAllFilteredLocalFilesUseCase = GetAllFilteredLocalFilesUseCase(repository)
    private val getAllFilteredRemoteFilesUseCase = GetAllFilteredRemoteFilesUseCase(repository)
    val fileScope = repository.dataFromStore
    val chatScope = repository.allChats

    var needLaunchIntent = MutableLiveData<Intent>()

    val selectedItems = mutableListOf<TdObject>()
    private lateinit var currentDirectory : TdObject

    private val contentResolver by lazy {
        application.contentResolver
    }
    /*private val application by lazy {
        application
    }*/

init {
    //repository.reload()
    tempPathsForSendUseCase().observeForever {
        shareItems(it)
    }
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
            selectedItems.toTypedArray(),
            currentDirectory,
            true
            )
        ContextCompat.startForegroundService(getApplication(), startIntent)
        refreshSelectedItems()
    }
    fun moveFile() {
        val startIntent = FileBackgroundTransfer.getIntent(
            getApplication(),
            selectedItems.toTypedArray(),
            currentDirectory,
            false
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

    fun getLocalFilesFiltered(filter:FiltersFromType){
        viewModelScope.launch { getAllFilteredLocalFilesUseCase(filter) }
    }
    fun getRemoteFilesFiltered(chatId:Long,filter:FiltersFromType){
        viewModelScope.launch { getRemoteFilesFiltered(chatId,filter) }
    }


    fun createFolder(name:String){
        viewModelScope.launch { createFolderUseCase(currentDirectory, name) }
    }

    fun renameItem(newName: String) {
        val file = selectedItems[0]
        viewModelScope.launch{
            if (file.is_file())
                renameFileUseCase(file,newName)
            else
                renameFolderUseCase(file,newName)
            refreshSelectedItems()
            refresh()
        }
    }

    fun deleteItem() {
        viewModelScope.launch{
            for (i in selectedItems.toList()){
                if (i.is_file())
                    deleteFileUseCase(i)
                else
                    deleteFolderUseCase(i)
            }
            refreshSelectedItems()
            refresh()
        }
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


    fun openLocalFile(path:String){
        val uri = FileProvider.getUriForFile(getApplication(),
            BuildConfig.APPLICATION_ID + ".provider", File(path)
        )
        val intent = Intent(Intent.ACTION_VIEW)
        val type = contentResolver.getType(uri)
        intent.setDataAndType(uri,type)
        intent.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        //return intent
        needLaunchIntent.value = intent
    }

    fun shareItems(listSelected: List<TdObject> = selectedItems){
        if (listSelected.isEmpty())
            return
        if (!listSelected[0].is_local()){
            val startIntent = FileBackgroundTransfer.getIntent(
                getApplication(),
                listSelected.toTypedArray()
            )
            ContextCompat.startForegroundService(getApplication(), startIntent)
            return
        }

        val urisList = arrayListOf<Uri>()

        for (i in listSelected){
            if (i.is_file()){
                urisList.add(FileProvider.getUriForFile(getApplication(),
                    BuildConfig.APPLICATION_ID + ".provider", File(i.path)
                ))
            } else{
                File(i.path).walk().filter{ it.isFile }.forEach {
                    urisList.add(FileProvider.getUriForFile(getApplication(),
                        BuildConfig.APPLICATION_ID + ".provider", it
                    ))
                }
            }
        }

        if (urisList.size == 1){
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "*/*"//contentResolver.getType(urisList[0])
            intent.putExtra(Intent.EXTRA_STREAM, urisList[0])
            needLaunchIntent.value = intent
        }
        if (urisList.size > 1){
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "*/*"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisList);
            needLaunchIntent.value = intent
        }
    }
    fun shareLocalFile(items:List<TdObject> = selectedItems){

        if (selectedItems.size > 1){
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            val urisList = arrayListOf<Uri>()
            for (i in selectedItems){
                if (i.is_file() && i.is_local()){
                    urisList.add(FileProvider.getUriForFile(getApplication(),
                        BuildConfig.APPLICATION_ID + ".provider", File(i.path)
                    ))
                }
            }
            intent.type = "*/*"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisList);
            needLaunchIntent.value = intent
        }else{
            val intent = Intent(Intent.ACTION_SEND)
            val uri = FileProvider.getUriForFile(getApplication(),
                BuildConfig.APPLICATION_ID + ".provider", File(selectedItems[0].path)
            )
            intent.type = "*/*"//contentResolver.getType(uri)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            needLaunchIntent.value = intent
        }
    }
}