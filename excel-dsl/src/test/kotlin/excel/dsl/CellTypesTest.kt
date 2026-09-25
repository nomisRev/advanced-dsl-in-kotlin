package excel.dsl

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import org.apache.poi.ss.usermodel.CellType

enum class Status { Paid, Open }

@JvmInline
value class Money(val cents: Long)

/** How users add a type of their own: an extension with its own name, mapping to a supported type. */
fun <T> SheetBuilder<T>.money(value: (T) -> Money?): ColumnDelegate<Number> =
  column(CellFormat.currency("€")) { row -> value(row)?.let { it.cents.toBigDecimal().movePointLeft(2) } }

class CellTypesTest {
  private val at = LocalDateTime.of(2026, 1, 5, 9, 30)

  @Test
  fun `the lambda return type picks the column type`() {
    sheet<Invoice> {
      val text: Column<String> by column { it.customer }
      val number: Column<Number> by column { it.hours }
      val money: Column<Number> by column { it.rate.toBigDecimal() }
      val flag: Column<Boolean> by column { it.hours > 10 }
      val date: Column<java.time.LocalDate> by column { it.issuedOn }
      val dateTime: Column<LocalDateTime> by column { at }
      val instant: Column<Instant> by column { Instant.EPOCH }
      val status: Column<String> by column { Status.Paid }
      val cents: Column<Number> by money { Money(100) }
    }
  }

  @Test
  fun `booleans are Excel booleans`() {
    val sheet = sheet<Invoice> { val long by column { it.hours > 10 } }
    val cell = sheet.writeToBytes().read().getSheetAt(0).getRow(1).getCell(0)
    assertEquals(CellType.BOOLEAN, cell.cellType)
    assertEquals(true, cell.booleanCellValue)
  }

  @Test
  fun `date-times keep their time`() {
    val sheet = sheet<Invoice> { val at by column { at } }
    val cell = sheet.writeToBytes().read().getSheetAt(0).getRow(1).getCell(0)
    assertEquals(at, cell.localDateTimeCellValue)
    assertEquals("2026-01-05 09:30:00", sheet.writeToBytes().read().cells()[1].single())
  }

  @Test
  fun `instants show in the configured zone, UTC by default`() {
    val instant = Instant.parse("2026-01-05T09:30:00Z")
    val utc = sheet<Invoice> { val sent by column { instant } }
    val brussels = sheet<Invoice>(ExcelConfig(zone = ZoneId.of("Europe/Brussels"))) { val sent by column { instant } }
    assertEquals("2026-01-05 09:30:00", utc.writeToBytes().read().cells()[1].single())
    assertEquals("2026-01-05 10:30:00", brussels.writeToBytes().read().cells()[1].single())
    assertEquals(mapOf("Sent" to instant), brussels.evaluate(invoices[0]))
  }

  @Test
  fun `the zone config keeps the default header style`() {
    val sheet = sheet<Invoice>(ExcelConfig(zone = ZoneId.of("Europe/Brussels"))) { val issuedOn by column { it.issuedOn } }
    assertEquals(listOf("IssuedOn"), sheet.headers)
  }

  @Test
  fun `big decimals are numbers formulas can use`() {
    val sheet = sheet<Invoice> {
      val amount by column(CellFormat.Decimal) { BigDecimal("1080.25") }
      val gross by formula { amount * 1.21 }
    }
    val cells = sheet.writeToBytes().read().getSheetAt(0).getRow(1)
    assertEquals(1080.25, cells.getCell(0).numericCellValue)
    assertEquals(mapOf("Amount" to BigDecimal("1080.25"), "Gross" to 1080.25 * 1.21), sheet.evaluate(invoices[0]))
  }

  @Test
  fun `every integer type is a number`() {
    val sheet = sheet<Invoice> {
      "Byte" { 1.toByte() }
      "Short" { 2.toShort() }
      "Long" { 3L }
      "Float" { 4.5f }
      "BigInteger" { 6.toBigInteger() }
    }
    assertEquals(listOf("1", "2", "3", "4.5", "6"), sheet.writeToBytes().read().cells()[1])
  }

  @Test
  fun `enums are written by name`() {
    val sheet = sheet<Invoice> {
      val status by column { if (it.hours > 10) Status.Paid else Status.Open }
      "Also" { Status.Open }
    }
    assertEquals(listOf("Paid", "Open"), sheet.writeToBytes().read().cells()[1])
    assertEquals(mapOf("Status" to "Paid", "Also" to "Open"), sheet.evaluate(invoices[0]))
  }

  @Test
  fun `users add a type with a named extension`() {
    val sheet = sheet<Invoice> {
      val amount by money { Money(it.hours * it.rate * 100L + 25) }
      val gross by formula { amount * 1.21 }
    }
    assertEquals(listOf("€1,080.25", "=A2*1.21"), sheet.writeToBytes().read().cells()[1])
  }

  @Test
  fun `a string header takes every type`() {
    val sheet = sheet<Invoice> {
      val flag: Column<Boolean> = "Long" { it.hours > 10 }
      val dateTime: Column<LocalDateTime> = "At" { at }
      val instant: Column<Instant> = "Sent" { Instant.EPOCH }
      val status: Column<String> = "Status" { Status.Paid }
    }
    assertEquals(listOf("TRUE", "2026-01-05 09:30:00", "1970-01-01 00:00:00", "Paid"), sheet.writeToBytes().read().cells()[1])
  }
}
