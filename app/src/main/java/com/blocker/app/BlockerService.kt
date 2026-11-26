package com.blocker.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class BlockerService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            serviceInfo = info
        }
        Toast.makeText(this, "Блокировщик запущен", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return
        val blocked = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
            .getStringSet(MainActivity.KEY_BLOCKED, emptySet()) ?: emptySet()
        if (pkg in blocked) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() {}
}
