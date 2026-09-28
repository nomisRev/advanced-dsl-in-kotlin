package excel.dsl

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import java.util.Locale
import kotlin.io.path.inputStream
import kotlin.test.assertEquals
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.FormulaError
import org.apache.poi.xssf.usermodel.XSSFWorkbook

data class Invoice(
  val customer: String,
  val hours: Int,
  val rate: Int,
  val issuedOn: LocalDate?,
)

val invoices: List<Invoice> = listOf(
  Invoice("Ada", 12, 90, LocalDate.of(2026, 1, 5)),
  Invoice("Linus", 8, 80, null),
  Invoice("Grace", 20, 110, LocalDate.of(2026, 3, 30)),
)

val invoiceSheet: Sheet<Invoice> = sheet {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val issuedOn by column { it.issuedOn }
  val total by formula { hours * rate }
  val gross by formula { total * 1.21 }
}

fun Sheet<Invoice>.writeToBytes(rows: Sequence<Invoice> = invoices.asSequence()): ByteArray =
  ByteArrayOutputStream().also { write(it, rows) }.toByteArray()

fun ByteArray.read(): XSSFWorkbook = XSSFWorkbook(ByteArrayInputStream(this))

fun Path.read(): XSSFWorkbook = inputStream().use(::XSSFWorkbook)

/** Every row of the first sheet as the text Excel shows, formulas as their source. */
fun XSSFWorkbook.cells(sheet: Int = 0): List<List<String?>> {
  val formatter = DataFormatter(Locale.US)
  return getSheetAt(sheet).map { row ->
    (0 until row.lastCellNum).map { index ->
      val cell = row.getCell(index) ?: return@map null
      if (cell.cellType == CellType.FORMULA) "=${cell.cellFormula}" else formatter.formatCellValue(cell)
    }
  }
}

/** The files SXSSF flushes rows to; closing the workbook deletes them. */
fun sxssfTempFiles(): Set<String> =
  File(System.getProperty("java.io.tmpdir"), "poifiles")
    .listFiles { file -> file.name.startsWith("poi-sxssf-sheet") }
    ?.map { it.name }
    .orEmpty()
    .toSet()

/**
 * Writes [rows], lets Apache POI's formula engine compute every cell as
 * Excel would, and asserts it matches [Sheet.evaluate] and [Sheet.totals].
 * Only for sheets of numbers, booleans and text.
 */
fun <T> assertExcelAgrees(sheet: Sheet<T>, rows: List<T>) {
  val workbook = ByteArrayOutputStream().also { sheet.write(it, rows.asSequence()) }.toByteArray().read()
  val excel = workbook.creationHelper.createFormulaEvaluator()
  val poi = workbook.getSheetAt(0)
  fun computed(cell: Cell?): Any? {
    val value = cell?.let(excel::evaluate) ?: return null
    return when (value.cellType) {
      CellType.NUMERIC -> value.numberValue
      CellType.BOOLEAN -> value.booleanValue
      CellType.STRING -> value.stringValue
      CellType.ERROR -> FormulaError.forInt(value.errorValue).string
      CellType.BLANK, CellType.FORMULA, CellType._NONE, null -> null
    }
  }
  fun expected(value: Any?): Any? = when (value) {
    is ExcelError -> value.text
    is Double -> if (value.isFinite()) value else ExcelError.Number.text
    is Number -> value.toDouble()
    else -> value
  }
  rows.forEachIndexed { index, row ->
    val cells = poi.getRow(index + 1)
    assertEquals(
      sheet.evaluate(row).mapValues { expected(it.value) },
      sheet.headers.withIndex().associate { [column, header] -> header to computed(cells.getCell(column)) },
      "row ${index + 2}",
    )
  }
  val totals = sheet.totals(rows.asSequence())
  if (totals.isNotEmpty()) {
    val cells = poi.getRow(rows.size + 1)
    assertEquals(
      totals.mapValues { expected(it.value) },
      totals.keys.associateWith { computed(cells.getCell(sheet.headers.indexOf(it))) },
      "totals",
    )
  }
}
