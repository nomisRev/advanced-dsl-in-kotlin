package excel.dsl

import java.time.LocalDateTime
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class FormatTest {
  @Test
  fun `the predefined formats`() {
    assertEquals(
      listOf("#,##0", "#,##0.00", "0.00%", "yyyy-mm-dd", "yyyy-mm-dd hh:mm:ss"),
      listOf(CellFormat.Integer, CellFormat.Decimal, CellFormat.Percent, CellFormat.Date, CellFormat.DateTime)
        .map { it.pattern },
    )
  }

  @Test
  fun `currency quotes the symbol`() {
    assertEquals("\"€\"#,##0.00", CellFormat.currency("€").pattern)
    assertEquals("\"USD \"#,##0", CellFormat.currency("USD ", decimals = 0).pattern)
    assertEquals("\"a\"\"b\"#,##0.0", CellFormat.currency("a\"b", decimals = 1).pattern)
    assertFailsWith<IllegalArgumentException> { CellFormat.currency("€", decimals = -1) }
  }

  private fun XSSFWorkbook.formats(row: Int): List<String> =
    getSheetAt(0).getRow(row).map { it.cellStyle.dataFormatString }

  @Test
  fun `a column's format applies to its cells and its total`() {
    val euro = CellFormat.currency("€")
    val sheet = sheet<Invoice> {
      val hours by column(CellFormat.Integer) { it.hours }
      val rate by column(format = euro) { it.rate }
      val total by formula(euro) { hours * rate }
      val share by formula(CellFormat.Percent) { hours / 100 }
      totals { total.sum() }
    }
    val workbook = sheet.writeToBytes().read()
    assertEquals(listOf("#,##0", euro.pattern, euro.pattern, "0.00%"), workbook.formats(1))
    assertEquals(euro.pattern, workbook.formats(4).single())
    val excel = workbook.creationHelper.createFormulaEvaluator()
    val formatter = DataFormatter(Locale.US)
    val shown = workbook.getSheetAt(0).getRow(1).drop(2).map { formatter.formatCellValue(it, excel) }
    assertEquals(listOf("€1,080.00", "12.00%"), shown)
  }

  @Test
  fun `without a format, numbers and text are General and dates get theirs`() {
    val sheet = sheet<Invoice> {
      val customer by column { it.customer }
      val hours by column { it.hours }
      val issued by column { it.issuedOn }
      val at by column { LocalDateTime.of(2026, 1, 5, 9, 30) }
      val double by formula { hours * 2 }
    }
    assertEquals(listOf("General", "General", "yyyy-mm-dd", "yyyy-mm-dd hh:mm:ss", "General"), sheet.writeToBytes().read().formats(1))
  }

  @Test
  fun `a format overrides the date default`() {
    val sheet = sheet<Invoice> { val issued by column(CellFormat("dd/mm/yyyy")) { it.issuedOn } }
    val workbook = sheet.writeToBytes().read()
    assertEquals("dd/mm/yyyy", workbook.formats(1).single())
    assertEquals("05/01/2026", workbook.cells()[1].single())
  }

  @Test
  fun `a string header takes a format too`() {
    val sheet = sheet<Invoice> { "Rate"(CellFormat.Decimal) { it.rate } }
    assertEquals("90.00", sheet.writeToBytes().read().cells()[1].single())
  }

  @Test
  fun `the header is never formatted`() {
    val sheet = sheet<Invoice> { val rate by column(CellFormat.Percent) { it.rate } }
    assertEquals(listOf("General"), sheet.writeToBytes().read().formats(0))
  }
}
