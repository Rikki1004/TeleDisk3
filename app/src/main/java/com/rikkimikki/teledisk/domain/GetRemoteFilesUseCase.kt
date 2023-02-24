package com.rikkimikki.teledisk.domain

import androidx.lifecycle.LiveData

class GetRemoteFilesUseCase(private val repository:TdRepository) {
    operator fun invoke() = repository.getRemoteFiles()
}