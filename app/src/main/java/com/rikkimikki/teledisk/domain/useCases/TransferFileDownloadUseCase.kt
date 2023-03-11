package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.baseClasses.TdObject
import com.rikkimikki.teledisk.domain.TdRepository

class TransferFileDownloadUseCase(private val repository: TdRepository) {
    suspend operator fun invoke(from: TdObject) = repository.transferFileDownload(from)
}