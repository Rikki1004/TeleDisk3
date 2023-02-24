package com.rikkimikki.teledisk.data.tdLib

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.rikkimikki.teledisk.domain.FileType
import com.rikkimikki.teledisk.domain.TdObject
import com.rikkimikki.teledisk.domain.Tfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.telegram.core.TelegramException
import kotlinx.telegram.core.TelegramFlow
import kotlinx.telegram.coroutines.*
import kotlinx.telegram.extensions.ChatKtx
import kotlinx.telegram.extensions.UserKtx
import kotlinx.telegram.flows.authorizationStateFlow
import kotlinx.telegram.flows.userFlow
import kotlinx.telegram.flows.userStatusFlow
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.Chat
import org.drinkless.td.libcore.telegram.TdApi.Message

object TelegramRepository : UserKtx, ChatKtx {

    val dataFromRemote = MutableLiveData<List<TdObject>>()

    val is_ready = MutableLiveData<Boolean>()
    var counter = 0


    override val api: TelegramFlow = TelegramFlow()

    val allChats = MutableLiveData<List<Chat>>()

    //val chathFlow = api.getUpdatesFlowOfType<TdApi.UpdateChatChatList>().onEach {
//TdApi.UpdateChatChatList

        //chatOffset = it.chatId
    // chatChatListFlow().onEach {
    //Thread.sleep(1000)
        //println("chat---------")
    //println("chat---------"+api.getChat(it.chatId).title)
//}

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
                //api.runCatching { api.getMe() }.exceptionOrNull()
                //is_ready.value = retryConnect()
                /*try {
                    api.runCatching { api.getMe() }.isSuccess

                    is_ready.value = true
                }catch (e:java.lang.Exception){
                    is_ready.value = false
                }*/

                //loadAllChats()
                //loadFolder(-567578282)
            }

        }
    }

    suspend fun retryConnect() :Boolean{
        delay(100)
        if (counter++ >= 5) return false
        return api.runCatching { api.getMe() }.fold({true},{retryConnect()})
    }


    private val messagesResult = mutableListOf<TdObject>()
    private suspend fun loadFolder(chatId: Long, order:Long = 0, offset:Int = -1){
        val messages = api.getChatHistory(chatId,order,offset,100,false).messages
        if (messages.isEmpty()) {
            dataFromRemote.value = messagesResult
            messagesResult.clear()
            return
        }
        for (message in messages){
            when(message.content.constructor){
                TdApi.MessageText.CONSTRUCTOR -> println(message.content as TdApi.MessageText)
                TdApi.MessageDocument.CONSTRUCTOR -> {
                    val doc = message.content as TdApi.MessageDocument
                    val thumbnail = doc.document.thumbnail?.photo
                    val name = doc.caption.text.ifBlank { doc.document.fileName.ifBlank { "noNameFile" } }
                    val id = doc.document.document.id.toLong()
                    val size = doc.document.document.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(Tfile(name,FileType.TeleDiskFile, size,"/"+name,time,thumbnail,chatId,id))
                }
                TdApi.MessagePhoto.CONSTRUCTOR -> {
                    val photo = message.content as TdApi.MessagePhoto
                    val thumbnail = photo.photo.sizes[photo.photo.sizes.size-1].photo
                    val name = photo.caption.text.ifBlank { "noNameFile" }
                    val id = photo.photo.sizes[0].photo.id.toLong()
                    val size = photo.photo.sizes[0].photo.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(Tfile(name,FileType.TeleDiskFile, size,"/"+name,time,thumbnail,chatId,id))
                }
                TdApi.MessageVideo.CONSTRUCTOR -> {
                    val video = message.content as TdApi.MessageVideo
                    val thumbnail = video.video.thumbnail?.photo
                    val name = video.caption.text.ifBlank { video.video.fileName.ifBlank { "noNameFile" } }
                    val id = video.video.video.id.toLong()
                    val size = video.video.video.size.toLong()
                    val time = (if (message.editDate == 0) message.date else message.editDate )*1000L
                    messagesResult.add(Tfile(name,FileType.TeleDiskFile, size,"/"+name,time,thumbnail,chatId,id))
                }
                else -> null
            }
        }
        loadFolder(chatId,messages.last().id,0)
    }

    private var offsetChatId = 0L
    private var offsetOrder = 9223372036854775807L
    private val chatsResult = mutableListOf<Chat>()
    private suspend fun loadAllChats() {
        val chatsPart = api.getChats(TdApi.ChatListMain(), offsetOrder, offsetChatId,100).chatIds
        if (chatsPart.isEmpty()) {
            offsetChatId = 0L
            offsetOrder = 9223372036854775807L
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



    val userInfoFlow = flowOf(
        api.userFlow(),
        api.userStatusFlow().map {
            api.getUser(it.userId)
        }
    ).flattenMerge().map { user: TdApi.User ->

        if (api.getMe().id == user.id) "it's me!"
        else {
            val userInfo = arrayListOf(user.firstName)

            if (user.getFullInfo().groupInCommonCount > 0) {
                user.getGroupsInCommon(0, 10).chatIds.map {
                    api.getChat(it).let { chat ->
                        val admins = chat.getAdministrators().administrators.map { admin ->
                            api.getUser(admin.userId).firstName
                        }.joinToString()
                        "    '${chat.title}'" +
                            (" admins: $admins".takeIf { admins.isNotBlank() } ?: "")
                    }
                }.joinToString("\n").let {
                    userInfo.add(" has chats in common:\n$it")
                }
            }

            userInfo.joinToString()
        }
    }.retryWhen { cause, _ -> cause is TelegramException }
}