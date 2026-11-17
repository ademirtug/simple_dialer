package info.pakkudev.simple_dialer

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.ui.PhoneDialpad
import info.pakkudev.simple_dialer.ui.theme.DialerTheme
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allDeclaredLocalesResolveTranslationsAndKeepFormatArguments() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locales = listOf("en", "tr", "hi", "ja", "pt", "pt-BR", "ru", "es", "ar", "fr", "de", "id", "zh-Hans", "zh-Hant", "ko", "it", "bn", "ur", "fa")
        for (tag in locales) {
            val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
            val translated = context.createConfigurationContext(config).resources
            val settings = translated.getString(R.string.admin_settings)
            if (tag != "en") assertNotEquals("$tag unexpectedly fell back to English", "Settings", settings)
            assertTrue("$tag lost the contact-name format argument", translated.getString(R.string.call_person, "Alice").contains("Alice"))
            assertTrue("$tag lost the error format argument", translated.getString(R.string.operation_failed, "test-error").contains("test-error"))
            if (tag == "zh-Hans") assertEquals("设置", settings)
            if (tag == "zh-Hant") assertEquals("設定", settings)
        }
        for (tag in listOf("zh-TW", "zh-HK")) {
            val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
            assertEquals("$tag must use Traditional Chinese", "設定", context.createConfigurationContext(config).getString(R.string.admin_settings))
        }
    }

    @Test fun rightToLeftInterfaceKeepsStandardKeypadAndPhoneNumberOrder() {
        compose.setContent {
            DialerTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    PhoneDialpad("+90555123", {}, {}, {})
                }
            }
        }
        val one = compose.onNodeWithText("1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val three = compose.onNodeWithText("3", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("RTL must not reverse telephone keypad columns", one.left < three.left)
        compose.onNode(hasSetTextAction()).assertTextContains("+90555123")
    }
}
