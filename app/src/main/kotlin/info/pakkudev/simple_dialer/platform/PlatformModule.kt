package info.pakkudev.simple_dialer.platform

import info.pakkudev.simple_dialer.data.platform.AudioRouteSpeakerRouting
import info.pakkudev.simple_dialer.data.platform.EndpointSpeakerRouting
import info.pakkudev.simple_dialer.data.platform.SpeakerRouting
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** The composition root owns version selection. Consumers depend only on interfaces. */
@Suppress("NewApi")
val platformModule = module {
    single { PlatformProfile.current() }
    single<DefaultDialerAccess> { createDefaultDialerAccess(androidContext(), get()) }
    single<CallPlacement> { TelecomCallPlacement(androidContext(), get()) }
    single<SpeakerRouting> {
        if (get<PlatformProfile>().usesCallEndpoints) EndpointSpeakerRouting() else AudioRouteSpeakerRouting()
    }
    single<CallWindowPolicy> {
        if (get<PlatformProfile>().usesLockScreenMethods) ModernCallWindowPolicy() else LegacyCallWindowPolicy()
    }
    single<NotificationAccess> {
        when {
            get<PlatformProfile>().needsNotificationPermission -> RuntimeNotificationAccess(androidContext())
            get<PlatformProfile>().usesNotificationChannels -> ChannelNotificationAccess(androidContext())
            else -> LegacyNotificationAccess(androidContext())
        }
    }
    single<CallNotificationChannels> {
        if (get<PlatformProfile>().usesNotificationChannels) AndroidCallNotificationChannels(androidContext())
        else NoCallNotificationChannels()
    }
    single { PermissionHistory(androidContext()) }
    single<RuntimePermissions> { AndroidRuntimePermissions(androidContext(), get()) }
    single<PhoneHardware> { AndroidPhoneHardware(androidContext()) }
    single<FullScreenCallAccess> { createFullScreenCallAccess(androidContext(), get()) }
}
