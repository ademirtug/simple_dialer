package info.pakkudev.simple_dialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.pakkudev.simple_dialer.R
import info.pakkudev.simple_dialer.domain.model.*
import java.text.DateFormat
import java.util.Date

enum class PhoneTab(val label: Int, val icon: ImageVector) {
    FAVORITES(R.string.favorites, Icons.Default.Star),
    RECENTS(R.string.recents, Icons.Default.History),
    CONTACTS(R.string.contacts, Icons.Default.Contacts),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialerApp(
    viewModel: MainViewModel,
    externalNumber: String?,
    openDialpadRequest: Int,
    proximityAvailable: Boolean,
    onDial: (String) -> Unit,
    onRequestAccess: (DialerCapability) -> Unit,
    onSystemPhoneSettings: () -> Unit,
    onReturnToCall: () -> Unit,
    onCreateContact: (String) -> Unit,
    onEditContact: (String) -> Unit,
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val access by viewModel.readiness.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val calls by viewModel.activeCalls.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var requestedTab by rememberSaveable { mutableStateOf(PhoneTab.CONTACTS) }
    var admin by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var overflow by remember { mutableStateOf(false) }
    var dialpad by rememberSaveable { mutableStateOf(false) }
    var number by rememberSaveable { mutableStateOf("") }
    var detail by remember { mutableStateOf<Contact?>(null) }
    LaunchedEffect(preferences.showContactDetails) { if (!preferences.showContactDetails) detail = null }
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val tabs = buildList {
        if (preferences.showFavorites) add(PhoneTab.FAVORITES)
        if (preferences.showRecents) add(PhoneTab.RECENTS)
        if (preferences.showContacts) add(PhoneTab.CONTACTS)
    }
    val selected = requestedTab.takeIf { it in tabs }
        ?: tabs.firstOrNull { it == PhoneTab.CONTACTS }
        ?: tabs.firstOrNull()
    LaunchedEffect(selected) { selected?.let { requestedTab = it } }
    LaunchedEffect(externalNumber, openDialpadRequest) {
        if (externalNumber != null || openDialpadRequest > 0) { externalNumber?.let { number = it }; dialpad = true }
    }
    LaunchedEffect(viewModel, resources) {
        viewModel.feedback.collect { message ->
            snackbar.showSnackbar(when (message) {
                Feedback.InvalidNumber -> resources.getString(R.string.invalid_number)
                is Feedback.Failed -> resources.getString(R.string.operation_failed, message.reason)
            })
        }
    }
    BackHandler(admin) { admin = false }
    if (admin) {
        AdminScreen(preferences, access, proximityAvailable, viewModel, { admin = false }, onRequestAccess, onSystemPhoneSettings)
        return
    }
    val visibleContacts = contacts.items.filter { it.name.contains(query, true) || it.number.contains(query) }
    val favorites = favoriteContacts(contacts.items, preferences)
    val contactsScrollState = rememberLazyListState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                Row(Modifier.statusBarsPadding().fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selected == PhoneTab.CONTACTS) OutlinedTextField(query, { query = it }, Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_contacts)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, stringResource(R.string.clear_search)) } },
                        shape = CircleShape, singleLine = true)
                    else Text(stringResource(selected?.label ?: R.string.app_name), Modifier.weight(1f).padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.headlineSmall)
                    Box {
                        IconButton(onClick = { overflow = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }
                        DropdownMenu(overflow, { overflow = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.admin_settings)) }, onClick = { overflow = false; admin = true }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                        }
                    }
                }
                if (calls.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth().clickable(onClick = onReturnToCall)) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(PhoneIcons.Call, null); Text(stringResource(R.string.return_to_call))
                    }
                }
            }
        },
        bottomBar = {
            if (tabs.size > 1) NavigationBar {
                tabs.forEach { tab -> NavigationBarItem(selected == tab, { requestedTab = tab },
                    icon = { Icon(tab.icon, null) }, label = { Text(stringResource(tab.label)) }) }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialpad = true }, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Dialpad, stringResource(R.string.open_dialpad))
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                null -> EmptyPhoneList(Icons.Default.Dialpad, R.string.keypad)
                PhoneTab.CONTACTS -> when {
                    !access.canReadContacts -> AccessEmptyState(Icons.Default.Contacts, R.string.contacts_permission_description, R.string.grant_contacts) { onRequestAccess(DialerCapability.CONTACTS) }
                    else -> PullToRefreshBox(
                        isRefreshing = contacts.loading,
                        onRefresh = viewModel::refreshContacts,
                        modifier = Modifier.fillMaxSize().testTag("contacts_refresh"),
                    ) {
                        when {
                            contacts.failure != null -> ScrollableContactsPlaceholder {
                                AccessEmptyState(Icons.Default.ErrorOutline, R.string.data_load_failed, R.string.try_again, viewModel::retryContacts)
                            }
                            visibleContacts.isEmpty() -> ScrollableContactsPlaceholder {
                                if (!contacts.loading) EmptyPhoneList(Icons.Default.Contacts, if (query.isEmpty()) R.string.no_contacts else R.string.no_results)
                            }
                            else -> LazyColumn(state = contactsScrollState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                                items(visibleContacts, key = { it.id }) { contact ->
                                    ListItem(headlineContent = { Text(contact.name) },
                                        leadingContent = { ContactAvatar(contact.name, contact.photoUri) },
                                        trailingContent = { IconButton(onClick = { onDial(contact.number) }) { Icon(PhoneIcons.Call, stringResource(R.string.call_person, contact.name), tint = callAccentColor()) } },
                                        modifier = if (preferences.showContactDetails) Modifier.clickable { detail = contact } else Modifier)
                                }
                            }
                        }
                    }
                }
                PhoneTab.FAVORITES -> {
                    if (favorites.isEmpty()) {
                        if (preferences.showContacts) AccessEmptyState(Icons.Default.StarOutline, R.string.no_favorites, R.string.add_favorite) { requestedTab = PhoneTab.CONTACTS }
                        else EmptyPhoneList(Icons.Default.StarOutline, R.string.no_favorites)
                    }
                    else LazyVerticalGrid(GridCells.Adaptive(150.dp), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favorites, key = { phoneNumberKey(it.number) }) { contact ->
                            Card(onClick = { onDial(contact.number) }) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    ContactAvatar(contact.name, contact.photoUri, 80.dp)
                                    Spacer(Modifier.height(12.dp))
                                    Text(contact.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    if (preferences.showContactDetails) IconButton(onClick = { detail = contact }) {
                                        Icon(Icons.Default.MoreHoriz, stringResource(R.string.contact_details))
                                    }
                                }
                            }
                        }
                    }
                }
                PhoneTab.RECENTS -> when {
                    !access.canReadHistory -> AccessEmptyState(Icons.Default.History, R.string.history_permission_description,
                        if (access.defaultPhoneApp != AccessState.GRANTED) R.string.make_default else R.string.grant_history) {
                        onRequestAccess(if (access.defaultPhoneApp != AccessState.GRANTED) DialerCapability.DEFAULT_PHONE_APP else DialerCapability.HISTORY)
                    }
                    recents.failure != null -> AccessEmptyState(Icons.Default.ErrorOutline, R.string.data_load_failed, R.string.try_again, viewModel::retryHistory)
                    recents.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    else -> {
                        if (recents.items.isEmpty()) EmptyPhoneList(Icons.Default.History, R.string.no_history)
                        else LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                            items(recents.items, key = { it.id }) { call ->
                                val contact = contacts.items.firstOrNull { phoneNumberKey(it.number) == phoneNumberKey(call.number) }
                                val name = contact?.name ?: call.name.ifBlank { call.number.ifBlank { resources.getString(R.string.unknown_caller) } }
                                ListItem(headlineContent = { Text(name, color = if (call.direction == CallDirection.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                                    supportingContent = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(when (call.direction) { CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived; CallDirection.MISSED -> Icons.AutoMirrored.Filled.CallMissed; else -> Icons.AutoMirrored.Filled.CallMade }, null, Modifier.size(14.dp))
                                        Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(call.timestampMillis)))
                                    } }, leadingContent = { ContactAvatar(name, contact?.photoUri) },
                                    trailingContent = { IconButton(onClick = { onDial(call.number) }, enabled = call.number.isNotBlank()) { Icon(PhoneIcons.Call, stringResource(R.string.call_person, name), tint = if (call.number.isNotBlank()) callAccentColor() else LocalContentColor.current) } },
                                    modifier = if (preferences.showContactDetails) Modifier.clickable { detail = contact ?: Contact("recent:${call.id}", name, call.number) } else Modifier)
                            }
                        }
                    }
                }
            }
        }
    }
    if (dialpad) ModalBottomSheet(onDismissRequest = { dialpad = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        PhoneDialpad(number, { number = it }, { if (!busy) { dialpad = false; onDial(number) } }, { onCreateContact(number) }, busy)
    }
    detail?.takeIf { preferences.showContactDetails }?.let { contact ->
        ModalBottomSheet(onDismissRequest = { detail = null }) {
            Column(Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ContactAvatar(contact.name, contact.photoUri, 80.dp)
                Text(contact.name, style = MaterialTheme.typography.headlineSmall)
                Text(contact.number, style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    FilledTonalIconButton(onClick = { detail = null; onDial(contact.number) }, modifier = Modifier.size(56.dp)) { Icon(PhoneIcons.Call, stringResource(R.string.call), tint = callAccentColor()) }
                    IconToggleButton(checked = isFavorite(contact, preferences), onCheckedChange = { viewModel.setFavorite(contact.number, it) }, modifier = Modifier.size(56.dp)) {
                        Icon(if (isFavorite(contact, preferences)) Icons.Default.Star else Icons.Default.StarOutline,
                            stringResource(if (isFavorite(contact, preferences)) R.string.remove_favorite else R.string.add_favorite))
                    }
                }
                contact.lookupUri?.let { lookupUri ->
                    OutlinedButton(onClick = { detail = null; onEditContact(lookupUri) }) {
                        Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.edit_contact))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ScrollableContactsPlaceholder(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(maxHeight)) { content() }
    }
}

@Composable
private fun EmptyPhoneList(icon: ImageVector, text: Int) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp)); Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AccessEmptyState(icon: ImageVector, description: Int, action: Int, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp)); Text(stringResource(description), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp)); TextButton(onClick = onAction) { Text(stringResource(action)) }
    }
}
