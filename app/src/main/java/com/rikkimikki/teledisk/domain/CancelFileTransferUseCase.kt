package com.rikkimikki.teledisk.domain

class CancelFileTransferUseCase (private val repository:TdRepository){
    suspend operator fun invoke(id:Int,is_download:Boolean) = repository.cancelFileTransfer(id,is_download)
}