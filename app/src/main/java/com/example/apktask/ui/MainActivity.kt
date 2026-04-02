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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.apktask.R
import com.example.apktask.data.LocalDataSource
import com.example.apktask.databinding.ActivityMainBinding
import com.example.apktask.util.BiometricHelper
import com.example.apktask.util.NotificationHelper
import com.example.apktask.util.WorkScheduler
import kotlinx.coroutines.launch

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
 *
 * Biometric setting loading:
 *  - [biometricLockEnabled] is refreshed via [lifecycleScope] on each [onResume].
 *    This picks up changes made in ProfileFragment without requiring a restart.
 *  - [onStop] uses the cached value (always set before [onResume] completes).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — NotificationWorker checks at runtime */ }

    // ── Biometric lock state ──────────────────────────────────────────────────

    private var isUnlocked = false
    private var backgroundedAtMs = 0L

    /** Cached value loaded from Room via [lifecycleScope] on each [onResume]. */
    private var biometricLockEnabled = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
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
        if (isBiometricLockEnabled()) isUnlocked = false
    }

    override fun onResume() {
        super.onResume()
        // Capture elapsed before the suspend so the comparison is accurate.
        val elapsed = SystemClock.elapsedRealtime() - backgroundedAtMs
        lifecycleScope.launch {
            biometricLockEnabled = LocalDataSource.getInstance(this@MainActivity)
                .loadProfile().biometricLockEnabled
            if (!isUnlocked && elapsed > LOCK_TIMEOUT_MS && isBiometricLockEnabled()) {
                showLockScreen()
            }
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

    /** Uses the cached [biometricLockEnabled] — always current after each [onResume]. */
    private fun isBiometricLockEnabled() = biometricLockEnabled && BiometricHelper.isAvailable(this)

    private fun showLockScreen() {
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
            onError = { finish() }
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
        private const val LOCK_TIMEOUT_MS = 30_000L
    }
}
