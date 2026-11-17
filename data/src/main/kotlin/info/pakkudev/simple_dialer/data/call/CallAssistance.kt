package info.pakkudev.simple_dialer.data.call

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import info.pakkudev.simple_dialer.domain.model.*
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.domain.usecase.CallAssistancePolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

interface CallScreenLock { fun setEnabled(enabled: Boolean); fun release() }

class AndroidCallScreenLock(context: Context) : CallScreenLock {
    private val power = context.getSystemService(PowerManager::class.java)
    private val lock = if (power.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
        power.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "simple_dialer:call_proximity").apply { setReferenceCounted(false) } else null

    @SuppressLint("WakelockTimeout") // Owned by the bound call service; released on route change and service destruction.
    override fun setEnabled(enabled: Boolean) {
        if (enabled && lock?.isHeld == false) lock.acquire()
        if (!enabled) release()
    }
    override fun release() { if (lock?.isHeld == true) lock.release() }
}

class CallAssistance(
    private val calls: CallRepository,
    private val preferences: PreferencesRepository,
    private val proximity: ProximityMonitor,
    private val screenLock: CallScreenLock,
    private val nowMillis: () -> Long = SystemClock::elapsedRealtime,
) {
    private data class Input(val calls: List<ActiveCall>, val audio: AudioState, val settings: DialerPreferences, val near: Boolean?)

    fun start(scope: CoroutineScope) = scope.launch {
        val policy = CallAssistancePolicy()
        try {
            combine(calls.calls, calls.audio, preferences.preferences, proximity.near) { active, audio, settings, near ->
                Input(active, audio, settings, near)
            }.collectLatest { input ->
                screenLock.setEnabled(proximity.available && input.calls.any { it.status == CallStatus.ACTIVE } && !input.audio.speakerEnabled && !input.audio.externalDevice)
                // New input cancels the wait. Policy timestamps preserve elapsed time
                // across unrelated updates such as mute or caller-name changes.
                while (true) {
                    val actions = policy.evaluate(input.settings, input.calls, input.near, input.audio, nowMillis())
                    try {
                        actions.answer.forEach(calls::answer)
                        actions.speaker?.let { calls.setSpeakerEnabled(it) }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        Log.w("CallAssistance", "Automatic call action unavailable", error)
                    }
                    delay(actions.nextEvaluationDelayMillis ?: break)
                }
            }
        } finally { screenLock.release() }
    }
}
