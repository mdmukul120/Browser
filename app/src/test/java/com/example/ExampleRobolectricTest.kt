package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.adblock.AdBlockEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DevChrome Browser", appName)
    }

    @Test
    fun `verify adblock engine identifies ad domains`() {
        assertTrue(AdBlockEngine.isAdOrTracker("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
        assertTrue(AdBlockEngine.isAdOrTracker("https://securepubads.g.doubleclick.net/gampad/ads"))
        assertTrue(AdBlockEngine.isAdOrTracker("https://cdn.taboola.com/libtrc/uncompressed_loader.js"))
        assertFalse(AdBlockEngine.isAdOrTracker("https://www.wikipedia.org/wiki/Kotlin"))
    }
}
