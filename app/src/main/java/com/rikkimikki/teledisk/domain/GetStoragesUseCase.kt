package com.rikkimikki.teledisk.domain

class GetStoragesUseCase(private val repository:TdRepository) {
    operator fun invoke() = repository.getStorages()
}