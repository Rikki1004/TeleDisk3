package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.TdRepository

class GetAllChatsUseCase(private val repository: TdRepository) {
    suspend operator fun invoke() = repository.getAllChats()
}

