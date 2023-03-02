package com.rikkimikki.teledisk.domain

class TransferFileDownloadUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(from:TdObject) = repository.transferFileDownload(from)
}