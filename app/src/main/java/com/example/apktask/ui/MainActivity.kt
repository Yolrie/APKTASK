package com.example.apktask.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.apktask.R
import com.example.apktask.data.LocalDataSource
import com.example.apktask.databinding.ActivityMainBinding
import com.example.apktask.util.BiometricHelper
import com.example.apktask.util.NotificationHelper
import com.example.apktask.util.WorkScheduler

/**
 * Shell activity — hosts the NavHostFragment and wires up bottom navigation.
 *
 * Responsibilities:
 *  1. Enforce FLAG_SECURE before super.onCreate() (no frame ever exposed).
 *  2. Set up NavController ↔ BottomNavigationView.
 *  3. Bootstrap WorkManager and notification channel (idempotent).
 *  4. Request POST_NOTIFICATIONS permission on Android 13+.
 *  5. Biometric lock gate: if the user enabled biometric lock, prompt on
 *     every resume after the app was backgrounded for > LOCK_TIMEOUT_MS.
 *
 * Biometric lock design:
 *  - navHostFragment + bottomNav hidden (not just disabled) when locked.
 *    FLAG_SECURE already prevents screenshots, but hiding the content
 *    ensures nothing is visible while the system prompt is shown.
 *  - Re-lock on onStop so every background-to-foreground transition
 *    triggers a fresh auth (unless within LOCK_TIMEOUT_MS).
 *  - LOCK_TIMEOUT_MS = 30 s: graceful for screen rotations and brief
 *    task switches without requiring repeated auth.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * Must be registered before STARTED.
     * NotificationWorker re-checks permission at execution time; no action needed on denial.
     */
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — NotificationWorker checks at runtime */ }

    // ── Biometric lock state ──────────────────────────────────────────────────

    private var isUnlocked = false
    private var backgroundedAtMs = 0L

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        // FLAG_SECURE must precede super.onCreate() so no unprotected frame is ever rendered.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupNavigation()

        NotificationHelper.createChannel(this)
        WorkScheduler.init(this)
        requestNotificationPermission()
    }

    override fun onStop() {
        super.onStop()
        backgroundedAtMs = SystemClock.elapsedRealtime()
        // Re-lock when biometric is enabled
        if (isBiometricLockEnabled()) {
            isUnlocked = false
        }
    }

    override fun onResume() {
        super.onResume()
        val elapsed = SystemClock.elapsedRealtime() - backgroundedAtMs
        if (isBiometricLockEnabled() && !isUnlocked && elapsed > LOCK_TIMEOUT_MS) {
            showLockScreen()
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, 0, bars.right, 0)
            binding.bottomNav.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
    }

    // ── Biometric lock gate ───────────────────────────────────────────────────

    private fun isBiometricLockEnabled(): Boolean {
        val profile = LocalDataSource.getInstance(this).loadProfile()
        return profile.biometricLockEnabled && BiometricHelper.isAvailable(this)
    }

    private fun showLockScreen() {
        // Hide content — nothing is visible while the prompt is displayed.
        binding.navHostFragment.visibility = View.INVISIBLE
        binding.bottomNav.visibility = View.INVISIBLE

        BiometricHelper.prompt(
            activity = this,
            title = getString(R.string.biometric_prompt_title),
            subtitle = getString(R.string.biometric_prompt_subtitle),
            onSuccess = {
                isUnlocked = true
                binding.navHostFragment.visibility = View.VISIBLE
                binding.bottomNav.visibility = View.VISIBLE
            },
            onError = {
                // Auth cancelled or failed — keep content hidden and finish.
                // The user can reopen the app to try again.
                finish()
            }
        )
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        /** Grace period before re-locking after backgrounding (milliseconds). */
        private const val LOCK_TIMEOUT_MS = 30_000L
    }
}
