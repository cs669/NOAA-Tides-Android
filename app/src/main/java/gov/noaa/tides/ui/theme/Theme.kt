package gov.noaa.tides.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OceanBgTop = Color(0xFF07111C)
val OceanBgMiddle = Color(0xFF09192D)
val OceanBgBottom = Color(0xFF050B14)

val HeroBgStart = Color(0xEB0C182B)
val HeroBgEnd = Color(0xD6070F1C)
val CardSurface = Color(0x0DFFFFFF)
val CardBorder = Color(0x1AFFFFFF)

val TextPrimary = Color(0xFFEDF5FB)
val TextSecondary = Color(0xFFA6B4C9)
val TextMuted = Color(0xFFD7E4F0)

val TideLineBlue = Color(0xFF76D0FF)
val TideFillBlue = Color(0x2976D0FF)
val TideAccentMint = Color(0xFF7CF0C7)
val WarningAmber = Color(0xFFFFC857)
val ErrorCoral = Color(0xFFFF6B6B)

private val DarkColorScheme = darkColorScheme(
    primary = TideLineBlue,
    secondary = TideAccentMint,
    background = OceanBgTop,
    surface = OceanBgMiddle,
    onPrimary = OceanBgBottom,
    onSecondary = OceanBgBottom,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorCoral
)

@Composable
fun NOAATidesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
