package presentation.support.delegated

import presentation.support.ExcelDsl
import presentation.support.WorkbookSheets

@ExcelDsl
class WorkbookBuilder(val header: (String) -> String = { it }) : WorkbookSheets() {
  fun <T> sheet(name: String, rows: Sequence<T>, block: SheetBuilder<T>.() -> Unit) =
    addSheet(name, rows, SheetBuilder<T>(header).apply(block))
}

fun workbook(path: String, block: WorkbookBuilder.() -> Unit) =
  WorkbookBuilder().apply(block).write(path)
