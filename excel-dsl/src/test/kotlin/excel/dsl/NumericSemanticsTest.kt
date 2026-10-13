package excel.dsl

import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FormulaError

class NumericSemanticsTest {
  @Test
  fun `all comparisons use Excel precision and select the same branches after recalculation`() {
    val schema = sheet<Double> {
      val n = column("N") { it }
      formula("Equal") { (n + 0.2) eq 0.3 }
      formula("Not equal") { (n + 0.2) ne 0.3 }
      formula("Greater") { (n + 0.2) gt 0.3 }
      formula("Greater or equal") { (n + 0.2) ge 0.3 }
      formula("Less") { (n + 0.2) lt 0.3 }
      formula("Less or equal") { (n + 0.2) le 0.3 }
      val branch = formula("Branch") { (n + 0.2) eq 0.3 then "equal" otherwise "different" }
      val negative = formula("Negative") { (-n - 0.2) eq -0.3 }
      totals {
        branch.total { (sum(n) + 0.2) eq 0.3 then "equal" otherwise "different" }
        negative.total { (-sum(n) - 0.2) eq -0.3 }
      }
    }
    assertEquals(
      listOf(true, false, false, true, false, true, "equal", true),
      schema.evaluate(0.1).values.drop(1),
    )
    assertEquals(mapOf("Branch" to "equal", "Negative" to true), schema.totals(listOf(0.1)))
    assertExcelAgrees(schema, listOf(0.1))
    assertExcelAgrees(schema, listOf(0.0, 0.1, 0.2))
  }

  @Test
  fun `numeric comparisons still distinguish numbers beyond Excel precision`() {
    assertTrue(compareExcelNumbers(0.300000000000001, 0.3) > 0)
    assertTrue(compareExcelNumbers(-0.300000000000001, -0.3) < 0)
    assertEquals(0, compareExcelNumbers(-0.0, 0.0))
    assertEquals(0, compareExcelNumbers(0.0, -0.0))
    for (value in listOf(java.lang.Double.MIN_NORMAL, -java.lang.Double.MIN_NORMAL, Double.MAX_VALUE, -Double.MAX_VALUE)) {
      assertEquals(0, compareExcelNumbers(value, value))
    }
  }

  @Test
  fun `subnormal inputs fail evaluation totals and writing with location`() {
    val schema = sheet<Double> {
      val n = column("N") { it }
      totals { n.sum() }
    }
    for (value in listOf(Double.MIN_VALUE, -Double.MIN_VALUE, 1e-310, -1e-310, Math.nextDown(java.lang.Double.MIN_NORMAL))) {
      assertTrue(assertFailsWith<IllegalArgumentException> { schema.evaluate(value) }.message!!.contains("N:"))
      assertFailsWith<IllegalArgumentException> { schema.totals(listOf(value)) }
      val error = assertFailsWith<IllegalArgumentException> { schema.write(ByteArrayOutputStream(), listOf(value)) }
      assertTrue(error.message!!.contains("N, row 2:"))
    }
  }

  @Test
  fun `tiny decimal inputs are rejected before conversion can silently underflow`() {
    val schema = sheet<BigDecimal> {
      val n = column("Decimal") { it }
      totals { n.sum() }
    }
    for (value in listOf(BigDecimal("1E-310"), BigDecimal("-1E-310"), BigDecimal("1E-400"))) {
      val error = assertFailsWith<IllegalArgumentException> { schema.evaluate(value) }
      assertTrue(error.message!!.contains("outside the supported numeric range"))
      assertFailsWith<IllegalArgumentException> { schema.totals(listOf(value)) }
      assertFailsWith<IllegalArgumentException> { schema.write(ByteArrayOutputStream(), listOf(value)) }
    }
  }

  @Test
  fun `numeric literals reject subnormals through arithmetic comparisons and branches`() {
    for (value in listOf<Number>(Double.MIN_VALUE, -1e-310, BigDecimal("1E-310"))) {
      assertFailsWith<IllegalArgumentException> { sheet<Unit> { formula("Literal") { literal(value) } } }
      assertFailsWith<IllegalArgumentException> { sheet<Unit> { formula("Arithmetic") { literal(1) + value } } }
      assertFailsWith<IllegalArgumentException> { sheet<Unit> { formula("Compare") { literal(1) eq value } } }
      assertFailsWith<IllegalArgumentException> { sheet<Unit> { formula("Branch") { literal(true) then value otherwise 0 } } }
    }
    assertFailsWith<IllegalArgumentException> { Expr.Constant(Double.MIN_VALUE) }
  }

