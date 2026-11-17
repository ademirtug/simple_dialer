package info.pakkudev.simple_dialer.di

import info.pakkudev.simple_dialer.data.call.AndroidCallRepository
import info.pakkudev.simple_dialer.data.call.AndroidProximityMonitor
import info.pakkudev.simple_dialer.data.call.ProximityMonitor
import info.pakkudev.simple_dialer.data.call.AndroidCallScreenLock
import info.pakkudev.simple_dialer.data.call.CallScreenLock
import info.pakkudev.simple_dialer.data.call.CallAssistance
import info.pakkudev.simple_dialer.data.preferences.DataStorePreferencesRepository
import info.pakkudev.simple_dialer.data.provider.AndroidContactsRepository
import info.pakkudev.simple_dialer.data.provider.AndroidRecentCallsRepository
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.domain.repository.CallLauncher
import info.pakkudev.simple_dialer.domain.repository.DialerReadinessRepository
import info.pakkudev.simple_dialer.platform.AndroidDialerReadinessRepository
import info.pakkudev.simple_dialer.platform.LauncherIcons
import info.pakkudev.simple_dialer.domain.repository.ContactsRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.domain.repository.RecentCallsRepository
import info.pakkudev.simple_dialer.domain.usecase.DialNumber
import info.pakkudev.simple_dialer.telecom.CallNotifications
import info.pakkudev.simple_dialer.telecom.CallUiVisibility
import info.pakkudev.simple_dialer.ui.MainViewModel
import info.pakkudev.simple_dialer.ui.call.CallViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<DialerReadinessRepository> { AndroidDialerReadinessRepository(get(), get(), get(), get(), get(), get()) }
    single<PreferencesRepository> { DataStorePreferencesRepository(androidContext()) }
    single { LauncherIcons(androidContext(), get()) }
    single<ContactsRepository> { AndroidContactsRepository(androidContext(), Dispatchers.IO) }
    single<RecentCallsRepository> { AndroidRecentCallsRepository(androidContext(), Dispatchers.IO) }
    single { AndroidCallRepository(get(), get(), Dispatchers.Main.immediate) }
    single<CallRepository> { get<AndroidCallRepository>() }
    single<CallLauncher> { get<AndroidCallRepository>() }
    single<ProximityMonitor> { AndroidProximityMonitor(androidContext()) }
    factory<CallScreenLock> { AndroidCallScreenLock(androidContext()) }
    factory { CallAssistance(get(), get(), get(), get()) }
    factory { DialNumber(get()) }
    factory { CallNotifications(androidContext(), get()) }
    single { CallUiVisibility() }
    viewModel { MainViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { CallViewModel(get(), get(), get(), get()) }
}
