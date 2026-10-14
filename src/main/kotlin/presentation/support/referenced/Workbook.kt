package presentation.support.referenced

import presentation.support.ExcelDsl
import presentation.support.WorkbookSheets

@ExcelDsl
class WorkbookBuilder : WorkbookSheets() {
  fun <T> sheet(name: String, rows: Sequence<T>, block: SheetBuilder<T>.() -> Unit) =
    addSheet(name, rows, SheetBuilder<T>().apply(block))
}

fun workbook(path: String, block: WorkbookBuilder.() -> Unit) =
  WorkbookBuilder().apply(block).write(path)
