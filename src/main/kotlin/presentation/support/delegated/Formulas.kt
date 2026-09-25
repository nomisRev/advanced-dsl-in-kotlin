package presentation.support.delegated

import presentation.support.Cell
import presentation.support.Column
import presentation.support.reference

/*
 * Parts 3 and 4: `*` is an extension declared inside `Formulas`, so it only
 * exists where a `Formulas` is the receiver, and each implementation of the
 * interface gives it another meaning.
 */

interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
  operator fun Column<Number>.times(factor: Double): Cell
}

/** Writes an Excel formula for [row], the 1-based row number Excel shows. */
class ExcelFormulas(private val row: Int) : Formulas {
  override fun Column<Number>.times(other: Column<Number>): Cell =
    Cell.Formula("=${reference(row)}*${other.reference(row)}")

  override fun Column<Number>.times(factor: Double): Cell =
    Cell.Formula("=${reference(row)}*$factor")
}

/** Computes the value from the row's numbers: CSV export, previews, tests. */
class Evaluate(private val row: Map<Column<*>, Number>) : Formulas {
  override fun Column<Number>.times(other: Column<Number>): Cell =
    Cell.Value(
      row.getValue(this).toDouble() * row.getValue(other).toDouble(),
    )

  override fun Column<Number>.times(factor: Double): Cell =
    Cell.Value(row.getValue(this).toDouble() * factor)
}
