package com.rikkimikki.teledisk.domain

class TransferFileLocalToLocalUseCase(private val repository:TdRepository) {
    operator fun invoke(from:TdObject,to: TdObject) = repository.transferFileLocalToLocal(from,to)
}