  @Test
  fun `normal double boundaries zero and tiny floats remain valid`() {
    val schema = sheet<Double> { column("N") { it } }
    val values = listOf(0.0, -0.0, java.lang.Double.MIN_NORMAL, -java.lang.Double.MIN_NORMAL, Double.MAX_VALUE)
    for (value in values) assertEquals(value, schema.evaluate(value)["N"])
    assertExcelAgrees(schema, values)
    // A Float subnormal is well within Excel's Double range.
    assertEquals(Float.MIN_VALUE.toDouble(), Float.MIN_VALUE.checkedExcelDouble())
    assertEquals(2.22507385850721e-308, BigDecimal("2.22507385850721E-308").checkedExcelDouble())
  }

  @Test
  fun `arithmetic underflow becomes zero before comparisons conditions and dependent formulas`() {
    val schema = sheet<Double> {
      val n = column("N") { it }
      val divided = formula("Divided") { n / 100 }
      formula("Multiplied") { n * 0.01 }
      formula("Equal") { divided eq 0 }
      formula("Branch") { divided eq 0 then "zero" otherwise "nonzero" }
      formula("Propagated") { divided * 100 }
      formula("Division by underflow") { literal(1) / divided }
      totals { divided.sum() }
    }
    for (value in listOf(1e-307, -1e-307)) {
      assertEquals(
        listOf(0.0, 0.0, true, "zero", 0.0, ExcelError.DivisionByZero),
        schema.evaluate(value).values.drop(1),
      )
    }
    assertCachedResults(schema, listOf(1e-307, -1e-307))
  }

  @Test
  fun `addition subtraction and rounding flush subnormal results`() {
    val normal = 2.3e-308
    val schema = sheet<Double> {
      val n = column("N") { it }
      formula("Difference") { n - normal }
      formula("Addition") { n + -normal }
      formula("Rounded") { round(n, 308) }
    }
    for (value in listOf(Math.nextUp(normal), Math.nextDown(normal))) {
      assertEquals(listOf(0.0, 0.0, 0.0), schema.evaluate(value).values.drop(1))
    }
    assertCachedResults(schema, listOf(Math.nextUp(normal), Math.nextDown(normal)))
  }

  @Test
  fun `row and range aggregates flush underflow and preserve counts and errors`() {
    val normal = java.lang.Double.MIN_NORMAL
    val schema = sheet<Double> {
      val n = column("N") { it }
      val negative = column("Negative") { -normal }
      val zero = column("Zero") { 0.0 }
      val sum = formula("Sum") { sum(listOf(n, negative)) }
      val average = formula("Average") { average(listOf(n, zero)) }
      val error = formula("Error") { n / 0 }
      totals { n.sum(); negative.average(); sum.sum(); average.average(); error.sum() }
    }
    val values = schema.evaluate(Math.nextUp(normal))
    assertEquals(0.0, values["Sum"])
    assertEquals(0.0, values["Average"])
    assertEquals(ExcelError.DivisionByZero, values["Error"])
    assertCachedResults(schema, listOf(Math.nextUp(normal)))

    val rangeSchema = sheet<Double> {
      val sum = column("Sum") { it }
      val average = column("Average") { it }
      totals { sum.sum(); average.average() }
    }
    assertEquals(mapOf("Sum" to 0.0, "Average" to 0.0), rangeSchema.totals(listOf(Math.nextUp(normal), -normal)))
    assertEquals(0.0, rangeSchema.totals(listOf(normal, 0.0))["Average"])
    assertCachedResults(rangeSchema, listOf(Math.nextUp(normal), -normal))
    assertCachedResults(rangeSchema, listOf(normal, 0.0))
  }

  // Excel flushes underflow to zero; POI 5.5.1 retains some computed subnormals,
  // so these tests inspect serialized caches instead of using POI as an oracle.
  // https://learn.microsoft.com/en-us/troubleshoot/microsoft-365-apps/excel/floating-point-arithmetic-inaccurate-result
  private fun <T> assertCachedResults(schema: Sheet<T>, rows: List<T>) {
    ByteArrayOutputStream().also { schema.write(it, rows) }.toByteArray().read().use { workbook ->
      val sheet = workbook.getSheetAt(0)
      fun cached(rowIndex: Int, values: Map<String, Any?>) {
        for ((name, expected) in values) {
          val cell = sheet.getRow(rowIndex).getCell(schema.headers.indexOf(name))
          val type = if (cell.cellType == CellType.FORMULA) cell.cachedFormulaResultType else cell.cellType
          val actual: Any? = when (type) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.BOOLEAN -> cell.booleanCellValue
            CellType.STRING -> cell.stringCellValue
            CellType.ERROR -> ExcelError.entries.single { it.text == FormulaError.forInt(cell.errorCellValue).string }
            else -> null
          }
          assertEquals(expected, actual, "$name, row ${rowIndex + 1}")
        }
      }
      rows.forEachIndexed { index, row -> cached(index + 1, schema.evaluate(row)) }
      schema.totals(rows).takeIf { it.isNotEmpty() }?.let { cached(rows.size + 1, it) }
    }
  }
}
