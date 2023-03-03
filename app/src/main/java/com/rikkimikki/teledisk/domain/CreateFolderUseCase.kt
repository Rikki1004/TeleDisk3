package com.rikkimikki.teledisk.domain

class CreateFolderUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(folder:TdObject,name:String) = repository.createFolder(folder,name)
}