package com.example.messenger.presentation.components.common
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import com.example.messenger.presentation.screens.ui.theme.Dimens
import com.example.messenger.presentation.screens.ui.theme.avatarColorFor

@Composable
fun MessengerAvatar(
    name: String,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
    size: Dp = Dimens.avatarMedium,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColorFor(name)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase().ifBlank { "?" },
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.38f).sp,
        )
        if (!photoUrl.isNullOrBlank()) {
            val cachedPainter = remember(photoUrl) {
                val cache = SingletonImageLoader.get(context).memoryCache
                    ?: return@remember null
                val key = cache.keys.firstOrNull { it.key == photoUrl }
                    ?: return@remember null
                val bitmap = (cache[key]?.image as? BitmapImage)?.bitmap
                    ?: return@remember null
                BitmapPainter(bitmap.asImageBitmap())
            }
            if (cachedPainter != null) {
                Image(
                    painter = cachedPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            val scope = rememberCoroutineScope()
            val appearScale = remember(photoUrl) {
                Animatable(if (cachedPainter == null) 0.85f else 1f)
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .placeholderMemoryCacheKey(photoUrl)
                    .memoryCacheKey(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success && appearScale.value < 1f) {
                        scope.launch { appearScale.animateTo(1f, MotionSprings.pop) }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = appearScale.value
                        scaleY = appearScale.value
                    },
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessengerAvatarPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MessengerAvatar(name = "Dasha")
                MessengerAvatar(name = "Bob", size = 40.dp)
                MessengerAvatar(name = "", size = 64.dp)
            }
        }
    }
}
