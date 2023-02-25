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
    fun renameFile(file: Tfile,newName:String)
    fun renameFolder(folder: Tfolder,newName:String)
    fun transferFileLocalToLocal(from:Tfile,to: Tfolder)
    fun transferFileLocalToRemote(from:Tfile,to: Tfolder)
    fun transferFileRemoteToLocal(from:Tfile,to: Tfolder)
    fun transferFileRemoteToRemote(from:Tfile,to: Tfolder)
}