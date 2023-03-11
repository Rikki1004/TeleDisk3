package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.baseClasses.TdObject
import com.rikkimikki.teledisk.domain.TdRepository

class CreateFolderUseCase(private val repository: TdRepository) {
    suspend operator fun invoke(folder: TdObject, name:String) = repository.createFolder(folder,name)
}