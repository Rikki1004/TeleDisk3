package com.rikkimikki.teledisk.domain

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.drinkless.td.libcore.telegram.TdApi

interface TdRepository {
    suspend fun getAllChats() : LiveData<List<TdApi.Chat>>
    fun getChatFolder(id:Int)
    suspend fun createFile(folder:TdObject,name:String)
    suspend fun createFolder(folder:TdObject,name:String)
    fun getLocalFiles(path: String) : LiveData<List<TdObject>>
    fun getLocalFilesNoLD(path: String) : List<TdObject>
    fun tempPathsForSend() : MutableLiveData<List<TdObject>>
    suspend fun getRemoteFiles(id: Long,path: String) : LiveData<List<TdObject>>
    suspend fun getRemoteFilesNoLD(id: Long,path: String) : List<TdObject>
    fun getStorages() : LiveData<List<ScopeType>>


    suspend fun getRemoteFilesFiltered(id: Long,filter:FiltersFromType) : LiveData<List<TdObject>>
    fun getLocalFilesFiltered(filter:FiltersFromType) : LiveData<List<TdObject>>

    suspend fun renameFile(file: TdObject,newName:String)
    suspend fun renameFolder(folder: TdObject,newName:String)
    suspend fun deleteFolder(folder:TdObject)
    suspend fun deleteFile(file:TdObject)

    suspend fun transferFileDownload(from:TdObject) : TdApi.File
    suspend fun transferFileUpload(from:TdObject) : TdApi.File

    suspend fun sendUploadedFile(chatId:Long,doc: TdApi.InputMessageContent):TdApi.File
    suspend fun loadThumbnail(id:Int) : TdApi.File

    fun fileOperationComplete() : MutableLiveData<Pair<String, Boolean>>

}