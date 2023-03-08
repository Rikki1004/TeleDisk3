package com.rikkimikki.teledisk.domain

class GetAllFilteredRemoteFilesUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(id: Long,filter:FiltersFromType) = repository.getRemoteFilesFiltered(id,filter)
}