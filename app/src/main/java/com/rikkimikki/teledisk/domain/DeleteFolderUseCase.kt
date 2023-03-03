package com.rikkimikki.teledisk.domain

class DeleteFolderUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(folder:TdObject) = repository.deleteFolder(folder)
}