package excel.dsl

import excel.dsl.AggregateFunction.Average
import excel.dsl.AggregateFunction.CountNonBlank as Count
import excel.dsl.AggregateFunction.Max
import excel.dsl.AggregateFunction.Min
import excel.dsl.AggregateFunction.Sum
import excel.dsl.Comparison.Equal
import excel.dsl.Comparison.Greater
import excel.dsl.Comparison.GreaterOrEqual
import excel.dsl.Comparison.Less
import excel.dsl.Comparison.LessOrEqual
import excel.dsl.Comparison.NotEqual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExprTest {
  private val a = Column<Number>("a", 0, Unit)
  private val b = Column<Number>("b", 1, Unit)
  private val c = Column<Number>("c", 2, Unit)
  private val ra = Formula.Ref(a)
  private val rb = Formula.Ref(b)
  private val rc = Formula.Ref(c)
  private fun k(value: Double) = Formula.Constant(value)
  private fun sum(vararg terms: Formula<*>) = Formula.Aggregate(Sum, terms.toList())

  private fun excel(Formula: Formula<*>, row: Int = 2) = Formula.toExcel(row)

  @Test
  fun `a reference renders the cell in the given row`() {
    assertEquals("A2", excel(ra))
    assertEquals("B7", excel(rb, row = 7))
  }

  @Test
  fun `a range renders the data rows`() {
    assertEquals("B2:B4", Formula.Range(b).toExcel(5, data = 2..4))
    assertEquals("SUM(C2:C9)", sum(Formula.Range(c)).toExcel(10, data = 2..9))
  }

  @Test
  fun `a range needs data rows`() {
    assertFailsWith<IllegalArgumentException> { excel(Formula.Range(a)) }
  }

  @Test
  fun `constants render without a trailing zero or exponent`() {
    assertEquals("2", excel(k(2.0)))
    assertEquals("1.21", excel(k(1.21)))
    assertEquals("1000", excel(k(1000.0)))
    assertEquals("0.00001", excel(k(1e-5)))
    assertEquals("0", excel(k(0.0)))
  }

  @Test
  fun `negative constants are parenthesised`() {
    assertEquals("(-1.5)", excel(k(-1.5)))
    assertEquals("A2*(-1)", excel(Formula.Times(ra, k(-1.0))))
  }

  @Test
  fun `constants must be finite`() {
    assertFailsWith<IllegalArgumentException> { k(Double.POSITIVE_INFINITY) }
    assertFailsWith<IllegalArgumentException> { k(Double.NaN) }
  }

  @Test
  fun `binary operators render infix`() {
    assertEquals("A2+B2", excel(Formula.Plus(ra, rb)))
    assertEquals("A2-B2", excel(Formula.Minus(ra, rb)))
    assertEquals("A2*B2", excel(Formula.Times(ra, rb)))
    assertEquals("A2/B2", excel(Formula.Div(ra, rb)))
  }

  @Test
  fun `comparisons render with Excel's symbols`() {
    val symbols = listOf(Greater, GreaterOrEqual, Less, LessOrEqual, Equal, NotEqual)
      .map { excel(Formula.Compare(ra, it, rb)) }
    assertEquals(listOf("A2>B2", "A2>=B2", "A2<B2", "A2<=B2", "A2=B2", "A2<>B2"), symbols)
  }

  @Test
  fun `functions render as calls`() {
    assertEquals("SUM(A2, B2, C2)", excel(sum(ra, rb, rc)))
    assertEquals("SUM(A2*B2, 3)", excel(sum(Formula.Times(ra, rb), k(3.0))))
    assertEquals(
      listOf("AVERAGE(A2)", "MIN(A2)", "MAX(A2)", "COUNTA(A2)"),
      listOf(Average, Min, Max, Count).map { excel(Formula.Aggregate(it, listOf(ra))) },
    )
    assertEquals("ROUND(A2*1.21, 2)", excel(Formula.Round(Formula.Times(ra, k(1.21)), 2)))
    assertEquals("ROUND(A2, -2)", excel(Formula.Round(ra, -2)))
    assertEquals("IF(A2>B2, A2, 0)", excel(Formula.If(Formula.Compare(ra, Greater, rb), ra, k(0.0))))
  }

  @Test
  fun `an aggregate needs a term`() {
    assertFailsWith<IllegalArgumentException> { Formula.Aggregate(Sum, emptyList()) }
  }

  @Test
  fun `parentheses only where precedence needs them`() {
    assertEquals("A2*B2*C2", excel(Formula.Times(Formula.Times(ra, rb), rc)))
    assertEquals("A2*(B2*C2)", excel(Formula.Times(ra, Formula.Times(rb, rc))))
    assertEquals("(A2+B2)*C2", excel(Formula.Times(Formula.Plus(ra, rb), rc)))
    assertEquals("A2+B2*C2", excel(Formula.Plus(ra, Formula.Times(rb, rc))))
    assertEquals("A2-B2-C2", excel(Formula.Minus(Formula.Minus(ra, rb), rc)))
    assertEquals("A2-(B2-C2)", excel(Formula.Minus(ra, Formula.Minus(rb, rc))))
    assertEquals("A2/(B2*C2)", excel(Formula.Div(ra, Formula.Times(rb, rc))))
    assertEquals("A2+B2>C2", excel(Formula.Compare(Formula.Plus(ra, rb), Greater, rc)))
    assertEquals("IF(A2>B2, 1, 0)*C2", excel(Formula.Times(Formula.If(Formula.Compare(ra, Greater, rb), k(1.0), k(0.0)), rc)))
    assertEquals("SUM(A2+B2)*2", excel(Formula.Times(sum(Formula.Plus(ra, rb)), k(2.0))))
  }

  @Test
  fun `evaluate computes from the referenced values`() {
    val row = mapOf(a to 3, b to 4.5, c to 2L)
    assertEquals(3.0, ra.computed(row))
    assertEquals(7.5, Formula.Plus(ra, rb).computed(row))
    assertEquals(-1.5, Formula.Minus(ra, rb).computed(row))
    assertEquals(13.5, Formula.Times(ra, rb).computed(row))
    assertEquals(1.5, Formula.Div(ra, rc).computed(row))
    assertEquals(1.21, k(1.21).computed(row))
  }

  @Test
  fun `comparisons evaluate to booleans`() {
    val row = mapOf(a to 3, b to 4)
    fun compare(comparison: Comparison, left: Formula<*>, right: Formula<*>) = Formula.Compare(left, comparison, right).computed(row)
    assertEquals(listOf(false, true, false), listOf(compare(Greater, ra, rb), compare(Greater, rb, ra), compare(Greater, ra, ra)))
    assertEquals(listOf(false, true), listOf(compare(GreaterOrEqual, ra, rb), compare(GreaterOrEqual, ra, ra)))
    assertEquals(listOf(true, false), listOf(compare(Less, ra, rb), compare(Less, ra, ra)))
    assertEquals(listOf(true, false), listOf(compare(LessOrEqual, ra, ra), compare(LessOrEqual, rb, ra)))
    assertEquals(listOf(true, false), listOf(compare(Equal, ra, ra), compare(Equal, ra, rb)))
    assertEquals(listOf(false, true), listOf(compare(NotEqual, ra, ra), compare(NotEqual, ra, rb)))
  }

  @Test
  fun `aggregates evaluate like Excel`() {
    val row = mapOf(a to 3, b to 5, c to null)
    assertEquals(8.0, sum(ra, rb, rc).computed(row))
    assertEquals(4.0, Formula.Aggregate(Average, listOf(ra, rb, rc)).computed(row))
    assertEquals(3.0, Formula.Aggregate(Min, listOf(ra, rb, rc)).computed(row))
    assertEquals(5.0, Formula.Aggregate(Max, listOf(ra, rb, rc)).computed(row))
    assertEquals(2.0, Formula.Aggregate(Count, listOf(ra, rb, rc)).computed(row))
  }

  @Test
  fun `aggregates of nothing`() {
    val row = mapOf(a to null)
    assertEquals(0.0, sum(ra).computed(row))
    assertEquals(ExcelError.DivisionByZero, Formula.Aggregate(Average, listOf(ra)).computed(row))
    assertEquals(0.0, Formula.Aggregate(Min, listOf(ra)).computed(row))
    assertEquals(0.0, Formula.Aggregate(Max, listOf(ra)).computed(row))
    assertEquals(0.0, Formula.Aggregate(Count, listOf(ra)).computed(row))
  }

  @Test
  fun `round goes half away from zero`() {
    fun round(value: Double, digits: Int) = Formula.Round(k(value), digits).computed(emptyMap())
    assertEquals(3.0, round(2.5, 0))
    assertEquals(-3.0, round(-2.5, 0))
    assertEquals(2.68, round(2.675, 2))
    assertEquals(1200.0, round(1249.0, -2))
    assertEquals(ExcelError.DivisionByZero, Formula.Round(Formula.Div(k(1.0), k(0.0)), 2).computed(emptyMap()))
  }

  @Test
  fun `if picks a branch on any non-zero condition`() {
    fun pick(condition: Double) = Formula.If(Formula.Compare(k(condition), NotEqual, k(0.0)), k(1.0), k(2.0)).computed(emptyMap())
    assertEquals(listOf(1.0, 2.0, 1.0), listOf(pick(1.0), pick(0.0), pick(-0.5)))
  }

  @Test
  fun `if only evaluates the branch it picks`() {
    val missing = Column<Number>("missing", 9, Unit)
    assertEquals(1.0, Formula.If(Formula.Bool(true), k(1.0), Formula.Ref(missing)).computed(emptyMap()))
  }

  @Test
  fun `a null value counts as zero in arithmetic, like an empty cell`() {
    assertEquals(4.0, Formula.Plus(ra, rb).computed(mapOf(a to null, b to 4)))
  }

  @Test
  fun `a missing column fails`() {
    assertFailsWith<NoSuchElementException> { rb.computed(mapOf(a to 1)) }
  }

  @Test
  fun `a range cannot be evaluated for one row`() {
    assertFailsWith<IllegalArgumentException> { sum(Formula.Range(a)).computed(mapOf(a to 1)) }
  }

  @Test
  fun `division by zero produces a spreadsheet error`() {
    assertEquals(ExcelError.DivisionByZero, Formula.Div(ra, rb).computed(mapOf(a to 1, b to 0)))
    assertEquals(ExcelError.DivisionByZero, Formula.Div(ra, rb).computed(mapOf(a to 0, b to 0)))
  }

  @Test
  fun `trees compare structurally`() {
    assertEquals(Formula.Times(ra, k(2.0)), Formula.Times(Formula.Ref(a), Formula.Constant(2.0)))
  }
}

private fun Formula<*>.computed(row: Map<out Column<*>, Any?>): Any? = when (val result = evaluate(row)) {
  is CellResult.Value -> result.value
  is CellResult.Error -> result.error
}
