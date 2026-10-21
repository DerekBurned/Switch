package com.example.messenger.presentation.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.presentation.components.media.CameraCaptureScreen
import com.example.messenger.presentation.components.profile.AvatarPhotoViewer
import com.example.messenger.presentation.featureflag.featureEnabled

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onStartEditing: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showViewer by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    val uploadEnabled = featureEnabled(FeatureFlag.PROFILE_AVATAR_UPLOAD)
    val cameraEnabled = featureEnabled(FeatureFlag.PROFILE_AVATAR_CAMERA)

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
                showChooser = uploadEnabled
            }
        },
    )

    if (showViewer && viewerPhotos.isNotEmpty()) {
        AvatarPhotoViewer(
            photos = viewerPhotos,
            onDismiss = { showViewer = false },
            onAddClick = if (uploadEnabled) {
                { showChooser = true }
            } else {
                null
            },
        )
    }

    if (showChooser && uploadEnabled) {
        AvatarSourceSheet(
            cameraEnabled = cameraEnabled,
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

    if (showCamera && cameraEnabled) {
        CameraCaptureScreen(
            onResult = { uri, _, _ ->
                viewModel.addPhoto(uri)
                showCamera = false
            },
            onDismiss = { showCamera = false },
        )
    }
}
