package com.example.faceauthenticationdemo.data

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Wraps the AndroidX BiometricManager + BiometricPrompt into a single, reusable helper.
 *
 * Android OS handles face recognition entirely on-device inside a secure enclave.
 * Apps never access the camera stream or face images — the OS returns only a
 * pass/fail signal after the secure match.
 *
 * BIOMETRIC_STRONG covers all Class-3 biometrics (face on Pixel 4+, fingerprint, iris).
 * The OS presents the biometric UI for the requested type when available.
 */
class BiometricHelper(private val activity: AppCompatActivity) {

    /** Which biometric the user intends to use for this authentication attempt. */
    enum class AuthenticationType { FACE, FINGERPRINT }

    /**
     * What the device actually supports via [BiometricPrompt].
     *
     * Key insight: on most Android phones face unlock (Settings → Security → Face) is
     * registered only for the lock screen, NOT as a [BiometricPrompt] authenticator.
     * [fingerprintAvailable] reflects whether Class-3 fingerprint is enrolled.
     * [faceDistinctFromFingerprint] is true only when Class-2 face is the SOLE enrolled
     * biometric — the one case where tapping "Face" reliably shows the face camera.
     * When fingerprint is also enrolled it satisfies BIOMETRIC_WEAK too, so the OS
     * still shows the fingerprint sheet for both buttons (Android platform limitation).
     */
    data class BiometricAvailability(
        val faceForPrompt: Boolean,       // canAuthenticate(BIOMETRIC_WEAK) == SUCCESS
        val fingerprintAvailable: Boolean  // canAuthenticate(BIOMETRIC_STRONG) == SUCCESS
    ) {
        val faceDistinctFromFingerprint: Boolean
            get() = faceForPrompt && !fingerprintAvailable
    }

    // ── Availability result ──────────────────────────────────────────────────

    sealed class AvailabilityStatus {
        /** At least one BIOMETRIC_STRONG or device-credential method is ready. */
        object Ready : AvailabilityStatus()

        /** Hardware exists but no biometric is enrolled; supply an enrollment Intent. */
        data class NotEnrolled(val enrollIntent: Intent?) : AvailabilityStatus()

        /** The device has no biometric hardware. */
        object NoHardware : AvailabilityStatus()

        /** Hardware exists but is temporarily unavailable (e.g. too many attempts). */
        object TemporarilyUnavailable : AvailabilityStatus()
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Queries [BiometricManager] and returns the current availability status for [type].
     *
     * Face uses [BIOMETRIC_WEAK] (Class 2 — covers face recognition on most Android phones).
     * Fingerprint uses [BIOMETRIC_STRONG] (Class 3 — covers fingerprint sensors).
     *
     * Call this before showing any auth UI to surface helpful guidance to the user.
     */
    fun checkAvailability(type: AuthenticationType): AvailabilityStatus {
        val mgr = BiometricManager.from(activity)
        // Check biometric ONLY — no DEVICE_CREDENTIAL — so we detect whether the actual
        // sensor is enrolled. Including DEVICE_CREDENTIAL would return SUCCESS whenever
        // a PIN/password exists, causing the app to open a PIN sheet instead of blocking.
        val biometricOnlyCheck = when (type) {
            AuthenticationType.FACE        -> BIOMETRIC_WEAK
            AuthenticationType.FINGERPRINT -> BIOMETRIC_STRONG
        }
        return when (mgr.canAuthenticate(biometricOnlyCheck)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                AvailabilityStatus.Ready

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                AvailabilityStatus.NotEnrolled(buildEnrollIntent(type))

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                AvailabilityStatus.NoHardware

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                AvailabilityStatus.TemporarilyUnavailable

            else -> AvailabilityStatus.TemporarilyUnavailable
        }
    }

    /**
     * Returns what the device actually has enrolled for [BiometricPrompt].
     * Call once at startup to drive UI availability indicators.
     */
    fun checkBiometricAvailability(): BiometricAvailability {
        val mgr = BiometricManager.from(activity)
        return BiometricAvailability(
            faceForPrompt      = mgr.canAuthenticate(BIOMETRIC_WEAK)   == BiometricManager.BIOMETRIC_SUCCESS,
            fingerprintAvailable = mgr.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        )
    }

    // Face → BIOMETRIC_WEAK (Class 2+): covers face recognition on most Android phones.
    // Fingerprint → BIOMETRIC_STRONG (Class 3): covers fingerprint sensors only.
    private fun authenticatorsFor(type: AuthenticationType): Int = when (type) {
        AuthenticationType.FACE        -> BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        AuthenticationType.FINGERPRINT -> BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    }

    /**
     * Shows the system biometric prompt for the specified [type].
     *
     * The OS handles all sensor interaction inside a secure enclave.
     * On devices where both face and fingerprint are enrolled, the OS will present
     * whichever sensor matches the requested type when possible.
     *
     * @param type               Whether to request face or fingerprint authentication.
     * @param requireConfirmation Maps to [BiometricPrompt.PromptInfo.Builder.setConfirmationRequired].
     *   `true`  → User must tap a "Confirm" button after match (recommended for payments).
     *   `false` → OS authenticates immediately on match (suitable for passive content unlock).
     */
    fun authenticate(
        type: AuthenticationType,
        requireConfirmation: Boolean,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, message: String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errorCode, errString.toString())
                }

                // Called when a single attempt failed but the dialog stays open for retry.
                override fun onAuthenticationFailed() {
                    onFailed()
                }
            }
        )

        val (title, subtitle, description) = when (type) {
            AuthenticationType.FACE -> Triple(
                "Face Authentication",
                "Verify your identity with your face",
                "Look directly at the camera to authenticate.\n" +
                    "Make sure your face is well-lit and fully visible."
            )
            AuthenticationType.FINGERPRINT -> Triple(
                "Fingerprint Authentication",
                "Verify your identity with your fingerprint",
                "Place your finger on the fingerprint sensor.\n" +
                    "Ensure your finger is clean and dry."
            )
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            // Face → BIOMETRIC_WEAK (Class 2+) so face recognition sensors are included.
            // Fingerprint → BIOMETRIC_STRONG (Class 3) to target fingerprint sensors only.
            // Cannot pair with setNegativeButtonText() when DEVICE_CREDENTIAL is allowed.
            .setAllowedAuthenticators(authenticatorsFor(type))
            // true  → requires explicit Confirm tap after recognition (payments, sensitive actions)
            // false → auto-approves on successful match (passive unlock, viewing content)
            .setConfirmationRequired(requireConfirmation)
            .build()

        prompt.authenticate(promptInfo)
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun buildEnrollIntent(type: AuthenticationType): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    authenticatorsFor(type)
                )
            }
        } else {
            // Older devices: open generic security settings
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }
}
