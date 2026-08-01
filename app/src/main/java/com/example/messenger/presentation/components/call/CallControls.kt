package com.example.messenger.presentation.components.call
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.screens.ui.theme.messengerTokens

@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    iconTint: Color = Color.White,
    size: Dp = 58.dp,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(size).background(backgroundColor, CircleShape),
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = iconTint)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = messengerTokens.textPrimary.copy(0.7f), fontSize = 12.sp)
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CallControlButtonPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CallControlButton(Icons.Default.Call, "Accept", MaterialTheme.colorScheme.primary, onClick = {})
                CallControlButton(Icons.Default.Call, "End", Color.Red, onClick = {})
            }
        }
    }
}
