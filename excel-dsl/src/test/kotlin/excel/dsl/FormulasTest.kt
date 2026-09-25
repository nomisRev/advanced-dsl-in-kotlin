package excel.dsl

import excel.dsl.Comparison.Equal
import excel.dsl.Comparison.Greater
import excel.dsl.Comparison.GreaterOrEqual
import excel.dsl.Comparison.Less
import excel.dsl.Comparison.LessOrEqual
import excel.dsl.Comparison.NotEqual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** A helper written the way users write their own: a context, not a member. */
context(_: Formulas)
fun Column<Number>.withVat(): Expr<Double> = this * 1.21

/** One that works in a row and in a total, since it only needs arithmetic. */
context(_: Arithmetic)
fun Expr<Number>.withVat(): Expr<Double> = this * 1.21

class FormulasTest {
  private val a = Column<Number>("a", 0, Unit)
  private val b = Column<Number>("b", 1, Unit)
  private val ra = Expr.Ref(a)
  private val rb = Expr.Ref(b)
  private val two = Expr.Constant(2.0)

  private fun <V> formula(block: Formulas.() -> Expr<V>): Expr<V> = FormulaScope.block()
  private fun <V> total(block: Totals.() -> Expr<V>): Expr<V> = TotalsScope.block()

  @Test
  fun `every operand pairing builds the same node`() {
    val cases = listOf(
      Triple(Expr::Plus, formula { ra + rb }, listOf(formula { a + b }, formula { ra + b }, formula { a + rb })),
      Triple(Expr::Minus, formula { ra - rb }, listOf(formula { a - b }, formula { ra - b }, formula { a - rb })),
      Triple(Expr::Times, formula { ra * rb }, listOf(formula { a * b }, formula { ra * b }, formula { a * rb })),
      Triple(Expr::Div, formula { ra / rb }, listOf(formula { a / b }, formula { ra / b }, formula { a / rb })),
    )
    for ((node, exprs, columns) in cases) {
      assertEquals(node(ra, rb), exprs)
      columns.forEach { assertEquals(node(ra, rb), it) }
    }
  }

  @Test
  fun `every comparison has every operand pairing`() {
    val cases = mapOf(
      Greater to listOf(formula { a gt b }, formula { ra gt rb }, formula { ra gt b }, formula { a gt rb }),
      GreaterOrEqual to listOf(formula { a ge b }, formula { ra ge rb }, formula { ra ge b }, formula { a ge rb }),
      Less to listOf(formula { a lt b }, formula { ra lt rb }, formula { ra lt b }, formula { a lt rb }),
      LessOrEqual to listOf(formula { a le b }, formula { ra le rb }, formula { ra le b }, formula { a le rb }),
      Equal to listOf(formula { a eq b }, formula { ra eq rb }, formula { ra eq b }, formula { a eq rb }),
      NotEqual to listOf(formula { a ne b }, formula { ra ne rb }, formula { ra ne b }, formula { a ne rb }),
    )
    for ((comparison, exprs) in cases) exprs.forEach { assertEquals(Expr.Compare(ra, comparison, rb), it) }
    assertEquals(Expr.Compare(ra, Greater, two), formula { a gt 2 })
    assertEquals(Expr.Compare(ra, NotEqual, two), formula { ra ne 2 })
  }

  @Test
  fun `any number becomes a double constant`() {
    assertEquals(Expr.Plus(ra, two), formula { a + 2 })
    assertEquals(Expr.Minus(ra, two), formula { a - 2L })
    assertEquals(Expr.Times(ra, two), formula { a * 2.0 })
    assertEquals(Expr.Div(ra, two), formula { a / 2.0f })
    assertEquals(Expr.Times(ra, two), formula { a * 2.toBigDecimal() })
    assertEquals(Expr.Plus(ra, two), formula { ra + 2 })
    assertEquals(Expr.Minus(ra, two), formula { ra - 2 })
    assertEquals(Expr.Times(ra, two), formula { ra * 2 })
    assertEquals(Expr.Div(ra, two), formula { ra / 2 })
  }

  @Test
  fun `operators nest left to right`() {
    assertEquals(Expr.Times(Expr.Times(ra, rb), Expr.Constant(1.21)), formula { a * b * 1.21 })
    assertEquals(Expr.Plus(ra, Expr.Times(rb, two)), formula { a + b * 2 })
  }

  @Test
  fun `row aggregates reference every column`() {
    assertEquals(Expr.Aggregate(AggregateFunction.Sum, listOf(ra, rb)), formula { sum(listOf(a, b)) })
    assertEquals(Expr.Aggregate(AggregateFunction.Sum, listOf(ra, rb)), formula { sum([a, b]) })
    assertEquals(Expr.Aggregate(AggregateFunction.Average, listOf(ra, rb)), formula { average([a, b]) })
    assertEquals(Expr.Aggregate(AggregateFunction.Min, listOf(ra, rb)), formula { min([a, b]) })
    assertEquals(Expr.Aggregate(AggregateFunction.Max, listOf(ra, rb)), formula { max([a, b]) })
  }

  @Test
  fun `a row aggregate of no columns fails`() {
    assertFailsWith<IllegalArgumentException> { formula { sum(emptyList()) } }
  }

  @Test
  fun `round takes a column or an expression`() {
    assertEquals(Expr.Round(ra, 0), formula { round(a) })
    assertEquals(Expr.Round(Expr.Times(ra, two), 2), formula { round(a * 2, 2) })
  }

  @Test
  fun `then and otherwise build an if`() {
    val condition = Expr.Compare(ra, Greater, Expr.Constant(1000.0))
    assertEquals(
      Expr.If(condition, Expr.Times(ra, Expr.Constant(0.9)), ra),
      formula { a gt 1000 then a * 0.9 otherwise a },
    )
    assertEquals(Expr.If(condition, two, Expr.Constant(0.0)), formula { a gt 1000 then 2 otherwise 0 })
    assertEquals(Expr.If(condition, rb, ra), formula { a gt 1000 then b otherwise a })
    assertEquals(Expr.If(condition, rb, two), formula { a gt 1000 then b otherwise two })
  }

  @Test
  fun `totals aggregate whole columns`() {
    val text = Column<String>("t", 2, Unit)
    fun aggregate(function: AggregateFunction, column: Column<*>) =
      Expr.Aggregate(function, listOf(Expr.Range(column)))
    assertEquals(aggregate(AggregateFunction.Sum, a), total { sum(a) })
    assertEquals(aggregate(AggregateFunction.Average, a), total { average(a) })
    assertEquals(aggregate(AggregateFunction.Min, a), total { min(a) })
    assertEquals(aggregate(AggregateFunction.Max, a), total { max(a) })
    assertEquals(aggregate(AggregateFunction.Count, text), total { count(text) })
  }

  @Test
  fun `totals share the arithmetic`() {
    assertEquals(
      Expr.Round(Expr.Times(Expr.Aggregate(AggregateFunction.Sum, listOf(Expr.Range(a))), Expr.Constant(1.21)), 2),
      total { round(sum(a).withVat(), 2) },
    )
    assertEquals(
      Expr.If(Expr.Compare(Expr.Aggregate(AggregateFunction.Max, listOf(Expr.Range(a))), Greater, two), two, two),
      total { max(a) gt 2 then 2 otherwise 2 },
    )
  }

  @Test
  fun `user helpers compose through the context`() {
    assertEquals(Expr.Times(ra, Expr.Constant(1.21)), formula { a.withVat() })
    assertEquals(Expr.Times(Expr.Times(ra, rb), Expr.Constant(1.21)), formula { (a * b).withVat() })
  }
}
