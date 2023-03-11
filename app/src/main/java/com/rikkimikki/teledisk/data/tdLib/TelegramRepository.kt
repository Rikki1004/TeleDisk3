package com.rikkimikki.teledisk.data.tdLib


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.rikkimikki.teledisk.domain.*
import com.rikkimikki.teledisk.domain.baseClasses.FileType
import com.rikkimikki.teledisk.domain.baseClasses.FiltersFromType
import com.rikkimikki.teledisk.domain.baseClasses.PlaceType
import com.rikkimikki.teledisk.domain.baseClasses.TdObject
import com.rikkimikki.teledisk.utils.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.telegram.core.TelegramFlow
import kotlinx.telegram.coroutines.*
import kotlinx.telegram.extensions.ChatKtx
import kotlinx.telegram.extensions.UserKtx
import kotlinx.telegram.flows.authorizationStateFlow
import kotlinx.telegram.flows.fileFlow
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.Chat
import java.io.File
import kotlin.concurrent.thread


object TelegramRepository : UserKtx, ChatKtx , TdRepository {
    val dataFromStore = SingleLiveData<List<TdObject>>()
    val is_ready = MutableLiveData<Boolean>()
    val allChats = MutableLiveData<List<Chat>>()
    private val needOpenLD = SingleLiveData<Pair<String,Boolean>>()
    private val shareRemoteFiles = SingleLiveData<List<TdObject>>()
    override val api: TelegramFlow = TelegramFlow()
    private var counter = 0
    var currentLocalFolderPath = GLOBAL_MAIN_STORAGE_PATH

