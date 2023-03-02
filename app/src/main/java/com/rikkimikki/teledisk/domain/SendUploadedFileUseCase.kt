package com.rikkimikki.teledisk.domain

import org.drinkless.td.libcore.telegram.TdApi

class SendUploadedFileUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(chatId:Long,doc: TdApi.InputMessageContent) = repository.sendUploadedFile(chatId,doc)
}