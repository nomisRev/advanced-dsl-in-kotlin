package presentation.support.varargs

import presentation.support.Cell
import presentation.support.Column
import presentation.support.ColumnDelegate
import presentation.support.DelegatedColumns
import presentation.support.ExcelDsl
import presentation.support.Timesheet
import presentation.support.sumFormula
import presentation.support.writeWorkbook
import presentation.support.contexts.ExcelFormulas as MultiplyExcelFormulas
import presentation.support.contexts.Formulas as MultiplyFormulas

/* Part 6: the formulas of Part 5, plus a `sum` over any number of columns. */

interface Formulas : MultiplyFormulas {
  fun sum(vararg columns: Column<Number>): Cell
}

class ExcelFormulas(private val row: Int) :
  Formulas,
  MultiplyFormulas by MultiplyExcelFormulas(row) {
  override fun sum(vararg columns: Column<Number>): Cell =
    sumFormula(row, columns.asList())
}

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

/* The timesheet the slides sum with `val subtotal by formula { }`. */

val timesheetSheet = SheetBuilder<Timesheet>()

val customer by timesheetSheet.column { it.customer }
val hours by timesheetSheet.column { it.hours }
val overtime by timesheetSheet.column { it.overtime }

fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> =
  timesheetSheet.formula(block)
