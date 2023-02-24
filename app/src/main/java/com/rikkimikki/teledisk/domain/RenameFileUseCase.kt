package com.rikkimikki.teledisk.domain

class RenameFileUseCase(private val repository:TdRepository) {
    operator fun invoke(file: Tfile,newName:String) = repository.renameFile(file,newName)
}