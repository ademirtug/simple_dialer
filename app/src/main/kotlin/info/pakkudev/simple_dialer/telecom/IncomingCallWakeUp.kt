package info.pakkudev.simple_dialer.telecom

import android.content.Context
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Give the full-screen notification time to launch before applying the OEM fallback. */
internal class IncomingCallWakeUp(
    private val scope: CoroutineScope,
    private val screen: IncomingCallScreenWakeLock,
    private val openCallScreen: () -> Unit,
) {
    private var ringingIds = emptySet<String>()
    private var pending: Job? = null

    fun update(ids: Set<String>) {
        val newCall = (ids - ringingIds).isNotEmpty()
        ringingIds = ids
        if (ids.isEmpty()) {
            close()
        } else if (newCall && !screen.isInteractive) {
            pending?.cancel()
            pending = scope.launch {
                // Waking before posting the notification can reduce it to heads-up UI.
                delay(1_000)
                if (ringingIds.isNotEmpty() && !screen.isInteractive) {
                    screen.wake()
                    openCallScreen()
                }
            }
        }
    }

    fun close() {
        pending?.cancel()
        pending = null
        ringingIds = emptySet()
        screen.release()
    }
}

internal interface IncomingCallScreenWakeLock {
    val isInteractive: Boolean
    fun wake()
    fun release()
}

/** Bounded, service-owned fallback for devices that fail to wake for the call activity. */
internal class AndroidIncomingCallScreenWakeLock(context: Context) : IncomingCallScreenWakeLock {
    private val power = context.getSystemService(PowerManager::class.java)
    private var lock: PowerManager.WakeLock? = null
    override val isInteractive get() = power.isInteractive

    @Suppress("DEPRECATION")
    override fun wake() {
        release()
        try {
            lock = power.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "simple_dialer:incoming_call_screen",
            ).apply {
                setReferenceCounted(false)
                acquire(30_000L)
            }
        } catch (error: RuntimeException) {
            Log.w("DialerInCallService", "Incoming screen wake unavailable", error)
        }
    }

    override fun release() {
        lock?.let { if (it.isHeld) it.release() }
        lock = null
    }
}
