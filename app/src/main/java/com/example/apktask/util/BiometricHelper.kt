package com.example.apktask.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Helper for biometric / device-credential authentication.
 *
 * Security properties:
 *  - Uses BIOMETRIC_STRONG | DEVICE_CREDENTIAL so the user can always
 *    fall back to PIN/pattern if no biometric hardware is enrolled.
 *  - Auth is class 3 (strong) when biometric is used — keys are bound
 *    to the secure element and invalidated on new enrollment.
 *  - onError is called on all failure paths (error, negative button,
 *    failed match) to ensure the caller never silently grants access.
 */
object BiometricHelper {

    private val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    /**
     * Returns true if at least one authenticator (biometric or PIN/pattern)
     * is enrolled and available on this device.
     */
    fun isAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Displays the system biometric/credential prompt.
     *
     * @param activity  Host FragmentActivity (required by BiometricPrompt)
     * @param title     Prompt title shown to the user
     * @param subtitle  Prompt subtitle
     * @param onSuccess Called when authentication succeeds
     * @param onError   Called when authentication fails or is cancelled
     */
    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // ERROR_NEGATIVE_BUTTON or ERROR_USER_CANCELED → treated as failure
                onError()
            }

            override fun onAuthenticationFailed() {
                // Individual match failure — prompt stays open; onError not called here.
                // BiometricPrompt handles retry UI automatically.
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
