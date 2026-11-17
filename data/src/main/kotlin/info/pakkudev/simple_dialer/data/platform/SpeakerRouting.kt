package info.pakkudev.simple_dialer.data.platform

import android.os.OutcomeReceiver
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

interface SpeakerRouting {
    fun updateEndpoints(endpoints: List<CallEndpoint>) = Unit
    fun clear() = Unit
    suspend fun setEnabled(service: InCallService, enabled: Boolean)
}

@Suppress("DEPRECATION")
class AudioRouteSpeakerRouting : SpeakerRouting {
    override suspend fun setEnabled(service: InCallService, enabled: Boolean) {
        service.setAudioRoute(if (enabled) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE)
    }
}

@RequiresApi(34)
class EndpointSpeakerRouting : SpeakerRouting {
    private val availableEndpoints = MutableStateFlow<List<CallEndpoint>>(emptyList())
    override fun updateEndpoints(endpoints: List<CallEndpoint>) { availableEndpoints.value = endpoints.toList() }
    override fun clear() { availableEndpoints.value = emptyList() }
    override suspend fun setEnabled(service: InCallService, enabled: Boolean) {
        val endpoints = availableEndpoints.first { it.isNotEmpty() }
        val preferred = if (enabled) listOf(CallEndpoint.TYPE_SPEAKER) else
            listOf(CallEndpoint.TYPE_WIRED_HEADSET, CallEndpoint.TYPE_BLUETOOTH, CallEndpoint.TYPE_EARPIECE)
        val endpoint = preferred.firstNotNullOfOrNull { type -> endpoints.firstOrNull { it.endpointType == type } }
            ?: error("The selected audio output is unavailable.")
        suspendCancellableCoroutine { continuation ->
            service.requestCallEndpointChange(endpoint, Executor { it.run() }, object : OutcomeReceiver<Void, CallEndpointException> {
                override fun onResult(result: Void?) { if (continuation.isActive) continuation.resume(Unit) }
                override fun onError(error: CallEndpointException) { if (continuation.isActive) continuation.resumeWithException(error) }
            })
        }
    }
}
