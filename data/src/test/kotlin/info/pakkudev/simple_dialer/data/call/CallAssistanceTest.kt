package info.pakkudev.simple_dialer.data.call

import info.pakkudev.simple_dialer.domain.model.*
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallAssistanceTest {
    private val ringing = ActiveCall("one", "555", "", CallStatus.RINGING)
    private val repository = FakeCalls()
    private val settings = FakePreferences()
    private val proximity = object : ProximityMonitor {
        override val available = true
        override val near = MutableStateFlow<Boolean?>(false)
    }
    private val lock = object : CallScreenLock {
        var held = false
        var released = false
        override fun setEnabled(enabled: Boolean) { held = enabled }
        override fun release() { held = false; released = true }
    }

    private fun TestScope.start() = CallAssistance(repository, settings, proximity, lock) { currentTime }.start(backgroundScope)
    private fun TestScope.tick(millis: Long) { advanceTimeBy(millis); runCurrent() }

    @Test fun `a stable sensor triggers answer without further sensor events`() = runTest {
        settings.preferences.value = DialerPreferences(autoAnswer = true)
        repository.calls.value = listOf(ringing)
        start()
        runCurrent()
        tick(500)
        proximity.near.value = true
        runCurrent()
        tick(799)
        assertTrue(repository.answers.isEmpty())
        tick(1)
        assertEquals(listOf("one"), repository.answers)
        tick(5_000)
        assertEquals(1, repository.answers.size)
    }

    @Test fun `uncovering during pending answer cancels it`() = runTest {
        settings.preferences.value = DialerPreferences(autoAnswer = true)
        repository.calls.value = listOf(ringing)
        start()
        runCurrent()
        tick(500)
        proximity.near.value = true
        runCurrent()
        tick(799)
        proximity.near.value = false
        runCurrent()
        tick(100)
        proximity.near.value = true
        runCurrent()
        tick(5_000)
        assertTrue(repository.answers.isEmpty())
    }

    @Test fun `disabling auto answer or ending ringing cancels pending answer`() = runTest {
        settings.preferences.value = DialerPreferences(autoAnswer = true)
        repository.calls.value = listOf(ringing)
        start()
        runCurrent()
        tick(500)
        proximity.near.value = true
        runCurrent()
        tick(400)
        settings.preferences.value = DialerPreferences()
        runCurrent()
        tick(1_000)
        assertTrue(repository.answers.isEmpty())
        settings.preferences.value = DialerPreferences(autoAnswer = true)
        proximity.near.value = false
        runCurrent()
        tick(500)
        proximity.near.value = true
        runCurrent()
        tick(400)
        repository.calls.value = emptyList()
        runCurrent()
        tick(1_000)
        assertTrue(repository.answers.isEmpty())
    }

    @Test fun `speaker timer survives unrelated audio updates and near turns it off immediately`() = runTest {
        settings.preferences.value = DialerPreferences(autoSpeaker = true)
        repository.calls.value = listOf(ringing.copy(status = CallStatus.ACTIVE))
        start()
        runCurrent()
        tick(700)
        repository.audio.value = AudioState(muted = true)
        runCurrent()
        tick(799)
        assertTrue(repository.speakers.isEmpty())
        tick(1)
        assertEquals(listOf(true), repository.speakers)
        proximity.near.value = true
        runCurrent()
        assertEquals(listOf(true, false), repository.speakers)
    }

    @Test fun `near cancels speaker timer and uncovering starts a full new delay`() = runTest {
        settings.preferences.value = DialerPreferences(autoSpeaker = true)
        repository.calls.value = listOf(ringing.copy(status = CallStatus.ACTIVE))
        start()
        runCurrent()
        tick(1_499)
        proximity.near.value = true
        runCurrent()
        tick(1_000)
        assertTrue(repository.speakers.isEmpty())
        proximity.near.value = false
        runCurrent()
        tick(1_499)
        assertTrue(repository.speakers.isEmpty())
        tick(1)
        assertEquals(listOf(true), repository.speakers)
    }

    @Test fun `a headset cancels pending speaker switching`() = runTest {
        settings.preferences.value = DialerPreferences(autoSpeaker = true)
        repository.calls.value = listOf(ringing.copy(status = CallStatus.ACTIVE))
        start()
        runCurrent()
        tick(1_000)
        repository.audio.value = AudioState(externalDevice = true)
        runCurrent()
        tick(5_000)
        assertTrue(repository.speakers.isEmpty())
        repository.audio.value = AudioState()
        runCurrent()
        tick(1_499)
        assertTrue(repository.speakers.isEmpty())
        tick(1)
        assertEquals(listOf(true), repository.speakers)
    }

    @Test fun `stopping assistance cancels timers and releases screen lock`() = runTest {
        settings.preferences.value = DialerPreferences(autoSpeaker = true)
        repository.calls.value = listOf(ringing.copy(status = CallStatus.ACTIVE))
        val job = start()
        runCurrent()
        assertTrue(lock.held)
        tick(1_000)
        job.cancel()
        runCurrent()
        tick(5_000)
        assertTrue(repository.speakers.isEmpty())
        assertTrue(lock.released)
        assertFalse(lock.held)
    }

    private class FakeCalls : CallRepository {
        override val calls = MutableStateFlow<List<ActiveCall>>(emptyList())
        override val lastDisconnectedCall = MutableStateFlow<ActiveCall?>(null)
        override val audio = MutableStateFlow(AudioState())
        override val canAddCall = MutableStateFlow(false)
        val answers = mutableListOf<String>()
        val speakers = mutableListOf<Boolean>()
        override fun answer(id: String) {
            answers += id
            calls.value = calls.value.map { if (it.id == id) it.copy(status = CallStatus.ACTIVE) else it }
        }
        override suspend fun setSpeakerEnabled(enabled: Boolean) {
            speakers += enabled
            audio.value = audio.value.copy(speakerEnabled = enabled)
        }
        override fun end(id: String) = error("Unexpected end")
        override fun setMuted(muted: Boolean) = error("Unexpected mute")
        override fun setHeld(id: String, held: Boolean) = error("Unexpected hold")
        override fun merge(id: String) = error("Unexpected merge")
        override fun swap(id: String) = error("Unexpected swap")
        override fun playDtmf(id: String, digit: Char) = error("Unexpected DTMF")
        override fun stopDtmf(id: String) = error("Unexpected DTMF")
    }

    private class FakePreferences : PreferencesRepository {
        override val preferences = MutableStateFlow(DialerPreferences())
        override suspend fun setShowContacts(enabled: Boolean) = error("Unexpected preference write")
        override suspend fun setShowRecents(enabled: Boolean) = error("Unexpected preference write")
        override suspend fun setShowFavorites(enabled: Boolean) = error("Unexpected preference write")
        override suspend fun setShowContactDetails(enabled: Boolean) = error("Unexpected preference write")
        override suspend fun setAutoAnswer(enabled: Boolean) = error("Unexpected preference write")
        override suspend fun setAutoSpeaker(enabled: Boolean) = error("Unexpected preference write")
        override suspend fun setFavorite(number: String, favorite: Boolean) = error("Unexpected preference write")
        override suspend fun setControlVisible(control: CallControl, visible: Boolean) = error("Unexpected preference write")
        override suspend fun setColorPalette(palette: ColorPalette) = error("Unexpected preference write")
        override suspend fun setIncomingCallStyle(style: IncomingCallStyle) = error("Unexpected preference write")
    }
}
