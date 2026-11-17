package info.pakkudev.simple_dialer.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.os.SystemClock
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.domain.repository.ContactsRepository
import info.pakkudev.simple_dialer.domain.repository.DialerReadinessRepository
import info.pakkudev.simple_dialer.domain.model.DialerPreferences
import info.pakkudev.simple_dialer.domain.model.CallStatus
import info.pakkudev.simple_dialer.domain.usecase.CallScreenPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CallViewModel(private val repository: CallRepository, settings: PreferencesRepository,
                    contacts: ContactsRepository, access: DialerReadinessRepository) : ViewModel() {
    init { access.refresh() }
    private val presentation = CallScreenPresentation()
    private val presentedCalls = MutableStateFlow(repository.calls.value)
    init {
        viewModelScope.launch {
            combine(repository.calls, repository.lastDisconnectedCall) { current, last -> current to last }.collectLatest { (current, last) ->
                val result = presentation.update(current, SystemClock.elapsedRealtime(), last)
                presentedCalls.value = result.calls
                result.closeAfterMillis?.let { remaining ->
                    delay(remaining)
                    presentedCalls.value = presentation.update(emptyList(), SystemClock.elapsedRealtime(), last).calls
                }
            }
        }
    }
    private val people = access.readiness.map { it.canReadContacts }.distinctUntilChanged().flatMapLatest { allowed ->
        if (allowed) contacts.observeContacts().catch { emit(emptyList()) } else flowOf(emptyList())
    }
    val calls = combine(presentedCalls, people) { active, contacts -> active.map { call ->
        val digits = call.number.filter(Char::isDigit)
        val person = contacts.firstOrNull { person ->
            val other = person.number.filter(Char::isDigit)
            digits.isNotBlank() && (digits == other || (digits.length >= 10 && other.length >= 10 && digits.takeLast(10) == other.takeLast(10)))
        }
        if (person != null) call.copy(displayName = person.name, photoUri = person.photoUri) else call
    } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), presentedCalls.value)
    val audio = repository.audio
    val preferences: StateFlow<DialerPreferences?> = settings.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val canAddCall = repository.canAddCall
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    fun answer(id: String) = perform { repository.answer(id) }
    fun end(id: String) = perform {
        presentation.localEndRequested(id)
        try { repository.end(id) } catch (error: Exception) { presentation.localEndFailed(id); throw error }
    }
    fun toggleMute() = perform { repository.setMuted(!audio.value.muted) }
    fun toggleSpeaker() = perform { repository.setSpeakerEnabled(!audio.value.speakerEnabled) }
    fun toggleHold(id: String) = perform { repository.setHeld(id, calls.value.firstOrNull { it.id == id }?.status != CallStatus.HOLDING) }
    fun swap(id: String) = perform {
        val held = calls.value.firstOrNull { it.status == CallStatus.HOLDING }
        if (held != null) repository.setHeld(held.id, false) else repository.swap(id)
    }
    fun sendDigit(id: String, digit: Char) = perform {
        repository.playDtmf(id, digit)
        try { delay(180) } finally { repository.stopDtmf(id) }
    }
    private fun perform(action: suspend () -> Unit) { viewModelScope.launch {
        mutableError.value = null
        try { action() } catch (error: Exception) {
            if (error is CancellationException) throw error
            mutableError.value = error.message.orEmpty()
        }
    } }
}
