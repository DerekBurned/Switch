package com.example.messenger.presentation.screens.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val PrimaryBlue = Color(0xFF3C85B0)
val PrimaryBlueDark = Color(0xFF2E6F93)
val LightGray = Color(0xFFE0E0E0)
val ChatBackground = Color(0xFFF5F7FA)
val BubbleSent = Color(0xFF3C85B0)
val BubbleSentTop = Color(0xFF4FA0C9)
val BubbleReceived = Color(0xFFFFFFFF)
val BubbleReceivedText = Color(0xFF1F2937)
val OnlineGreen = Color(0xFF30C44A)
val AwayYellow = Color(0xFFFFC107)
val OfflineGray = Color(0xFF9E9E9E)
val DeliveredBlue = Color(0xFF2196F3)
val FailedRed = Color(0xFFF44336)
val DangerRed = Color(0xFFE0352B)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1F2937)
val OnSurfaceMuted = Color(0xFF6B7280)

val AccentTeal = Color(0xFF3C85B0)
val AccentTealStrong = Color(0xE63C85B0)
val AccentLight = Color(0xFF2E6F93)
val CallAcceptGreen = Color(0xFF30C44A)
val CallAcceptGreenLight = Color(0xFF1E9E3A)
val DeclineRed = Color(0xFFFF453A)
val DeclineRedLight = Color(0xFFE0352B)

val BgDarkTop = Color(0xFF1C1A20)
val BgDarkBottom = Color(0xFF16141B)
val BgLightTop = Color(0xFFEDE7F4)
val BgLightBottom = Color(0xFFD7CEE6)

val FieldFillDark = Color(0xEDFFFFFF)
val FieldFillLight = Color(0xEDFFFFFF)
val CardFillDark = Color(0x8C18151F)
val CardFillLight = Color(0xBDFFFFFF)

val PillTranslucentDark = Color(0x1AFFFFFF)
val PillTranslucentLight = Color(0x8CFFFFFF)
val PillWhite = Color(0xFFFFFFFF)
val TabTrackTranslucent = Color(0x52787880)
val TabTrackTranslucentLight = Color(0x8FFFFFFF)
val NeutralButtonDark = Color(0x29FFFFFF)
val NeutralButtonLight = Color(0x1F000000)

val LabelMutedDark = Color(0xA3FFFFFF)
val TextDarkPrimary = Color(0xFFFFFFFF)
val TextLightPrimary = Color(0xFF1A1722)
val TextLightMuted = Color(0x991A1722)
val TextOnFieldDark = Color(0xFF16141B)
val DividerOnLight = Color(0x12000000)
val DividerOnDark = Color(0x14FFFFFF)
val PanelBorderDark = Color(0x24FFFFFF)
val PanelBorderLight = Color(0xB3FFFFFF)

val AvatarPalette = listOf(
    Color(0xFF3C85B0),
    Color(0xFF5AA9C9),
    Color(0xFFEC407A),
    Color(0xFF30C44A),
    Color(0xFFFF9F0A),
    Color(0xFFE0584A),
    Color(0xFF2E6F93),
)

fun avatarColorFor(name: String): Color =
    if (name.isBlank()) AvatarPalette[0]
    else AvatarPalette[(name.hashCode().let { if (it < 0) -it else it }) % AvatarPalette.size]
