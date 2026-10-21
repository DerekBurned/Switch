package com.example.messenger.presentation.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.theme.messengerTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AvatarSourceSheet(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onDismiss: () -> Unit,
    cameraEnabled: Boolean = true,
) {
    val tokens = messengerTokens
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            AvatarSourceRow(
                icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = tokens.accent) },
                label = "Choose from gallery",
                onClick = onGallery,
            )
            if (cameraEnabled) {
                AvatarSourceRow(
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = tokens.accent) },
                    label = "Take a photo",
                    onClick = onCamera,
                )
            }
        }
    }
}
