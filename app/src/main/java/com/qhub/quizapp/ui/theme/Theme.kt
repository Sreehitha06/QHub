
package com.qhub.quizapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val QHubLightColorScheme = lightColorScheme(
    primary = QHubDeepBlue,
    onPrimary = QHubWhite,

    secondary = QHubBlueGray,
    onSecondary = QHubWhite,

    tertiary = QHubTaupe,
    onTertiary = QHubDeepBlue,

    background = QHubCream,
    onBackground = QHubDarkText,

    surface = QHubCream,
    onSurface = QHubDarkText,

    surfaceVariant = QHubLightTaupe,
    onSurfaceVariant = QHubMutedText,

    outline = QHubBorder
)

private val QHubDarkColorScheme = darkColorScheme(
    primary = QHubBlueGray,
    onPrimary = QHubDeepBlue,

    secondary = QHubTaupe,
    onSecondary = QHubDeepBlue,

    tertiary = QHubCream,
    onTertiary = QHubDeepBlue,

    background = QHubDeepBlue,
    onBackground = QHubCream,

    surface = QHubDeepBlue,
    onSurface = QHubCream,

    surfaceVariant = QHubBlueGray,
    onSurfaceVariant = QHubCream,

    outline = QHubTaupe
)

@Composable
fun QHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        QHubDarkColorScheme
    } else {
        QHubLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
