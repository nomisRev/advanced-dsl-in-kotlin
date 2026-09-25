package excel.dsl

import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.io.TempDir

class TypedApiTest {
  @TempDir lateinit var dir: Path

  @Test
  fun `typed results preserve values blanks and errors`() {
    lateinit var input: Column<Number>
    lateinit var flag: Column<Boolean>
    lateinit var label: Column<String>
    lateinit var error: Column<Double>
    val schema = sheet<Int?> {
      input = "Input" { it }
      flag = formula("Flag") { input gt 0 }
      label = formula("Label") { flag then "positive" otherwise "other" }
      error = formula("Error") { input / 0 }
    }
    val row = schema.evaluateTyped(null)
    val number: CellResult<Number> = row[input]
    val boolean: CellResult<Boolean> = row[flag]
    val text: CellResult<String> = row[label]
    assertEquals(CellResult.Value(null), number)
    assertEquals(CellResult.Value(false), boolean)
    assertEquals(CellResult.Value("other"), text)
    assertEquals(CellResult.Error(ExcelError.DivisionByZero), row[error])
    lateinit var foreign: Column<Number>
    sheet<Int> { foreign = "Input" { it } }
    assertFailsWith<IllegalArgumentException> { row[foreign] }
  }

  @Test
  fun `text comparisons conditionals and escaped literals agree with POI`() {
    val schema = sheet<String?> {
      val text by column { it }
      val alias: Column<String> by formula { text.ref() }
      val matching by formula { text eq "Ada" }
      val before by formula { text lt "Grace" }
      val nonempty by formula { text ne "" }
      val label by formula { matching then "Hello \"Ada\"" otherwise text }
      val selected by formula { nonempty then text.ref() otherwise "(empty)" }
    }
    assertExcelAgrees(schema, listOf(null, "", "Ada", "ADA", "Grace", "Linus"))
  }

  @Test
  fun `named formulas and symmetric arithmetic work with list inputs`() {
    val schema = sheet<Int> {
      val n = "Input" { it }
      formula("Invoice total") { 2 * n + 10 / n - (3 - n) + -n }
      formula("Alias") { n.ref() }
      formula("Left expression") { 10 - (n * 2) + 2 / (n * 3) }
    }
    assertEquals(listOf("Input", "Invoice total", "Alias", "Left expression"), schema.headers)
    assertExcelAgrees(schema, listOf(1, 2, 3))
    val output = ByteArrayOutputStream()
    schema.write(output, listOf(1, 2))
    output.toByteArray().read().use { assertEquals(2, it.getSheetAt(0).lastRowNum) }
    assertFailsWith<IllegalArgumentException> {
      sheet<Int> { val n = "Same" { it }; formula("Same") { n * 2 } }
    }
  }

  @Test
  fun `explicit sheet name allows long file names and validates before truncation`() {
    val file = dir.resolve("a-very-long-report-file-name-that-exceeds-31-characters.xlsx")
    val schema = sheet<Int> { val n by column { it } }
    schema.write(file, listOf(1), name = "Report")
    file.read().use { assertEquals("Report", it.getSheetName(0)) }
    file.writeText("keep this")
    assertFailsWith<IllegalArgumentException> { schema.write(file, listOf(1), name = "bad/name") }
    assertEquals("keep this", file.readText())
    generateExcel(file.toString(), listOf(1, 2), name = "Generated") { val n by column { it } }
    file.read().use { assertEquals("Generated", it.getSheetName(0)) }
  }

  @Test
  fun `formula literals use the same precision and range checks as cells`() {
    for (invalid in listOf<Number>(9_007_199_254_740_993L, BigDecimal("1E-400"), BigDecimal("1E400"), Double.NaN)) {
      assertFailsWith<IllegalArgumentException> { sheet<Int> { val n by column { it }; val bad by formula { n + invalid } } }
      assertFailsWith<IllegalArgumentException> { sheet<Int> { val n by column { it }; val bad by formula { invalid * n } } }
      assertFailsWith<IllegalArgumentException> { sheet<Int> { val n by column { it }; totals { n.total { sum(n) + invalid } } } }
    }
    val schema = sheet<Int> {
      val n by column { it }
      val fine by formula { n + BigDecimal("1.2345") }
    }
    assertExcelAgrees(schema, listOf(0, 1))
  }

  @Test
  fun `aggregate snapshots prevent changes and ownership bypass`() {
    val terms = mutableListOf<Expr<*>>(Expr.Constant(1.0))
    lateinit var foreign: Column<Number>
    sheet<Int> { foreign = "Foreign" { it } }
    val aggregate = Expr.Aggregate(AggregateFunction.Sum, terms)
    val schema = sheet<Int> { val n by column { it }; val total by formula { aggregate } }
    terms[0] = Expr.Ref(foreign)
    assertEquals(1.0, schema.evaluate(7)["Total"])
    assertFailsWith<UnsupportedOperationException> { (aggregate.terms as MutableList).clear() }
    assertExcelAgrees(schema, listOf(7))
  }

  @Test
  fun `errors propagate through references arithmetic conditions logic aggregates and totals`() {
    val schema = sheet<Int> {
      val n by column { it }
      val error by formula { n / 0 }
      val compared by formula { error gt 0 }
      val selected by formula { compared then 1 otherwise 2 }
      val arithmetic by formula { error + 1 }
      val inverted by formula { !compared }
      val combined by formula { compared and false }
      val summed by formula { sum(listOf(error, n)) }
      val lazy by formula { n gt 0 then 1 otherwise error }
      totals { error.sum(); compared.count(); selected.total { sum(error) gt 0 then 1 otherwise 2 } }
    }
    assertEquals(ExcelError.DivisionByZero, schema.evaluate(1)["Selected"])
    assertEquals(1.0, schema.evaluate(1)["Lazy"])
    assertExcelAgrees(schema, listOf(1, 0, -1))
  }

  @Test
  fun `totals stream once and evaluate only aggregate dependencies`() {
    var pulled = 0
    var evaluated = 0
    fun unusedValue(): String = error("Unrelated column must not run")
    val schema = sheet<Int> {
      val n by column { evaluated++; it }
      val unused by column { unusedValue() }
      val double by formula { n * 2 }
      val chained by formula { double + 1 }
      totals { chained.sum(); double.average(); n.count() }
    }
    val rows = sequence { repeat(100_000) { pulled++; yield(1) } }.constrainOnce()
    assertEquals(mapOf("N" to 100_000.0, "Double" to 2.0, "Chained" to 300_000.0), schema.totals(rows))
    assertEquals(100_000, pulled)
    assertEquals(pulled, evaluated)
  }
}
