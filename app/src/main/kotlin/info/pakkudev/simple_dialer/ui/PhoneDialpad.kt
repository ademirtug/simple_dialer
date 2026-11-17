package info.pakkudev.simple_dialer.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.pakkudev.simple_dialer.R
import info.pakkudev.simple_dialer.ui.theme.LocalMonochrome
import info.pakkudev.simple_dialer.platform.DialpadTonePlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ContactAvatar(name: String, uri: String? = null, size: Dp = 44.dp) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            try { resolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }
            catch (_: Exception) { null }
        }
    }
    Surface(Modifier.size(size), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        val image = bitmap
        if (image != null) Image(image, null, Modifier.clip(CircleShape), contentScale = ContentScale.Crop)
        else Box(contentAlignment = Alignment.Center) { Text(name.trim().take(1).uppercase().ifBlank { "?" }, style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
fun callAccentColor(): Color = if (LocalMonochrome.current) Color(0xFF188038) else MaterialTheme.colorScheme.primary

@Composable
fun PhoneDialpad(number: String, onNumber: (String) -> Unit, onCall: () -> Unit, onCreateContact: () -> Unit, busy: Boolean = false) {
    val numberLabel = stringResource(R.string.phone_number)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(number, { if (it.length <= 60) onNumber(it) }, Modifier.weight(1f).padding(vertical = 16.dp).semantics { contentDescription = numberLabel },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 30.sp, textAlign = TextAlign.Center, textDirection = TextDirection.Ltr),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            IconButton(onClick = { onNumber(number.dropLast(1)) }, enabled = number.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Backspace, stringResource(R.string.delete_digit)) }
        }
        DigitGrid(allowPlus = true) { if (number.length < 40) onNumber(number + it) }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCreateContact, enabled = number.isNotBlank()) { Icon(Icons.Default.PersonAdd, stringResource(R.string.create_contact)) }
            FilledIconButton(onClick = onCall, enabled = number.isNotBlank() && !busy, modifier = Modifier.width(120.dp).height(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = callAccentColor(),
                    contentColor = if (LocalMonochrome.current) Color.White else MaterialTheme.colorScheme.onPrimary)) { Icon(PhoneIcons.Call, stringResource(R.string.call), Modifier.size(28.dp)) }
            Spacer(Modifier.size(48.dp))
        }
    }
    }
}

@Composable
fun DigitGrid(allowPlus: Boolean = false, localToneFeedback: Boolean = true, compact: Boolean = false,
              enabled: Boolean = true, onDigit: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val tones = if (localToneFeedback) remember(context) { DialpadTonePlayer(context) } else null
    DisposableEffect(tones, owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) tones?.release() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); tones?.release() }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    val letters = mapOf("2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL", "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "0" to "+")
    listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("*", "0", "#")).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            row.forEach { digit ->
                Box(modifier = Modifier.weight(1f).heightIn(min = if (compact) 56.dp else 68.dp).clip(CircleShape).combinedClickable(
                    enabled = enabled, role = Role.Button, onClick = { tones?.play(digit.single()); onDigit(digit) },
                    onLongClick = if (allowPlus && digit == "0") ({ tones?.play('0'); onDigit("+") }) else null,
                ), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(digit, fontSize = 30.sp, lineHeight = if (compact) 32.sp else TextUnit.Unspecified, color = MaterialTheme.colorScheme.onSurface)
                        Text(letters[digit].orEmpty(), fontSize = 10.sp, lineHeight = if (compact) 14.sp else TextUnit.Unspecified,
                            letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    }
}
