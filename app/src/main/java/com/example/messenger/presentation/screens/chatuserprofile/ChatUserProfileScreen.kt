package com.example.messenger.presentation.screens.chatuserprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChatUserProfileScreen(
    viewModel: ChatUserProfileViewModel = hiltViewModel(),
    sharedKeyPartnerId: String = "",
    onBackClick: () -> Unit = {},
    onCallClick: (partnerId: String, partnerName: String, partnerPhone: String) -> Unit = { _, _, _ -> },
    onVideoCallClick: (partnerId: String, partnerName: String, partnerPhone: String) -> Unit = { _, _, _ -> },
    onEditClick: () -> Unit = {}
    ) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipPlayback = viewModel.clipPlayback.collectAsStateWithLifecycle()
    ChatUserProfileScreenContent(
        state = state,
        sharedKeyPartnerId = sharedKeyPartnerId,
        onBackClick = onBackClick,
        onCallClick = onCallClick,
        onVideoCallClick = onVideoCallClick,
        onEditClick = onEditClick,
        onMediaTabChange = viewModel::onMediaTabChange,
        clipPlayback = clipPlayback,
        onLoadMoreMedia = viewModel::loadMoreMedia,
        onDownloadMedia = viewModel::downloadMedia,
        onCancelDownload = viewModel::cancelDownload,
        onCancelUpload = viewModel::cancelUpload,
        onToggleVoice = viewModel::toggleVoice,
    )
}
