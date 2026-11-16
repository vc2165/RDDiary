package com.vinay.rddiary.ui

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

val Brand = Color(0xFF0B6E5F)       // calm teal
val BrandDark = Color(0xFF064E43)
val BrandTint = Color(0xFFD7EFEA)
val CUpToDate = Color(0xFF1E8A4C)   // green
val CPending = Color(0xFFD9480F)    // orange-red
val CAdvance = Color(0xFF1F5FBF)    // blue
val CDone = Color(0xFF8A8F98)
val CGrey = Color(0xFFE6E8EC)

private val scheme = lightColorScheme(
    primary = Brand, onPrimary = Color.White,
    primaryContainer = BrandTint, onPrimaryContainer = Color(0xFF00201B),
    secondaryContainer = Color(0xFFDDEBE8),
    background = Color(0xFFF5F7F6), surface = Color(0xFFF5F7F6),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEDF1F0), surfaceContainerHigh = Color(0xFFE7ECEA), surfaceContainerHighest = Color(0xFFE1E7E5),
    onSurface = Color(0xFF181C1B), onSurfaceVariant = Color(0xFF55605D),
    outline = Color(0xFF7A8582), outlineVariant = Color(0xFFD5DDDA), error = CPending,
)

private fun TextStyle.x(k: Float) = copy(fontSize = fontSize * k, lineHeight = lineHeight * k)

/** Larger than normal Android text by default (for easy reading); "Large text" makes it bigger still. */
@Composable
fun RdTheme(big: Boolean, content: @Composable () -> Unit) {
    val k = if (big) 1.28f else 1.1f
    val t = Typography()
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(
            displaySmall = t.displaySmall.x(k).copy(fontWeight = FontWeight.SemiBold),
            headlineLarge = t.headlineLarge.x(k).copy(fontWeight = FontWeight.SemiBold),
            headlineMedium = t.headlineMedium.x(k).copy(fontWeight = FontWeight.SemiBold),
            headlineSmall = t.headlineSmall.x(k).copy(fontWeight = FontWeight.SemiBold),
            titleLarge = t.titleLarge.x(k).copy(fontWeight = FontWeight.SemiBold),
            titleMedium = t.titleMedium.x(k).copy(fontWeight = FontWeight.SemiBold),
            titleSmall = t.titleSmall.x(k).copy(fontWeight = FontWeight.SemiBold),
            bodyLarge = t.bodyLarge.x(k), bodyMedium = t.bodyMedium.x(k), bodySmall = t.bodySmall.x(k),
            labelLarge = t.labelLarge.x(k).copy(fontWeight = FontWeight.SemiBold), labelMedium = t.labelMedium.x(k), labelSmall = t.labelSmall.x(k),
        ),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
        content = content,
    )
}

/** Calm scrolling, like Ledger: a flick travels about half as far as Android's default. */
@Composable
fun rememberCalmFling(): FlingBehavior {
    val decay = rememberSplineBasedDecay<Float>()
    return remember(decay) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val v = initialVelocity * 0.5f
                if (abs(v) < 50f) return initialVelocity
                var left = v
                var last = 0f
                AnimationState(0f, v).animateDecay(decay) {
                    val delta = value - last
                    val used = scrollBy(delta)
                    last = value
                    left = velocity
                    if (abs(delta - used) > 0.5f) cancelAnimation()
                }
                return left
            }
        }
    }
}
