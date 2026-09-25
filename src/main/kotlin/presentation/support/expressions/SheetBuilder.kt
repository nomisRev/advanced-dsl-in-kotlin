package presentation.support.expressions

import presentation.support.Cell
import presentation.support.ColumnDelegate
import presentation.support.DelegatedColumns
import presentation.support.ExcelDsl
import presentation.support.writeWorkbook

@ExcelDsl
class SheetBuilder<T> : DelegatedColumns<T>() {
  /** Builds the tree once, here; every row only renders it. */
  fun formula(block: Formulas.() -> Expr): ColumnDelegate<Number> {
    val expr = Scope.block()
    return formulaColumn { row -> Cell.Formula("=${expr.toExcel(row)}") }
  }

  private object Scope : Formulas
}

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) = writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
