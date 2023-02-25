package com.rikkimikki.teledisk.domain

import androidx.lifecycle.LiveData

class GetRemoteFilesUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(id: Long,path: String) = repository.getRemoteFiles(id,path)
}