package presentation.support.contexts

import presentation.support.Cell
import presentation.support.Column
import presentation.support.reference

/*
 * Part 5: the interface keeps plain functions and the operators take it as a
 * context, so users can write helpers such as `withVat` as `total.withVat()`.
 */

interface Formulas {
  fun multiply(column: Column<Number>, other: Column<Number>): Cell
  fun multiply(column: Column<Number>, factor: Double): Cell
}

context(formulas: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Cell =
  formulas.multiply(this, other)

context(formulas: Formulas)
operator fun Column<Number>.times(factor: Double): Cell =
  formulas.multiply(this, factor)

context(formulas: Formulas)
fun Column<Number>.withVat(): Cell = this * 1.21

class ExcelFormulas(private val row: Int) : Formulas {
  override fun multiply(column: Column<Number>, other: Column<Number>): Cell =
    Cell.Formula("=${column.reference(row)}*${other.reference(row)}")

  override fun multiply(column: Column<Number>, factor: Double): Cell =
    Cell.Formula("=${column.reference(row)}*$factor")
}

class Evaluate(private val row: Map<Column<*>, Number>) : Formulas {
  override fun multiply(column: Column<Number>, other: Column<Number>): Cell =
    Cell.Value(value(column) * value(other))

  override fun multiply(column: Column<Number>, factor: Double): Cell =
    Cell.Value(value(column) * factor)

  private fun value(column: Column<*>): Double =
    row.getValue(column).toDouble()
}
