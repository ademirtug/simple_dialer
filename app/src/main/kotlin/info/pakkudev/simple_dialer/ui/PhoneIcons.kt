package info.pakkudev.simple_dialer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import info.pakkudev.simple_dialer.R

/** Handset shapes generated from the same SVG as the launcher icon. */
object PhoneIcons {
    val Call: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_phone)

    val CallEnd: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_phone_end)

    val AddCall: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_phone_add)
}
