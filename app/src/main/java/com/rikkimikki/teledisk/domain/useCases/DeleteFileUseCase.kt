package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.baseClasses.TdObject
import com.rikkimikki.teledisk.domain.TdRepository

class DeleteFileUseCase(private val repository: TdRepository) {
    suspend operator fun invoke(file: TdObject) = repository.deleteFile(file)
}