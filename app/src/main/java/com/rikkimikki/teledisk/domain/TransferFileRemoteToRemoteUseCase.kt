package com.rikkimikki.teledisk.domain

class TransferFileRemoteToRemoteUseCase(private val repository:TdRepository) {
    operator fun invoke(from:Tfile,to: Tfolder) = repository.transferFileRemoteToRemote(from,to)
}