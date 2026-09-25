package excel.dsl

import kotlin.test.Test
import kotlin.test.assertEquals

class BooleanFormulaTest {
  @Test
  fun `comparisons and boolean formulas retain their types`() {
    val schema = sheet<Int?> {
      val value by column { it }
      val flag: Column<Boolean> by formula { value gt 0 }
      val other: Column<Boolean> by formula { value lt 0 }
      val same by formula { flag eq other }
      val alias: Column<Boolean> by formula { flag.ref() }
      val nested by formula { flag then alias otherwise other }
      val numeric by formula { flag.asNumber() }
      val arithmetic by formula { numeric + 1 }
      totals { numeric.sum(); flag.count() }
    }
    assertEquals(true, schema.evaluate(2)["Flag"])
    assertEquals(false, schema.evaluate(null)["Flag"])
    assertEquals(2.0, schema.totals(listOf(-1, 0, 1, 2))["Numeric"])
    assertExcelAgrees(schema, listOf(null, -1, 0, 1, 2))
  }

  @Test
  fun `boolean data columns support conditions and logical operations including blanks`() {
    val schema = sheet<Boolean?> {
      val flag by column { it }
      val alias by formula { flag.ref() }
      val inverse by formula { !flag }
      val both by formula { flag and inverse }
      val either by formula { flag or inverse }
      val picked by formula { flag then 10 otherwise 20 }
    }
    assertExcelAgrees(schema, listOf(null, false, true))
  }

  @Test
  fun `conditional totals preserve Boolean results`() {
    val schema = sheet<Int> {
      val value by column { it }
      totals { value.total { sum(value) gt 0 then (sum(value) gt 1) otherwise (sum(value) lt 0) } }
    }
    for (value in listOf(-1, 0, 1, 2)) assertExcelAgrees(schema, listOf(value))
  }

  @Test
  fun `standalone evaluation preserves booleans`() {
    val yes = Expr.Compare(Expr.Constant(1.0), Comparison.Equal, Expr.Constant(1.0))
    assertEquals(CellResult.Value(true), Expr.If(yes, yes, Expr.Bool(false)).evaluate(emptyMap()))
  }
}
