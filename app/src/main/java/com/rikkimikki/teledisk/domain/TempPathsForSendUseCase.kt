package com.rikkimikki.teledisk.domain

class TempPathsForSendUseCase(private val repository:TdRepository) {
    operator fun invoke() = repository.tempPathsForSend()
}