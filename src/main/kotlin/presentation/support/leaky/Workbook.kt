package presentation.support.leaky

import presentation.support.NamedColumns
import presentation.support.WorkbookSheets

/*
 * The workbook DSL of Part 1 before `@DslMarker`:
 * a `sheet` nested in a `sheet` still resolves, on the outer receiver.
 */

class WorkbookBuilder : WorkbookSheets() {
  fun <T> sheet(
    name: String,
    rows: Sequence<T>,
    block: SheetBuilder<T>.() -> Unit,
  ) = addSheet(name, rows, SheetBuilder<T>().apply(block))
}

class SheetBuilder<T> : NamedColumns<T>()

fun workbook(path: String, block: WorkbookBuilder.() -> Unit) =
  WorkbookBuilder().apply(block).write(path)
