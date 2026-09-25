package excel.dsl

import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FormulaError
import org.junit.jupiter.api.io.TempDir

class ExportContractTest {
  @TempDir lateinit var dir: Path

  @Test
  fun `cached results retain their types without a formula evaluator after rows flush`() {
    var evaluated = 0
    val schema = sheet<Int?> {
      val n = column("N") { evaluated++; it }
      val double = formula("Double") { n * 2 }
      val flag = formula("Flag") { n gt 0 }
      val label = formula("Label") { flag then "yes" otherwise "" }
      val error = formula("Error") { n / 0 }
      totals {
        n.count()
        double.sum()
        flag.total { sum(double) gt 0 }
        label.total { sum(double) gt 0 then "positive" otherwise "empty" }
        error.average()
      }
    }
    val rows = listOf<Int?>(null) + (1..250).toList()
    val bytes = ByteArrayOutputStream().also { schema.write(it, rows) }.toByteArray()
    assertEquals(rows.size, evaluated)
    bytes.read().use { wb ->
      val sheet = wb.getSheetAt(0)
      for (index in rows.indices) {
        val row = sheet.getRow(index + 1)
        assertEquals(CellType.FORMULA, row.getCell(1).cellType)
        assertEquals(CellType.NUMERIC, row.getCell(1).cachedFormulaResultType)
        assertEquals((rows[index] ?: 0) * 2.0, row.getCell(1).numericCellValue)
        assertEquals(CellType.BOOLEAN, row.getCell(2).cachedFormulaResultType)
        assertEquals(index > 0, row.getCell(2).booleanCellValue)
        assertEquals(CellType.STRING, row.getCell(3).cachedFormulaResultType)
        assertEquals(if (index > 0) "yes" else "", row.getCell(3).stringCellValue)
        assertEquals(CellType.ERROR, row.getCell(4).cachedFormulaResultType)
        assertEquals(FormulaError.DIV0.code, row.getCell(4).errorCellValue)
      }
      val total = sheet.getRow(rows.size + 1)
      assertEquals(250.0, total.getCell(0).numericCellValue)
      assertEquals(62_750.0, total.getCell(1).numericCellValue)
      assertEquals(true, total.getCell(2).booleanCellValue)
      assertEquals("positive", total.getCell(3).stringCellValue)
      assertEquals(FormulaError.DIV0.code, total.getCell(4).errorCellValue)
      assertTrue(wb.forceFormulaRecalculation)
    }
  }

  @Test
  fun `numeric count ignores errors while nonblank count includes them`() {
    val schema = sheet<Int?> {
      val n by column { it }
      val error by formula { n / 0 }
      val errorAgain by formula { error.ref() }
      val text by column { it?.toString() }
      totals { n.count(); error.count(); errorAgain.countNonBlank(); text.countNonBlank() }
    }
    val rows = listOf(null, 0, 1)
    assertEquals(mapOf("N" to 2.0, "Error" to 0.0, "ErrorAgain" to 3.0, "Text" to 2.0), schema.totals(rows))
    assertExcelAgrees(schema, rows)
  }

  @Test
  fun `decimal overloads preserve types and reject the same values in evaluation and export`() {
    val decimal: Column<BigDecimal>
    val schema = sheet<BigDecimal> {
      decimal = column("Decimal") { it }
      val alias = formula("Alias") { decimal.ref() }
      totals { alias.sum() }
    }
    val valid = BigDecimal("1080.25")
    assertEquals(CellResult.Value(valid), schema.evaluateTyped(valid)[decimal])
    assertExcelAgrees(schema, listOf(valid))
    for (value in listOf(BigDecimal("1234567890123456.1"), BigDecimal("1E400"), BigDecimal("1E-400"))) {
      assertFailsWith<IllegalArgumentException> { schema.evaluate(value) }
      assertFailsWith<IllegalArgumentException> { schema.totals(listOf(value)) }
      assertFailsWith<IllegalArgumentException> { schema.write(ByteArrayOutputStream(), listOf(value)) }
    }
    val integer: Column<BigInteger>
    val integers = sheet<BigInteger> { integer = "Integer" { it } }
    assertEquals(CellResult.Value(BigInteger.TEN), integers.evaluateTyped(BigInteger.TEN)[integer])
  }

  @Test
  fun `file options keep standard truncate default and allow create new`() = runTest {
    val path = dir.resolve("report.xlsx")
    val schema = sheet<Int> { column("N") { it } }
    path.writeText("original")
    assertFailsWith<FileAlreadyExistsException> { schema.write(path, listOf(1), options = arrayOf(CREATE_NEW)) }
    assertFailsWith<FileAlreadyExistsException> { schema.write(path, listOf(1).asFlow(), options = arrayOf(CREATE_NEW)) }
    assertFailsWith<FileAlreadyExistsException> { workbook { sheet("N", schema, listOf(1)) }.write(path, CREATE_NEW) }
    assertFailsWith<FileAlreadyExistsException> { flowWorkbook { sheet("N", schema, listOf(1)) }.write(path, CREATE_NEW) }
    assertFailsWith<FileAlreadyExistsException> {
      generateExcel(path.toString(), listOf(1), options = arrayOf(CREATE_NEW)) { column("N") { it } }
    }
    assertEquals("original", path.readText())
    schema.write(path, listOf(1))
    path.read().use { assertEquals(1.0, it.getSheetAt(0).getRow(1).getCell(0).numericCellValue) }
    val invalid = sheet<Int> { column("N") { Double.NaN } }
    assertFailsWith<IllegalArgumentException> { invalid.write(path, listOf(1)) }
    assertEquals("", path.readText())
  }
}
