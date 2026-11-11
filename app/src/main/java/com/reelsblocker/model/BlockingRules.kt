package com.reelsblocker.model

object BlockingRules {

    // Instagram package name
    const val INSTAGRAM_PACKAGE = "com.instagram.android"

    // Keywords to detect Reels
    val REELS_KEYWORDS = listOf(
        "Reels",
        "reels"
    )

    // Keywords to detect Explore page
    val EXPLORE_KEYWORDS = listOf(
        "Explore",
        "explore"
    )

    // Keywords to detect Stories (should NOT be blocked)
    val STORIES_KEYWORDS = listOf(
        "Story",
        "story",
        "Stories",
        "stories"
    )

    // Time estimates for blocked Reels (in minutes)
    const val ESTIMATED_TIME_PER_REEL = 3 // Average 3 minutes per Reels session blocked
}
