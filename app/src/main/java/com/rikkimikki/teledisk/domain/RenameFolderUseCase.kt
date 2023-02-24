package com.rikkimikki.teledisk.domain

class RenameFolderUseCase(private val repository:TdRepository) {
    operator fun invoke(folder: Tfolder,newName:String) = repository.renameFolder(folder,newName)
}