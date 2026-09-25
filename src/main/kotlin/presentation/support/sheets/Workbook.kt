package presentation.support.sheets

import java.io.OutputStream
import presentation.support.ExcelDsl
import presentation.support.SheetSpec
import presentation.support.writeWorkbook

/** Named sheets with their rows, still not pulled until [write]. */
class Workbook internal constructor(private val sheets: List<SheetSpec<*>>) {
  /** Streams every sheet into [output], pulling each `Sequence` once. */
  fun write(output: OutputStream) = writeWorkbook(output, sheets)
}

@ExcelDsl
class WorkbookBuilder internal constructor() {
  private val sheets = mutableListOf<SheetSpec<*>>()

  fun <T> sheet(name: String, sheet: Sheet<T>, rows: Sequence<T>) {
    sheets += sheet.toSpec(name, rows)
  }

  internal fun build(): Workbook = Workbook(sheets.toList())
}

fun workbook(block: WorkbookBuilder.() -> Unit): Workbook =
  WorkbookBuilder().apply(block).build()
