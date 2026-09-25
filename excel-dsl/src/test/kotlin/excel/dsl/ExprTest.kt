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
  private val ra = Expr.Ref(a)
  private val rb = Expr.Ref(b)
  private val rc = Expr.Ref(c)
  private fun k(value: Double) = Expr.Constant(value)
  private fun sum(vararg terms: Expr<*>) = Expr.Aggregate(Sum, terms.toList())

  private fun excel(expr: Expr<*>, row: Int = 2) = expr.toExcel(row)

  @Test
  fun `a reference renders the cell in the given row`() {
    assertEquals("A2", excel(ra))
    assertEquals("B7", excel(rb, row = 7))
  }

  @Test
  fun `a range renders the data rows`() {
    assertEquals("B2:B4", Expr.Range(b).toExcel(5, data = 2..4))
    assertEquals("SUM(C2:C9)", sum(Expr.Range(c)).toExcel(10, data = 2..9))
  }

  @Test
  fun `a range needs data rows`() {
    assertFailsWith<IllegalArgumentException> { excel(Expr.Range(a)) }
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
    assertEquals("A2*(-1)", excel(Expr.Times(ra, k(-1.0))))
  }

  @Test
  fun `constants must be finite`() {
    assertFailsWith<IllegalArgumentException> { k(Double.POSITIVE_INFINITY) }
    assertFailsWith<IllegalArgumentException> { k(Double.NaN) }
  }

  @Test
  fun `binary operators render infix`() {
    assertEquals("A2+B2", excel(Expr.Plus(ra, rb)))
    assertEquals("A2-B2", excel(Expr.Minus(ra, rb)))
    assertEquals("A2*B2", excel(Expr.Times(ra, rb)))
    assertEquals("A2/B2", excel(Expr.Div(ra, rb)))
  }

  @Test
  fun `comparisons render with Excel's symbols`() {
    val symbols = listOf(Greater, GreaterOrEqual, Less, LessOrEqual, Equal, NotEqual)
      .map { excel(Expr.Compare(ra, it, rb)) }
    assertEquals(listOf("A2>B2", "A2>=B2", "A2<B2", "A2<=B2", "A2=B2", "A2<>B2"), symbols)
  }

  @Test
  fun `functions render as calls`() {
    assertEquals("SUM(A2, B2, C2)", excel(sum(ra, rb, rc)))
    assertEquals("SUM(A2*B2, 3)", excel(sum(Expr.Times(ra, rb), k(3.0))))
    assertEquals(
      listOf("AVERAGE(A2)", "MIN(A2)", "MAX(A2)", "COUNTA(A2)"),
      listOf(Average, Min, Max, Count).map { excel(Expr.Aggregate(it, listOf(ra))) },
    )
    assertEquals("ROUND(A2*1.21, 2)", excel(Expr.Round(Expr.Times(ra, k(1.21)), 2)))
    assertEquals("ROUND(A2, -2)", excel(Expr.Round(ra, -2)))
    assertEquals("IF(A2>B2, A2, 0)", excel(Expr.If(Expr.Compare(ra, Greater, rb), ra, k(0.0))))
  }

  @Test
  fun `an aggregate needs a term`() {
    assertFailsWith<IllegalArgumentException> { Expr.Aggregate(Sum, emptyList()) }
  }

  @Test
  fun `parentheses only where precedence needs them`() {
    assertEquals("A2*B2*C2", excel(Expr.Times(Expr.Times(ra, rb), rc)))
    assertEquals("A2*(B2*C2)", excel(Expr.Times(ra, Expr.Times(rb, rc))))
    assertEquals("(A2+B2)*C2", excel(Expr.Times(Expr.Plus(ra, rb), rc)))
    assertEquals("A2+B2*C2", excel(Expr.Plus(ra, Expr.Times(rb, rc))))
    assertEquals("A2-B2-C2", excel(Expr.Minus(Expr.Minus(ra, rb), rc)))
    assertEquals("A2-(B2-C2)", excel(Expr.Minus(ra, Expr.Minus(rb, rc))))
    assertEquals("A2/(B2*C2)", excel(Expr.Div(ra, Expr.Times(rb, rc))))
    assertEquals("A2+B2>C2", excel(Expr.Compare(Expr.Plus(ra, rb), Greater, rc)))
    assertEquals("IF(A2>B2, 1, 0)*C2", excel(Expr.Times(Expr.If(Expr.Compare(ra, Greater, rb), k(1.0), k(0.0)), rc)))
    assertEquals("SUM(A2+B2)*2", excel(Expr.Times(sum(Expr.Plus(ra, rb)), k(2.0))))
  }

  @Test
  fun `evaluate computes from the referenced values`() {
    val row = mapOf(a to 3, b to 4.5, c to 2L)
    assertEquals(3.0, ra.computed(row))
    assertEquals(7.5, Expr.Plus(ra, rb).computed(row))
    assertEquals(-1.5, Expr.Minus(ra, rb).computed(row))
    assertEquals(13.5, Expr.Times(ra, rb).computed(row))
    assertEquals(1.5, Expr.Div(ra, rc).computed(row))
    assertEquals(1.21, k(1.21).computed(row))
  }

  @Test
  fun `comparisons evaluate to booleans`() {
    val row = mapOf(a to 3, b to 4)
    fun compare(comparison: Comparison, left: Expr<*>, right: Expr<*>) = Expr.Compare(left, comparison, right).computed(row)
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
    assertEquals(4.0, Expr.Aggregate(Average, listOf(ra, rb, rc)).computed(row))
    assertEquals(3.0, Expr.Aggregate(Min, listOf(ra, rb, rc)).computed(row))
    assertEquals(5.0, Expr.Aggregate(Max, listOf(ra, rb, rc)).computed(row))
    assertEquals(2.0, Expr.Aggregate(Count, listOf(ra, rb, rc)).computed(row))
  }

  @Test
  fun `aggregates of nothing`() {
    val row = mapOf(a to null)
    assertEquals(0.0, sum(ra).computed(row))
    assertEquals(ExcelError.DivisionByZero, Expr.Aggregate(Average, listOf(ra)).computed(row))
    assertEquals(0.0, Expr.Aggregate(Min, listOf(ra)).computed(row))
    assertEquals(0.0, Expr.Aggregate(Max, listOf(ra)).computed(row))
    assertEquals(0.0, Expr.Aggregate(Count, listOf(ra)).computed(row))
  }

  @Test
  fun `round goes half away from zero`() {
    fun round(value: Double, digits: Int) = Expr.Round(k(value), digits).computed(emptyMap())
    assertEquals(3.0, round(2.5, 0))
    assertEquals(-3.0, round(-2.5, 0))
    assertEquals(2.68, round(2.675, 2))
    assertEquals(1200.0, round(1249.0, -2))
    assertEquals(ExcelError.DivisionByZero, Expr.Round(Expr.Div(k(1.0), k(0.0)), 2).computed(emptyMap()))
  }

  @Test
  fun `if picks a branch on any non-zero condition`() {
    fun pick(condition: Double) = Expr.If(Expr.Compare(k(condition), NotEqual, k(0.0)), k(1.0), k(2.0)).computed(emptyMap())
    assertEquals(listOf(1.0, 2.0, 1.0), listOf(pick(1.0), pick(0.0), pick(-0.5)))
  }

  @Test
  fun `if only evaluates the branch it picks`() {
    val missing = Column<Number>("missing", 9, Unit)
    assertEquals(1.0, Expr.If(Expr.Bool(true), k(1.0), Expr.Ref(missing)).computed(emptyMap()))
  }

  @Test
  fun `a null value counts as zero in arithmetic, like an empty cell`() {
    assertEquals(4.0, Expr.Plus(ra, rb).computed(mapOf(a to null, b to 4)))
  }

  @Test
  fun `a missing column fails`() {
    assertFailsWith<NoSuchElementException> { rb.computed(mapOf(a to 1)) }
  }

  @Test
  fun `a range cannot be evaluated for one row`() {
    assertFailsWith<IllegalArgumentException> { sum(Expr.Range(a)).computed(mapOf(a to 1)) }
  }

  @Test
  fun `division by zero produces a spreadsheet error`() {
    assertEquals(ExcelError.DivisionByZero, Expr.Div(ra, rb).computed(mapOf(a to 1, b to 0)))
    assertEquals(ExcelError.DivisionByZero, Expr.Div(ra, rb).computed(mapOf(a to 0, b to 0)))
  }

  @Test
  fun `trees compare structurally`() {
    assertEquals(Expr.Times(ra, k(2.0)), Expr.Times(Expr.Ref(a), Expr.Constant(2.0)))
  }
}

private fun Expr<*>.computed(row: Map<out Column<*>, Any?>): Any? = when (val result = evaluate(row)) {
  is CellResult.Value -> result.value
  is CellResult.Error -> result.error
}
