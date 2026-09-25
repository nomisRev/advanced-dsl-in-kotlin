package excel.dsl

import java.io.ByteArrayOutputStream
import java.io.FilterOutputStream
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.junit.jupiter.api.io.TempDir

class WriteTest {
  @TempDir
  lateinit var dir: Path

  @Test
  fun `writes the header, one row per value, and a formula per row`() {
    assertEquals(
      listOf(
        listOf("Customer", "Hours", "Rate", "IssuedOn", "Total", "Gross"),
        listOf("Ada", "12", "90", "2026-01-05", "=B2*C2", "=E2*1.21"),
        listOf("Linus", "8", "80", null, "=B3*C3", "=E3*1.21"),
        listOf("Grace", "20", "110", "2026-03-30", "=B4*C4", "=E4*1.21"),
      ),
      invoiceSheet.writeToBytes().read().cells(),
    )
  }

  @Test
  fun `the sheet is called Sheet1 unless named`() {
    val named = ByteArrayOutputStream().also { invoiceSheet.write(it, emptySequence(), "Q1") }
    assertEquals("Sheet1", invoiceSheet.writeToBytes().read().getSheetAt(0).sheetName)
    assertEquals("Q1", named.toByteArray().read().getSheetAt(0).sheetName)
  }

  @Test
  fun `cells get the type of their value`() {
    val row = invoiceSheet.writeToBytes().read().getSheetAt(0).getRow(1)
    assertEquals(CellType.STRING, row.getCell(0).cellType)
    assertEquals(CellType.NUMERIC, row.getCell(1).cellType)
    assertEquals(CellType.NUMERIC, row.getCell(3).cellType)
    assertTrue(DateUtil.isCellDateFormatted(row.getCell(3)))
    assertEquals("yyyy-mm-dd", row.getCell(3).cellStyle.dataFormatString)
    assertEquals(CellType.FORMULA, row.getCell(4).cellType)
  }

  @Test
  fun `null leaves no cell`() {
    val row = invoiceSheet.writeToBytes().read().getSheetAt(0).getRow(2)
    assertEquals(null, row.getCell(3))
  }

  @Test
  fun `only the header row is bold`() {
    val workbook = invoiceSheet.writeToBytes().read()
    val sheet = workbook.getSheetAt(0)
    val boldHeaders = sheet.getRow(0).map { workbook.getFontAt(it.cellStyle.fontIndex).bold }
    val boldCells = sheet.getRow(1).map { workbook.getFontAt(it.cellStyle.fontIndex).bold }
    assertEquals(List(6) { true }, boldHeaders)
    assertEquals(List(6) { false }, boldCells)
  }

  @Test
  fun `Excel computes what evaluate computes`() {
    val sheet = sheet<Invoice> {
      val hours by column { it.hours }
      val rate by column { it.rate }
      val total by formula { hours * rate }
      val discounted by formula { total - (total - 100) / 2 }
      val sum by formula { sum([hours, rate, total]) }
      val average by formula { average([hours, rate]) }
      val min by formula { min([hours, rate]) }
      val max by formula { max([hours, rate]) }
      val large by formula { total gt 1000 }
      val comparisons by formula { (hours ge 12) + (hours lt 10) + (hours le 8) + (rate eq 80) + (rate ne 80) }
      val weighted by formula { (large + 1) * hours }
      val negated by formula { hours * -1 }
      val rounded by formula { round(total / 7, 2) }
      val hundreds by formula { round(total, -2) }
      val picked by formula { large then total * 0.9 otherwise total }
    }
    assertExcelAgrees(sheet, invoices)
  }

  @Test
  fun `rows beyond the in-memory window are all written`() {
    val many = (1..ROW_WINDOW * 3 + 7).map { Invoice("c$it", it, 1, null) }
    val sheet = invoiceSheet.writeToBytes(many.asSequence()).read().getSheetAt(0)
    assertEquals(many.size, sheet.lastRowNum)
    assertEquals("B${many.size + 1}*C${many.size + 1}", sheet.getRow(many.size).getCell(4).cellFormula)
  }

  @Test
  fun `rows are pulled lazily, once per write`() {
    var pulled = 0
    val rows = sequence {
      invoices.forEach {
        pulled++
        yield(it)
      }
    }
    assertEquals(0, pulled)
    invoiceSheet.writeToBytes(rows)
    assertEquals(3, pulled)
    invoiceSheet.writeToBytes(rows)
    assertEquals(6, pulled)
  }

  @Test
  fun `writing twice gives the same cells`() {
    assertEquals(invoiceSheet.writeToBytes().read().cells(), invoiceSheet.writeToBytes().read().cells())
  }

  @Test
  fun `the output stream is left open`() {
    var closed = false
    val output = object : FilterOutputStream(ByteArrayOutputStream()) {
      override fun close() {
        closed = true
      }
    }
    invoiceSheet.write(output, invoices.asSequence())
    assertFalse(closed)
  }

  @Test
  fun `a path names the sheet after the file`() {
    val file = dir.resolve("q1.xlsx")
    invoiceSheet.write(file, invoices.asSequence())
    val workbook = file.read()
    assertEquals("q1", workbook.getSheetAt(0).sheetName)
    assertEquals(invoiceSheet.writeToBytes().read().cells(), workbook.cells())
  }

  @Test
  fun `an invalid sheet name fails before writing`() {
    assertFailsWith<IllegalArgumentException> {
      invoiceSheet.write(ByteArrayOutputStream(), invoices.asSequence(), "a/b")
    }
  }

  @Test
  fun `a failing row propagates and leaves no temp files`() {
    val before = sxssfTempFiles()
    val sheet = sheet<Invoice> { val hours by column { check(it.hours < 0) { "boom" }; it.hours } }
    val file = dir.resolve("broken.xlsx")
    val error = assertFailsWith<IllegalStateException> { sheet.write(file, invoices.asSequence()) }
    assertEquals("boom", error.message)
    assertEquals(before, sxssfTempFiles())
  }
}
