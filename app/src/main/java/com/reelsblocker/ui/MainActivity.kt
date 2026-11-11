package com.reelsblocker.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.reelsblocker.R
import com.reelsblocker.util.PreferencesManager

class MainActivity : AppCompatActivity() {

    private lateinit var masterSwitch: SwitchMaterial
    private lateinit var statusSubtitle: TextView
    private lateinit var serviceStatusText: TextView
    private lateinit var enableAccessibilityButton: Button

    private lateinit var blockReelsSwitch: SwitchMaterial
    private lateinit var blockExploreSwitch: SwitchMaterial
    private lateinit var toastSwitch: SwitchMaterial
    private lateinit var vibrationSwitch: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun initViews() {
        masterSwitch = findViewById(R.id.masterSwitch)
        statusSubtitle = findViewById(R.id.statusSubtitle)
        serviceStatusText = findViewById(R.id.serviceStatusText)
        enableAccessibilityButton = findViewById(R.id.enableAccessibilityButton)

        blockReelsSwitch = findViewById(R.id.blockReelsSwitch)
        blockExploreSwitch = findViewById(R.id.blockExploreSwitch)
        toastSwitch = findViewById(R.id.toastSwitch)
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
    }

    private fun setupListeners() {
        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setBlockingEnabled(this, isChecked)
            updateUI()
        }

        blockReelsSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setReelsBlockingEnabled(this, isChecked)
        }

        blockExploreSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setExploreBlockingEnabled(this, isChecked)
        }

        toastSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setToastEnabled(this, isChecked)
        }

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setVibrationEnabled(this, isChecked)
        }

        enableAccessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }
    }

    private fun updateUI() {
        val isMasterEnabled = PreferencesManager.isBlockingEnabled(this)
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()

        masterSwitch.isChecked = isMasterEnabled
        blockReelsSwitch.isChecked = PreferencesManager.isReelsBlockingEnabled(this)
        blockExploreSwitch.isChecked = PreferencesManager.isExploreBlockingEnabled(this)
        toastSwitch.isChecked = PreferencesManager.isToastEnabled(this)
        vibrationSwitch.isChecked = PreferencesManager.isVibrationEnabled(this)

        if (!isAccessibilityEnabled) {
            statusSubtitle.text = "Permission Required"
            statusSubtitle.setTextColor(Color.parseColor("#F59E0B")) // Warning amber

            serviceStatusText.text = "⚠️ v1.5 Accessibility permission is OFF. Tap below to enable."
            serviceStatusText.setTextColor(Color.parseColor("#FDE68A"))
            enableAccessibilityButton.visibility = View.VISIBLE
        } else if (isMasterEnabled) {
            statusSubtitle.text = "v1.5 Protection Active"
            statusSubtitle.setTextColor(Color.parseColor("#4ADE80")) // Green

            serviceStatusText.text = "✅ v1.5 Service is active • DMs & Stories protected"
            serviceStatusText.setTextColor(Color.parseColor("#BBF7D0"))
            enableAccessibilityButton.visibility = View.GONE
        } else {
            statusSubtitle.text = "v1.5 Protection Paused"
            statusSubtitle.setTextColor(Color.parseColor("#94A3B8")) // Muted

            serviceStatusText.text = "⏸️ v1.5 Protection is currently paused"
            serviceStatusText.setTextColor(Color.parseColor("#CBD5E1"))
            enableAccessibilityButton.visibility = View.GONE
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(packageName) == true
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}
