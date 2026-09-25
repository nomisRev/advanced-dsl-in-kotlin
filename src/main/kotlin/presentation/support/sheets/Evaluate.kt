package presentation.support.sheets

import presentation.support.Cell
import presentation.support.Column

/**
 * The second interpretation of [Formulas], as in Part 4: computes a value
 * from the row's [values] so far, indexed like the columns.
 */
class Evaluate(private val values: List<Any?>) : Formulas {
  override fun multiply(column: Column<Number>, other: Column<Number>): Cell =
    Cell.Value(number(column) * number(other))

  override fun multiply(column: Column<Number>, factor: Double): Cell =
    Cell.Value(number(column) * factor)

  override fun sum(columns: List<Column<Number>>): Cell =
    Cell.Value(columns.sumOf(::number))

  private fun number(column: Column<Number>): Double =
    (values[column.index] as Number).toDouble()
}
