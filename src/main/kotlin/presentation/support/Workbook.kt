package presentation.support

import java.io.FileOutputStream
import java.time.LocalDate
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook

/**
 * The columns of one sheet, collected by a builder of the DSL.
 *
 * Every stage of the DSL on the slides (named, typed, delegated columns)
 * extends this class, so they all write the same workbook.
 */
abstract class SheetColumns<T> {
  private val columns = mutableListOf<ColumnSpec<T>>()
  private val headerStyles = mutableListOf<CellStyle.() -> Unit>()

  /** Adds a column and returns its index. [value] receives the 1-based Excel row too. */
  protected fun addColumn(
    header: String,
    bold: Boolean = false,
    value: (row: T, excelRow: Int) -> Any?,
  ): Int {
    columns += ColumnSpec(header, bold, value)
    return columns.lastIndex
  }

  protected fun addHeaderStyle(block: CellStyle.() -> Unit) {
    headerStyles += block
  }

  @PublishedApi
  internal fun toSheet(name: String, rows: Sequence<T>): SheetSpec<T> =
    SheetSpec(name, rows, columns.toList(), headerStyles.toList())
}

class ColumnSpec<in T>(
  val header: String,
  val bold: Boolean,
  val value: (row: T, excelRow: Int) -> Any?,
)

class SheetSpec<T>(
  val name: String,
  val rows: Sequence<T>,
  val columns: List<ColumnSpec<T>>,
  val headerStyles: List<CellStyle.() -> Unit>,
)

/** Writes one sheet named after the file, `invoices.xlsx` gets `invoices`. */
fun <T> writeWorkbook(path: String, rows: Sequence<T>, columns: SheetColumns<T>) =
  writeWorkbook(path, listOf(columns.toSheet(sheetName(path), rows)))

/**
 * Streams every sheet: rows are pulled from their `Sequence` one at a time and
 * only the last [ROW_WINDOW] stay in memory, the rest is flushed to disk.
 */
fun writeWorkbook(path: String, sheets: List<SheetSpec<*>>) {
  SXSSFWorkbook(ROW_WINDOW).use { workbook ->
    try {
      val styles = Styles(workbook)
      for (sheet in sheets) workbook.write(sheet, styles)
      FileOutputStream(path).use(workbook::write)
    } finally {
      workbook.dispose()
    }
  }
}

private const val ROW_WINDOW = 100

private fun sheetName(path: String): String =
  path.substringAfterLast('/').substringBeforeLast('.')

private fun <T> Workbook.write(spec: SheetSpec<T>, styles: Styles) {
  val sheet = createSheet(spec.name)
  val headerStyle = styles.header(spec.headerStyles)
  val header = sheet.createRow(0)
  spec.columns.forEachIndexed { index, column ->
    header.createCell(index).apply {
      setCellValue(column.header)
      setCellStyle(headerStyle)
    }
  }
  spec.rows.forEachIndexed { index, value ->
    val row = sheet.createRow(index + 1)
    spec.columns.forEachIndexed { column, spec ->
      row.write(column, spec.value(value, index + 2), spec.bold, styles)
    }
  }
}

/**
 * The cell type follows the value: text, number, date, or formula.
 * Anything else is written as its `toString()`, the `Any?` trap of Part 2.
 */
private fun Row.write(index: Int, value: Any?, bold: Boolean, styles: Styles) {
  if (value == null) return
  val cell = createCell(index)
  cell.cellStyle = styles.cell(bold = bold, date = value is LocalDate)
  when (value) {
    is String -> cell.setCellValue(value)
    is Number -> cell.setCellValue(value.toDouble())
    is LocalDate -> cell.setCellValue(value)
    is Cell.Formula -> cell.cellFormula = value.text.removePrefix("=")
    is Cell.Value -> cell.setCellValue(value.number)
    else -> cell.setCellValue(value.toString())
  }
}

private class Styles(private val workbook: Workbook) {
  private val cells = mutableMapOf<Pair<Boolean, Boolean>, CellStyle>()
  private val boldFont = workbook.createFont().apply { bold = true }

  fun header(blocks: List<CellStyle.() -> Unit>): CellStyle =
    workbook.createCellStyle().apply {
      setFont(boldFont)
      blocks.forEach { it() }
    }

  fun cell(bold: Boolean, date: Boolean): CellStyle =
    cells.getOrPut(bold to date) {
      workbook.createCellStyle().apply {
        if (bold) setFont(boldFont)
        if (date) {
          dataFormat = workbook.creationHelper.createDataFormat()
            .getFormat("yyyy-mm-dd")
        }
      }
    }
}
