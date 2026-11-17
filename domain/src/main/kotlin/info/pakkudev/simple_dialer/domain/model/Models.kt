package info.pakkudev.simple_dialer.domain.model

data class Contact(
    val id: String,
    val name: String,
    val number: String,
    val photoUri: String? = null,
    val starred: Boolean = false,
    val contactId: String = id,
    val lookupUri: String? = null,
)

enum class CallDirection { INCOMING, OUTGOING, MISSED, OTHER }

data class RecentCall(
    val id: Long,
    val name: String,
    val number: String,
    val timestampMillis: Long,
    val direction: CallDirection,
)

enum class CallControl { MUTE, KEYPAD, SPEAKER, ADD_CALL, HOLD, SWAP }

data class DialerPreferences(
    val showRecents: Boolean = true,
    val showFavorites: Boolean = false,
    val autoAnswer: Boolean = false,
    val autoSpeaker: Boolean = false,
    val favoriteNumbers: Set<String> = emptySet(),
    val excludedFavorites: Set<String> = emptySet(),
    val hiddenControls: Set<CallControl> = setOf(CallControl.ADD_CALL, CallControl.HOLD, CallControl.SWAP),
    val colorPalette: ColorPalette = ColorPalette.DEFAULT,
    val showContactDetails: Boolean = true,
    val showContacts: Boolean = true,
    val incomingCallStyle: IncomingCallStyle = IncomingCallStyle.DEFAULT,
)

enum class CallStatus { CONNECTING, DIALING, RINGING, ACTIVE, HOLDING, DISCONNECTING, DISCONNECTED }

data class ActiveCall(
    val id: String,
    val number: String,
    val displayName: String,
    val status: CallStatus,
    val connectedAtMillis: Long = 0,
    val canHold: Boolean = false,
    val canMerge: Boolean = false,
    val canSwap: Boolean = false,
    val isConference: Boolean = false,
    val photoUri: String? = null,
    val disconnectInfo: CallDisconnectInfo? = null,
)

data class AudioState(val muted: Boolean = false, val speakerEnabled: Boolean = false, val externalDevice: Boolean = false,
                      val availableOutputs: Int = 0, val earpieceAvailable: Boolean = true, val speakerAvailable: Boolean = true)
