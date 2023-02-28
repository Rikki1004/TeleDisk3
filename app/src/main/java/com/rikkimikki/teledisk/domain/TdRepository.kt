package com.rikkimikki.teledisk.domain

import androidx.lifecycle.LiveData
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
    fun transferFileLocalToLocal(from:TdObject,to: TdObject)
    fun transferFileLocalToRemote(from:TdObject,to: TdObject)
    fun transferFileRemoteToLocal(from:TdObject,to: TdObject)
    fun transferFileRemoteToRemote(from:TdObject,to: TdObject)
}