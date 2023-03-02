package com.rikkimikki.teledisk.domain

class TransferFileUploadUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(from:TdObject) = repository.transferFileUpload(from)
}