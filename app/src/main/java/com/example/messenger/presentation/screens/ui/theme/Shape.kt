package com.example.messenger.presentation.screens.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object MessengerShapes {
    val pill = RoundedCornerShape(percent = 50)
    val button = RoundedCornerShape(28.dp)
    val field = RoundedCornerShape(28.dp)
    val card = RoundedCornerShape(24.dp)
    val tab = RoundedCornerShape(24.dp)
    val bubble = RoundedCornerShape(20.dp)
    val menu = RoundedCornerShape(24.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

val MessengerMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
