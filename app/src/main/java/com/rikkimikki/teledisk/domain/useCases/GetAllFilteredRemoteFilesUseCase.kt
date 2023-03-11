package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.baseClasses.FiltersFromType
import com.rikkimikki.teledisk.domain.TdRepository

class GetAllFilteredRemoteFilesUseCase(private val repository: TdRepository) {
    suspend operator fun invoke(id: Long,filter: FiltersFromType) = repository.getRemoteFilesFiltered(id,filter)
}