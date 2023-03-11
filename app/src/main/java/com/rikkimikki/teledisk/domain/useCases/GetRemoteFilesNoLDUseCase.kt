package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.TdRepository


class GetRemoteFilesNoLDUseCase(private val repository: TdRepository) {
    suspend operator fun invoke(id: Long,path: String) = repository.getRemoteFilesNoLD(id,path)
}