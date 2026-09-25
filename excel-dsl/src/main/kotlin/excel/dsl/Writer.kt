package excel.dsl

import java.io.OutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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
    if (sheet.freezeHeader) poi.createFreezePane(0, 1)
  }

  fun write(row: T) {
    check(written < maxDataRows) { "Excel sheets hold at most $MAX_ROWS rows, header and totals included" }
    written++
    val cells = poi.createRow(written)
    val excelRow = written + 1
    sheet.columns.forEachIndexed { index, spec ->
      val value = when (spec) {
        is ColumnSpec.Data -> spec.value(row) ?: return@forEachIndexed
        is ColumnSpec.Formula -> spec.expr
      }
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
        is Expr -> cell.cellFormula = value.toExcel(excelRow)
        else -> fail("unsupported cell value $value")
      }
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

/** `null` when Excel cannot hold the number without losing digits. */
private fun Number.toExcelDouble(): Double? = when (this) {
  is Double -> takeIf { it.isFinite() }
  is Float -> toDouble().takeIf { it.isFinite() }
  is BigDecimal -> takeIf { it.fits() }?.toDouble()
  is BigInteger -> takeIf { it.toBigDecimal().fits() }?.toDouble()
  is Long -> takeIf { it.toBigDecimal().fits() }?.toDouble()
  else -> toDouble()
}

private fun BigDecimal.fits(): Boolean = signum() == 0 || stripTrailingZeros().precision() <= MAX_DIGITS

private fun Number.unsupported(): String =
  if (this is Double || this is Float) "$this has no Excel equivalent"
  else "$this has more than $MAX_DIGITS significant digits, which Excel cannot store; write it as text"
