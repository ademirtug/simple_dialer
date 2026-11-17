package info.pakkudev.simple_dialer.domain.usecase

import info.pakkudev.simple_dialer.domain.model.ActiveCall
import info.pakkudev.simple_dialer.domain.model.CallEndReason
import info.pakkudev.simple_dialer.domain.model.CallStatus

data class PresentedCalls(val calls: List<ActiveCall> = emptyList(), val closeAfterMillis: Long? = null)

/** Retains only a UI snapshot after Telecom removes a call; never keeps the call itself alive. */
class CallScreenPresentation {
    private var lastLive: ActiveCall? = null
    private var ended: ActiveCall? = null
    private var endedAt = 0L
    private val locallyEnded = mutableSetOf<String>()

    fun localEndRequested(id: String) { locallyEnded += id }
    fun localEndFailed(id: String) { locallyEnded -= id }

    fun update(calls: List<ActiveCall>, nowMillis: Long, lastDisconnected: ActiveCall? = null): PresentedCalls {
        val live = calls.filter { it.status != CallStatus.DISCONNECTED }
        if (live.isNotEmpty()) {
            lastLive = live.firstOrNull { it.status == CallStatus.RINGING }
                ?: live.firstOrNull { it.status == CallStatus.ACTIVE } ?: live.first()
            ended = null
            locallyEnded.retainAll(live.map { it.id }.toSet())
            return PresentedCalls(live)
        }
        val prior = ended ?: lastLive
        val terminal = calls.firstOrNull { it.id == prior?.id }
            ?: lastDisconnected?.takeIf { prior != null && it.id == prior.id } ?: calls.lastOrNull()
            ?: prior?.copy(status = CallStatus.DISCONNECTED) ?: return PresentedCalls()
        if (ended?.id != terminal.id) endedAt = nowMillis
        ended = terminal
        lastLive = null
        val duration = if (terminal.id in locallyEnded) 0L else when (terminal.disconnectInfo?.reason) {
            CallEndReason.LOCAL, CallEndReason.CANCELED, CallEndReason.MISSED -> 0L
            CallEndReason.BUSY, CallEndReason.REJECTED, CallEndReason.ERROR, CallEndReason.RESTRICTED -> 5_000L
            else -> 2_000L
        }
        val remaining = endedAt + duration - nowMillis
        return if (remaining > 0) PresentedCalls(listOf(terminal), remaining) else PresentedCalls()
    }
}
