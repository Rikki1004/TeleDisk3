package com.rikkimikki.teledisk.domain

class GetAllFilteredLocalFilesUseCase(private val repository:TdRepository) {
    operator fun invoke(filter:FiltersFromType) = repository.getLocalFilesFiltered(filter)
}