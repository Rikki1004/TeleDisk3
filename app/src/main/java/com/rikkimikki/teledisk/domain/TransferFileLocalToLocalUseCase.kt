package com.rikkimikki.teledisk.domain

class TransferFileLocalToLocalUseCase(private val repository:TdRepository) {
    operator fun invoke(from:Tfile,to: Tfolder) = repository.transferFileLocalToLocal(from,to)
}