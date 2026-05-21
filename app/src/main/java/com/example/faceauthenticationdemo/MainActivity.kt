package com.example.faceauthenticationdemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.faceauthenticationdemo.data.BiometricHelper
import com.example.faceauthenticationdemo.ui.screen.AuthScreen
import com.example.faceauthenticationdemo.ui.theme.FaceAuthenticationDemoTheme
import com.example.faceauthenticationdemo.ui.viewmodel.AuthViewModel

/**
 * Single-activity entry point.
 *
 * Why AppCompatActivity?
 *   BiometricPrompt requires a FragmentActivity host.
 *   AppCompatActivity → FragmentActivity → ComponentActivity, so it satisfies
 *   BiometricPrompt, Compose setContent, and ViewModelProvider all at once
 *   with a single, always-visible explicit dependency (appcompat).
 *
 * Architecture (MVVM):
 *   MainActivity  ──►  BiometricHelper        (data / system layer)
 *       │
 *       └──────────►  AuthViewModel           (UI state via StateFlow)
 *                         │
 *                         └──────────────►  AuthScreen  (Compose UI)
 */
class MainActivity : AppCompatActivity() {

    // ViewModelProvider keeps the ViewModel alive across configuration changes.
    private val viewModel: AuthViewModel by lazy {
        ViewModelProvider(this)[AuthViewModel::class.java]
    }

    // BiometricHelper owns the BiometricPrompt reference.
    // Initialised once here so it is NOT recreated on every Compose recomposition.
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        biometricHelper = BiometricHelper(this)
        viewModel.refreshBiometricAvailability(biometricHelper)

        setContent {
            FaceAuthenticationDemoTheme {
//hello
                // Observe ViewModel StateFlows as Compose State
                val uiState               by viewModel.uiState.collectAsState()
                val requireConfirmation   by viewModel.requireConfirmation.collectAsState()
                val faceLockEnabled       by viewModel.faceLockEnabled.collectAsState()
                val biometricAvailability by viewModel.biometricAvailability.collectAsState()
///comment check
                AuthScreen(
                    uiState                  = uiState,
                    requireConfirmation      = requireConfirmation,
                    faceLockEnabled          = faceLockEnabled,
                    biometricAvailability    = biometricAvailability,
                    onConfirmationToggle     = viewModel::setRequireConfirmation,
                    onFaceLockToggle         = viewModel::setFaceLockEnabled,
                    onAuthenticateFace       = {
                        viewModel.authenticate(
                            biometricHelper,
                            com.example.faceauthenticationdemo.data.BiometricHelper.AuthenticationType.FACE
                        )
                    },
                    onAuthenticateFingerprint = {
                        viewModel.authenticate(
                            biometricHelper,
                            com.example.faceauthenticationdemo.data.BiometricHelper.AuthenticationType.FINGERPRINT
                        )
                    },
                    onReset                  = viewModel::reset,
                    onOpenSettings           = { intent: Intent -> startActivity(intent) }
                )
            }
        }
    }
}
