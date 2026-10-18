package ru.openrustore.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

  @Test
  fun `maps sdk levels onto android releases`() {
    assertEquals("Android 6.0", androidVersionOf(23))
    assertEquals("Android 9", androidVersionOf(28))
    assertEquals("Android 12", androidVersionOf(31))
    assertEquals("Android 12L", androidVersionOf(32))
    assertEquals("Android 13", androidVersionOf(33))
    assertEquals("Android 16", androidVersionOf(36))
    assertEquals("API 0", androidVersionOf(0))
  }

  @Test
  fun `formats package sizes`() {
    assertEquals("0 B", formatSize(0))
    assertEquals("512 B", formatSize(512))
    assertEquals("1.0 KB", formatSize(1024))
    assertEquals("65.5 MB", formatSize(68_719_043))
  }

  @Test
  fun `unescapes descriptions coming from the web api`() {
    assertEquals("first\nsecond", unescapeText("first\\nsecond "))
  }
}
