package com.example.messenger.domain.usecase.message

import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.repository.IMediaRepository
import javax.inject.Inject

class DownloadMediaUseCase @Inject constructor(
    private val mediaRepository: IMediaRepository,
) {
    operator fun invoke(item: MediaItem) {
        if (item.url.isBlank()) return
        mediaRepository.downloadMedia(item)
    }
}
