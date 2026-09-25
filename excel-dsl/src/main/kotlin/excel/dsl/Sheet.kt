package excel.dsl

import java.io.OutputStream
import java.nio.file.Path
import java.time.ZoneId
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * The schema of a sheet: its columns, formulas, totals and layout, without a
 * file or rows. Immutable, built by [sheet], and written any number of times.
 */
class Sheet<in T> internal constructor(
  internal val columns: List<ColumnSpec<T>>,
  internal val totals: Map<Int, TotalSpec>,
  internal val freezeHeader: Boolean,
  internal val autoFilter: Boolean,
  internal val zone: ZoneId,
) {
  /** The header row, in column order. */
  val headers: List<String> = columns.map { it.column.name }

  /**
   * One row as values, keyed by header in column order, without Excel.
   * Data columns hold what their lambda returned; formulas the `Double`
   * that [Expr.evaluate] computes from the columns before them, or a
   * `Boolean` when the formula is a comparison, as Excel shows it.
   */
  fun evaluate(row: T): Map<String, Any?> =
    evaluateRow(row).mapKeys { it.key.name }

  /**
   * The totals row for [rows], keyed by header in column order, only for
   * columns with a total. Empty without rows, since no totals row is written.
   */
  fun totals(rows: Sequence<T>): Map<String, Any?> {
    val values = columns.associate { it.column to mutableListOf<Any?>() }
    for (row in rows) evaluateRow(row).forEach { (column, value) -> values.getValue(column) += value }
    if (values.values.first().isEmpty()) return emptyMap()
    return totals.toSortedMap().entries.associate { (index, spec) ->
      headers[index] to when (spec) {
        is TotalSpec.Label -> spec.text
        is TotalSpec.Formula -> spec.expr.result(
          spec.expr.evaluate(cell = { error("A total has no row") }, range = values::getValue),
        )
      }
    }
  }

  /**
   * Streams [rows] into [output] as a workbook with one sheet called [name].
   * [rows] is pulled once per call, one row at a time; [output] is left open.
   */
  fun write(output: OutputStream, rows: Sequence<T>, name: String = DEFAULT_NAME) {
    workbook { sheet(name, this@Sheet, rows) }.write(output)
  }

  /** Like the `Sequence` overload, collecting [rows] on `Dispatchers.IO`, since Apache POI blocks. */
  suspend fun write(output: OutputStream, rows: Flow<T>, name: String = DEFAULT_NAME) {
    withContext(Dispatchers.IO) {
      streamWorkbook(output) {
        sheet(name, this@Sheet) { writer -> rows.collect(writer::write) }
      }
    }
  }

  private fun evaluateRow(row: T): Map<Column<*>, Any?> {
    val values = LinkedHashMap<Column<*>, Any?>()
    for (spec in columns) {
      values[spec.column] = when (spec) {
        is ColumnSpec.Data -> spec.value(row)
        is ColumnSpec.Formula -> spec.expr.result(
          spec.expr.evaluate(cell = values::get, range = { error("A row has no range") }),
        )
      }
    }
    return values
  }

  internal companion object {
    const val DEFAULT_NAME = "Sheet1"
  }
}

/** A comparison shows as `TRUE` or `FALSE`, everything else as a number. */
private fun Expr.result(value: Double): Any = if (this is Expr.Compare) value != 0.0 else value

/** Writes a file, and names the sheet after it: `q1.xlsx` gets `q1`. */
fun <T> Sheet<T>.write(path: Path, rows: Sequence<T>) {
  path.outputStream().use { write(it, rows, path.nameWithoutExtension) }
}

/** Writes a file from a [Flow], naming the sheet after it. */
suspend fun <T> Sheet<T>.write(path: Path, rows: Flow<T>) {
  withContext(Dispatchers.IO) {
    path.outputStream().use { write(it, rows, path.nameWithoutExtension) }
  }
}
