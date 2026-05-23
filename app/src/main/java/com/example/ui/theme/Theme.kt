package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NaturalPrimaryLight,
    secondary = NaturalSecondaryBlueBgDark,
    tertiary = NaturalTertiaryLavenderBgDark,
    background = NaturalBgDark,
    surface = Color(0xFF1E2125),
    onBackground = NaturalTextLight,
    onSurface = NaturalTextLight,
    outline = NaturalBorderDark,
    secondaryContainer = NaturalSecondaryBlueBgDark,
    onSecondaryContainer = NaturalSecondaryBlueTextDark,
    tertiaryContainer = NaturalTertiaryLavenderBgDark,
    onTertiaryContainer = NaturalTertiaryLavenderTextDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NaturalPrimaryDark,
    secondary = NaturalSecondaryBlueBg,
    tertiary = NaturalTertiaryLavenderBg,
    background = NaturalBgLight,
    surface = Color.White,
    onBackground = NaturalTextDark,
    onSurface = NaturalTextDark,
    outline = NaturalBorderCPlaza,
    secondaryContainer = NaturalSecondaryBlueBg,
    onSecondaryContainer = NaturalSecondaryBlueText,
    tertiaryContainer = NaturalTertiaryLavenderBg,
    onTertiaryContainer = NaturalTertiaryLavenderText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors by default so our curated "Natural Tones" palette displays perfectly
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
