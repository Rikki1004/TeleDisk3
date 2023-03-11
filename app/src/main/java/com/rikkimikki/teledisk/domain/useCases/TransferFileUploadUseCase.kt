package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.baseClasses.TdObject
import com.rikkimikki.teledisk.domain.TdRepository

class TransferFileUploadUseCase(private val repository: TdRepository) {
    suspend operator fun invoke(from: TdObject) = repository.transferFileUpload(from)
}