package presentation.support.lists

import presentation.support.Cell
import presentation.support.Column
import presentation.support.ColumnDelegate
import presentation.support.DelegatedColumns
import presentation.support.ExcelDsl
import presentation.support.Timesheet
import presentation.support.sumFormula
import presentation.support.writeWorkbook
import kotlin.text.replaceFirstChar
import presentation.support.contexts.ExcelFormulas as MultiplyExcelFormulas
import presentation.support.contexts.Formulas as MultiplyFormulas

/*
 * The DSL at the end of Part 6: `sum` takes a `List`, so a collection literal
 * or an existing list passes without a copy, `generateExcel` is `inline`, and
 * the named `column` of Part 1 is hidden but still in the binary.
 */

interface Formulas : MultiplyFormulas {
  fun sum(columns: List<Column<Number>>): Cell
}

class ExcelFormulas(private val row: Int) :
  Formulas,
  MultiplyFormulas by MultiplyExcelFormulas(row) {
  override fun sum(columns: List<Column<Number>>): Cell =
    sumFormula(row, columns)
}

@ExcelDsl
class SheetBuilder<T> : DelegatedColumns<T>() {
  @Deprecated(
    message = "Use a delegated column: val name by column { }",
    level = DeprecationLevel.HIDDEN,
  )
  fun column(name: String, bold: Boolean = false, value: (T) -> Any?) {
    addColumn(name, bold) { row, _ -> value(row) }
  }

  fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> =
    formulaColumn { row -> ExcelFormulas(row).block() }
}

data class ExcelConfig(val columnNameStyle: (String) -> String)

companion val ExcelConfig.Default =
  ExcelConfig { it.replaceFirstChar(Char::uppercase) }

inline fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  config: ExcelConfig = ExcelConfig.Default,
  block: SheetBuilder<T>.() -> Unit,
) = writeWorkbook(path, rows, SheetBuilder<T>().apply(block))

/* The timesheet the slides sum with `val subtotal by formula { }`. */

val timesheetSheet = SheetBuilder<Timesheet>()

val customer by timesheetSheet.column { it.customer }
val hours by timesheetSheet.column { it.hours }
val overtime by timesheetSheet.column { it.overtime }

fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> =
  timesheetSheet.formula(block)
