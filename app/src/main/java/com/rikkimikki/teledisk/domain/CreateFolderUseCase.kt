package com.rikkimikki.teledisk.domain

class CreateFolderUseCase(private val repository:TdRepository) {
    operator fun invoke(path: String,name:String) = repository.createFolder(path,name)
}