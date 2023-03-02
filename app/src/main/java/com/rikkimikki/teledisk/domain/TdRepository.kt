package com.rikkimikki.teledisk.domain

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.drinkless.td.libcore.telegram.TdApi

interface TdRepository {
    suspend fun getAllChats() : LiveData<List<TdApi.Chat>>
    fun getChatFolder(id:Int)
    fun createFile(path: String,name:String)
    fun createFolder(path: String,name:String)
    fun getLocalFiles(path: String) : LiveData<List<TdObject>>
    suspend fun getRemoteFiles(id: Long,path: String) : LiveData<List<TdObject>>
    fun getStorages() : LiveData<List<ScopeType>>
    fun renameFile(file: TdObject,newName:String)
    fun renameFolder(folder: TdObject,newName:String)
    suspend fun transferFileDownload(from:TdObject) : TdApi.File
    suspend fun transferFileUpload(from:TdObject) : TdApi.File

    suspend fun sendUploadedFile(chatId:Long,doc: TdApi.InputMessageContent):TdApi.File
    suspend fun loadThumbnail(id:Int) : TdApi.File

    fun fileOperationComplete() : MutableLiveData<Pair<String, Boolean>>

}