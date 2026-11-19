package com.example

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Study Time", appName)
  }

  @Test
  fun `bundled custom fonts load cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fontIds = listOf(
      R.font.inter,
      R.font.manrope,
      R.font.geist,
      R.font.general_sans,
      R.font.plus_jakarta_sans,
      R.font.jetbrains_mono
    )
    fontIds.forEach { fontId ->
      assertNotNull("Expected font resource $fontId to load", ResourcesCompat.getFont(context, fontId))
    }
  }
}
