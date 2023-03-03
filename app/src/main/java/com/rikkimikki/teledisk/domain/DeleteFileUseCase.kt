package com.rikkimikki.teledisk.domain

class DeleteFileUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(file:TdObject) = repository.deleteFile(file)
}