package presentation.support.contexts

import presentation.support.Cell
import presentation.support.ColumnDelegate
import presentation.support.DelegatedColumns
import presentation.support.ExcelDsl
import presentation.support.writeWorkbook

@ExcelDsl
class SheetBuilder<T> : DelegatedColumns<T>() {
  fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> =
    formulaColumn { row -> ExcelFormulas(row).block() }
}

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) = writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
