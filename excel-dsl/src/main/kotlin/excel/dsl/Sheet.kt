package excel.dsl

import java.io.OutputStream
import java.nio.file.OpenOption
import java.nio.file.Path
import java.time.ZoneId
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream
import kotlinx.coroutines.flow.Flow

/**
 * The schema of a sheet: its columns, formulas, totals and layout, without a
 * file or rows. Immutable, built by [sheet], and written any number of times.
 */
class Sheet<in T> internal constructor(
  internal val columns: List<ColumnSpec<T>>,
  internal val totals: Map<Int, TotalSpec>,
  internal val freezePanes: Boolean,
  internal val autoFilter: Boolean,
  internal val zone: ZoneId,
) {
  /** The header row, in column order. */
  val headers: List<String> = columns.map { it.column.name }

  /**
   * One row as values, keyed by header in column order, without Excel.
   * Data columns hold what their lambda returned; formulas the `Double`
   * computed from the columns before them, or a `Boolean` when the result
   * is a comparison, text for text formulas, or an [ExcelError].
   */
  fun evaluate(row: T): Map<String, Any?> =
    evaluateTyped(row).toMap()

  /**
   * The totals row for [rows], keyed by header in column order, only for
   * columns with a total. Empty without rows, since no totals row is written.
   */
  fun totals(rows: Sequence<T>): Map<String, Any?> {
    if (totals.isEmpty()) return emptyMap()
    val referenced = totals.values.filterIsInstance<TotalSpec.Formula>()
      .flatMap { it.expr.references() }.mapNotNull { it.referencedColumn }.toSet()
    val summaries = referenced.associateWith { RangeSummary() }
    // Include dependencies of formula columns, but never evaluate unrelated columns.
    val needed = referenced.toMutableSet()
    for (spec in columns.asReversed()) {
      if (spec.column in needed && spec is ColumnSpec.Formula) {
        needed += spec.expr.references().mapNotNull { it.referencedColumn }
      }
    }
    var anyRows = false
    for (row in rows) {
      anyRows = true
      val values = evaluateRow(row, needed)
      summaries.forEach { (column, summary) -> summary.add(values.getValue(column)) }
    }
    if (!anyRows) return emptyMap()
    return totals.toSortedMap().entries.associate { (index, spec) ->
      headers[index] to when (spec) {
        is TotalSpec.Label -> spec.text
        is TotalSpec.Formula -> spec.expr.evaluateRaw(
          cell = { error("A total has no row") },
          range = summaries::getValue
        )
      }
    }
  }

  fun totals(rows: Iterable<T>): Map<String, Any?> = totals(rows.asSequence())

  /** Values, blanks and errors, indexed by typed column handles. */
  fun evaluateTyped(row: T): EvaluatedRow = EvaluatedRow(evaluateRow(row))

  /**
   * Streams [rows] into [output] as a workbook with one sheet called [name].
   * [rows] is pulled once per call, one row at a time; [output] is left open.
   */
  fun write(output: OutputStream, rows: Sequence<T>, name: String = DEFAULT_NAME) {
    workbook { sheet(name, this@Sheet, rows) }.write(output)
  }

  fun write(output: OutputStream, rows: Iterable<T>, name: String = DEFAULT_NAME) {
    write(output, rows.asSequence(), name)
  }

  /** Like the `Sequence` overload, collecting [rows], make sure to run on appropriate dispatcher since Apache POI blocks. */
  suspend fun write(output: OutputStream, rows: Flow<T>, name: String = DEFAULT_NAME) {
    flowWorkbook { sheet(name, this@Sheet, rows) }.write(output)
  }

  internal fun evaluateRow(row: T, needed: Set<Column<*>>? = null, excelRow: Int? = null): Map<Column<*>, Any?> {
    val values = LinkedHashMap<Column<*>, Any?>()
    for (spec in columns) {
      if (needed != null && spec.column !in needed) continue
      values[spec.column] = when (spec) {
        is ColumnSpec.Data -> spec.value(row).also { validateCell(it, spec.column.name, excelRow, zone) }
        is ColumnSpec.Formula -> spec.expr.evaluateRaw(cell = values::getValue, range = { error("A row has no range") })
      }
    }
    return values
  }

  internal companion object {
    const val DEFAULT_NAME = "Sheet1"
  }
}

/**
 * Writes a file, named after it by default: `q1.xlsx` gets `q1`.
 * [options] follow [java.nio.file.Files.newOutputStream]: create or truncate by default.
 * Use `CREATE_NEW` to refuse replacement. A failed write can leave a partial file;
 * write a temporary file and move it afterward when replacement must be atomic.
 */
fun <T> Sheet<T>.write(
  path: Path,
  rows: Sequence<T>,
  name: String = path.nameWithoutExtension,
  vararg options: OpenOption
) {
  checkSheetName(name)
  path.outputStream(*options).use { write(it, rows, name) }
}

/** Writes a file from a [Flow], naming the sheet after it. */
suspend fun <T> Sheet<T>.write(
  path: Path,
  rows: Flow<T>,
  name: String = path.nameWithoutExtension,
  vararg options: OpenOption
) {
  checkSheetName(name)
  path.outputStream(*options).use { write(it, rows, name) }
}

fun <T> Sheet<T>.write(
  path: Path,
  rows: Iterable<T>,
  name: String = path.nameWithoutExtension,
  vararg options: OpenOption
) {
  write(path, rows.asSequence(), name, *options)
}
