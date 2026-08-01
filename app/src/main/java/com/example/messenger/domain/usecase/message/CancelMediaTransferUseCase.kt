package com.example.messenger.domain.usecase.message

import com.example.messenger.domain.repository.IMediaRepository
import javax.inject.Inject

class CancelMediaTransferUseCase @Inject constructor(
    private val mediaRepository: IMediaRepository,
) {
    fun upload(messageId: String, itemId: String) =
        mediaRepository.cancelUpload(messageId, itemId)

    fun download(itemId: String) = mediaRepository.cancelDownload(itemId)
}
