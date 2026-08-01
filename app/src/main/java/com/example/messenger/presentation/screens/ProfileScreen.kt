package com.example.messenger.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.NavHeaderPill
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.components.media.CameraCaptureScreen
import com.example.messenger.presentation.components.profile.AvatarPhotoViewer
import com.example.messenger.presentation.screens.ui.theme.Dimens
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.state.ProfileUiState
import com.example.messenger.presentation.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onStartEditing: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showViewer by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.addPhoto(it) } }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) showCamera = true }

    val viewerPhotos = uiState.photos.ifEmpty {
        listOfNotNull(uiState.user?.avatarUrl).filter { it.isNotBlank() }
    }

    ProfileScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onEditClick = onStartEditing,
        onAvatarClick = {
            if (viewerPhotos.isNotEmpty()) {
                viewModel.loadPhotos()
                showViewer = true
            } else {
                showChooser = true
            }
        },
    )

    if (showViewer && viewerPhotos.isNotEmpty()) {
        AvatarPhotoViewer(
            photos = viewerPhotos,
            onDismiss = { showViewer = false },
            onAddClick = { showChooser = true },
        )
    }

    if (showChooser) {
        AvatarSourceSheet(
            onGallery = {
                showChooser = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onCamera = {
                showChooser = false
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) showCamera = true else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onDismiss = { showChooser = false },
        )
    }

    if (showCamera) {
        CameraCaptureScreen(
            onResult = { uri, _, _ ->
                viewModel.addPhoto(uri)
                showCamera = false
            },
            onDismiss = { showCamera = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarSourceSheet(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = messengerTokens
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            AvatarSourceRow(
                icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = tokens.accent) },
                label = "Choose from gallery",
                onClick = onGallery,
            )
            AvatarSourceRow(
                icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = tokens.accent) },
                label = "Take a photo",
                onClick = onCamera,
            )
        }
    }
}

@Composable
private fun AvatarSourceRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    val tokens = messengerTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.size(16.dp))
        Text(text = label, color = tokens.textPrimary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ProfileScreenContent(
    uiState: ProfileUiState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onAvatarClick: () -> Unit = {},
) {
    val tokens = messengerTokens
    val user = uiState.user
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        NavHeaderPill(title = "Profile", onBack = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            MessengerAvatar(
                name = user?.username.orEmpty(),
                photoUrl = uiState.photos.firstOrNull() ?: user?.avatarUrl,
                size = Dimens.avatarLarge,
                modifier = Modifier.clickable(onClick = onAvatarClick),
            )
            Spacer(modifier = Modifier.height(24.dp))
            RoundedCard {
                ProfileDisplayRow(
                    label = "Phone number",
                    value = user?.phoneNumber?.getFullNumber().orEmpty(),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            RoundedCard {
                ProfileDisplayRow(label = "Name", value = user?.username.orEmpty())
                CardDivider()
                ProfileDisplayRow(
                    label = "Username",
                    value = user?.username?.let { "@$it" }.orEmpty(),
                )
                CardDivider()
                ProfileDisplayRow(label = "Email", value = user?.email.orEmpty())
            }
            Spacer(modifier = Modifier.height(28.dp))
            PillButton(text = "Edit profile", onClick = onEditClick)
        }
    }
}

@Composable
private fun ProfileDisplayRow(label: String, value: String) {
    val tokens = messengerTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = tokens.textMuted,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value.ifBlank { "—" },
            color = tokens.textOnField,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileScreenPreview() {
    MessengerTheme {
        WallpaperBackground {
            ProfileScreenContent(
                uiState = ProfileUiState(
                    user = User(
                        id = "preview-uid",
                        username = "Jane Doe",
                        email = "jane@example.com",
                    ),
                ),
                onBackClick = {},
                onEditClick = {},
            )
        }
    }
}
