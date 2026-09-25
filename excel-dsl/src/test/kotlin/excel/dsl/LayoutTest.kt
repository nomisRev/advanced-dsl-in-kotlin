package excel.dsl

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.apache.poi.xssf.usermodel.XSSFSheet

class LayoutTest {
  private fun Sheet<Invoice>.written(): XSSFSheet = writeToBytes().read().getSheetAt(0)

  @Test
  fun `the header is frozen by default`() {
    val pane = invoiceSheet.written().paneInformation
    assertEquals(1, pane.horizontalSplitPosition.toInt())
    assertEquals(0, pane.verticalSplitPosition.toInt())
  }

  @Test
  fun `freezing can be turned off`() {
    val sheet = sheet<Invoice> {
      freezeHeader = false
      val customer by column { it.customer }
    }
    assertNull(sheet.written().paneInformation)
  }

  @Test
  fun `no filter by default`() {
    assertNull(invoiceSheet.written().ctWorksheet.autoFilter)
  }

  @Test
  fun `the filter spans the header and the data, not the totals`() {
    val sheet = sheet<Invoice> {
      autoFilter = true
      val customer by column { it.customer }
      val hours by column { it.hours }
      totals { hours.sum() }
    }
    assertEquals("A1:B4", sheet.written().ctWorksheet.autoFilter.ref)
  }

  @Test
  fun `widths fit the header, at least 10 characters`() {
    val sheet = sheet<Invoice> {
      val customer by column { it.customer }
      "A rather long header" { it.hours }
      val at by column { LocalDateTime.of(2026, 1, 5, 9, 30) }
    }
    val widths = (0..2).map { sheet.written().getColumnWidth(it) / 256 }
    assertEquals(listOf(10, 22, 10), widths)
  }

  @Test
  fun `a date-time format widens the default`() {
    val sheet = sheet<Invoice> { val at by column(CellFormat.DateTime) { LocalDateTime.of(2026, 1, 5, 9, 30) } }
    assertEquals(20, sheet.written().getColumnWidth(0) / 256)
  }

  @Test
  fun `an explicit width wins`() {
    val sheet = sheet<Invoice> {
      val customer by column(width = 30) { it.customer }
      val total by formula(width = 4) { Expr.Constant(1.0) }
      "Hours"(width = 255) { it.hours }
    }
    assertEquals(listOf(30, 4, 255), (0..2).map { sheet.written().getColumnWidth(it) / 256 })
  }

  @Test
  fun `a width must fit Excel`() {
    for (width in listOf(0, -1, 256)) {
      assertFailsWith<IllegalArgumentException>("$width") { sheet<Invoice> { val customer by column(width = width) { it.customer } } }
    }
  }
}
