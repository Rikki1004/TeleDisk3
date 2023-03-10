package com.rikkimikki.teledisk.domain

class CreateGroupUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(name:String) = repository.createGroup(name)
}