package presentation.support.marked

import org.apache.poi.ss.usermodel.CellStyle
import presentation.support.ExcelDsl
import presentation.support.NamedColumns
import presentation.support.WorkbookSheets
import presentation.support.writeWorkbook

/*
 * The workbook DSL of Part 1 with `@DslMarker`: only the closest receiver is
 * implicit, also inside `header { }` on Apache POI's `CellStyle`.
 */

@ExcelDsl
class WorkbookBuilder : WorkbookSheets() {
  fun <T> sheet(
    name: String,
    rows: Sequence<T>,
    block: SheetBuilder<T>.() -> Unit,
  ) = addSheet(name, rows, SheetBuilder<T>().apply(block))
}

@ExcelDsl
class SheetBuilder<T> : NamedColumns<T>() {
  fun header(block: @ExcelDsl CellStyle.() -> Unit) = addHeaderStyle(block)
}

fun workbook(path: String, block: WorkbookBuilder.() -> Unit) =
  WorkbookBuilder().apply(block).write(path)

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) = writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
