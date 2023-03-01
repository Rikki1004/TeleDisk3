package com.rikkimikki.teledisk.domain

class TransferFileUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(from:TdObject) = repository.transferFile(from)
}