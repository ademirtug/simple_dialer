package info.pakkudev.simple_dialer.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import info.pakkudev.simple_dialer.R
import info.pakkudev.simple_dialer.domain.model.IncomingCallStyle
import info.pakkudev.simple_dialer.telecom.CallActionButton
import kotlin.math.abs
import kotlin.math.roundToInt

private val AnswerGreen = Color(0xFF188038)
private val DeclineRed = Color(0xFFC5221F)

fun IncomingCallStyle.labelResource() = when (this) {
    IncomingCallStyle.BUTTONS -> R.string.incoming_style_buttons
    IncomingCallStyle.VERTICAL_SWIPE -> R.string.incoming_style_vertical
    IncomingCallStyle.HORIZONTAL_SLIDE -> R.string.incoming_style_horizontal
}

fun IncomingCallStyle.hintResource() = when (this) {
    IncomingCallStyle.BUTTONS -> R.string.incoming_buttons_hint
    IncomingCallStyle.VERTICAL_SWIPE -> R.string.incoming_vertical_hint
    IncomingCallStyle.HORIZONTAL_SLIDE -> R.string.incoming_horizontal_hint
}

/** Key this control by call ID so an unfinished drag can never carry into another call. */
@Composable
fun IncomingCallControls(style: IncomingCallStyle, onAnswer: () -> Unit, onDecline: () -> Unit, modifier: Modifier = Modifier) {
    val answerLabel = stringResource(R.string.answer)
    val declineLabel = stringResource(R.string.decline)
    val controlsLabel = stringResource(R.string.incoming_call_style)
    val hint = stringResource(style.hintResource())
    val currentAnswer by rememberUpdatedState(onAnswer)
    val currentDecline by rememberUpdatedState(onDecline)
    val haptics = LocalHapticFeedback.current
    var committed by remember(style) { mutableStateOf(false) }
    val complete: (Boolean) -> Boolean = { answer ->
        if (committed) false else {
            committed = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (answer) currentAnswer() else currentDecline()
            true
        }
    }
    val currentComplete by rememberUpdatedState(complete)

    if (style == IncomingCallStyle.BUTTONS) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            CallActionButton(PhoneIcons.CallEnd, R.string.decline, DeclineRed, enabled = !committed) { complete(false) }
            CallActionButton(PhoneIcons.Call, R.string.answer, AnswerGreen, enabled = !committed) { complete(true) }
        }
        return
    }

    val horizontal = style == IncomingCallStyle.HORIZONTAL_SLIDE
    val density = LocalDensity.current
    val verticalHeight = if (LocalConfiguration.current.screenHeightDp < 600) 160.dp else 208.dp
    var dragOffset by remember(style) { mutableFloatStateOf(0f) }
    var dragging by remember(style) { mutableStateOf(false) }
    val visualOffset by animateFloatAsState(dragOffset,
        animationSpec = if (dragging) snap() else spring(stiffness = Spring.StiffnessMediumLow), label = "incoming call handle")

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Directions describe physical movement, including in right-to-left locales.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(if (horizontal) Modifier.widthIn(max = 360.dp).fillMaxWidth().height(88.dp)
                else Modifier.width(96.dp).height(verticalHeight)) {
                val travel = with(density) { ((if (horizontal) maxWidth else maxHeight) / 2 - 44.dp).toPx() }.coerceAtLeast(1f)
                Surface(Modifier.fillMaxSize().testTag("incoming_call_track"), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    if (horizontal) Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChevronLeft, null, Modifier.size(28.dp), tint = DeclineRed)
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(28.dp), tint = AnswerGreen)
                    } else Column(Modifier.fillMaxSize().padding(vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(28.dp), tint = AnswerGreen)
                        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(28.dp), tint = DeclineRed)
                    }
                }
                Surface(Modifier.align(Alignment.Center)
                    .offset { IntOffset(if (horizontal) visualOffset.roundToInt() else 0, if (horizontal) 0 else visualOffset.roundToInt()) }
                    .size(72.dp).testTag("incoming_call_handle")
                    .semantics {
                        contentDescription = controlsLabel
                        stateDescription = hint
                        role = Role.Button
                        if (committed) disabled()
                        customActions = listOf(
                            CustomAccessibilityAction(answerLabel) { currentComplete(true) },
                            CustomAccessibilityAction(declineLabel) { currentComplete(false) },
                        )
                    }
                    .pointerInput(style, travel) {
                        detectDragGestures(
                            onDragStart = { if (!committed) dragging = true },
                            onDragCancel = { dragging = false; dragOffset = 0f },
                            onDragEnd = {
                                dragging = false
                                if (!committed && abs(dragOffset) >= travel * 0.75f) {
                                    currentComplete(if (horizontal) dragOffset > 0 else dragOffset < 0)
                                } else if (!committed) dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                if (!committed) {
                                    change.consume()
                                    dragOffset = (dragOffset + if (horizontal) amount.x else amount.y).coerceIn(-travel, travel)
                                }
                            },
                        )
                    }, shape = CircleShape, color = AnswerGreen, contentColor = Color.White) {
                    Box(contentAlignment = Alignment.Center) { Icon(PhoneIcons.Call, null, Modifier.size(32.dp)) }
                }
            }
        }
        Text(hint, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
