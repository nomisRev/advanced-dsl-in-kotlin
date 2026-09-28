package presentation.support.contextual

import presentation.support.Cell
import presentation.support.ColumnDelegate
import presentation.support.DelegatedColumns
import presentation.support.ExcelDsl
import presentation.support.contexts.ExcelFormulas
import presentation.support.contexts.Formulas
import presentation.support.writeWorkbook

/*
 * Extra "What the talk leaves out": `formula { }` takes a context lambda
 * instead of a receiver lambda. Part 5's operators and `withVat()` still
 * resolve, `Formulas`' own members no longer do.
 */

@ExcelDsl
class SheetBuilder<T> : DelegatedColumns<T>() {
  fun formula(block: context(Formulas) () -> Cell): ColumnDelegate<Number> =
    formulaColumn { row -> block(ExcelFormulas(row)) }
}

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) = writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
