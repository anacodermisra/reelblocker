# Instagram Reels Blocker (v1.5 Pure Precision)

A free, private, personal Android Accessibility Service that blocks Instagram Reels and the Explore page to prevent doomscrolling — while keeping your Following feed, Stories, Direct Messages, and post creation completely uninterrupted.

---

## What It Blocks vs What It Allows

| Feature | Status | Behavior |
| :--- | :--- | :--- |
| **Reels Tab (Bottom Bar)** | 🚫 **BLOCKED** | Tapping the Reels tab immediately redirects you back to the Home feed. |
| **Explore Tab (Bottom Bar)** | 🚫 **BLOCKED** | Tapping the Explore/Search tab immediately redirects you back to the Home feed. |
| **Explore Reels** | 🚫 **BLOCKED** | Tapping any reel in the Explore grid dismisses the modal player and returns to Home. |
| **Home Following Feed** | ✅ **ALLOWED** | Scroll through your following feed, photos, and feed videos with zero interruptions. |
| **Direct Messages (DMs)** | ✅ **ALLOWED** | Chat in 1-on-1 and group chats, send voice notes, view inbox without any kicks. |
| **Friends' Shared Reels** | ✅ **ALLOWED** | Open and watch any reel link sent by a friend in DMs freely. |
| **Stories & Reactions** | ✅ **ALLOWED** | Watch stories, reply, and react without interference. |
| **Post & Story Creation** | ✅ **ALLOWED** | Camera, post composer, and story editor remain fully functional. |

---

## Key Design Principles (v1.5)

1. **In-App Programmatic Redirection**:
   - Never closes, minimizes, or crashes the Instagram app.
   - Programmatically executes an accessibility click on the Home tab node to keep your flow smooth.
2. **Zero False Positives in DMs & Stories**:
   - Evaluates UI landmarks in real time.
   - Bottom bar detection ensures modal screens like Stories and DMs are mathematically guaranteed safe.
3. **Fail-Open Policy**:
   - Unknown or ambiguous screens are always allowed. Only confirmed Reels and Explore surfaces are blocked.
4. **100% On-Device & Private**:
   - No internet permission.
   - Zero analytics, trackers, or background telemetry.

---

## Tech Stack & Architecture

- **Language**: Kotlin
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 / 15 (API 34)
- **Primary Mechanism**: Android `AccessibilityService` (`android.permission.BIND_ACCESSIBILITY_SERVICE`)
- **UI Framework**: Material Design 3 (Dark `#0A0E14`)

---

## Installation

### Option 1: Direct APK Installation (Easiest)
1. Download `InstagramReelsBlocker-v1.5.apk` from the latest release.
2. Tap the APK on your Android phone to install.
3. Open **Reels Blocker**, tap **Enable Accessibility Permission**, and switch the service to **ON**.

### Option 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/anacodermisra/reelblocker.git
cd reelblocker

# Build debug APK
gradlew.bat assembleDebug

# Install directly to connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## License

Free and open source for personal productivity and digital wellbeing.
