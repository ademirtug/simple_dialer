package info.pakkudev.simple_dialer.domain.usecase

import info.pakkudev.simple_dialer.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class CallAssistancePolicyTest {
    private val policy = CallAssistancePolicy()
    private val ringing = listOf(ActiveCall("one", "555", "", CallStatus.RINGING))
    private val active = ringing.map { it.copy(status = CallStatus.ACTIVE) }
    private val answerPrefs = DialerPreferences(autoAnswer = true)
    private val speakerPrefs = DialerPreferences(autoSpeaker = true)

    private fun answer(at: Long, near: Boolean?, calls: List<ActiveCall> = ringing, prefs: DialerPreferences = answerPrefs) =
        policy.evaluate(prefs, calls, near, AudioState(), at)

    private fun speaker(at: Long, near: Boolean?, audio: AudioState = AudioState(), calls: List<ActiveCall> = active,
                        prefs: DialerPreferences = speakerPrefs) = policy.evaluate(prefs, calls, near, audio, at)

    @Test fun `automatic modes default to off`() {
        assertEquals(AssistanceActions(), answer(0, false, prefs = DialerPreferences()))
        assertEquals(AssistanceActions(), speaker(2_000, false, prefs = DialerPreferences()))
    }

    @Test fun `initial pocket reading never answers`() {
        assertEquals(AssistanceActions(), answer(0, true))
        assertEquals(AssistanceActions(), answer(10_000, true))
    }

    @Test fun `answer requires 500ms far followed by 800ms near and happens once`() {
        assertEquals(500L, answer(0, false).nextEvaluationDelayMillis)
        assertEquals(1L, answer(499, false).nextEvaluationDelayMillis)
        assertNull(answer(500, false).nextEvaluationDelayMillis)
        assertEquals(800L, answer(600, true).nextEvaluationDelayMillis)
        assertEquals(1L, answer(1_399, true).nextEvaluationDelayMillis)
        assertEquals(listOf("one"), answer(1_400, true).answer)
        assertEquals(AssistanceActions(), answer(2_000, true))
    }

    @Test fun `far to near at arm deadline qualifies even without timer evaluation`() {
        answer(0, false)
        assertEquals(800L, answer(500, true).nextEvaluationDelayMillis)
        assertEquals(listOf("one"), answer(1_300, true).answer)
    }

    @Test fun `brief uncovered pocket movement does not arm`() {
        answer(0, false)
        assertEquals(AssistanceActions(), answer(499, true))
        assertTrue(answer(5_000, true).answer.isEmpty())
    }

    @Test fun `uncovering cancels answer countdown and requires fresh arming`() {
        answer(0, false)
        answer(500, true)
        answer(1_000, false)
        assertEquals(AssistanceActions(), answer(1_100, true))
        assertTrue(answer(5_000, true).answer.isEmpty())
        answer(6_000, false)
        answer(6_500, true)
        assertEquals(listOf("one"), answer(7_300, true).answer)
    }

    @Test fun `new ringing calls never inherit another call's arming`() {
        answer(0, false)
        answer(500, false)
        val next = listOf(ringing.single().copy(id = "two"))
        assertEquals(AssistanceActions(), answer(600, true, next))
        assertTrue(answer(2_000, true, next).answer.isEmpty())
    }

    @Test fun `far readings before ringing do not count toward arming`() {
        answer(0, false, emptyList())
        assertEquals(500L, answer(10_000, false).nextEvaluationDelayMillis)
        assertEquals(AssistanceActions(), answer(10_100, true))
    }

    @Test fun `disabling auto answer clears pending and armed state`() {
        answer(0, false)
        answer(500, true)
        answer(700, true, prefs = DialerPreferences())
        assertEquals(AssistanceActions(), answer(1_300, true))
    }

    @Test fun `unknown sensor cancels all answer progress`() {
        answer(0, false)
        answer(500, true)
        assertEquals(AssistanceActions(), answer(700, null))
        assertEquals(AssistanceActions(), answer(1_300, true))
    }

    @Test fun `answer countdown stops when call is no longer ringing`() {
        answer(0, false)
        answer(500, true)
        assertEquals(AssistanceActions(), answer(1_300, true, active))
        assertEquals(AssistanceActions(), answer(2_000, true))
    }

    @Test fun `speaker waits 1500ms of active far and turns off immediately near`() {
        assertEquals(AssistanceActions(), speaker(0, false, calls = ringing))
        assertEquals(1_500L, speaker(1_000, false).nextEvaluationDelayMillis)
        assertEquals(1L, speaker(2_499, false).nextEvaluationDelayMillis)
        assertEquals(true, speaker(2_500, false).speaker)
        assertEquals(false, speaker(2_501, true, AudioState(speakerEnabled = true)).speaker)
    }

    @Test fun `near or unknown readings restart speaker countdown`() {
        speaker(0, false)
        assertEquals(AssistanceActions(), speaker(1_000, true))
        assertEquals(1_500L, speaker(1_100, false).nextEvaluationDelayMillis)
        speaker(2_000, null)
        assertEquals(1_500L, speaker(3_000, false).nextEvaluationDelayMillis)
    }

    @Test fun `unrelated audio updates do not extend speaker countdown`() {
        speaker(0, false)
        assertEquals(1_000L, speaker(500, false, AudioState(muted = true)).nextEvaluationDelayMillis)
        assertEquals(true, speaker(1_500, false, AudioState(muted = true)).speaker)
    }

    @Test fun `headsets and unavailable routes cancel speaker countdown`() {
        speaker(0, false)
        assertEquals(AssistanceActions(), speaker(1_000, false, AudioState(externalDevice = true)))
        assertEquals(1_500L, speaker(2_000, false).nextEvaluationDelayMillis)
        assertEquals(AssistanceActions(), speaker(3_000, false, AudioState(speakerAvailable = false)))
        assertEquals(1_500L, speaker(4_000, false).nextEvaluationDelayMillis)
        assertNull(speaker(5_000, true, AudioState(speakerEnabled = true, earpieceAvailable = false)).speaker)
    }

    @Test fun `ending calls disabling setting and new active calls reset speaker delay`() {
        speaker(0, false)
        speaker(1_000, false, calls = emptyList())
        assertEquals(1_500L, speaker(2_000, false).nextEvaluationDelayMillis)
        speaker(3_000, false, prefs = DialerPreferences())
        assertEquals(1_500L, speaker(4_000, false).nextEvaluationDelayMillis)
        val next = listOf(active.single().copy(id = "two"))
        assertEquals(1_500L, speaker(5_000, false, calls = next).nextEvaluationDelayMillis)
    }

    @Test fun `answer and speaker countdowns run independently`() {
        val prefs = DialerPreferences(autoAnswer = true, autoSpeaker = true)
        val calls = active + ringing.single().copy(id = "two")
        assertEquals(500L, policy.evaluate(prefs, calls, false, AudioState(), 0).nextEvaluationDelayMillis)
        assertEquals(1_000L, policy.evaluate(prefs, calls, false, AudioState(), 500).nextEvaluationDelayMillis)
        policy.evaluate(prefs, calls, true, AudioState(), 600)
        val result = policy.evaluate(prefs, calls, true, AudioState(), 1_400)
        assertEquals(listOf("two"), result.answer)
        assertNull(result.speaker)
    }
}
