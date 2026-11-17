package info.pakkudev.simple_dialer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.pakkudev.simple_dialer.domain.model.Contact
import info.pakkudev.simple_dialer.domain.model.DialerPreferences
import info.pakkudev.simple_dialer.domain.model.CallControl
import info.pakkudev.simple_dialer.domain.model.ColorPalette
import info.pakkudev.simple_dialer.domain.model.IncomingCallStyle
import info.pakkudev.simple_dialer.domain.model.RecentCall
import info.pakkudev.simple_dialer.domain.model.latestCallsByNumber
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.domain.repository.DialerReadinessRepository
import info.pakkudev.simple_dialer.domain.repository.ContactsRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.domain.repository.RecentCallsRepository
import info.pakkudev.simple_dialer.domain.usecase.DialNumber
import info.pakkudev.simple_dialer.domain.usecase.DialResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ListFailure { ACCESS, UNAVAILABLE }
data class ListState<T>(val items: List<T> = emptyList(), val loading: Boolean = false, val failure: ListFailure? = null)

sealed interface Feedback {
    data object InvalidNumber : Feedback
    data class Failed(val reason: String) : Feedback
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val settings: PreferencesRepository,
    contactsRepository: ContactsRepository,
    recentCallsRepository: RecentCallsRepository,
    private val dialNumber: DialNumber,
    calls: CallRepository,
    private val access: DialerReadinessRepository,
) : ViewModel() {
    val preferences = settings.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DialerPreferences())
    val activeCalls = calls.calls
    val readiness = access.readiness
    private val contactsRetry = MutableStateFlow(0)
    private val historyRetry = MutableStateFlow(0)
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val feedbackChannel = Channel<Feedback>(Channel.BUFFERED)
    val feedback = feedbackChannel.receiveAsFlow()

    val contacts: StateFlow<ListState<Contact>> = readiness.map { it.canReadContacts }.distinctUntilChanged().combine(contactsRetry) { allowed, _ -> allowed }.flatMapLatest { allowed ->
        if (!allowed) flowOf(ListState<Contact>()) else contactsRepository.observeContacts()
            .map { ListState(items = it) }.onStart { emit(ListState(items = contacts.value.items, loading = true)) }
            .catch { error ->
                access.refresh()
                emit(ListState(failure = if (error is SecurityException) ListFailure.ACCESS else ListFailure.UNAVAILABLE))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListState())

    val recents = readiness.map { it.canReadHistory }.distinctUntilChanged().combine(historyRetry) { allowed, _ -> allowed }.flatMapLatest { allowed ->
        if (!allowed) flowOf(ListState<RecentCall>()) else recentCallsRepository.observeRecentCalls()
            .map { ListState(items = latestCallsByNumber(it)) }.onStart { emit(ListState(loading = true)) }
            .catch { error ->
                access.refresh()
                emit(ListState(failure = if (error is SecurityException) ListFailure.ACCESS else ListFailure.UNAVAILABLE))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListState())

    fun refreshContacts() { access.refresh(); contactsRetry.value++ }
    fun retryContacts() = refreshContacts()
    fun retryHistory() { access.refresh(); historyRetry.value++ }

    fun dial(number: String) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        viewModelScope.launch {
            try {
                when (val result = dialNumber(number)) {
                    DialResult.InvalidNumber -> feedbackChannel.send(Feedback.InvalidNumber)
                    DialResult.CallStarted -> Unit
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                access.refresh()
                feedbackChannel.send(Feedback.Failed(error.message.orEmpty()))
            } finally { mutableBusy.value = false }
        }
    }

    fun reportFailure(reason: String) { viewModelScope.launch { feedbackChannel.send(Feedback.Failed(reason)) } }
    fun setShowContacts(value: Boolean) = save { settings.setShowContacts(value) }
    fun setShowRecents(value: Boolean) = save { settings.setShowRecents(value) }
    fun setColorPalette(value: ColorPalette) = save { settings.setColorPalette(value) }
    fun setIncomingCallStyle(value: IncomingCallStyle) = save { settings.setIncomingCallStyle(value) }
    fun setShowFavorites(value: Boolean) = save { settings.setShowFavorites(value) }
    fun setShowContactDetails(value: Boolean) = save { settings.setShowContactDetails(value) }
    fun setAutoAnswer(value: Boolean) = save { settings.setAutoAnswer(value) }
    fun setAutoSpeaker(value: Boolean) = save { settings.setAutoSpeaker(value) }
    fun setFavorite(number: String, favorite: Boolean) = save { settings.setFavorite(number, favorite) }
    fun setControlVisible(control: CallControl, visible: Boolean) = save { settings.setControlVisible(control, visible) }

    private fun save(action: suspend () -> Unit) { viewModelScope.launch {
        try { action() } catch (error: Exception) {
            if (error is CancellationException) throw error
            feedbackChannel.send(Feedback.Failed(error.message.orEmpty()))
        }
    } }
}
