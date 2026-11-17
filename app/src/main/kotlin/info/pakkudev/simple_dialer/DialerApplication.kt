package info.pakkudev.simple_dialer

import android.app.Application
import android.util.Log
import info.pakkudev.simple_dialer.di.appModule
import info.pakkudev.simple_dialer.platform.platformModule
import info.pakkudev.simple_dialer.platform.LauncherIcons
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DialerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DialerApplication)
            modules(platformModule, appModule)
        }
        val settings = get<PreferencesRepository>()
        val icons = get<LauncherIcons>()
        applicationScope.launch {
            settings.preferences.map { it.colorPalette }.distinctUntilChanged().collect { palette ->
                runCatching { icons.apply(palette) }.onFailure { Log.w("LauncherIcons", "Could not update launcher icon", it) }
            }
        }
    }
}
