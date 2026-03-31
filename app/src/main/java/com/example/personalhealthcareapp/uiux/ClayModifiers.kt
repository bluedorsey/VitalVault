package com.example.personalhealthcareapp.uiux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.personalhealthcareapp.ui.theme.WhiteCore

/**
 * Applies a soft "claymorphism" aesthetic with an outer drop shadow,
 * a surface background color, and a subtle inner white border mimicking a rim light.
 */
fun Modifier.claySurface(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = WhiteCore,
    elevation: Dp = 12.dp,
    shadowColor: Color = Color(0xFFD8E2DF),
    borderAlpha: Float = 0.8f
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = shadowColor,
        ambientColor = shadowColor
    )
    .background(backgroundColor, shape)
    .border(1.5.dp, Color.White.copy(alpha = borderAlpha), shape)

/** An elevated circle modifier, used for floating buttons. */
fun Modifier.clayCircle(
    backgroundColor: Color = WhiteCore,
    elevation: Dp = 12.dp,
    shadowColor: Color = Color(0xFFD8E2DF)
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = androidx.compose.foundation.shape.CircleShape,
        spotColor = shadowColor,
        ambientColor = shadowColor
    )
    .background(backgroundColor, androidx.compose.foundation.shape.CircleShape)
    .border(1.dp, Color.White.copy(alpha = 0.9f), androidx.compose.foundation.shape.CircleShape)
