package com.rikkimikki.teledisk.domain

class CreateFileUseCase (private val repository:TdRepository){

    operator fun invoke(path: String,name:String) = repository.createFile(path,name)
}