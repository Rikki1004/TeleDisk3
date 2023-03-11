package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.TdRepository


class GetLocalFilesNoLDUseCase(private val repository: TdRepository) {
    operator fun invoke(path: String) = repository.getLocalFilesNoLD(path)
}