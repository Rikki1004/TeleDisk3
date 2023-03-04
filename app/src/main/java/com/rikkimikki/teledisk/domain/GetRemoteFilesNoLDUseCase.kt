package com.rikkimikki.teledisk.domain


class GetRemoteFilesNoLDUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(id: Long,path: String) = repository.getRemoteFilesNoLD(id,path)
}