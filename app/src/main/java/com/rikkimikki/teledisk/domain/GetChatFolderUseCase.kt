package com.rikkimikki.teledisk.domain

class GetChatFolderUseCase(private val repository:TdRepository)  {
    operator fun invoke(id:Int) = repository.getChatFolder(id)
}