package com.rikkimikki.teledisk.domain


class GetLocalFilesNoLDUseCase(private val repository:TdRepository) {
    operator fun invoke(path: String) = repository.getLocalFilesNoLD(path)
}