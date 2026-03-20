package com.example.apktask.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.apktask.R
import com.example.apktask.databinding.ActivityMainBinding
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
 *
 * All business logic lives in the per-tab ViewModels.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * Must be registered before STARTED — declared here, not inside onCreate.
     * NotificationWorker re-checks the permission at execution time; no action needed on denial.
     */
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — NotificationWorker checks at runtime */ }

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

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Top padding handled per-fragment; apply left/right/bottom here.
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

    // ── Permissions ───────────────────────────────────────────────────────────

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
