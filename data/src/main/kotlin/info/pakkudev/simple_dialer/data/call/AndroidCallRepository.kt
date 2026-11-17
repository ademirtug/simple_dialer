package info.pakkudev.simple_dialer.data.call

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import info.pakkudev.simple_dialer.platform.CallPlacement
import info.pakkudev.simple_dialer.data.platform.SpeakerRouting
import info.pakkudev.simple_dialer.domain.model.ActiveCall
import info.pakkudev.simple_dialer.domain.model.AudioState
import info.pakkudev.simple_dialer.domain.model.CallStatus
import info.pakkudev.simple_dialer.domain.model.CallDisconnectInfo
import info.pakkudev.simple_dialer.domain.model.CallEndReason
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.domain.repository.CallLauncher
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Owned by the application, fed by InCallService. Contains no Activity references. */
@Suppress("DEPRECATION")
class AndroidCallRepository(
    private val placement: CallPlacement,
    private val speaker: SpeakerRouting,
    private val main: CoroutineDispatcher,
) : CallRepository, CallLauncher {
    private data class Entry(val id: String, val call: Call, val callback: Call.Callback, var locallyEnded: Boolean = false)
    private val entries = linkedMapOf<Call, Entry>()
    private val mutableCalls = MutableStateFlow<List<ActiveCall>>(emptyList())
    private val mutableLastDisconnectedCall = MutableStateFlow<ActiveCall?>(null)
    private val mutableAudio = MutableStateFlow(AudioState(earpieceAvailable = false, speakerAvailable = false))
    private val mutableCanAddCall = MutableStateFlow(false)
    private var service: InCallService? = null
    override val calls = mutableCalls.asStateFlow()
    override val lastDisconnectedCall = mutableLastDisconnectedCall.asStateFlow()
    override val audio = mutableAudio.asStateFlow()
    override val canAddCall = mutableCanAddCall.asStateFlow()
    fun updateCanAddCall(allowed: Boolean) { mutableCanAddCall.value = allowed }

    fun attach(service: InCallService) { this.service = service }

    fun detach() {
        entries.values.forEach { it.call.unregisterCallback(it.callback) }
        entries.clear()
        service = null
        mutableCalls.value = emptyList()
        mutableAudio.value = AudioState(earpieceAvailable = false, speakerAvailable = false)
        mutableCanAddCall.value = false
    }

    fun add(call: Call) {
        if (entries.containsKey(call)) return
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) = publish()
            override fun onDetailsChanged(call: Call, details: Call.Details) = publish()
            override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: MutableList<Call>) = publish()
            override fun onChildrenChanged(call: Call, children: MutableList<Call>) = publish()
        }
        entries[call] = Entry(UUID.randomUUID().toString(), call, callback)
        call.registerCallback(callback, Handler(Looper.getMainLooper()))
        publish()
    }

    fun remove(call: Call) {
        entries.remove(call)?.let { entry ->
            if (call.parent == null) mutableLastDisconnectedCall.value = snapshot(entry).copy(status = CallStatus.DISCONNECTED)
            call.unregisterCallback(entry.callback)
        }
        publish()
    }

    fun updateAudio(state: CallAudioState) {
        mutableAudio.value = mutableAudio.value.copy(muted = state.isMuted, speakerEnabled = state.route == CallAudioState.ROUTE_SPEAKER,
            externalDevice = state.route == CallAudioState.ROUTE_BLUETOOTH || state.route == CallAudioState.ROUTE_WIRED_HEADSET,
            earpieceAvailable = state.supportedRouteMask and CallAudioState.ROUTE_EARPIECE != 0,
            speakerAvailable = state.supportedRouteMask and CallAudioState.ROUTE_SPEAKER != 0)
    }

    fun updateMuted(muted: Boolean) { mutableAudio.value = mutableAudio.value.copy(muted = muted) }
    fun updateAvailableOutputs(count: Int, earpiece: Boolean, speaker: Boolean) {
        mutableAudio.value = mutableAudio.value.copy(availableOutputs = count, earpieceAvailable = earpiece, speakerAvailable = speaker)
    }
    fun updateSpeaker(enabled: Boolean, externalDevice: Boolean = false) {
        mutableAudio.value = mutableAudio.value.copy(speakerEnabled = enabled, externalDevice = externalDevice)
    }

    override suspend fun placeCall(number: String) = withContext(main) { placement.place(number) }
    override fun answer(id: String) { find(id).answer(VideoProfile.STATE_AUDIO_ONLY) }
    override fun end(id: String) {
        val entry = entries.values.firstOrNull { it.id == id } ?: error("This call has ended.")
        entry.locallyEnded = true
        try {
            if (entry.call.state == Call.STATE_RINGING) entry.call.reject(false, null) else entry.call.disconnect()
        } catch (error: RuntimeException) { entry.locallyEnded = false; throw error }
    }
    override fun setMuted(muted: Boolean) { checkNotNull(service) { "No active phone service." }.setMuted(muted) }
    override suspend fun setSpeakerEnabled(enabled: Boolean) = withContext(main) {
        withTimeoutOrNull(5_000) {
            speaker.setEnabled(checkNotNull(service) { "No active phone service." }, enabled)
            true
        } ?: error("The audio output did not respond. Try again.")
        Unit
    }
    override fun setHeld(id: String, held: Boolean) { if (held) find(id).hold() else find(id).unhold() }
    override fun merge(id: String) {
        val call = find(id)
        val other = call.conferenceableCalls.firstOrNull()
        if (other != null) call.conference(other) else call.mergeConference()
    }
    override fun swap(id: String) { find(id).swapConference() }
    override fun playDtmf(id: String, digit: Char) { require(digit in "0123456789*#"); find(id).playDtmfTone(digit) }
    override fun stopDtmf(id: String) { entries.values.firstOrNull { it.id == id }?.call?.stopDtmfTone() }

    private fun find(id: String) = entries.values.firstOrNull { it.id == id }?.call ?: error("This call has ended.")

    private fun publish() {
        val snapshots = entries.values.filter { it.call.parent == null }.map(::snapshot)
        snapshots.lastOrNull { it.status == CallStatus.DISCONNECTED }?.let { mutableLastDisconnectedCall.value = it }
        mutableCalls.value = snapshots
    }

    private fun snapshot(entry: Entry): ActiveCall {
            val details = entry.call.details
            return ActiveCall(entry.id, details.handle?.schemeSpecificPart.orEmpty(), details.callerDisplayName.orEmpty(), when (entry.call.state) {
                Call.STATE_RINGING -> CallStatus.RINGING
                Call.STATE_DIALING -> CallStatus.DIALING
                Call.STATE_ACTIVE -> CallStatus.ACTIVE
                Call.STATE_HOLDING -> CallStatus.HOLDING
                Call.STATE_DISCONNECTING -> CallStatus.DISCONNECTING
                Call.STATE_DISCONNECTED -> CallStatus.DISCONNECTED
                else -> CallStatus.CONNECTING
            }, connectedAtMillis = details.connectTimeMillis,
                canHold = details.can(Call.Details.CAPABILITY_HOLD) || entry.call.state == Call.STATE_HOLDING,
                canMerge = details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) || entry.call.conferenceableCalls.isNotEmpty(),
                canSwap = details.can(Call.Details.CAPABILITY_SWAP_CONFERENCE),
                isConference = details.hasProperty(Call.Details.PROPERTY_CONFERENCE),
                disconnectInfo = if (entry.locallyEnded) CallDisconnectInfo(CallEndReason.LOCAL)
                    else details.disconnectCause.toCallDisconnectInfo())
    }
}
