package com.rikkimikki.teledisk.domain

class LoadThumbnailUseCase(private val repository:TdRepository) {
    suspend operator fun invoke(id:Int) = repository.loadThumbnail(id)
}