    val downloadLD = api.fileFlow().asLiveData()
    val authFlow = api.authorizationStateFlow()
        .onEach {
            checkRequiredParams(it)
        }
        .map {
            when (it) {
                is TdApi.AuthorizationStateReady -> AuthState.LoggedIn
                is TdApi.AuthorizationStateWaitCode -> AuthState.EnterCode
                is TdApi.AuthorizationStateWaitPassword -> AuthState.EnterPassword(it.passwordHint)
                is TdApi.AuthorizationStateWaitPhoneNumber -> AuthState.EnterPhone
                else -> null
            }
        }
    private suspend fun checkRequiredParams(state: TdApi.AuthorizationState?) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters ->
                api.setTdlibParameters(TelegramCredentials.parameters)
            is TdApi.AuthorizationStateWaitEncryptionKey ->{
                api.checkDatabaseEncryptionKey(null)
                is_ready.value = api.runCatching { api.getMe() }.isSuccess
            }
        }
    }

    private val messagesResult = mutableListOf<TdObject>()
    private suspend fun loadFolder(chatId: Long,requiredPath: String, order:Long = 0, offset:Int = -1,needShow:Boolean = true){
        val messages = api.getChatHistory(chatId,order,offset,100,false).messages
        if (offset == -1)
            messagesResult.clear()

        if (messages.isEmpty()) {
            if (needShow)
                dataFromStore.value = messagesResult
            folderList.clear()
            return
        }
        for (message in messages){
            when(message.content.constructor){
                TdApi.MessageDocument.CONSTRUCTOR -> {
                    val doc = message.content as TdApi.MessageDocument
                    val (name,path) = if (doc.document.fileName == FOLDER){
                        prepareFileName(doc.caption.text,requiredPath,chatId)

                        if (!needShow && doc.caption.text == (delL(requiredPath))+"/"){
                            messagesResult.add(
                                TdObject(
                                doc.document.fileName,
                                PlaceType.TeleDisk,
                                FileType.File,
                                doc.caption.text,
                                groupID = chatId,
                                fileID = doc.document.document.id,
                                messageID = message.id)
                            )
                        }
                        continue
                    }
                    else{
                        prepareFileName(doc.caption.text.ifBlank { doc.document.fileName.ifBlank { NO_NAME_FILE_NAME + counter++ } },requiredPath,chatId)

                    }
                    if (name.isBlank())
                        continue
                    val thumbnail = doc.document.thumbnail?.photo?.id
                    val id = doc.document.document.id
                    val messageId = message.id
                    val size = doc.document.document.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(
                        TdObject(name,
                            PlaceType.TeleDisk,FileType.File,path,size,time,thumbnail,chatId,id,messageId)
                    )
                }
                TdApi.MessageAudio.CONSTRUCTOR -> {
                    val audio = message.content as TdApi.MessageAudio
                    val (name,path) = prepareFileName(audio.caption.text.ifBlank { audio.audio.fileName.ifBlank { NO_NAME_FILE_NAME + counter++ } },requiredPath,chatId)

                    if (name.isBlank())
                        continue
                    val thumbnail = audio.audio.albumCoverThumbnail?.photo?.id
                    val id = audio.audio.audio.id
                    val messageId = message.id
                    val size = audio.audio.audio.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(
                        TdObject(name,
                            PlaceType.TeleDisk,FileType.File,path,size,time,thumbnail,chatId,id,messageId)
                    )
                }
                TdApi.MessagePhoto.CONSTRUCTOR -> {
                    val photo = message.content as TdApi.MessagePhoto
                    val thumbnail = photo.photo.sizes[0].photo.id
                    val (name,path) = prepareFileName(photo.caption.text.ifBlank { NO_NAME_FILE_NAME + counter++ +".jpeg" },requiredPath,chatId)
                    if (name.isBlank())
                        continue
                    val id = photo.photo.sizes[photo.photo.sizes.size-1].photo.id
                    val messageId = message.id
                    val size = photo.photo.sizes[photo.photo.sizes.size-1].photo.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(
                        TdObject(name,
                            PlaceType.TeleDisk,FileType.File,path,size,time,thumbnail,chatId,id,messageId)
                    )
                }
                TdApi.MessageVideo.CONSTRUCTOR -> {
                    val video = message.content as TdApi.MessageVideo
                    val thumbnail = video.video.thumbnail?.photo?.id
                    val (name,path) = prepareFileName(video.caption.text.ifBlank { video.video.fileName.ifBlank { NO_NAME_FILE_NAME + counter++ +".mp4" } },requiredPath,chatId)
                    if (name.isBlank())
                        continue
                    val id = video.video.video.id
                    val messageId = message.id
                    val size = video.video.video.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(
                        TdObject(name,
                            PlaceType.TeleDisk,FileType.File,path,size,time,thumbnail,chatId,id,messageId)
                    )
                }
                else -> {}
            }
        }
        loadFolder(chatId,requiredPath,messages.last().id,0,needShow)
    }



    private suspend fun getAllRemoteData(chatId: Long, filter: FiltersFromType, order:Long = 0, offset:Int = -1){
        val messages = api.getChatHistory(chatId,order,offset,100,false).messages
        if (offset == -1)
            messagesResult.clear()

        if (messages.isEmpty()) {
            dataFromStore.value = messagesResult
            return
        }
        for (message in messages){
            when(message.content.constructor){
                TdApi.MessageDocument.CONSTRUCTOR -> {
                    val doc = message.content as TdApi.MessageDocument

                    if (doc.document.fileName == FOLDER){
                        continue
                    }

                    val name = doc.caption.text.ifBlank { doc.document.fileName.ifBlank {NO_NAME_FILE_NAME + counter++ } }
                    val clearName = delL(name).substringAfterLast("/")

                    if (name.isBlank())
                        continue
                    val thumbnail = doc.document.thumbnail?.photo?.id
                    val id = doc.document.document.id
                    val messageId = message.id
                    val size = doc.document.document.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(
                        TdObject(clearName,
                            PlaceType.TeleDisk,FileType.File,name,size,time,thumbnail,chatId,id,messageId)
                    )
                }
                TdApi.MessageAudio.CONSTRUCTOR -> {
                    val audio = message.content as TdApi.MessageAudio
                    val name = audio.caption.text.ifBlank { audio.audio.fileName.ifBlank { NO_NAME_FILE_NAME + counter++ } }
                    val clearName = delL(name).substringAfterLast("/")

                    if (name.isBlank())
                        continue
                    val thumbnail = audio.audio.albumCoverThumbnail?.photo?.id
                    val id = audio.audio.audio.id
                    val messageId = message.id
                    val size = audio.audio.audio.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(
                        TdObject(clearName,
                            PlaceType.TeleDisk,FileType.File,name,size,time,thumbnail,chatId,id,messageId)
                    )
                }
                TdApi.MessagePhoto.CONSTRUCTOR -> {
                    val photo = message.content as TdApi.MessagePhoto
                    val thumbnail = photo.photo.sizes[0].photo.id
                    val name = photo.caption.text.ifBlank { NO_NAME_FILE_NAME + counter++ +".jpeg" }
                    val clearName = delL(name).substringAfterLast("/")
                    if (name.isBlank())
                        continue
                    val id = photo.photo.sizes[photo.photo.sizes.size-1].photo.id
                    val messageId = message.id
                    val size = photo.photo.sizes[photo.photo.sizes.size-1].photo.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(
                        TdObject(clearName,
                            PlaceType.TeleDisk,FileType.File,name,size,time,thumbnail,chatId,id,messageId)
                    )
                }
                TdApi.MessageVideo.CONSTRUCTOR -> {
                    val video = message.content as TdApi.MessageVideo
                    val thumbnail = video.video.thumbnail?.photo?.id
                    val name = video.caption.text.ifBlank { video.video.fileName.ifBlank { NO_NAME_FILE_NAME + counter++ +".mp4" } }
                    val clearName = delL(name).substringAfterLast("/")
                    if (name.isBlank())
                        continue
                    val id = video.video.video.id
                    val messageId = message.id
                    val size = video.video.video.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(
                        TdObject(clearName,
                            PlaceType.TeleDisk,FileType.File,name,size,time,thumbnail,chatId,id,messageId)
                    )
                }
                else -> {}
            }
        }
        getAllRemoteData(chatId,filter,messages.last().id,0)
    }


    private val folderList = mutableListOf<String>()
    private fun prepareFileName(name: String,requiredPath: String,chatId:Long):Pair <String,String>{
        val clearName = delL(name)
        val slashPosition = clearName.indexOf((requiredPath+(if (requiredPath=="/") "" else "/")).substring(1))
        val secondSlashPosition = clearName.indexOf("/",requiredPath.length)
        println(messagesResult)
        if (slashPosition == 0 ){
            if (secondSlashPosition != -1){
                val currentFolderPath = clearName.substring(0,secondSlashPosition)
                if (currentFolderPath !in folderList){
                    folderList.add(currentFolderPath)
                    messagesResult.add(
                        TdObject(currentFolderPath.substring(
                        currentFolderPath.lastIndexOf("/")+1),
                        PlaceType.TeleDisk,
                        FileType.Folder,
                        "/$currentFolderPath",
                        groupID = chatId,
                        fileID = (1..Int.MAX_VALUE).random())
                    )
                }
            }else
                return Pair(clearName.substring(clearName.lastIndexOf("/")+1), "/$clearName")
        }
        println(slashPosition)
        return Pair("","")
    }
    private fun delL(word:String):String{
        var newWord = word.replace("\\", "/")
        if (newWord.startsWith("/") || newWord.startsWith(".")){
            newWord = delL(newWord.substring(1))
        }
        return newWord
    }
    private fun delP(word:String):String{
        var newWord = word
        if (word.startsWith("/"))
            newWord = delP(word.substring(1))
        if (word.endsWith("/"))
            newWord = delP(word.substring(word.length))
        return newWord
    }

    private var offsetChatId = 0L
    private var offsetOrder = Long.MAX_VALUE
    private val chatsResult = mutableListOf<Chat>()
    private suspend fun loadAllChats() {
        val chatsPart = api.getChats(TdApi.ChatListMain(), offsetOrder, offsetChatId,100).chatIds
        if (chatsPart.isEmpty()) {
            offsetChatId = 0L
            offsetOrder = Long.MAX_VALUE
            /*if (chatsResult.isEmpty()){
                chatsResult.add(api.createNewBasicGroupChat(intArrayOf(api.getMe().id),"|$TELEDISK|"))
            }*/
            allChats.value = chatsResult
            chatsResult.clear()
            return
        }
        offsetChatId = api.getChat(chatsPart.last()).id
        offsetOrder = api.getChat(chatsPart.last()).order

        for (chat in chatsPart){
            api.getChat(chat).let {
                if (it.title.startsWith("|") && it.title.endsWith("|")){
                    chatsResult.add(api.getChat(chat))
                }
            }
        }
        loadAllChats()
    }

    private fun getDataFromDisk(path:String){
        val tempList = mutableListOf<TdObject>()

        File(path).listFiles()?.forEach {
            if (it.isFile)
                tempList.add(
                    TdObject(it.name,
                        PlaceType.Local,FileType.File,it.absolutePath,it.length(),it.lastModified())
                )
            else
                tempList.add(
                    TdObject(it.name,
                        PlaceType.Local,FileType.Folder,it.absolutePath, 0L,it.lastModified())
                )
        }
        dataFromStore.postValue(tempList)
    }

    override suspend fun getRemoteFilesFiltered(id: Long,filter: FiltersFromType): LiveData<List<TdObject>> {
        getAllRemoteData(id,filter)
        return dataFromStore
    }

    override fun getLocalFilesFiltered(filter: FiltersFromType): LiveData<List<TdObject>> {
        thread {
            val tempList = mutableListOf<TdObject>()
            val path = currentLocalFolderPath

            File(path).walk().filter{ it.isFile && filter.ext.any { suffix -> it.name.lowercase().endsWith(suffix) } }.forEach {
                tempList.add(
                    TdObject(it.name,
                        PlaceType.Local,FileType.File,it.absolutePath,it.length(),it.lastModified())
                )
            }
            dataFromStore.postValue(tempList)
        }
        return dataFromStore

    }

    suspend fun sendPhone(phone: String) {
        api.setAuthenticationPhoneNumber(phone, null)
    }

    suspend fun sendCode(code: String) {
        api.checkAuthenticationCode(code)
    }

    suspend fun sendPassword(password: String) {
        api.checkAuthenticationPassword(
            password
        )
    }

    override fun fileOperationComplete(): SingleLiveData<Pair<String, Boolean>> {
        return needOpenLD
    }
    override suspend fun transferFileDownload(from: TdObject) : TdApi.File{
        return api.downloadFile(from.fileID,31,0,0,false)
    }
    override suspend fun transferFileUpload(from: TdObject) : TdApi.File{
        val file = TdApi.InputFileLocal(from.path)
        return api.uploadFile(file, TdApi.FileTypeDocument(),31)
    }
    override suspend fun sendUploadedFile(chatId:Long,doc: TdApi.InputMessageContent):TdApi.File{
        val options = TdApi.SendMessageOptions(true,false,null)
        val result = api.sendMessage(chatId,0, options,null, doc).content as TdApi.MessageDocument
        return result.document.document
    }
    override suspend fun loadThumbnail(id: Int): TdApi.File {
        return api.downloadFile(id,32,0,0,true)
    }
    override suspend fun getAllChats() : LiveData<List<Chat>>{
        loadAllChats()
        return allChats
    }
    override suspend fun createGroup(name: String) {
        api.createNewBasicGroupChat(intArrayOf(api.getMe().id),name)
        loadAllChats()
    }
    override suspend fun getRemoteFiles(id: Long, path: String): LiveData<List<TdObject>> {
        loadFolder(id,path)
        return dataFromStore
    }
    override suspend fun getRemoteFilesNoLD(id: Long, path: String): List<TdObject> {
        loadFolder(id,path, needShow = false)
        return messagesResult.toList()
    }
    override fun getLocalFiles(path: String): LiveData<List<TdObject>> {
        thread { getDataFromDisk(path) }
        return dataFromStore
    }
    override fun getLocalFilesNoLD(path: String): List<TdObject> {
        val tempList = mutableListOf<TdObject>()

        File(path).listFiles()?.forEach {
            if (it.isFile)
                tempList.add(
                    TdObject(it.name,
                        PlaceType.Local,FileType.File,it.absolutePath,it.length(),it.lastModified())
                )
            else
                tempList.add(
                    TdObject(it.name,
                        PlaceType.Local,FileType.Folder,it.absolutePath, 0L,it.lastModified())
                )
        }
        return tempList
    }
    override suspend fun createFolder(folder: TdObject, name: String) {
        val remotePath = delL(folder.path)
        if (folder.is_local()){
            File("$remotePath/$name/").mkdir()
        }else{
            val groupId = folder.groupID
            val tempFileDir = File(GLOBAL_PATH_TO_FILES,FOLDER)

            tempFileDir.writeText(FOLDER)
            val inputFileLocal = TdApi.InputFileLocal(tempFileDir.absolutePath)
            val formattedText = TdApi.FormattedText(remotePath+(if (remotePath.isBlank()) "" else "/")+name+"/", arrayOf())
            val doc = TdApi.InputMessageDocument(inputFileLocal,TdApi.InputThumbnail(), formattedText)
            sendUploadedFile(groupId,doc)
        }
    }
    override suspend fun renameFile(file: TdObject, newName: String) {
        if (file.is_local()){
            File(file.path).renameTo(File(file.getFilePath(),newName))
        }else{
            val chatId = file.groupID
            val messageId = file.messageID
            val folderPath = file.getFilePath()
            val caption = TdApi.FormattedText(folderPath+newName, arrayOf())
            api.editMessageCaption(chatId,messageId,null,caption)
        }
    }
    override suspend fun renameFolder(folder: TdObject, newName: String) {
        val clear = delP(folder.path)
        val startPath = Pair(delL(folder.path), clear.substringBeforeLast("/")+"/"+newName)
        renameFolderHelper(folder,newName,startPath)
    }
    private suspend fun renameFolderHelper(folder: TdObject, newName: String, startPath :Pair<String,String>) {
        if (folder.is_local()){
            File(folder.path).renameTo(File(folder.getFilePath(),newName))
        }else{
            loadFolder(folder.groupID,folder.path, needShow = false)

            for (item in messagesResult.toList()){
                if (item.is_folder()){
                    renameFolderHelper(item,newName,startPath)
                }else{
                    val newPath = item.path.replaceFirst(startPath.first, startPath.second)
                    val caption = TdApi.FormattedText(newPath, arrayOf())
                    api.editMessageCaption(item.groupID,item.messageID,null,caption)
                }
            }
        }
    }
    override suspend fun deleteFolder(folder: TdObject) {
        val messages = mutableListOf<Long>()

        suspend fun deleteFolderHelper(folder: TdObject) {
            if (folder.is_local()) {
                File(folder.path).deleteRecursively()
            } else {
                loadFolder(folder.groupID, folder.path, needShow = false)

                for (item in messagesResult.toList()) {
                    if (item.is_folder()) {
                        deleteFolder(item)
                    } else {
                        messages.add(item.messageID)
                    }
                }
            }
        }
        deleteFolderHelper(folder)
        if (!folder.is_local() && messages.isNotEmpty())
            api.deleteMessages(folder.groupID,messages.toLongArray(),true)
    }
    override suspend fun deleteFile(file: TdObject) {
        if (file.is_local()){
            File(file.path).delete()
        }else{
            val chatId = file.groupID
            val messageId = file.messageID
            api.deleteMessages(chatId, longArrayOf(messageId),true)
        }
    }
    override suspend fun cancelFileTransfer(id: Int, is_download: Boolean) {
        if (is_download)
            api.cancelDownloadFile(id,false)
        else
            api.cancelUploadFile(id)
    }
    override fun tempPathsForSend(): SingleLiveData<List<TdObject>> {
        return shareRemoteFiles
    }
}