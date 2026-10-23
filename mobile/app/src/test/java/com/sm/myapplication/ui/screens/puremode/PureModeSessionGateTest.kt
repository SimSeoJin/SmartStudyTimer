package com.sm.myapplication.ui.screens.puremode

import org.junit.Assert.*
import org.junit.Test

class PureModeSessionGateTest {
    @Test
    fun finishAndLaterComposeDisposeReleaseTheBindingExactlyOnce() {
        val events = mutableListOf<String>()
        val session = PureModeSessionGate().apply {
            setReleaseAction {
                events += "detach"
                events += "stop"
                events += "unbind"
            }
        }

        assertTrue(session.close()) // finish, before navigation
        events += "timer-stop"
        events += "navigate-back"
        assertFalse(session.close()) // delayed onDispose

        assertEquals(listOf("detach", "stop", "unbind", "timer-stop", "navigate-back"), events)
    }

    @Test
    fun oldDisposeAndLateBinderCallbacksCannotStopReenteredSession() {
        var oldStops = 0
        var newStops = 0
        var displayedSession = "old"
        val old = PureModeSessionGate().apply { setReleaseAction { oldStops++ } }
        old.close()

        val current = PureModeSessionGate().apply { setReleaseAction { newStops++ } }
        current.runIfOpen { displayedSession = "new" }
        old.close()
        assertFalse(old.runIfOpen { displayedSession = "old-late-connect"; newStops++ })
        assertFalse(old.runIfOpen { displayedSession = "old-late-disconnect"; newStops++ })

        assertEquals(1, oldStops)
        assertEquals(0, newStops)
        assertEquals("new", displayedSession)
        assertTrue(current.isOpen)
    }

    @Test
    fun oldPreviewReleaseAndLifecycleStopCannotDetachTheNewDisplay() {
        val old = PureModeSessionGate().apply { setReleaseAction {} }
        val oldSurface = Any()
        var displayedSurface: Any? = oldSurface
        old.close()
        val current = PureModeSessionGate().apply { setReleaseAction {} }
        val newSurface = Any()
        current.runIfOpen { displayedSurface = newSurface }

        assertFalse(old.runIfOpen { displayedSurface = null }) // old PreviewView.onRelease
        assertFalse(old.runIfOpen { displayedSurface = null }) // old Lifecycle.ON_STOP

        assertSame(newSurface, displayedSurface)
    }

    @Test
    fun liveDisplayToggleCallbacksRemainAcceptedWithoutClosingAnalysis() {
        var releaseCount = 0
        var attached = true
        val session = PureModeSessionGate().apply { setReleaseAction { releaseCount++ } }

        assertTrue(session.runIfOpen { attached = false })
        assertFalse(attached)
        assertTrue(session.runIfOpen { attached = true })
        assertTrue(attached)
        assertTrue(session.isOpen)
        assertEquals(0, releaseCount)
    }

    @Test
    fun callbackReenteredDuringReleaseIsAlreadyRejected() {
        var acceptedDuringRelease = true
        val session = PureModeSessionGate()
        session.setReleaseAction {
            acceptedDuringRelease = session.runIfOpen { fail("Closed before releasing the service") }
            assertFalse(session.close())
        }

        session.close()
        assertFalse(acceptedDuringRelease)
    }

    @Test
    fun replacingTheCapturedBindingCleanupIsRejected() {
        val session = PureModeSessionGate().apply { setReleaseAction {} }
        assertThrows(IllegalStateException::class.java) { session.setReleaseAction {} }
    }
}
