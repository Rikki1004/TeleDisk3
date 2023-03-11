package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.TdRepository

class TempPathsForSendUseCase(private val repository: TdRepository) {
    operator fun invoke() = repository.tempPathsForSend()
}