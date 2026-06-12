package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("Packzy Node", appName)
  }

  @Test
  fun `compileTargetUrl should map default URL to consignment single path`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(application)
    
    // Test default URL mapping
    val scannedId = "002-0002-001-00257385994"
    val result = viewModel.compileTargetUrl(scannedId)
    assertEquals("https://admin.packzy.com/admin/consignment/single/002-0002-001-00257385994", result)
  }
}
