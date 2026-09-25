package presentation.support

/** `column("Customer") { }` from Part 1: a header and an `Any?` per row. */
abstract class NamedColumns<T> : SheetColumns<T>() {
  fun column(name: String, bold: Boolean = false, value: (T) -> Any?) {
    addColumn(name, bold) { row, _ -> value(row) }
  }
}

/** The sheets of a `workbook { }`, written in the order they were declared. */
abstract class WorkbookSheets {
  private val sheets = mutableListOf<SheetSpec<*>>()

  protected fun <T> addSheet(name: String, rows: Sequence<T>, columns: SheetColumns<T>) {
    sheets += columns.toSheet(name, rows)
  }

  @PublishedApi
  internal fun write(path: String) = writeWorkbook(path, sheets.toList())
}
