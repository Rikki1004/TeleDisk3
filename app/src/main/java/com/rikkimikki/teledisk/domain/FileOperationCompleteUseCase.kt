package com.rikkimikki.teledisk.domain

class FileOperationCompleteUseCase (private val repository:TdRepository){
    operator fun invoke() = repository.fileOperationComplete()
}