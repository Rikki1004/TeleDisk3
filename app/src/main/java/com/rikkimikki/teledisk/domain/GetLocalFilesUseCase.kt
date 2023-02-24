package com.rikkimikki.teledisk.domain

import androidx.lifecycle.LiveData

class GetLocalFilesUseCase(private val repository:TdRepository) {
    operator fun invoke(path: String) = repository.getLocalFiles(path)
}