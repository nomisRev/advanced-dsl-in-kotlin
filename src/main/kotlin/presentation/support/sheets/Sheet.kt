package presentation.support.sheets

import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream
import presentation.support.Cell
import presentation.support.ColumnSpec
import presentation.support.SheetSpec

/**
 * The schema of a sheet: its columns and formulas, without a file or rows.
 * Immutable, built by [sheet], and written by [write] or a [workbook].
 */
class Sheet<T> internal constructor(
  private val columns: List<ColumnSpec<T>>,
  private val formulas: Map<Int, Formulas.() -> Cell>,
) {
  /** The header row, in column order. */
  val headers: List<String> = columns.map { it.header }

  /**
   * One row as values, without Excel: data columns read [row], formulas are
   * computed by [Evaluate] from the columns before them.
   */
  fun evaluate(row: T): Map<String, Any?> {
    val values = mutableListOf<Any?>()
    columns.forEachIndexed { index, column ->
      val formula = formulas[index]
      values += if (formula == null) {
        column.value(row, FIRST_DATA_ROW)
      } else {
        when (val cell = Evaluate(values).formula()) {
          is Cell.Value -> cell.number
          is Cell.Formula -> cell.text
        }
      }
    }
    return headers.zip(values).toMap()
  }

  /**
   * Streams [rows] into [output] as a workbook with one sheet called [name].
   * [rows] is pulled here, once per call; [output] is left open.
   */
  fun write(output: OutputStream, rows: Sequence<T>, name: String = "Sheet1") =
    workbook { sheet(name, this@Sheet, rows) }.write(output)

  internal fun toSpec(name: String, rows: Sequence<T>): SheetSpec<T> =
    SheetSpec(name, rows, columns, emptyList())
}

/** Writes to a file, and names the sheet after it: `q1.xlsx` gets `q1`. */
fun <T> Sheet<T>.write(path: Path, rows: Sequence<T>) =
  path.outputStream().use { write(it, rows, path.nameWithoutExtension) }

private const val FIRST_DATA_ROW = 2
