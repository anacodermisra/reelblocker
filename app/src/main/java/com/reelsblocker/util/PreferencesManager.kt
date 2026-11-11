package com.reelsblocker.util

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {

    private const val PREFS_NAME = "reels_blocker_prefs"
    private const val KEY_MASTER_ENABLED = "master_enabled"
    private const val KEY_REELS_BLOCKING_ENABLED = "reels_blocking_enabled"
    private const val KEY_EXPLORE_BLOCKING_ENABLED = "explore_blocking_enabled"
    private const val KEY_TOAST_ENABLED = "toast_enabled"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isBlockingEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MASTER_ENABLED, true)
    }

    fun setBlockingEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
    }

    fun isReelsBlockingEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REELS_BLOCKING_ENABLED, true)
    }

    fun setReelsBlockingEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REELS_BLOCKING_ENABLED, enabled).apply()
    }

    fun isExploreBlockingEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_EXPLORE_BLOCKING_ENABLED, true)
    }

    fun setExploreBlockingEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_EXPLORE_BLOCKING_ENABLED, enabled).apply()
    }

    fun isToastEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TOAST_ENABLED, true)
    }

    fun setToastEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TOAST_ENABLED, enabled).apply()
    }

    fun isVibrationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VIBRATION_ENABLED, false)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }
}
