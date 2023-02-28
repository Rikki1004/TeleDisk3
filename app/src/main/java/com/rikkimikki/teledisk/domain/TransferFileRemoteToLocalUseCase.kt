package com.rikkimikki.teledisk.domain

class TransferFileRemoteToLocalUseCase(private val repository:TdRepository) {
    operator fun invoke(from:TdObject,to: TdObject) = repository.transferFileRemoteToLocal(from,to)
}