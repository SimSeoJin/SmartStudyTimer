package com.sm.myapplication.ui.screens.puremode

/**
 * Main-thread gate for one screen's camera binding. Closing before navigation
 * makes a later Compose disposal, binder callback, or PreviewView release inert.
 * A new screen receives its own gate; it is never reopened or reused.
 */
internal class PureModeSessionGate {
    var isOpen: Boolean = true
        private set

    private var releaseAction: (() -> Unit)? = null

    fun setReleaseAction(action: () -> Unit) {
        check(isOpen && releaseAction == null) { "A session owns exactly one release action" }
        releaseAction = action
    }

    fun runIfOpen(action: () -> Unit): Boolean {
        if (!isOpen) return false
        action()
        return true
    }

    fun close(): Boolean {
        if (!isOpen) return false
        // Close first: synchronous service callbacks during cleanup must also be ignored.
        isOpen = false
        val release = releaseAction
        releaseAction = null
        release?.invoke()
        return true
    }
}
