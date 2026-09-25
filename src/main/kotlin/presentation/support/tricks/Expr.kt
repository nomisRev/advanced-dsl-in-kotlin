package presentation.support.tricks

import presentation.support.Column
import presentation.support.reference

/*
 * Extra: DSL tricks. The expression tree of Part 6, grown by a comparison
 * that `>` cannot build, so `infix fun gt` builds it instead.
 */

sealed interface Expr {
  data class Ref(val column: Column<Number>) : Expr
  data class Constant(val value: Double) : Expr
  data class Times(val left: Expr, val right: Expr) : Expr
  data class Greater(val left: Expr, val right: Expr) : Expr
}

fun Expr.toExcel(row: Int): String = when (this) {
  is Expr.Ref -> column.reference(row)
  is Expr.Constant -> "$value"
  is Expr.Times -> "${left.toExcel(row)}*${right.toExcel(row)}"
  is Expr.Greater -> "${left.toExcel(row)}>${right.toExcel(row)}"
}

/** The scope of `formula { }`, only brings the operators below into scope. */
interface Formulas

context(_: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Expr =
  Expr.Times(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.gt(limit: Double): Expr =
  Expr.Greater(Expr.Ref(this), Expr.Constant(limit))
