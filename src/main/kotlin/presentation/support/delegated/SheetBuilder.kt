package presentation.support.delegated

import presentation.support.Cell
import presentation.support.ColumnDelegate
import presentation.support.DelegatedColumns
import presentation.support.ExcelDsl
import presentation.support.Invoice
import presentation.support.writeWorkbook

@ExcelDsl
class SheetBuilder<T> : DelegatedColumns<T>() {
  fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> =
    formulaColumn { row -> block(ExcelFormulas(row)) }
}

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) = writeWorkbook(path, rows, SheetBuilder<T>().apply(block))

/*
 * The invoice sheet the slides extend outside a `generateExcel { }` block,
 * such as `val gross by formula { withVat(total) }`: its columns and a
 * `formula` that adds to it.
 */

val invoiceSheet = SheetBuilder<Invoice>()

val customer by invoiceSheet.column { it.customer }
val hours by invoiceSheet.column { it.hours }
val rate by invoiceSheet.column { it.rate }
val total by invoiceSheet.formula { hours * rate }

fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> =
  invoiceSheet.formula(block)
