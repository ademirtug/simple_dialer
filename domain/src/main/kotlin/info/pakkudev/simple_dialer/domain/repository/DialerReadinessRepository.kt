package info.pakkudev.simple_dialer.domain.repository

import info.pakkudev.simple_dialer.domain.model.DialerReadiness
import kotlinx.coroutines.flow.StateFlow

interface DialerReadinessRepository {
    val readiness: StateFlow<DialerReadiness>
    /** Refresh on foreground entry, permission/settings results, and access failures. */
    fun refresh(): DialerReadiness
}
