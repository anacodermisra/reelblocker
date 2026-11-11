package com.reelsblocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.reelsblocker.util.PreferencesManager

/**
 * InstagramBlockerService (v1.5 Pure Precision Edition)
 *
 * Core Architecture & Design:
 * 1. Targeted Tab Protection: Blocks the dedicated Reels Tab and Explore Tab in the bottom nav bar.
 * 2. Explore Reel Detection: Dismisses any fullscreen reel viewer opened from Explore back to Home.
 * 3. DM & Story Whitelist Guarantee:
 *    - Reels opened from DMs are allowed (user can view friends' shared reels).
 *    - Stories are never touched (verified by progress indicators and reply landmarks).
 *    - DM chats and inbox are completely safe.
 * 4. Below Icon Bar DM Check:
 *    - Only checks the BELOW icon bar for DM/airplane focus/selection, ignoring top header icons.
 *    - Re-evaluates continuously on every event so leaving DMs immediately restores protection.
 * 5. In-App Navigation: Always programmatically clicks the Home tab node to keep Instagram open.
 */
class InstagramBlockerService : AccessibilityService() {

    private val TAG = "InstagramBlocker"

    // Action cooldown to prevent spamming
    private var lastActionTime = 0L
    private val ACTION_COOLDOWN_MS = 1000L

    // DM session tracking
    private var isInsideDmSession = false
    private var lastDmInteractionTime = 0L

