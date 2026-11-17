package info.pakkudev.simple_dialer.telecom

import android.content.Intent
import android.app.KeyguardManager
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.InCallService
import android.util.Log
import androidx.annotation.RequiresApi
import info.pakkudev.simple_dialer.data.call.AndroidCallRepository
import info.pakkudev.simple_dialer.data.call.CallAssistance
import info.pakkudev.simple_dialer.domain.model.CallStatus
import info.pakkudev.simple_dialer.data.platform.SpeakerRouting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import org.koin.android.ext.android.inject

class DialerInCallService : InCallService() {
    private val repository: AndroidCallRepository by inject()
    private val notifications: CallNotifications by inject()
    private val speaker: SpeakerRouting by inject()
    private val assistance: CallAssistance by inject()
    private val callUi: CallUiVisibility by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var incomingWakeUp: IncomingCallWakeUp

    override fun onCreate() {
        super.onCreate()
        incomingWakeUp = IncomingCallWakeUp(serviceScope, AndroidIncomingCallScreenWakeLock(this), ::openCallScreen)
        repository.attach(this)
        assistance.start(serviceScope)
        serviceScope.launch {
            var ringingIds = emptySet<String>()
            combine(repository.calls, callUi.visible) { calls, visible -> calls to visible }.collect { (calls, visible) ->
                val newRingingCall = calls.any { it.status == CallStatus.RINGING && it.id !in ringingIds }
                val answeredCall = calls.any { it.id in ringingIds && it.status == CallStatus.ACTIVE }
                ringingIds = calls.filter { it.status == CallStatus.RINGING }.map { it.id }.toSet()
                // Telecom binds this service for incoming calls. Wake its UI directly
                // on a locked/asleep phone; retain heads-up notifications while in use.
                val lockedOrAsleep = getSystemService(KeyguardManager::class.java).isKeyguardLocked ||
                    !getSystemService(PowerManager::class.java).isInteractive
                try { if (visible) notifications.clear() else notifications.update(calls) }
                catch (error: RuntimeException) { Log.w("DialerInCallService", "Call notification could not be posted", error) }
                if ((newRingingCall && lockedOrAsleep) || answeredCall) openCallScreen()
                incomingWakeUp.update(ringingIds)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        repository.add(call)
        repository.updateCanAddCall(canAddCall())
        if (call.state != Call.STATE_RINGING) openCallScreen()
    }
    override fun onCallRemoved(call: Call) { repository.remove(call); super.onCallRemoved(call) }
    override fun onBringToForeground(showDialpad: Boolean) { openCallScreen() }
    override fun onCanAddCallChanged(canAddCall: Boolean) { repository.updateCanAddCall(canAddCall) }

    @Deprecated("Legacy audio callback for Android 6–13")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        repository.updateAudio(audioState)
    }

    @RequiresApi(34)
    override fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        speaker.updateEndpoints(availableEndpoints)
        repository.updateAvailableOutputs(availableEndpoints.size, availableEndpoints.any { it.endpointType == CallEndpoint.TYPE_EARPIECE },
            availableEndpoints.any { it.endpointType == CallEndpoint.TYPE_SPEAKER })
    }

    @RequiresApi(34)
    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        repository.updateSpeaker(callEndpoint.endpointType == CallEndpoint.TYPE_SPEAKER,
            callEndpoint.endpointType == CallEndpoint.TYPE_BLUETOOTH || callEndpoint.endpointType == CallEndpoint.TYPE_WIRED_HEADSET)
    }

    @RequiresApi(34)
    override fun onMuteStateChanged(isMuted: Boolean) { repository.updateMuted(isMuted) }

    private fun openCallScreen() {
        try { startActivity(Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
        catch (error: RuntimeException) { Log.w("DialerInCallService", "Call screen will remain available through its notification", error) }
    }

    override fun onDestroy() {
        incomingWakeUp.close()
        serviceScope.cancel()
        notifications.clear()
        speaker.clear()
        repository.detach()
        super.onDestroy()
    }
}
