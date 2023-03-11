package com.rikkimikki.teledisk.domain.useCases

import com.rikkimikki.teledisk.domain.TdRepository

class LoadThumbnailUseCase(private val repository: TdRepository) {
    suspend operator fun invoke(id:Int) = repository.loadThumbnail(id)
}
