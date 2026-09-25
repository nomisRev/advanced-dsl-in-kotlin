package excel.dsl

import kotlin.test.Test
import kotlin.test.assertEquals

class BooleanFormulaTest {
  @Test
  fun `comparisons preserve booleans and handle blank operands like Excel`() {
    for (comparison in Comparison.entries) {
      val sheet = sheet<Int?> {
        val value by column { it }
        val flag by formula { value gt 0 }
        val other by formula { value lt 0 }
        val sameType by formula { Expr.Compare(Expr.Ref(flag), comparison, Expr.Ref(other)) }
        val mixed by formula { Expr.Compare(Expr.Ref(flag), comparison, Expr.Ref(value)) }
        val reversed by formula { Expr.Compare(Expr.Ref(value), comparison, Expr.Ref(flag)) }
        val inline by formula { Expr.Compare(value gt 0, comparison, Expr.Constant(1.0)) }
      }
      assertExcelAgrees(sheet, listOf(null, -1, 0, 1, 2))
    }
  }

  @Test
  fun `conditional booleans keep their types through references arithmetic and aggregates`() {
    val sheet = sheet<Int> {
      val value by column { it }
      val flag by formula { value gt 0 then (value gt 1) otherwise (value lt 0) }
      val alias by formula { Expr.Ref(flag) }
      val nested by formula { value gt 0 then (flag then alias otherwise flag) otherwise alias }
      val mixed by formula { value gt 0 then flag otherwise 5 }
      val arithmetic by formula { flag + 1 }
      val sum by formula { sum([flag, alias, nested, mixed]) }
      val direct by formula { Expr.Aggregate(AggregateFunction.Sum, listOf(value gt 0)) }
      totals {
        flag.sum()
        alias.sum()
        nested.sum()
        mixed.sum()
      }
    }
    assertEquals(true, sheet.evaluate(2)["Flag"])
    assertEquals(false, sheet.evaluate(1)["Flag"])
    assertEquals(0.0, sheet.totals(sequenceOf(-1, 0, 1, 2))["Flag"])
    assertExcelAgrees(sheet, listOf(-1, 0, 1, 2))
  }

  @Test
  fun `conditional totals preserve Boolean results`() {
    val sheet = sheet<Int> {
      val value by column { it }
      totals { value.total { sum(value) gt 0 then (sum(value) gt 1) otherwise (sum(value) lt 0) } }
    }
    for (value in listOf(-1, 0, 1, 2)) assertExcelAgrees(sheet, listOf(value))
  }

  @Test
  fun `standalone evaluation still returns doubles for Boolean results`() {
    val yes = Expr.Compare(Expr.Constant(1.0), Comparison.Equal, Expr.Constant(1.0))
    assertEquals(1.0, Expr.If(yes, yes, Expr.Constant(0.0)).evaluate(emptyMap()))
    assertEquals(0.0, Expr.Compare(yes, Comparison.Equal, Expr.Constant(1.0)).evaluate(emptyMap()))
  }
}
