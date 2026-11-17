package info.pakkudev.simple_dialer.telecom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.pakkudev.simple_dialer.MainActivity
import info.pakkudev.simple_dialer.R
import info.pakkudev.simple_dialer.domain.model.*
import info.pakkudev.simple_dialer.platform.CallWindowPolicy
import info.pakkudev.simple_dialer.ui.ContactAvatar
import info.pakkudev.simple_dialer.ui.DigitGrid
import info.pakkudev.simple_dialer.ui.PhoneIcons
import info.pakkudev.simple_dialer.ui.IncomingCallControls
import info.pakkudev.simple_dialer.ui.call.CallViewModel
import info.pakkudev.simple_dialer.ui.labelResource
import info.pakkudev.simple_dialer.ui.theme.DialerTheme
import info.pakkudev.simple_dialer.ui.theme.LocalMonochrome
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class CallActivity : ComponentActivity() {
    private val vm: CallViewModel by viewModel()
    private val windowPolicy: CallWindowPolicy by inject()
    private val visibility: CallUiVisibility by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        windowPolicy.apply(this)
        handleAction(intent)
        setContent {
            val settings by vm.preferences.collectAsStateWithLifecycle()
            val callSettings = settings
            DialerTheme(palette = callSettings?.colorPalette ?: ColorPalette.DEFAULT) {
                val calls by vm.calls.collectAsStateWithLifecycle()
                val audio by vm.audio.collectAsStateWithLifecycle()
                val canAdd by vm.canAddCall.collectAsStateWithLifecycle()
                val error by vm.error.collectAsStateWithLifecycle()
                LaunchedEffect(calls.isEmpty()) { if (calls.isEmpty()) finish() }
                val keepScreenOn = calls.any { it.status == CallStatus.RINGING || it.status == CallStatus.DISCONNECTED }
                DisposableEffect(keepScreenOn) {
                    if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                }
                val call = calls.firstOrNull { it.status == CallStatus.RINGING }
                    ?: calls.firstOrNull { it.status == CallStatus.ACTIVE }
                    ?: calls.firstOrNull()
                if (call != null && callSettings != null) CallScreen(call, calls, audio, callSettings, canAdd, error, vm, onClose = { finish() },
                    onAddCall = { startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_DIALPAD, true)) },
                    onMessage = {
                        try { startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", call.number, null))); vm.end(call.id) }
                        catch (_: RuntimeException) { /* Keep the call answerable when no SMS app is installed. */ }
                    })
                else if (call != null) Surface(Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        windowPolicy.apply(this)
        handleAction(intent)
    }
    override fun onStart() { super.onStart(); visibility.entered() }
    override fun onStop() { visibility.left(); super.onStop() }
    private fun handleAction(intent: Intent) {
        if (intent.action == CallNotifications.ACTION_ANSWER) {
            intent.getStringExtra(CallNotifications.EXTRA_CALL_ID)?.let(vm::answer)
            intent.action = null
        }
    }
}

@Composable
private fun CallScreen(call: ActiveCall, calls: List<ActiveCall>, audio: AudioState, settings: DialerPreferences,
                       canAdd: Boolean, error: String?, vm: CallViewModel, onClose: () -> Unit, onAddCall: () -> Unit, onMessage: () -> Unit) {
    var keypad by rememberSaveable(call.id) { mutableStateOf(false) }
    var digits by rememberSaveable(call.id) { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(call.connectedAtMillis) { while (true) { now = System.currentTimeMillis(); delay(1_000) } }
    val name = call.displayName.ifBlank { call.number.ifBlank { stringResource(R.string.unknown_caller) } }
    val ringing = call.status == CallStatus.RINGING
    val ended = call.status == CallStatus.DISCONNECTED
    val showKeypad = keypad && !ringing && !ended && CallControl.KEYPAD !in settings.hiddenControls
    val durationText = if (call.status == CallStatus.ACTIVE && call.connectedAtMillis > 0) {
        val seconds = ((now - call.connectedAtMillis).coerceAtLeast(0) / 1000).toInt()
        "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    } else null
    BackHandler(enabled = showKeypad) { keypad = false }
    val statusText = if (ended && !call.disconnectInfo?.label.isNullOrBlank()) call.disconnectInfo!!.label else stringResource(when (call.status) {
        CallStatus.RINGING -> R.string.incoming_call
        CallStatus.DIALING -> R.string.calling
        CallStatus.CONNECTING -> R.string.connecting_call
        CallStatus.HOLDING -> R.string.held_call
        CallStatus.DISCONNECTING -> R.string.ending_call
        CallStatus.DISCONNECTED -> when (call.disconnectInfo?.reason) {
            CallEndReason.BUSY -> R.string.call_line_busy
            CallEndReason.REJECTED -> R.string.call_declined
            CallEndReason.ERROR -> R.string.call_failed
            CallEndReason.RESTRICTED -> R.string.call_restricted
            else -> R.string.ended_call
        }
        CallStatus.ACTIVE -> R.string.active_call
    })
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight).padding(top = if (showKeypad) 8.dp else 24.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.fillMaxWidth().padding(bottom = if (showKeypad) 8.dp else 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(statusText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (showKeypad && durationText != null) Text(durationText, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (ended) call.disconnectInfo?.description?.takeIf { it.isNotBlank() && it != statusText }?.let {
                            Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (!showKeypad) {
                            Spacer(Modifier.height(32.dp))
                            ContactAvatar(if (call.isConference) stringResource(R.string.conference_call) else name, call.photoUri, size = 112.dp)
                            Spacer(Modifier.height(20.dp))
                        } else Spacer(Modifier.height(8.dp))
                        Text(if (call.isConference) stringResource(R.string.conference_call) else name, style = (if (showKeypad) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge).copy(
                            textDirection = if (!call.isConference && call.displayName.isBlank() && call.number.isNotBlank()) TextDirection.Ltr else TextDirection.Content),
                            maxLines = if (showKeypad) 1 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis)
                        if (!showKeypad && call.displayName.isNotBlank() && call.number.isNotBlank()) Text(call.number, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr))
                        if (!showKeypad && durationText != null) {
                            Text(durationText, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                        calls.filter { it.id != call.id }.forEach { other ->
                            Text("${other.displayName.ifBlank { other.number }} · ${stringResource(if (other.status == CallStatus.HOLDING) R.string.held_call else R.string.active_call)}", Modifier.padding(top = 12.dp))
                        }
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (ringing) {
                            if (call.number.isNotBlank()) TextButton(onClick = onMessage) { Icon(Icons.Default.Message, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.message)) }
                        } else if (showKeypad) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(digits.takeLast(24), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.headlineMedium.copy(textDirection = TextDirection.Ltr))
                                IconButton(onClick = { keypad = false }) {
                                    Icon(Icons.Default.Close, stringResource(R.string.hide_keypad))
                                }
                            }
                            DigitGrid(localToneFeedback = false, compact = true, enabled = call.status == CallStatus.ACTIVE) { digit ->
                                digits += digit; vm.sendDigit(call.id, digit.single())
                            }
                        }
                        error?.let { Text(stringResource(R.string.operation_failed, it), Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            if (!ringing && !ended) {
                InCallControls(call, calls, audio, settings.hiddenControls, canAdd, showKeypad,
                    onKeypad = { keypad = !showKeypad }, onMute = vm::toggleMute, onSpeaker = vm::toggleSpeaker,
                    onAddCall = onAddCall, onHold = { vm.toggleHold(call.id) }, onSwap = { vm.swap(call.id) })
            }
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp), contentAlignment = Alignment.Center) {
                if (ringing) key(call.id, error) {
                    IncomingCallControls(settings.incomingCallStyle, onAnswer = { vm.answer(call.id) }, onDecline = { vm.end(call.id) })
                } else CallActionButton(PhoneIcons.CallEnd, R.string.end_call, Color(0xFFC5221F),
                    enabled = call.status != CallStatus.DISCONNECTING) {
                    if (ended) onClose() else vm.end(call.id)
                }
            }
        }
    }
}

@Composable
private fun InCallControls(call: ActiveCall, calls: List<ActiveCall>, audio: AudioState,
                           hiddenControls: Set<CallControl>, canAdd: Boolean, keypad: Boolean,
                           onKeypad: () -> Unit, onMute: () -> Unit, onSpeaker: () -> Unit,
                           onAddCall: () -> Unit, onHold: () -> Unit, onSwap: () -> Unit) {
    val primary = listOf(CallControl.KEYPAD, CallControl.MUTE, CallControl.SPEAKER).filter { it !in hiddenControls }
    val secondary = listOf(CallControl.ADD_CALL, CallControl.HOLD, CallControl.SWAP).filter {
        it !in hiddenControls && (it != CallControl.SWAP || call.canSwap || calls.size > 1)
    }
    var more by remember(call.id) { mutableStateOf(false) }
    fun enabled(control: CallControl) = call.status != CallStatus.DISCONNECTING && when (control) {
        CallControl.ADD_CALL -> canAdd
        CallControl.HOLD -> call.canHold
        CallControl.SWAP -> call.canSwap || calls.any { it.status == CallStatus.HOLDING }
        CallControl.KEYPAD -> keypad || call.status == CallStatus.ACTIVE
        CallControl.SPEAKER -> if (audio.speakerEnabled) audio.earpieceAvailable || audio.externalDevice else audio.speakerAvailable
        else -> true
    }
    fun action(control: CallControl): () -> Unit = when (control) {
        CallControl.KEYPAD -> onKeypad
        CallControl.MUTE -> onMute
        CallControl.SPEAKER -> onSpeaker
        CallControl.ADD_CALL -> onAddCall
        CallControl.HOLD -> onHold
        CallControl.SWAP -> onSwap
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        primary.forEach { control ->
            val selected = when (control) {
                CallControl.KEYPAD -> keypad
                CallControl.MUTE -> audio.muted
                CallControl.SPEAKER -> audio.speakerEnabled
                else -> false
            }
            CallControlButton(callControlIcon(control, audio, call), stringResource(control.labelResource()), selected, enabled(control),
                Modifier.weight(1f), action(control))
        }
        if (secondary.isNotEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                CallControlButton(Icons.Default.MoreVert, stringResource(R.string.more_options), more,
                    call.status != CallStatus.DISCONNECTING, onClick = { more = !more })
                DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                    secondary.forEach { control ->
                        DropdownMenuItem(
                            text = { Text(stringResource(if (control == CallControl.HOLD && call.status == CallStatus.HOLDING) R.string.resume_call else control.labelResource())) },
                            leadingIcon = { Icon(callControlIcon(control, audio, call), null) }, enabled = enabled(control),
                            onClick = { more = false; action(control)() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun callControlIcon(control: CallControl, audio: AudioState, call: ActiveCall): ImageVector = when (control) {
    CallControl.KEYPAD -> Icons.Default.Dialpad
    CallControl.MUTE -> if (audio.muted) Icons.Default.MicOff else Icons.Default.Mic
    CallControl.SPEAKER -> Icons.Default.VolumeUp
    CallControl.ADD_CALL -> PhoneIcons.AddCall
    CallControl.HOLD -> if (call.status == CallStatus.HOLDING) Icons.Default.PlayArrow else Icons.Default.Pause
    CallControl.SWAP -> Icons.Default.SwapCalls
}

@Composable
private fun CallControlButton(icon: ImageVector, label: String, selected: Boolean, enabled: Boolean,
                              modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val monochrome = LocalMonochrome.current
    val dark = isSystemInDarkTheme()
    val activeContainer = if (monochrome) { if (dark) Color.White else Color.Black } else scheme.primary
    val activeContent = if (monochrome) { if (dark) Color.Black else Color.White } else scheme.onPrimary
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, enabled = enabled,
            modifier = Modifier.size(56.dp).semantics { this.selected = selected },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (selected) activeContainer else scheme.surfaceContainerHigh,
                contentColor = if (selected) activeContent else scheme.onSurface)) {
            Icon(icon, label, Modifier.size(28.dp), tint = when {
                !enabled -> scheme.onSurface.copy(alpha = 0.38f)
                selected -> activeContent
                else -> scheme.onSurface
            })
        }
        Text(label, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
internal fun CallActionButton(icon: ImageVector, label: Int, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(72.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = color, contentColor = Color.White)) {
            Icon(icon, stringResource(label), Modifier.size(32.dp), tint = Color.White)
        }
        Text(stringResource(label), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelLarge)
    }
}
