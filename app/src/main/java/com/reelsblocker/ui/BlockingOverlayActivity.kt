package com.reelsblocker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Retained as a stub to satisfy the AndroidManifest declaration.
 * Blocking is now handled entirely via in-app Home tab redirection
 * (clicking the Home tab node in InstagramBlockerService), so this
 * Activity is never launched. It simply finishes immediately if ever
 * invoked by mistake.
 */
class BlockingOverlayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
