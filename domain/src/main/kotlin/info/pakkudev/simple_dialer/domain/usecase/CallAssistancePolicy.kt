package info.pakkudev.simple_dialer.domain.usecase

import info.pakkudev.simple_dialer.domain.model.ActiveCall
import info.pakkudev.simple_dialer.domain.model.AudioState
import info.pakkudev.simple_dialer.domain.model.CallStatus
import info.pakkudev.simple_dialer.domain.model.DialerPreferences

data class AssistanceActions(
    val answer: List<String> = emptyList(),
    val speaker: Boolean? = null,
    val nextEvaluationDelayMillis: Long? = null,
)

/** Uses monotonic time and a fresh far-then-near sequence for each ringing call. */
class CallAssistancePolicy {
    private class AnswerTiming {
        var farSince: Long? = null
        var nearSince: Long? = null
        var armed = false
    }

    private val answerTiming = mutableMapOf<String, AnswerTiming>()
    private val answered = mutableSetOf<String>()
    private var speakerCalls = emptySet<String>()
    private var speakerFarSince: Long? = null

    fun evaluate(preferences: DialerPreferences, calls: List<ActiveCall>, near: Boolean?, audio: AudioState, nowMillis: Long): AssistanceActions {
        val ringing = calls.filter { it.status == CallStatus.RINGING }.map { it.id }.toSet()
        answerTiming.keys.retainAll(ringing)
        answered.retainAll(ringing)
        var nextDelay: Long? = null
        fun schedule(remaining: Long) {
            if (remaining > 0) nextDelay = minOf(nextDelay ?: remaining, remaining)
        }

        val answer = mutableListOf<String>()
        if (!preferences.autoAnswer || near == null) {
            answerTiming.clear()
        } else {
            (ringing - answered).forEach { id ->
                val timing = answerTiming.getOrPut(id) { AnswerTiming() }
                if (!near) {
                    val since = timing.farSince ?: nowMillis.also {
                        timing.farSince = it
                        timing.armed = false
                    }
                    timing.nearSince = null
                    val remaining = ANSWER_ARM_MILLIS - (nowMillis - since)
                    if (remaining <= 0) timing.armed = true else schedule(remaining)
                } else {
                    // A sensor event can arrive at the deadline before the timer resumes.
                    if (timing.farSince?.let { nowMillis - it >= ANSWER_ARM_MILLIS } == true) timing.armed = true
                    timing.farSince = null
                    if (timing.armed) {
                        val since = timing.nearSince ?: nowMillis.also { timing.nearSince = it }
                        val remaining = ANSWER_NEAR_MILLIS - (nowMillis - since)
                        if (remaining <= 0) answer += id else schedule(remaining)
                    }
                }
            }
        }
        answered.addAll(answer)

        val active = calls.filter { it.status == CallStatus.ACTIVE }.map { it.id }.toSet()
        if (active != speakerCalls) speakerFarSince = null
        speakerCalls = active
        var speaker: Boolean? = null
        if (!preferences.autoSpeaker || active.isEmpty() || near == null || audio.externalDevice) {
            speakerFarSince = null
        } else if (near) {
            speakerFarSince = null
            if (audio.speakerEnabled && audio.earpieceAvailable) speaker = false
        } else if (!audio.speakerEnabled && audio.speakerAvailable) {
            val since = speakerFarSince ?: nowMillis.also { speakerFarSince = it }
            val remaining = SPEAKER_FAR_MILLIS - (nowMillis - since)
            if (remaining <= 0) speaker = true else schedule(remaining)
        } else {
            speakerFarSince = null
        }
        return AssistanceActions(answer, speaker, nextDelay)
    }

    private companion object {
        const val ANSWER_ARM_MILLIS = 500L
        const val ANSWER_NEAR_MILLIS = 800L
        const val SPEAKER_FAR_MILLIS = 1_500L
    }
}
