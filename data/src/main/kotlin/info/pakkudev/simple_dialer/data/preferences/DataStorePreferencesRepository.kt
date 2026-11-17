package info.pakkudev.simple_dialer.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import info.pakkudev.simple_dialer.domain.model.DialerPreferences
import info.pakkudev.simple_dialer.domain.model.CallControl
import info.pakkudev.simple_dialer.domain.model.ColorPalette
import info.pakkudev.simple_dialer.domain.model.IncomingCallStyle
import info.pakkudev.simple_dialer.domain.model.normalizedFavorites
import info.pakkudev.simple_dialer.domain.model.withFavorite
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dialerDataStore by preferencesDataStore(name = "dialer_preferences")

class DataStorePreferencesRepository(context: Context) : PreferencesRepository {
    private val store = context.applicationContext.dialerDataStore

    override val preferences = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { values ->
        DialerPreferences(
            showRecents = values[RECENTS] ?: true,
            showFavorites = values[FAVORITES] ?: false,
            showContacts = values[CONTACTS] ?: true,
            showContactDetails = values[CONTACT_DETAILS] ?: true,
            autoAnswer = values[AUTO_ANSWER] ?: false,
            autoSpeaker = values[AUTO_SPEAKER] ?: false,
            favoriteNumbers = values[FAVORITE_NUMBERS] ?: emptySet(),
            excludedFavorites = values[EXCLUDED_FAVORITES] ?: emptySet(),
            hiddenControls = (values[HIDDEN_CONTROLS] ?: DEFAULT_HIDDEN_CONTROLS).mapNotNull { name -> CallControl.entries.firstOrNull { it.name == name } }.toSet(),
            colorPalette = ColorPalette.fromStored(values[COLOR_PALETTE]),
            incomingCallStyle = IncomingCallStyle.fromStored(values[INCOMING_CALL_STYLE]),
        ).normalizedFavorites()
    }

    override suspend fun setShowContacts(enabled: Boolean) { store.edit { it[CONTACTS] = enabled } }
    override suspend fun setShowRecents(enabled: Boolean) { store.edit { it[RECENTS] = enabled } }
    override suspend fun setShowFavorites(enabled: Boolean) { store.edit { it[FAVORITES] = enabled } }
    override suspend fun setShowContactDetails(enabled: Boolean) { store.edit { it[CONTACT_DETAILS] = enabled } }
    override suspend fun setAutoAnswer(enabled: Boolean) { store.edit { it[AUTO_ANSWER] = enabled } }
    override suspend fun setAutoSpeaker(enabled: Boolean) { store.edit { it[AUTO_SPEAKER] = enabled } }
    override suspend fun setColorPalette(palette: ColorPalette) { store.edit { it[COLOR_PALETTE] = palette.name } }
    override suspend fun setIncomingCallStyle(style: IncomingCallStyle) { store.edit { it[INCOMING_CALL_STYLE] = style.name } }
    override suspend fun setFavorite(number: String, favorite: Boolean) { store.edit {
        val updated = DialerPreferences(
            favoriteNumbers = it[FAVORITE_NUMBERS].orEmpty(),
            excludedFavorites = it[EXCLUDED_FAVORITES].orEmpty(),
        ).withFavorite(number, favorite)
        it[FAVORITE_NUMBERS] = updated.favoriteNumbers
        it[EXCLUDED_FAVORITES] = updated.excludedFavorites
    } }
    override suspend fun setControlVisible(control: CallControl, visible: Boolean) { store.edit {
        val hidden = it[HIDDEN_CONTROLS] ?: DEFAULT_HIDDEN_CONTROLS
        it[HIDDEN_CONTROLS] = if (visible) hidden - control.name else hidden + control.name
    } }

    private companion object {
        val DEFAULT_HIDDEN_CONTROLS = DialerPreferences().hiddenControls.map { it.name }.toSet()
        val CONTACTS = booleanPreferencesKey("show_contacts")
        val RECENTS = booleanPreferencesKey("show_recents")
        val FAVORITES = booleanPreferencesKey("show_favorites")
        val CONTACT_DETAILS = booleanPreferencesKey("show_contact_details")
        val AUTO_ANSWER = booleanPreferencesKey("auto_answer")
        val AUTO_SPEAKER = booleanPreferencesKey("auto_speaker")
        val FAVORITE_NUMBERS = stringSetPreferencesKey("favorite_numbers")
        val EXCLUDED_FAVORITES = stringSetPreferencesKey("excluded_favorites")
        val HIDDEN_CONTROLS = stringSetPreferencesKey("hidden_controls")
        val COLOR_PALETTE = stringPreferencesKey("color_palette")
        val INCOMING_CALL_STYLE = stringPreferencesKey("incoming_call_style")
    }
}
