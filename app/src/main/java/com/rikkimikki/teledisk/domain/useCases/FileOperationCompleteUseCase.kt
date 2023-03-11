package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.TdRepository

class FileOperationCompleteUseCase (private val repository: TdRepository){
    operator fun invoke() = repository.fileOperationComplete()
}