    // Pending navigation flag (e.g. after dismissing an Explore reel modal)
    private var pendingRedirectToHome = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName != "com.instagram.android") return
        if (!PreferencesManager.isBlockingEnabled(this)) return

        val rootNode = rootInActiveWindow ?: return

        try {
            val homeTab = findTab(rootNode, "Home")

            // ── CHECK 1: PENDING REDIRECT TO HOME ──────────────────────────
            // If an Explore reel modal was just dismissed and bottom bar appeared, land on Home!
            if (pendingRedirectToHome && homeTab != null) {
                pendingRedirectToHome = false
                clickHomeTab(rootNode)
                return
            }

            // ── CHECK 2: FULLSCREEN MODAL VIEWER (NO BOTTOM NAV BAR) ───────
            if (homeTab == null) {
                // If this is a dedicated fullscreen Reel player:
                if (isReelPlayer(rootNode)) {
                    val isFromDm = isInsideDmSession && (System.currentTimeMillis() - lastDmInteractionTime < 60_000L)
                    if (isFromDm) {
                        // Friend's shared reel in DM -> ALLOW!
                        return
                    }

                    // Otherwise, it was opened from Explore -> BLOCK & REDIRECT TO HOME!
                    if (PreferencesManager.isReelsBlockingEnabled(this) || PreferencesManager.isExploreBlockingEnabled(this)) {
                        Log.d(TAG, "[v1.5] Explore reel detected -> dismissing to Home")
                        dismissModalToHome("Explore reel blocked")
                        return
                    }
                }

                // If not a reel player (e.g. Stories, DM chat, camera, settings) -> ALLOW!
                return
            }

            // ── CHECK 3: FAST-PATH TAB TAP INTERCEPTION (<50ms) ───────────
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED
            ) {
                val desc = event.contentDescription?.toString() ?: ""
                val text = event.text.joinToString()

                // A. Reels Tab tapped
                if (PreferencesManager.isReelsBlockingEnabled(this) && isReelsTabMatch(desc, text)) {
                    isInsideDmSession = false
                    Log.d(TAG, "[v1.5] Fast-path: Reels tab clicked -> redirecting to Home")
                    blockAndRedirectToHome("Reels are blocked", rootNode)
                    return
                }

                // B. Explore Tab tapped
                if (PreferencesManager.isExploreBlockingEnabled(this) && isExploreTabMatch(desc, text)) {
                    isInsideDmSession = false
                    Log.d(TAG, "[v1.5] Fast-path: Explore tab clicked -> redirecting to Home")
                    blockAndRedirectToHome("Explore page is blocked", rootNode)
                    return
                }
            }

            // ── CHECK 4: BELOW ICON BAR DM FOCUS & MESSAGING PAGE WHITELIST ─
            // If the BELOW icon bar shows DM (airplane) icon in focus/selected,
            // or if the user is on any messaging page: NEVER REDIRECT!
            if (isDmBottomBarIconSelected(rootNode, homeTab) || isDmScreen(rootNode)) {
                isInsideDmSession = true
                lastDmInteractionTime = System.currentTimeMillis()
                return
            }

            // ── CHECK 5: HOME FEED & PROFILE WHITELIST ─────────────────────
            val profileTab = findTab(rootNode, "Profile")
            val isHomeSelected = isTabSelectedStrict(homeTab)
            val isProfileSelected = isTabSelectedStrict(profileTab)

            if (isHomeSelected || isProfileSelected) {
                // User is browsing their Home Following feed or their Profile -> ALLOW!
                isInsideDmSession = false
                return
            }

            // ── CHECK 6: PERSISTENT ACTIVE TAB FALLBACK ─────────────────────
            // Fallback if the user landed on Reels or Explore via swipe or deep link
            val reelsTab = findTab(rootNode, "Reels")
            val exploreTab = findTab(rootNode, "Search and explore") ?: findTab(rootNode, "Explore")

            // Check Reels Tab selected
            if (PreferencesManager.isReelsBlockingEnabled(this) &&
                isTabSelectedStrict(reelsTab) &&
                !isHomeSelected && !isProfileSelected
            ) {
                isInsideDmSession = false
                Log.d(TAG, "[v1.5] Reels tab active in bottom nav -> redirecting to Home")
                blockAndRedirectToHome("Reels are blocked", rootNode)
                return
            }

            // Check Explore Tab selected
            if (PreferencesManager.isExploreBlockingEnabled(this) &&
                isTabSelectedStrict(exploreTab) &&
                !isHomeSelected && !isProfileSelected
            ) {
                isInsideDmSession = false
                Log.d(TAG, "[v1.5] Explore tab active in bottom nav -> redirecting to Home")
                blockAndRedirectToHome("Explore page is blocked", rootNode)
                return
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling accessibility event", e)
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Reel & Story Recognition Engine
    // ───────────────────────────────────────────────────────────────

    /**
     * Checks if the screen is a dedicated fullscreen Reel player.
     * Features: Like, Comment, Share/Audio controls, absence of bottom bar, and not a story.
     */
    private fun isReelPlayer(root: AccessibilityNodeInfo): Boolean {
        // Must not have bottom nav bar
        if (findTab(root, "Home") != null) return false

        // Must not be a story viewer
        if (isStoryViewer(root)) return false

        // Must not be a DM chat
        if (isDmScreen(root)) return false

        var hasLike = false
        var hasComment = false
        var hasShare = false
        var hasAudio = false

        findNodeRecursively(root) { node ->
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""
            val id = node.viewIdResourceName ?: ""

            if (desc.startsWith("Like", ignoreCase = true) || desc.startsWith("Unlike", ignoreCase = true) || id.contains("like_button")) {
                hasLike = true
            }
            if (desc.contains("Comment", ignoreCase = true) || id.contains("comment_button")) {
                hasComment = true
            }
            if (desc.contains("Share", ignoreCase = true) || desc.contains("Send", ignoreCase = true) || id.contains("share_button")) {
                hasShare = true
            }
            if (text.contains("Original audio", ignoreCase = true) ||
                desc.contains("Original audio", ignoreCase = true) ||
                text.contains("Remix", ignoreCase = true) ||
                desc.contains("Remix", ignoreCase = true) ||
                id.contains("audio", ignoreCase = true)
            ) {
                hasAudio = true
            }

            false // Continue traversal
        }

        return hasLike && hasComment && (hasShare || hasAudio)
    }

    /**
     * Checks if the screen is a Story viewer.
     * Stories have segmented progress bars, "Reply to..." or "Send message" inputs.
     */
    private fun isStoryViewer(root: AccessibilityNodeInfo): Boolean {
        var isStory = false

        findNodeRecursively(root) { node ->
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""
            val id = node.viewIdResourceName ?: ""

            if (desc.contains("Reply to", ignoreCase = true) ||
                text.contains("Reply to", ignoreCase = true) ||
                desc.contains("Send message", ignoreCase = true) ||
                text.contains("Send message", ignoreCase = true) ||
                desc.contains("Story by", ignoreCase = true) ||
                id.contains("story", ignoreCase = true) ||
                id.contains("reel_viewer_progress_bar", ignoreCase = true) ||
                id.contains("segment", ignoreCase = true)
            ) {
                isStory = true
                return@findNodeRecursively true
            }

            false
        }

        return isStory
    }

    // ───────────────────────────────────────────────────────────────
    // Tab State Inspection (Strict: No Parent Leaks)
    // ───────────────────────────────────────────────────────────────

    /**
     * Checks if a tab is explicitly selected.
     * ONLY checks tab.isSelected or if contentDescription contains "selected".
     * NEVER checks tab.parent?.isSelected because Instagram's horizontal
     * container can leak isSelected=true to all children.
     */
    private fun isTabSelectedStrict(tab: AccessibilityNodeInfo?): Boolean {
        if (tab == null) return false
        if (tab.isSelected) return true
        val desc = tab.contentDescription?.toString() ?: ""
        if (desc.contains("selected", ignoreCase = true)) return true
        return false
    }

    private fun isReelsTabMatch(desc: String, text: String): Boolean {
        return (desc.contains("Reels", ignoreCase = true) && desc.contains("tab", ignoreCase = true)) ||
               desc.equals("Reels", ignoreCase = true) ||
               desc.startsWith("Reels,", ignoreCase = true) ||
               text.equals("Reels", ignoreCase = true)
    }

    private fun isExploreTabMatch(desc: String, text: String): Boolean {
        return desc.contains("Search and explore", ignoreCase = true) ||
               (desc.contains("Explore", ignoreCase = true) && desc.contains("tab", ignoreCase = true)) ||
               desc.equals("Explore", ignoreCase = true) ||
               desc.startsWith("Explore,", ignoreCase = true) ||
               text.equals("Explore", ignoreCase = true)
    }

    // ───────────────────────────────────────────────────────────────
    // Screen Whitelist Helpers
    // ───────────────────────────────────────────────────────────────

    /**
     * Checks if the BELOW icon bar (bottom navigation bar) shows a DM (paper airplane / direct / message)
     * icon as selected or in focus.
     * Uses homeTab's vertical bounds to ensure we ONLY inspect the bottom bar,
     * completely ignoring the top header's DM icon.
     */
    private fun isDmBottomBarIconSelected(root: AccessibilityNodeInfo, homeTab: AccessibilityNodeInfo): Boolean {
        val homeBounds = Rect()
        homeTab.getBoundsInScreen(homeBounds)

        var isDmActiveInBottomBar = false

        findNodeRecursively(root) { node ->
            val desc = node.contentDescription?.toString() ?: ""
            val id = node.viewIdResourceName ?: ""
            val text = node.text?.toString() ?: ""

            val isDmMatch = desc.contains("Direct", ignoreCase = true) ||
                            desc.contains("Message", ignoreCase = true) ||
                            desc.contains("Chat", ignoreCase = true) ||
                            desc.contains("Airplane", ignoreCase = true) ||
                            id.contains("direct", ignoreCase = true) ||
                            text.contains("Direct", ignoreCase = true)

            if (isDmMatch) {
                val nodeBounds = Rect()
                node.getBoundsInScreen(nodeBounds)

                // Must be in the exact same bottom bar as homeTab (within 60px vertically)
                val inBottomBar = Math.abs(nodeBounds.bottom - homeBounds.bottom) < 60 && nodeBounds.top > 200

                if (inBottomBar && (node.isFocused || node.isSelected || desc.contains("selected", ignoreCase = true))) {
                    isDmActiveInBottomBar = true
                    return@findNodeRecursively true
                }
            }

            false
        }

        return isDmActiveInBottomBar
    }

    /**
     * Checks if the screen is any messaging page (DM chat, inbox, threads, requests).
     */
    private fun isDmScreen(root: AccessibilityNodeInfo): Boolean {
        // Fast ID check for chat composer
        val composerNodes = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/row_thread_composer_edittext")
        if (composerNodes.isNotEmpty()) return true

        var hasDmSignatures = false

        findNodeRecursively(root) { node ->
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""
            val id = node.viewIdResourceName ?: ""
            val className = node.className?.toString() ?: ""

            // Chat input field or message composer
            if (className.contains("EditText", ignoreCase = true)) {
                if (desc.contains("Message", ignoreCase = true) ||
                    text.contains("Message", ignoreCase = true) ||
                    desc.contains("Chat", ignoreCase = true) ||
                    id.contains("composer", ignoreCase = true) ||
                    id.contains("message", ignoreCase = true) ||
                    id.contains("edit_text", ignoreCase = true)
                ) {
                    hasDmSignatures = true
                    return@findNodeRecursively true
                }
            }

            // Call buttons or direct thread/inbox headers
            if (desc.contains("Audio call", ignoreCase = true) ||
                desc.contains("Video call", ignoreCase = true) ||
                id.contains("action_bar_call", ignoreCase = true) ||
                id.contains("direct_inbox", ignoreCase = true) ||
                id.contains("direct_thread", ignoreCase = true) ||
                text.equals("Primary", ignoreCase = true) ||
                text.equals("General", ignoreCase = true) ||
                text.equals("Requests", ignoreCase = true)
            ) {
                hasDmSignatures = true
                return@findNodeRecursively true
            }

            false
        }

        return hasDmSignatures
    }

    // ───────────────────────────────────────────────────────────────
    // In-App Home Redirection
    // ───────────────────────────────────────────────────────────────

    /**
     * Programmatically clicks the Home tab node in the bottom navigation bar.
     * Keeps Instagram open in the foreground and returns focus to the Home feed.
     */
    private fun blockAndRedirectToHome(reason: String, rootNode: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < ACTION_COOLDOWN_MS) return
        lastActionTime = now

        val clicked = clickHomeTab(rootNode)
        if (!clicked) {
            // Fallback: If Home tab wasn't clickable, press Back once
            performGlobalAction(GLOBAL_ACTION_BACK)
        }

        showFeedback(reason)
    }

    /**
     * Dismisses a fullscreen modal reel player (e.g. from Explore) back to Home:
     * 1. Calls system Back to close the player.
     * 2. Sets pendingRedirectToHome = true so the instant bottom bar appears, it clicks Home!
     */
    private fun dismissModalToHome(reason: String) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < ACTION_COOLDOWN_MS) return
        lastActionTime = now

        pendingRedirectToHome = true
        performGlobalAction(GLOBAL_ACTION_BACK)
        showFeedback(reason)
    }

    private fun clickHomeTab(rootNode: AccessibilityNodeInfo): Boolean {
        val homeTab = findTab(rootNode, "Home") ?: return false
        return if (homeTab.isClickable) {
            homeTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            val parent = homeTab.parent
            if (parent != null && parent.isClickable) {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else {
                false
            }
        }
    }

    private fun showFeedback(reason: String) {
        if (PreferencesManager.isToastEnabled(this)) {
            Toast.makeText(this, "🛑 [v1.5] $reason", Toast.LENGTH_SHORT).show()
        }
        if (PreferencesManager.isVibrationEnabled(this)) {
            vibrateDevice()
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Node Search Helpers
    // ───────────────────────────────────────────────────────────────

    private fun findTab(root: AccessibilityNodeInfo, targetDesc: String): AccessibilityNodeInfo? {
        return findNodeRecursively(root) { node ->
            val desc = node.contentDescription?.toString() ?: ""
            desc.equals(targetDesc, ignoreCase = true) ||
            desc.startsWith("$targetDesc,", ignoreCase = true) ||
            desc.contains("tab, $targetDesc", ignoreCase = true)
        }
    }

    private fun findNodeRecursively(
        node: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeRecursively(child, predicate)
            if (result != null) return result
        }
        return null
    }

    // ───────────────────────────────────────────────────────────────
    // Device Haptics
    // ───────────────────────────────────────────────────────────────

    private fun vibrateDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(150)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating device", e)
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Lifecycle
    // ───────────────────────────────────────────────────────────────

    override fun onInterrupt() {
        Log.d(TAG, "InstagramBlockerService interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "InstagramBlockerService connected and active (v1.5)")
        Toast.makeText(this, "Reels Blocker v1.5 Active 🛡️", Toast.LENGTH_SHORT).show()
    }
}
