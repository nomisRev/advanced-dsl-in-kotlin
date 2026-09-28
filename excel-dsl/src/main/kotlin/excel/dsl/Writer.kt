package excel.dsl

import java.io.OutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.FormulaError
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook

/*
 * The only file that knows Apache POI. SXSSF keeps the last ROW_WINDOW rows in
 * memory and flushes the rest to temp files, which `close` deletes.
 */

internal const val ROW_WINDOW = 100

/** Excel's longest text cell. */
internal const val MAX_TEXT = 32_767

/** Excel keeps 15 significant digits of a number. */
internal const val MAX_DIGITS = 15

/** Opens a streaming workbook, lets [write] add sheets, saves it to [output], and cleans up. */
internal inline fun streamWorkbook(output: OutputStream, write: StreamingWorkbook.() -> Unit) {
  SXSSFWorkbook(ROW_WINDOW).use { poi ->
    StreamingWorkbook(poi).write()
    poi.setForceFormulaRecalculation(true)
    poi.write(output)
  }
}

internal fun checkSheetName(name: String) {
  WorkbookUtil.validateSheetName(name)
}

internal class StreamingWorkbook(private val poi: SXSSFWorkbook) {
  private val bold = poi.createFont().apply { bold = true }
  private val styles = mutableMapOf<Pair<CellFormat?, Boolean>, CellStyle>()

  /** Creates the sheet with its header, lets [write] add the rows, then adds the totals row. */
  inline fun <T> sheet(name: String, sheet: Sheet<T>, write: (SheetWriter<T>) -> Unit) {
    checkSheetName(name)
    val writer = SheetWriter(poi.createSheet(name), sheet, this)
    write(writer)
    writer.finish()
  }

  fun style(format: CellFormat?, bold: Boolean): CellStyle =
    styles.getOrPut(format to bold) {
      poi.createCellStyle().apply {
        if (bold) setFont(this@StreamingWorkbook.bold)
        if (format != null) dataFormat = poi.creationHelper.createDataFormat().getFormat(format.pattern)
      }
    }
}

/** One sheet being written: the header on creation, a row per [write], the totals on [finish]. */
internal class SheetWriter<T>(
  private val poi: SXSSFSheet,
  private val sheet: Sheet<T>,
  private val workbook: StreamingWorkbook,
) {
  private var written = 0
  private val summaries = sheet.totals.values.filterIsInstance<TotalSpec.Formula>()
    .flatMap { it.expr.references() }.mapNotNull { it.referencedColumn }.distinct()
    .associateWith { RangeSummary() }
  private val maxDataRows = MAX_ROWS - 1 - if (sheet.totals.isEmpty()) 0 else 1

  init {
    val header = poi.createRow(0)
    sheet.columns.forEachIndexed { index, spec ->
      header.createCell(index).apply {
        setCellValue(spec.column.name)
        cellStyle = workbook.style(null, bold = true)
      }
      poi.setColumnWidth(index, spec.width() * 256)
    }
    if (sheet.freezePanes) poi.createFreezePane(0, 1)
  }

  fun write(row: T) {
    check(written < maxDataRows) { "Excel sheets hold at most $MAX_ROWS rows, header and totals included" }
    written++
    val cells = poi.createRow(written)
    val excelRow = written + 1
    // Evaluate each extractor once. Keep only this row and constant-size totals summaries.
    val values = sheet.evaluateRow(row, excelRow = excelRow)
    summaries.forEach { (column, summary) -> summary.add(values.getValue(column)) }
    sheet.columns.forEachIndexed { index, spec ->
      val value = values.getValue(spec.column) ?: return@forEachIndexed
      val cell = cells.createCell(index)
      cell.cellStyle = workbook.style(spec.format(value), bold = false)
      fun fail(reason: String): Nothing = throw IllegalArgumentException("${spec.column.name}, row $excelRow: $reason")
      when (value) {
        is String -> {
          if (value.length > MAX_TEXT) fail("text longer than Excel's $MAX_TEXT characters")
          cell.setCellValue(value)
        }
        is Number -> cell.setCellValue(value.toExcelDouble() ?: fail(value.unsupported()))
        is Boolean -> cell.setCellValue(value)
        is LocalDate -> cell.setCellValue(value)
        is LocalDateTime -> cell.setCellValue(value)
        is Instant -> cell.setCellValue(LocalDateTime.ofInstant(value, sheet.zone))
        is ExcelError -> cell.setCellErrorValue(FormulaError.forString(value.text).code)
        else -> fail("unsupported cell value $value")
      }
      // POI preserves the typed value when turning the cell into a formula cell.
      if (spec is ColumnSpec.Formula) cell.cellFormula = spec.expr.toExcel(excelRow)
    }
  }

  /** Writes the totals row and the filter, both need the last row. */
  fun finish() {
    val data = 2..written + 1
    if (written > 0 && sheet.totals.isNotEmpty()) {
      val row = poi.createRow(written + 1)
      for ((index, total) in sheet.totals) {
        val cell = row.createCell(index)
        when (total) {
          is TotalSpec.Label -> {
            cell.setCellValue(total.text)
            cell.cellStyle = workbook.style(null, bold = true)
          }
          is TotalSpec.Formula -> {
            cell.setFormulaResult(total.expr.evaluateRaw(
              cell = { error("A total has no row") },
              range = summaries::getValue,
            ))
            cell.cellFormula = total.expr.toExcel(written + 2, data)
            cell.cellStyle = workbook.style(sheet.columns[index].format(total.expr), bold = true)
          }
        }
      }
    }
    if (sheet.autoFilter && sheet.columns.isNotEmpty()) {
      poi.setAutoFilter(CellRangeAddress(0, written, 0, sheet.columns.lastIndex))
    }
  }
}

