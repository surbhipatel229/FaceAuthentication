package com.example.faceauthenticationdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FaceAuthColorScheme = darkColorScheme(
    primary = AuthPrimary,
    secondary = AuthAccent,
    tertiary = AuthSuccess,
    background = AuthBackground,
    surface = AuthSurface,
    onPrimary = AuthBackground,
    onSecondary = AuthBackground,
    onBackground = AuthTextPrimary,
    onSurface = AuthTextPrimary,
    error = AuthError
)

@Composable
fun FaceAuthenticationDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FaceAuthColorScheme,
        typography = Typography,
        content = content
    )
}
