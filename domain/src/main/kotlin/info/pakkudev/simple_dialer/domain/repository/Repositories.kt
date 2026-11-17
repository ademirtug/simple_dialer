package info.pakkudev.simple_dialer.domain.repository

import info.pakkudev.simple_dialer.domain.model.ActiveCall
import info.pakkudev.simple_dialer.domain.model.AudioState
import info.pakkudev.simple_dialer.domain.model.Contact
import info.pakkudev.simple_dialer.domain.model.DialerPreferences
import info.pakkudev.simple_dialer.domain.model.CallControl
import info.pakkudev.simple_dialer.domain.model.RecentCall
import info.pakkudev.simple_dialer.domain.model.ColorPalette
import info.pakkudev.simple_dialer.domain.model.IncomingCallStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ContactsRepository {
    /** Collect only after READ_CONTACTS has been granted; provider changes are observed. */
    fun observeContacts(): Flow<List<Contact>>
}

interface RecentCallsRepository {
    /** Collect only after READ_CALL_LOG has been granted. */
    fun observeRecentCalls(): Flow<List<RecentCall>>
}

interface PreferencesRepository {
    val preferences: Flow<DialerPreferences>
    suspend fun setShowContacts(enabled: Boolean)
    suspend fun setShowRecents(enabled: Boolean)
    suspend fun setShowFavorites(enabled: Boolean)
    suspend fun setShowContactDetails(enabled: Boolean)
    suspend fun setAutoAnswer(enabled: Boolean)
    suspend fun setAutoSpeaker(enabled: Boolean)
    suspend fun setFavorite(number: String, favorite: Boolean)
    suspend fun setControlVisible(control: CallControl, visible: Boolean)
    suspend fun setColorPalette(palette: ColorPalette)
    suspend fun setIncomingCallStyle(style: IncomingCallStyle)
}

interface CallLauncher {
    suspend fun placeCall(number: String)
}

interface CallRepository {
    val calls: StateFlow<List<ActiveCall>>
    val lastDisconnectedCall: StateFlow<ActiveCall?>
    val audio: StateFlow<AudioState>
    val canAddCall: StateFlow<Boolean>
    fun answer(id: String)
    fun end(id: String)
    fun setMuted(muted: Boolean)
    suspend fun setSpeakerEnabled(enabled: Boolean)
    fun setHeld(id: String, held: Boolean)
    fun merge(id: String)
    fun swap(id: String)
    fun playDtmf(id: String, digit: Char)
    fun stopDtmf(id: String)
}
