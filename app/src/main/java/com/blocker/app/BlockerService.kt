package com.blocker.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val blocked = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
            .getStringSet(MainActivity.KEY_BLOCKED, emptySet()) ?: emptySet()
        if (pkg in blocked) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() {}
}

