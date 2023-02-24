package com.rikkimikki.teledisk.domain

class TransferFileLocalToRemoteUseCase(private val repository:TdRepository) {
    operator fun invoke(from:Tfile,to: Tfolder) = repository.transferFileLocalToRemote(from,to)
}