package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.TdRepository

class GetRemoteFilesUseCase(private val repository: TdRepository) {
    suspend operator fun invoke(id: Long,path: String) = repository.getRemoteFiles(id,path)
}