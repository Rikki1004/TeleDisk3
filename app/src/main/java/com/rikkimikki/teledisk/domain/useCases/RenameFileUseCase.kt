package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.baseClasses.TdObject
import com.rikkimikki.teledisk.domain.TdRepository

class RenameFileUseCase(private val repository: TdRepository) {
    suspend operator fun invoke(file: TdObject, newName:String) = repository.renameFile(file,newName)
}