package com.example.faceauthenticationdemo.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.faceauthenticationdemo.ui.theme.AuthAccent
import com.example.faceauthenticationdemo.ui.theme.AuthBackground
import com.example.faceauthenticationdemo.ui.theme.AuthCard
import com.example.faceauthenticationdemo.ui.theme.AuthDivider
import com.example.faceauthenticationdemo.ui.theme.AuthError
import com.example.faceauthenticationdemo.ui.theme.AuthPrimary
import com.example.faceauthenticationdemo.ui.theme.AuthSuccess
import com.example.faceauthenticationdemo.ui.theme.AuthSuccessDark
import com.example.faceauthenticationdemo.ui.theme.AuthSurface
import com.example.faceauthenticationdemo.ui.theme.AuthTextPrimary
import com.example.faceauthenticationdemo.ui.theme.AuthTextSecondary
import com.example.faceauthenticationdemo.ui.theme.AuthWarning
import com.example.faceauthenticationdemo.data.BiometricHelper
import com.example.faceauthenticationdemo.ui.viewmodel.AuthUiState
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// ROOT SCREEN — routes to the appropriate sub-screen based on AuthUiState
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    requireConfirmation: Boolean,
    faceLockEnabled: Boolean,
    biometricAvailability: BiometricHelper.BiometricAvailability?,
    onConfirmationToggle: (Boolean) -> Unit,
    onFaceLockToggle: (Boolean) -> Unit,
    onAuthenticateFace: () -> Unit,
    onAuthenticateFingerprint: () -> Unit,
    onReset: () -> Unit,
    onOpenSettings: (android.content.Intent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(AuthSurface, AuthBackground, AuthBackground),
                    radius = 1600f,
                    center = Offset(400f, 900f)
                )
            )
    ) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                fadeIn(tween(350)) togetherWith fadeOut(tween(250))
            },
            label = "screen_state"
        ) { state ->
            when (state) {
                is AuthUiState.Success -> SuccessScreen(
                    state = state,
                    requireConfirmation = requireConfirmation,
                    onReset = onReset
                )

                AuthUiState.NoHardware -> InfoScreen(
                    icon = "⚙️",
                    title = "No Biometric Hardware",
                    body = "This device does not have a biometric sensor (face or fingerprint). " +
                        "You can use a device credential (PIN / pattern / password) instead.",
                    buttonLabel = "Back",
                    buttonColor = AuthPrimary,
                    onButton = onReset
                )

                is AuthUiState.NotEnrolled -> {
                    val (icon, title, body) = if (state.requestedType == "FACE") {
                        Triple(
                            "👤",
                            "Face Auth Not Available",
                            "Face authentication via BiometricPrompt is not available on this device.\n\n" +
                                "On most Android phones, face recognition in Settings → Security is only " +
                                "for the lock screen and is NOT registered as an in-app biometric authenticator.\n\n" +
                                "In-app face authentication requires dedicated face-sensor hardware " +
                                "(e.g. Pixel 4 / 4a / 4 XL). Please use Fingerprint instead."
                        )
                    } else {
                        Triple(
                            "👆",
                            "Fingerprint Not Enrolled",
                            "No fingerprint is enrolled on this device.\n\n" +
                                "Go to Settings → Security → Fingerprint to set up fingerprint authentication."
                        )
                    }
                    InfoScreen(
                        icon = icon,
                        title = title,
                        body = body,
                        buttonLabel = if (state.enrollIntent != null) "Open Settings" else "Back",
                        buttonColor = AuthAccent,
                        onButton = {
                            state.enrollIntent?.let { onOpenSettings(it) } ?: onReset()
                        },
                        secondaryLabel = "Back",
                        onSecondary = onReset
                    )
                }

                is AuthUiState.AuthError -> ErrorScreen(
                    message = state.message,
                    onRetry = onReset
                )

                AuthUiState.Cancelled -> {
                    // Auto-return to Idle after a brief moment so the user sees the state change
                    LaunchedEffect(Unit) {
                        delay(600)
                        onReset()
                    }
                    InfoScreen(
                        icon = "✋",
                        title = "Authentication Cancelled",
                        body = "You cancelled the biometric prompt.",
                        buttonLabel = "Try Again",
                        buttonColor = AuthPrimary,
                        onButton = onReset
                    )
                }

                else -> IdleScreen(
                    isAuthenticating          = state is AuthUiState.Authenticating,
                    requireConfirmation       = requireConfirmation,
                    faceLockEnabled           = faceLockEnabled,
                    biometricAvailability     = biometricAvailability,
                    onConfirmationToggle      = onConfirmationToggle,
                    onFaceLockToggle          = onFaceLockToggle,
                    onAuthenticateFace        = onAuthenticateFace,
                    onAuthenticateFingerprint = onAuthenticateFingerprint
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IDLE / AUTHENTICATING SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IdleScreen(
    isAuthenticating: Boolean,
    requireConfirmation: Boolean,
    faceLockEnabled: Boolean,
    biometricAvailability: BiometricHelper.BiometricAvailability?,
    onConfirmationToggle: (Boolean) -> Unit,
    onFaceLockToggle: (Boolean) -> Unit,
    onAuthenticateFace: () -> Unit,
    onAuthenticateFingerprint: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        AppHeader()
        Spacer(Modifier.height(44.dp))

        // Biometric scanner animation
        BiometricAnimation(isScanning = isAuthenticating)
        Spacer(Modifier.height(24.dp))

        // Status label
        AnimatedContent(
            targetState = isAuthenticating,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            label = "status"
        ) { scanning ->
            Text(
                text = if (scanning) "Waiting for biometric prompt…" else "Ready to authenticate",
                color = if (scanning) AuthAccent else AuthTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))
        BiometricSettingsCard(
            faceLockEnabled     = faceLockEnabled,
            requireConfirmation = requireConfirmation,
            onFaceLockToggle    = onFaceLockToggle,
            onConfirmationToggle = onConfirmationToggle,
            enabled             = !isAuthenticating
        )
        Spacer(Modifier.height(20.dp))
        BiometricAuthButtons(
            isLoading             = isAuthenticating,
            faceLockEnabled       = faceLockEnabled,
            availability          = biometricAvailability,
            onAuthenticateFace    = onAuthenticateFace,
            onAuthenticateFingerprint = onAuthenticateFingerprint
        )
        Spacer(Modifier.height(20.dp))

        InfoNote(
            icon = "🔒",
            text = "Android OS handles biometric matching inside a secure enclave. " +
                "This app never accesses the camera or sensor data — only a pass/fail signal is returned."
        )
        Spacer(Modifier.height(12.dp))
        InfoNote(
            icon = "⚠",
            text = "Android limitation: on most phones, face added in Settings → Security is for " +
                "the lock screen only and is NOT available for in-app authentication via BiometricPrompt. " +
                "Only devices with a dedicated face-unlock hardware sensor (e.g. Pixel 4/4a) " +
                "can show the face camera inside an app."
        )
        Spacer(Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUCCESS SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuccessScreen(
    state: AuthUiState.Success,
    requireConfirmation: Boolean,
    onReset: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val checkScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "checkScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 250),
        label = "alpha"
    )
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        0.2f, 0.55f,
        infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "g"
    )

    val methodLabel = when {
        state.authType == 1                  -> "Device PIN / Pattern / Password"
        state.requestedType == "FINGERPRINT" -> "Fingerprint"
        state.requestedType == "FACE"        -> "Face Recognition"
        else                                 -> "Biometric"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        // Animated success circle
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color = AuthSuccess.copy(glowAlpha * 0.25f), radius = size.width / 2)
            }
            Canvas(Modifier.size(132.dp)) {
                drawCircle(
                    color = AuthSuccess.copy(0.2f),
                    radius = size.width / 2 - 1.dp.toPx(),
                    style = Stroke(2.dp.toPx())
                )
            }
            Box(
                modifier = Modifier
                    .size(102.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(AuthSuccessDark, AuthSuccess.copy(0.8f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(Modifier.alpha(alpha), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Authentication", color = AuthTextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Successful!", color = AuthSuccess, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Identity verified via Android Biometric API. Welcome back!",
                color = AuthTextSecondary, fontSize = 14.sp,
                textAlign = TextAlign.Center, lineHeight = 20.sp
            )
        }

        Spacer(Modifier.height(28.dp))

        // Result detail cards
        Column(
            modifier = Modifier.fillMaxWidth().alpha(alpha),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ResultCard("🔐", "Authentication Method", methodLabel)
            ResultCard("⏰", "Authenticated At", state.timestamp)
            ResultCard(
                if (requireConfirmation) "✋" else "⚡",
                "User Confirmation",
                if (requireConfirmation) "Required  (setConfirmationRequired = true)"
                else "Not required  (setConfirmationRequired = false)"
            )
            ResultCard("📋", "API Used", "BiometricPrompt  +  BIOMETRIC_STRONG  +  DEVICE_CREDENTIAL")
            ResultCard(
                "🛡️", "Security Note",
                "Face/fingerprint data never leaves the secure enclave. " +
                    "The app received only a pass/fail result."
            )
        }

        Spacer(Modifier.height(28.dp))

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(54.dp).alpha(alpha),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, AuthError.copy(0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AuthError)
        ) {
            Text("🔒  Lock Screen", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuthError)
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ERROR SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("❌", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("Authentication Failed", color = AuthError, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(message, color = AuthTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.height(32.dp))
        GradientButton("Try Again", listOf(AuthError.copy(0.8f), AuthError), onRetry)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GENERIC INFO SCREEN  (not enrolled, no hardware, cancelled)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoScreen(
    icon: String,
    title: String,
    body: String,
    buttonLabel: String,
    buttonColor: Color,
    onButton: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, color = AuthTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, color = AuthTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.height(32.dp))
        GradientButton(buttonLabel, listOf(buttonColor.copy(0.85f), buttonColor), onButton)
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSecondary,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AuthDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AuthTextSecondary)
            ) {
                Text(secondaryLabel, color = AuthTextSecondary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BIOMETRIC ANIMATION  — pulsing rings + face icon
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BiometricAnimation(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "bio")

    // Three rings with staggered phase
    val ring1Scale by infiniteTransition.animateFloat(
        1f, 1.28f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r1s"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        0.6f, 0.05f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r1a"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        1.1f, 1.42f, infiniteRepeatable(tween(1800, 600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r2s"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        0.4f, 0.02f, infiniteRepeatable(tween(1800, 600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r2a"
    )
    val ring3Scale by infiniteTransition.animateFloat(
        1.2f, 1.56f, infiniteRepeatable(tween(1800, 1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r3s"
    )
    val ring3Alpha by infiniteTransition.animateFloat(
        0.25f, 0f, infiniteRepeatable(tween(1800, 1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r3a"
    )

    // Rotating dashed ring (active during scanning)
    val rotation by infiniteTransition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "rot"
    )

    // Scan line sweep
    val scanLine by infiniteTransition.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(2200, easing = EaseInOut), RepeatMode.Reverse), label = "scan"
    )

    val accentColor = if (isScanning) AuthAccent else AuthPrimary

    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {

        // Pulsing rings
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val base = size.width / 2 * 0.58f
            drawCircle(accentColor.copy(ring1Alpha), base * ring1Scale, c, style = Stroke(1.5f))
            drawCircle(accentColor.copy(ring2Alpha), base * ring2Scale, c, style = Stroke(1.5f))
            drawCircle(accentColor.copy(ring3Alpha), base * ring3Scale, c, style = Stroke(1.5f))
        }

        // Rotating dashed outer ring
        Canvas(Modifier.size(200.dp)) {
            val dashCount = 20
            val sweepOn = 360f / dashCount - 3f
            for (i in 0 until dashCount) {
                if (i % 2 == 0) {
                    drawArc(
                        color = accentColor.copy(0.45f),
                        startAngle = i * (360f / dashCount) + if (isScanning) rotation else 0f,
                        sweepAngle = sweepOn,
                        useCenter = false,
                        style = Stroke(2.dp.toPx()),
                        topLeft = androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - 4.dp.toPx(), size.height - 4.dp.toPx()
                        )
                    )
                }
            }
        }

        // Corner bracket markers
        Canvas(Modifier.size(180.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.width / 2 - 2.dp.toPx()
            val arm = 20.dp.toPx(); val sw = 2.5.dp.toPx(); val pad = 4.dp.toPx()
            val col = accentColor
            drawLine(col, Offset(c.x - r + pad, c.y - r + pad + arm), Offset(c.x - r + pad, c.y - r + pad), sw, cap = StrokeCap.Round)
            drawLine(col, Offset(c.x - r + pad, c.y - r + pad), Offset(c.x - r + pad + arm, c.y - r + pad), sw, cap = StrokeCap.Round)
            drawLine(col, Offset(c.x + r - pad, c.y - r + pad + arm), Offset(c.x + r - pad, c.y - r + pad), sw, cap = StrokeCap.Round)
            drawLine(col, Offset(c.x + r - pad, c.y - r + pad), Offset(c.x + r - pad - arm, c.y - r + pad), sw, cap = StrokeCap.Round)
            drawLine(col, Offset(c.x - r + pad, c.y + r - pad - arm), Offset(c.x - r + pad, c.y + r - pad), sw, cap = StrokeCap.Round)
            drawLine(col, Offset(c.x - r + pad, c.y + r - pad), Offset(c.x - r + pad + arm, c.y + r - pad), sw, cap = StrokeCap.Round)
            drawLine(col, Offset(c.x + r - pad, c.y + r - pad - arm), Offset(c.x + r - pad, c.y + r - pad), sw, cap = StrokeCap.Round)
            drawLine(col, Offset(c.x + r - pad, c.y + r - pad), Offset(c.x + r - pad - arm, c.y + r - pad), sw, cap = StrokeCap.Round)
        }

        // Face icon (drawn with Canvas)
        Canvas(Modifier.size(90.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val headR = size.width * 0.36f
            val sw = 2.5.dp.toPx()
            drawCircle(accentColor, headR, c, style = Stroke(sw))
            // Eyes
            drawCircle(accentColor, headR * 0.12f, Offset(c.x - headR * 0.32f, c.y - headR * 0.18f))
            drawCircle(accentColor, headR * 0.12f, Offset(c.x + headR * 0.32f, c.y - headR * 0.18f))
            // Smile
            drawArc(
                color = accentColor,
                startAngle = 20f, sweepAngle = 140f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(c.x - headR * 0.36f, c.y + headR * 0.05f),
                size = androidx.compose.ui.geometry.Size(headR * 0.72f, headR * 0.46f),
                style = Stroke(sw, cap = StrokeCap.Round)
            )
        }

        // Scanning overlay with progress indicator
        AnimatedVisibility(
            visible = isScanning,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = AuthAccent,
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APP HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AuthPrimary, AuthAccent))),
            contentAlignment = Alignment.Center
        ) { Text("👤", fontSize = 28.sp) }
        Spacer(Modifier.height(14.dp))
        Text("FaceAuth", color = AuthTextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text("Secure Biometric Login  •  Android BiometricPrompt API", color = AuthTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BIOMETRIC SETTINGS CARD  (Face Lock + Confirmation toggles)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BiometricSettingsCard(
    faceLockEnabled: Boolean,
    requireConfirmation: Boolean,
    onFaceLockToggle: (Boolean) -> Unit,
    onConfirmationToggle: (Boolean) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AuthCard),
        border = BorderStroke(1.dp, AuthDivider)
    ) {
        Column(Modifier.padding(16.dp)) {

            // ── Face Lock row ────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (faceLockEnabled) AuthAccent.copy(0.18f) else AuthTextSecondary.copy(0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) { Text(if (faceLockEnabled) "👤" else "👁️", fontSize = 20.sp) }

                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Face Lock", color = AuthTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (faceLockEnabled) "Face authentication is primary"
                        else "Fingerprint authentication is primary",
                        color = AuthTextSecondary, fontSize = 12.sp
                    )
                }
                Switch(
                    checked = faceLockEnabled, onCheckedChange = onFaceLockToggle, enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AuthAccent,
                        uncheckedThumbColor = AuthTextSecondary,
                        uncheckedTrackColor = AuthDivider
                    )
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = AuthDivider)
            Spacer(Modifier.height(12.dp))

            // ── Require Confirmation row ──────────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (requireConfirmation) AuthAccent.copy(0.18f) else AuthTextSecondary.copy(0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) { Text(if (requireConfirmation) "✋" else "⚡", fontSize = 20.sp) }

                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Require Confirmation", color = AuthTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (requireConfirmation) "User taps Confirm after match"
                        else "Auto-approves immediately on match",
                        color = AuthTextSecondary, fontSize = 12.sp
                    )
                }
                Switch(
                    checked = requireConfirmation, onCheckedChange = onConfirmationToggle, enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AuthAccent,
                        uncheckedThumbColor = AuthTextSecondary,
                        uncheckedTrackColor = AuthDivider
                    )
                )
            }

            AnimatedVisibility(
                visible = requireConfirmation,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = AuthDivider)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Text("ℹ️", fontSize = 11.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "setConfirmationRequired(true) — the OS shows a Confirm button after the biometric is " +
                                "matched. Recommended for sensitive actions such as payments or authorisations.",
                            color = AuthTextSecondary.copy(0.65f),
                            fontSize = 11.sp, lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BIOMETRIC AUTH BUTTONS  (Face + Fingerprint)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BiometricAuthButtons(
    isLoading: Boolean,
    faceLockEnabled: Boolean,
    availability: BiometricHelper.BiometricAvailability?,
    onAuthenticateFace: () -> Unit,
    onAuthenticateFingerprint: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.linearGradient(listOf(AuthTextSecondary.copy(0.3f), AuthTextSecondary.copy(0.3f))),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Opening biometric prompt…", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }//hekllo
        } else {
            // ── Face button ──────────────────────────────────────────────────
            BiometricSingleButton(
                icon = "👤",
                label = "Authenticate with Face",
                isPrimary = faceLockEnabled,
                gradient = listOf(AuthPrimary, AuthAccent),
                onClick = onAuthenticateFace
            )
            val faceStatus = when {
                availability == null -> null
                availability.faceDistinctFromFingerprint ->
                    Pair(AuthSuccess, "Face biometric registered with BiometricPrompt")
                availability.fingerprintAvailable ->
                    Pair(AuthWarning, "Both enrolled — OS will show fingerprint (Android limitation)")
                availability.faceForPrompt ->
                    Pair(AuthWarning, "Face may not be available as in-app biometric on this device")
                else ->
                    Pair(AuthError, "Face not enrolled as a biometric authenticator")
            }
            if (faceStatus != null) {
                Text(
                    text = faceStatus.second,
                    color = faceStatus.first.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                )
            }

            // ── Fingerprint button ───────────────────────────────────────────
            BiometricSingleButton(
                icon = "👆",
                label = "Authenticate with Fingerprint",
                isPrimary = !faceLockEnabled,
                gradient = listOf(Color(0xFF7C4DFF), Color(0xFF5C6BC0)),
                onClick = onAuthenticateFingerprint
            )
            val fpStatus = when {
                availability == null -> null
                availability.fingerprintAvailable ->
                    Pair(AuthSuccess, "Fingerprint sensor available")
                else ->
                    Pair(AuthError, "Fingerprint not enrolled")
            }
            if (fpStatus != null) {
                Text(
                    text = fpStatus.second,
                    color = fpStatus.first.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun BiometricSingleButton(
    icon: String,
    label: String,
    isPrimary: Boolean,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(gradient), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.3.sp)
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, AuthDivider),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AuthTextSecondary)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(label, color = AuthTextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun GradientButton(label: String, colors: List<Color>, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE CARDS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultCard(icon: String, label: String, value: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AuthCard),
        border = BorderStroke(1.dp, AuthDivider)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, color = AuthTextSecondary, fontSize = 11.sp, letterSpacing = 0.3.sp)
                Spacer(Modifier.height(3.dp))
                Text(value, color = AuthTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun InfoNote(icon: String, text: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AuthCard.copy(0.6f)),
        border = BorderStroke(1.dp, AuthDivider.copy(0.5f))
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            Text(icon, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text(text, color = AuthTextSecondary.copy(0.7f), fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
    Spacer(Modifier.height(6.dp))
}
