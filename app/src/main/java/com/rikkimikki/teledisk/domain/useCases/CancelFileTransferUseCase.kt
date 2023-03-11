package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.TdRepository

class CancelFileTransferUseCase (private val repository: TdRepository){
    suspend operator fun invoke(id:Int,is_download:Boolean) = repository.cancelFileTransfer(id,is_download)
}