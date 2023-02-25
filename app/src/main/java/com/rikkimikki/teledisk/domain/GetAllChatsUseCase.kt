package com.rikkimikki.teledisk.domain

class GetAllChatsUseCase(private val repository:TdRepository) {
    suspend operator fun invoke() = repository.getAllChats()
}

