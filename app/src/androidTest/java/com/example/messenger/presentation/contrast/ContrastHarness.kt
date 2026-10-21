package com.example.messenger.presentation.contrast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.ThemeMode
import com.example.messenger.presentation.theme.messengerTokens
import org.junit.Assert.fail
import kotlin.math.roundToInt

private const val LARGE_TEXT_MIN_HEIGHT_SP = 24f

internal enum class Backdrop {
    AppBackground,
    Card,
    Field,
    VideoBlack,
}

@Composable
private fun backdropColor(backdrop: Backdrop): Color {
    val tokens = messengerTokens
    return when (backdrop) {
        Backdrop.AppBackground -> tokens.backgroundBottom
        Backdrop.Card -> tokens.cardFill.compositeOver(tokens.backgroundBottom)
        Backdrop.Field -> tokens.fieldFill.compositeOver(tokens.backgroundBottom)
        Backdrop.VideoBlack -> Color.Black
    }
}

internal fun ComposeContentTestRule.assertReadableInBothThemes(
    component: String,
    backdrop: Backdrop = Backdrop.AppBackground,
    texts: Set<String>? = null,
    content: @Composable () -> Unit,
) {
    val mode = mutableStateOf(ThemeMode.LIGHT)
    setContent {
        MessengerTheme(themeMode = mode.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backdropColor(backdrop))
                    .padding(12.dp),
            ) {
                content()
            }
        }
    }

    val violations = mutableListOf<String>()
    for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
        mode.value = theme
        waitForIdle()
        violations += collectViolations(component, theme, texts)
    }

    if (violations.isNotEmpty()) {
        fail(violations.joinToString(separator = "\n", prefix = "\n"))
    }
}

private fun ComposeContentTestRule.collectViolations(
    component: String,
    theme: ThemeMode,
    texts: Set<String>?,
): List<String> {
    val density = InstrumentationRegistry.getInstrumentation()
        .targetContext.resources.displayMetrics.scaledDensity
    val bitmap = onRoot().captureToImage().asAndroidBitmap()
    val nodes = onAllNodes(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
        useUnmergedTree = true,
    ).fetchSemanticsNodes()

    val measured = mutableSetOf<String>()
    val violations = nodes.mapNotNull { node ->
        val text = node.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString(separator = " ") { it.text }
            .orEmpty()
        if (text.isBlank()) return@mapNotNull null
        if (texts != null && text !in texts) return@mapNotNull null

        val bounds = node.boundsInRoot
        val sample = bitmap.probeTextRegion(
            left = bounds.left.roundToInt(),
            top = bounds.top.roundToInt(),
            right = bounds.right.roundToInt(),
            bottom = bounds.bottom.roundToInt(),
        ) ?: return@mapNotNull null
        measured += text

        val heightSp = bounds.height / density
        val minRatio =
            if (heightSp >= LARGE_TEXT_MIN_HEIGHT_SP) WCAG_LARGE_TEXT else WCAG_NORMAL_TEXT

        val ratio = contrastRatio(sample.foreground, sample.background)
        if (ratio >= minRatio) return@mapNotNull null

        buildString {
            append(component)
            append(" [")
            append(theme)
            append("] \"")
            append(text)
            append("\" -> ")
            append(String.format("%.2f", ratio))
            append(":1 (needs ")
            append(String.format("%.1f", minRatio))
            append(":1) text=")
            append(sample.foreground.hex())
            append(" on=")
            append(sample.background.hex())
        }
    }

    val unmeasured = texts.orEmpty() - measured
    return violations + unmeasured.map { text ->
        "$component [$theme] \"$text\" was not found on screen or could not be measured"
    }
}

private fun Color.hex(): String = String.format("#%08X", toArgb())
