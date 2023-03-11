package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.baseClasses.FiltersFromType
import com.rikkimikki.teledisk.domain.TdRepository

class GetAllFilteredLocalFilesUseCase(private val repository: TdRepository) {
    operator fun invoke(filter: FiltersFromType) = repository.getLocalFilesFiltered(filter)
}