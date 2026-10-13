package excel.dsl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FormulaLimitsTest {
  private val oversizedText = "x".repeat(256)

  private fun rejectsText(block: () -> Unit) {
    assertEquals(
      "Excel formula text literals hold at most 255 characters; got 256",
      assertFailsWith<IllegalArgumentException>(block = block).message,
    )
  }

  @Test
  fun `named and delegated formulas reject oversized text while building the schema`() {
    rejectsText { sheet<Int> { formula("Text") { literal(oversizedText) } } }
    rejectsText { sheet<Int> { val text by formula { literal(oversizedText) } } }
  }

  @Test
  fun `comparisons and both conditional branches validate implicit text literals`() {
    rejectsText {
      sheet<String> {
        val text by column { it }
        val matches by formula { text eq oversizedText }
      }
    }
    rejectsText {
      sheet<String> {
        val text by column { it }
        val matches by formula { text.ref() lt oversizedText }
      }
    }
    rejectsText {
      sheet<Boolean> {
        val flag by column { it }
        val text by formula { flag then oversizedText otherwise "short" }
      }
    }
    rejectsText {
      sheet<Boolean> {
        val flag by column { it }
        val text by formula { flag.ref() then "short" otherwise oversizedText }
      }
    }
  }

  @Test
  fun `totals validate explicit and implicit text literals`() {
    rejectsText {
      sheet<String> {
        val text by column { it }
        totals { text.total { literal(oversizedText) } }
      }
    }
    rejectsText {
      sheet<String> {
        val text by column { it }
        totals { text.total { countNonBlank(text) gt 0 then oversizedText otherwise "short" } }
      }
    }
    rejectsText {
      sheet<String> {
        val text by column { it }
        totals { text.total { countNonBlank(text) gt 0 then "short" otherwise oversizedText } }
      }
    }
    rejectsText {
      sheet<Boolean> {
        val flag by column { it }
        totals { flag.total { literal("short") eq oversizedText } }
      }
    }
  }

  @Test
  fun `255 character text literals including escaped quotes recalculate in rows and totals`() {
    for (text in listOf("x".repeat(255), "\"".repeat(255))) {
      val schema = sheet<Boolean> {
        val flag by column { it }
        val delegated by formula { literal(text) }
        val named = formula("Named") { flag then text otherwise "short" }
        val matches by formula { delegated eq text }
        totals { named.total { literal(text) } }
      }
      assertEquals(text, schema.evaluate(true)["Named"])
      assertEquals(text, schema.totals(listOf(true))["Named"])
      assertExcelAgrees(schema, listOf(false, true))
    }
  }

  @Test
  fun `long data text and aliases retain the cell text limit`() {
    val text = "x".repeat(32_767)
    val schema = sheet<String> {
      val data by column { it }
      val alias by formula { data.ref() }
    }
    assertEquals(text, schema.evaluate(text)["Alias"])
    assertExcelAgrees(schema, listOf(text))
  }

  private fun Formulas.aggregate(function: String, columns: List<Column<Number>>): Expr<Double> = when (function) {
    "SUM" -> sum(columns)
    "AVERAGE" -> average(columns)
    "MIN" -> min(columns)
    "MAX" -> max(columns)
    else -> error("Unknown aggregate: $function")
  }

  @Test
  fun `all row aggregates reject 256 arguments for named and delegated formulas`() {
    for (function in listOf("SUM", "AVERAGE", "MIN", "MAX")) {
      for (delegated in listOf(false, true)) {
        val failure = assertFailsWith<IllegalArgumentException> {
          sheet<Int> {
            val columns = List(256) { index -> "N$index" { it + index } }
            if (delegated) {
              val result by formula { aggregate(function, columns) }
            } else {
              formula("Result") { aggregate(function, columns) }
            }
          }
        }
        assertEquals("$function accepts at most 255 arguments; got 256", failure.message)
      }
    }
  }

  @Test
  fun `all row aggregates recalculate at the 255 argument boundary`() {
    val schema = sheet<Int> {
      val columns = List(255) { index -> "N$index" { it + index } }
      val total by formula { sum(columns) }
      formula("Average") { average(columns) }
      val minimum by formula { min(columns) }
      formula("Maximum") { max(columns) }
    }
    val row = schema.evaluate(1)
    assertEquals(32_640.0, row["Total"])
    assertEquals(128.0, row["Average"])
    assertEquals(1.0, row["Minimum"])
    assertEquals(255.0, row["Maximum"])
    assertExcelAgrees(schema, listOf(0, 1))
  }

  @Test
  fun `totals ranges may contain more than 255 rows`() {
    val schema = sheet<Int> {
      val n by column { it }
      totals { n.sum() }
    }
    val rows = (1..256).toList()
    assertEquals(32_896.0, schema.totals(rows)["N"])
    assertExcelAgrees(schema, rows)
  }
}
