package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.TdRepository

class CreateGroupUseCase(private val repository: TdRepository) {
    suspend operator fun invoke(name:String) = repository.createGroup(name)
}