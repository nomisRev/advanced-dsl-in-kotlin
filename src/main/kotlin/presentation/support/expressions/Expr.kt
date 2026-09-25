package presentation.support.expressions

import presentation.support.Column
import presentation.support.reference

/*
 * The end of Part 6: operators build a tree instead of a `Cell`, so formulas
 * compose (`hours * rate * 1.21`), the tree is built once per column, and
 * rendering it for a row, or evaluating it, is a plain `when`.
 */

sealed interface Expr {
  data class Ref(val column: Column<Number>) : Expr
  data class Constant(val value: Double) : Expr
  data class Times(val left: Expr, val right: Expr) : Expr
  data class Sum(val terms: List<Expr>) : Expr
}

/** The formula text of [row], the 1-based row number Excel shows, without `=`. */
fun Expr.toExcel(row: Int): String = when (this) {
  is Expr.Ref -> column.reference(row)
  is Expr.Constant -> "$value"
  is Expr.Times -> "${left.toExcel(row)}*${right.toExcel(row)}"
  is Expr.Sum -> terms.joinToString(", ", "SUM(", ")") { it.toExcel(row) }
}

/** The value of this formula for one row: CSV export, previews, tests. */
fun Expr.evaluate(row: Map<Column<*>, Number>): Double = when (this) {
  is Expr.Ref -> row.getValue(column).toDouble()
  is Expr.Constant -> value
  is Expr.Times -> left.evaluate(row) * right.evaluate(row)
  is Expr.Sum -> terms.sumOf { it.evaluate(row) }
}
