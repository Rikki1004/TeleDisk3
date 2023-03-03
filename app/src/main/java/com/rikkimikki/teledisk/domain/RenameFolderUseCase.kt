package com.rikkimikki.teledisk.domain

class RenameFolderUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(folder: TdObject,newName:String) = repository.renameFolder(folder,newName)
}