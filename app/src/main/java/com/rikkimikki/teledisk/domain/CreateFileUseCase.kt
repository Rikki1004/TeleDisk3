package com.rikkimikki.teledisk.domain

class CreateFileUseCase (private val repository:TdRepository){
    suspend operator fun invoke(folder:TdObject,name:String) = repository.createFile(folder,name)
}