private fun Cell.setFormulaResult(value: Any?) {
  when (value) {
    is Number -> setCellValue(value.toDouble())
    is String -> setCellValue(value)
    is Boolean -> setCellValue(value)
    is ExcelError -> setCellErrorValue(FormulaError.forString(value.text).code)
    else -> error("Unsupported formula result: $value")
  }
}

/** Shared by evaluation and writing; preserves the caller's typed data value. */
internal fun validateCell(value: Any?, header: String, excelRow: Int?) {
  fun fail(reason: String): Nothing {
    val location = if (excelRow == null) header else "$header, row $excelRow"
    throw IllegalArgumentException("$location: $reason")
  }
  when (value) {
    is Number -> if (value.toExcelDouble() == null) fail(value.unsupported())
    is String -> if (value.length > MAX_TEXT) fail("text longer than Excel's $MAX_TEXT characters")
    else -> Unit
  }
}

/** The column's own format, or the default of the value's type. */
private fun ColumnSpec<*>.format(value: Any): CellFormat? =
  layout.format ?: when (value) {
    is LocalDate -> CellFormat.Date
    is LocalDateTime, is Instant -> CellFormat.DateTime
    else -> null
  }

/** The header plus some room, at least 10 characters, 20 for a date and time. */
private fun ColumnSpec<*>.width(): Int =
  layout.width ?: maxOf(column.name.length + 2, if (layout.format == CellFormat.DateTime) 20 else 10)
    .coerceAtMost(MAX_WIDTH)

/** `null` when Excel cannot hold the number without losing digits. */
internal fun Number.toExcelDouble(): Double? = when (this) {
  is Double -> takeIf { it.isFinite() }
  is Float -> toDouble().takeIf { it.isFinite() }
  is BigDecimal -> takeIf { it.fits() }?.toDouble()?.takeIf { it.isFinite() && (it != 0.0 || signum() == 0) }
  is BigInteger -> takeIf { it.toBigDecimal().fits() }?.toDouble()?.takeIf { it.isFinite() }
  is Long -> takeIf { it.toBigDecimal().fits() }?.toDouble()
  else -> toDouble().takeIf { it.isFinite() }
}

private fun BigDecimal.fits(): Boolean = signum() == 0 || stripTrailingZeros().precision() <= MAX_DIGITS

private fun Number.unsupported(): String =
  if (this is Double || this is Float) "$this has no Excel equivalent"
  else if (!toDouble().isFinite() || (this is BigDecimal && signum() != 0 && toDouble() == 0.0))
    "$this is outside the supported numeric range; write it as text"
  else "$this has more than $MAX_DIGITS significant digits, which Excel cannot store; write it as text"

/** The same precision and range checks apply to cells and formula constants. */
internal fun Number.checkedExcelDouble(): Double =
  toExcelDouble() ?: throw IllegalArgumentException(unsupported())
