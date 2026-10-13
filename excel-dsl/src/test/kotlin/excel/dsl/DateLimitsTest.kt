package excel.dsl

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DateLimitsTest {
  private fun <T> assertRejected(sheet: Sheet<T>, valid: T, invalid: T) {
    val evaluation = assertFailsWith<IllegalArgumentException> { sheet.evaluate(invalid) }
    assertTrue(evaluation.message.orEmpty().startsWith("When: "))
    assertTrue(evaluation.message.orEmpty().contains("outside Excel's date range 1900-01-01 through 9999-12-31"))
    val export = assertFailsWith<IllegalArgumentException> {
      sheet.write(ByteArrayOutputStream(), listOf(valid, invalid))
    }
    assertEquals(evaluation.message?.replaceFirst("When: ", "When, row 3: "), export.message)
  }

  private fun <T> assertRoundTrip(sheet: Sheet<T>, value: T, expected: LocalDateTime) {
    assertEquals(mapOf("When" to value), sheet.evaluate(value))
    val bytes = ByteArrayOutputStream().also { sheet.write(it, listOf(value)) }.toByteArray()
    bytes.read().use { workbook ->
      assertEquals(expected, workbook.getSheetAt(0).getRow(1).getCell(0).localDateTimeCellValue)
    }
  }

  @Test
  fun `dates outside the Excel calendar fail during evaluation and export`() {
    val sheet = sheet<LocalDate> { "When" { it } }
    for (invalid in listOf(LocalDate.of(1800, 1, 1), LocalDate.of(1899, 12, 31), LocalDate.of(10000, 1, 1), LocalDate.MIN, LocalDate.MAX)) {
      assertRejected(sheet, LocalDate.of(2026, 1, 1), invalid)
    }
  }

  @Test
  fun `date boundaries and leap-year transitions round trip`() {
    val sheet = sheet<LocalDate> { "When" { it } }
    for (date in listOf(LocalDate.of(1900, 1, 1), LocalDate.of(1900, 2, 28), LocalDate.of(1900, 3, 1), LocalDate.of(2000, 2, 29), LocalDate.of(9999, 12, 31))) {
      assertRoundTrip(sheet, date, date.atStartOfDay())
    }
  }

  @Test
  fun `date-times validate their date before serialization`() {
    val sheet = sheet<LocalDateTime> { "When" { it } }
    val valid = LocalDateTime.of(2026, 1, 1, 12, 30)
    for (invalid in listOf(LocalDateTime.of(1899, 12, 31, 23, 59, 59), LocalDateTime.of(10000, 1, 1, 0, 0), LocalDateTime.MIN, LocalDateTime.MAX)) {
      assertRejected(sheet, valid, invalid)
    }
    for (dateTime in listOf(LocalDateTime.of(1900, 1, 1, 0, 0), LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_000_000))) {
      assertRoundTrip(sheet, dateTime, dateTime)
    }
  }

  @Test
  fun `instants use the configured zone for both date boundaries`() {
    val east = sheet<Instant>(ExcelConfig(zone = ZoneOffset.ofHours(1))) { "When" { it } }
    val west = sheet<Instant>(ExcelConfig(zone = ZoneOffset.ofHours(-1))) { "When" { it } }
    val utc = sheet<Instant> { "When" { it } }
    val beforeStart = Instant.parse("1899-12-31T23:30:00Z")
    val afterStart = Instant.parse("1900-01-01T00:30:00Z")
    val beforeEnd = Instant.parse("9999-12-31T23:30:00Z")
    val afterEnd = Instant.parse("+10000-01-01T00:30:00Z")
    assertRoundTrip(east, beforeStart, LocalDateTime.of(1900, 1, 1, 0, 30))
    assertRoundTrip(west, afterEnd, LocalDateTime.of(9999, 12, 31, 23, 30))
    assertRoundTrip(utc, afterStart, LocalDateTime.of(1900, 1, 1, 0, 30))
    assertRoundTrip(utc, beforeEnd, LocalDateTime.of(9999, 12, 31, 23, 30))
    assertRejected(utc, Instant.EPOCH, beforeStart)
    assertRejected(utc, Instant.EPOCH, afterEnd)
    assertRejected(west, Instant.EPOCH, afterStart)
    assertRejected(east, Instant.EPOCH, beforeEnd)
  }

  @Test
  fun `extreme instants fail with column and row context`() {
    val sheet = sheet<Instant> { "When" { it } }
    assertRejected(sheet, Instant.EPOCH, Instant.MIN)
    assertRejected(sheet, Instant.EPOCH, Instant.MAX)
  }

  @Test
  fun `nullable dates remain blank`() {
    val sheet = sheet<LocalDate?> { "When" { it } }
    assertEquals(mapOf("When" to null), sheet.evaluate(null))
    val bytes = ByteArrayOutputStream().also { sheet.write(it, listOf(null)) }.toByteArray()
    bytes.read().use { workbook ->
      assertEquals(null, workbook.getSheetAt(0).getRow(1).getCell(0))
    }
  }
}
