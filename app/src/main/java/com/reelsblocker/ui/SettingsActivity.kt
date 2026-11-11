package com.reelsblocker.ui

import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.reelsblocker.R
import com.reelsblocker.util.PreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var exploreBlockingSwitch: Switch
    private lateinit var toastSwitch: Switch
    private lateinit var vibrationSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        initViews()
        loadSettings()
        setupListeners()
    }

    private fun initViews() {
        exploreBlockingSwitch = findViewById(R.id.exploreBlockingSwitch)
        toastSwitch = findViewById(R.id.toastSwitch)
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
    }

    private fun loadSettings() {
        exploreBlockingSwitch.isChecked = PreferencesManager.isExploreBlockingEnabled(this)
        toastSwitch.isChecked = PreferencesManager.isToastEnabled(this)
        vibrationSwitch.isChecked = PreferencesManager.isVibrationEnabled(this)
    }

    private fun setupListeners() {
        exploreBlockingSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setExploreBlockingEnabled(this, isChecked)
        }

        toastSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setToastEnabled(this, isChecked)
        }

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setVibrationEnabled(this, isChecked)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
