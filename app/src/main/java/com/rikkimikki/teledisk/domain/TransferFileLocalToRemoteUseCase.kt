package com.rikkimikki.teledisk.domain

class TransferFileLocalToRemoteUseCase(private val repository:TdRepository) {
    operator fun invoke(from:TdObject,to: TdObject) = repository.transferFileLocalToRemote(from,to)
}