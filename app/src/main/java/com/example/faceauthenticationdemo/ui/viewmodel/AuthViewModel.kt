package com.example.faceauthenticationdemo.ui.viewmodel

import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import com.example.faceauthenticationdemo.data.BiometricHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── UI state ─────────────────────────────────────────────────────────────────

/**
 * All possible states the authentication screen can be in.
 * The Compose UI renders a different layout for each state.
 */
sealed class AuthUiState {
    /** Initial state — user has not yet tapped "Authenticate". */
    object Idle : AuthUiState()

    /** The system biometric dialog is visible. Our UI shows a waiting overlay. */
    object Authenticating : AuthUiState()

    /**
     * Authentication succeeded.
     * @param authType 1 = device credential, 2 = biometric (face / fingerprint)
     * @param timestamp Human-readable time of authentication.
     * @param requestedType Which biometric was requested ("FACE" or "FINGERPRINT").
     */
    data class Success(val authType: Int, val timestamp: String, val requestedType: String) : AuthUiState()

    /**
     * A non-recoverable error occurred (hardware error, lockout, etc.).
     * @param message Error string from [BiometricPrompt.AuthenticationCallback].
     */
    data class AuthError(val message: String) : AuthUiState()

    /**
     * The device has biometric hardware but the requested biometric is not enrolled.
     * @param enrollIntent Intent to the system enrollment screen (may be null on older APIs).
     * @param requestedType Which biometric was attempted ("FACE" or "FINGERPRINT").
     */
    data class NotEnrolled(
        val enrollIntent: android.content.Intent?,
        val requestedType: String
    ) : AuthUiState()

    /** Device has no biometric sensor at all. */
    object NoHardware : AuthUiState()

    /**
     * User explicitly dismissed the prompt (back button, cancel tap).
     * Returns to Idle after brief display.
     */
    object Cancelled : AuthUiState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class AuthViewModel : ViewModel() {

    // State exposed to the UI layer
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _requireConfirmation = MutableStateFlow(true)
    val requireConfirmation: StateFlow<Boolean> = _requireConfirmation.asStateFlow()

    private val _faceLockEnabled = MutableStateFlow(false)
    val faceLockEnabled: StateFlow<Boolean> = _faceLockEnabled.asStateFlow()

    private val _biometricAvailability = MutableStateFlow<BiometricHelper.BiometricAvailability?>(null)
    val biometricAvailability: StateFlow<BiometricHelper.BiometricAvailability?> = _biometricAvailability.asStateFlow()

    // ── Public actions ────────────────────────────────────────────────────────

    fun setRequireConfirmation(value: Boolean) {
        _requireConfirmation.value = value
    }

    fun setFaceLockEnabled(value: Boolean) {
        _faceLockEnabled.value = value
    }

    /** Call once at startup so the UI can show accurate per-button availability. */
    fun refreshBiometricAvailability(helper: BiometricHelper) {
        _biometricAvailability.value = helper.checkBiometricAvailability()
    }

    /**
     * Starts the authentication flow for the given [type] (face or fingerprint).
     *
     * 1. Checks availability via [BiometricHelper.checkAvailability].
     * 2. If ready, shows the system BiometricPrompt with type-specific UI.
     * 3. Updates [uiState] based on the callback result.
     *
     * [BiometricHelper] is passed in (not stored) so the ViewModel never holds
     * a reference to the Activity's context.
     */
    fun authenticate(helper: BiometricHelper, type: BiometricHelper.AuthenticationType) {
        when (val status = helper.checkAvailability(type)) {
            BiometricHelper.AvailabilityStatus.Ready -> {
                _uiState.value = AuthUiState.Authenticating
                helper.authenticate(
                    type = type,
                    requireConfirmation = _requireConfirmation.value,
                    onSuccess = { result -> handleSuccess(result, type) },
                    onError   = { code, msg -> handleError(code, msg) },
                    onFailed  = { /* single failed attempt; dialog stays open — no state change */ }
                )
            }

            is BiometricHelper.AvailabilityStatus.NotEnrolled ->
                _uiState.value = AuthUiState.NotEnrolled(status.enrollIntent, type.name)

            BiometricHelper.AvailabilityStatus.NoHardware ->
                _uiState.value = AuthUiState.NoHardware

            BiometricHelper.AvailabilityStatus.TemporarilyUnavailable ->
                _uiState.value = AuthUiState.AuthError(
                    "Biometric hardware is temporarily unavailable. Please try again."
                )
        }
    }

    /** Resets to [AuthUiState.Idle] so the user can retry. */
    fun reset() {
        _uiState.value = AuthUiState.Idle
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun handleSuccess(result: BiometricPrompt.AuthenticationResult, type: BiometricHelper.AuthenticationType) {
        val ts = SimpleDateFormat("HH:mm:ss  •  MMM dd, yyyy", Locale.getDefault()).format(Date())
        _uiState.value = AuthUiState.Success(
            authType      = result.authenticationType,
            timestamp     = ts,
            requestedType = type.name
        )
    }

    private fun handleError(errorCode: Int, message: String) {
        _uiState.value = when (errorCode) {
            // User-initiated dismissal — treat as non-error cancellation
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED ->
                AuthUiState.Cancelled

            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                AuthUiState.AuthError("Too many failed attempts. Please try again later.")

            else ->
                AuthUiState.AuthError(message)
        }
    }
}
