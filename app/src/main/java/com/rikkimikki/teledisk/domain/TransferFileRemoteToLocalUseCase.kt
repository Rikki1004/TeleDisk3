package com.rikkimikki.teledisk.domain

class TransferFileRemoteToLocalUseCase(private val repository:TdRepository) {
    operator fun invoke(from:Tfile,to: Tfolder) = repository.transferFileRemoteToLocal(from,to)
}