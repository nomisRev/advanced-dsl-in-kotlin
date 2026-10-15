package presentation.support.expressions

import presentation.support.Column
import presentation.support.reference

/*
 * The end of Part 6: operators build a tree instead of a `Cell`, so formulas
 * compose (`hours * rate * 1.21`), the tree is built once per column, and
 * rendering it for a row, or evaluating it, is a plain `when`.
 */

sealed interface Formula {
  data class Ref(val column: Column<Number>) : Formula
  data class Constant(val value: Double) : Formula
  data class Times(val left: Formula, val right: Formula) : Formula
  data class Sum(val terms: List<Formula>) : Formula
}

/** The formula text of [row], the 1-based row number Excel shows, without `=`. */
fun Formula.toExcel(row: Int): String = when (this) {
  is Formula.Ref -> column.reference(row)
  is Formula.Constant -> "$value"
  is Formula.Times -> "${left.toExcel(row)}*${right.toExcel(row)}"
  is Formula.Sum -> terms.joinToString(", ", "SUM(", ")") { it.toExcel(row) }
}

/** The value of this formula for one row: CSV export, previews, tests. */
fun Formula.evaluate(row: Map<Column<*>, Number>): Double = when (this) {
  is Formula.Ref -> row.getValue(column).toDouble()
  is Formula.Constant -> value
  is Formula.Times -> left.evaluate(row) * right.evaluate(row)
  is Formula.Sum -> terms.sumOf { it.evaluate(row) }
}
