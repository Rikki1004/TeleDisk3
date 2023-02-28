package com.rikkimikki.teledisk.domain

class TransferFileRemoteToRemoteUseCase(private val repository:TdRepository) {
    operator fun invoke(from:TdObject,to: TdObject) = repository.transferFileRemoteToRemote(from,to)
}