package presentation.support.tricks

import presentation.support.Column
import presentation.support.reference

/*
 * Extra: DSL tricks. The expression tree of Part 6, grown by a comparison
 * that `>` cannot build, so `infix fun gt` builds it instead.
 */

sealed interface Formula {
  data class Ref(val column: Column<Number>) : Formula
  data class Constant(val value: Double) : Formula
  data class Times(val left: Formula, val right: Formula) : Formula
  data class Greater(val left: Formula, val right: Formula) : Formula
}

fun Formula.toExcel(row: Int): String = when (this) {
  is Formula.Ref -> column.reference(row)
  is Formula.Constant -> "$value"
  is Formula.Times -> "${left.toExcel(row)}*${right.toExcel(row)}"
  is Formula.Greater -> "${left.toExcel(row)}>${right.toExcel(row)}"
}

/** The scope of `formula { }`, only brings the operators below into scope. */
interface Formulas

context(_: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Formula =
  Formula.Times(Formula.Ref(this), Formula.Ref(other))

context(_: Formulas)
infix fun Column<Number>.gt(limit: Double): Formula =
  Formula.Greater(Formula.Ref(this), Formula.Constant(limit))
