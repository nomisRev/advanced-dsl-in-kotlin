package excel.dsl

import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.outputStream

/** Named sheets with their rows, not pulled until [write]. Immutable. */
class Workbook internal constructor(private val sheets: List<Entry<*>>) {
  /** The sheet names, in the order they are written. */
  val names: List<String> = sheets.map { it.name }

  /** Streams every sheet into [output], pulling each `Sequence` once per call; [output] is left open. */
  fun write(output: OutputStream) {
    streamWorkbook(output) { sheets.forEach { it.writeTo(this) } }
  }

  internal class Entry<T>(val name: String, val sheet: Sheet<T>, val rows: Sequence<T>) {
    fun writeTo(workbook: StreamingWorkbook) {
      workbook.sheet(name, sheet) { writer -> rows.forEach(writer::write) }
    }
  }
}

/** Writes [Workbook.write] to a file. */
fun Workbook.write(path: Path) {
  path.outputStream().use(::write)
}

@ExcelDsl
class WorkbookBuilder @PublishedApi internal constructor() {
  private val sheets = mutableListOf<Workbook.Entry<*>>()

  /**
   * Adds [sheet] under [name], filled with [rows] when the workbook is written.
   *
   * @throws IllegalArgumentException when [name] is not a valid Excel sheet
   *   name, or is already used, ignoring case as Excel does.
   */
  fun <T> sheet(name: String, sheet: Sheet<T>, rows: Sequence<T>) {
    checkSheetName(name)
    require(sheets.none { it.name.equals(name, ignoreCase = true) }) { "Duplicate sheet name: $name" }
    sheets += Workbook.Entry(name, sheet, rows)
  }

  fun <T> sheet(name: String, sheet: Sheet<T>, rows: Iterable<T>) {
    sheet(name, sheet, rows.asSequence())
  }

  @PublishedApi
  internal fun build(): Workbook = Workbook(sheets.toList())
}

/**
 * Runs [block] once and returns the sheets it added.
 *
 * ```kotlin
 * val report = workbook {
 *   sheet("Q1", invoices, q1)
 *   sheet("Q2", invoices, q2)
 * }
 * ```
 */
inline fun workbook(block: WorkbookBuilder.() -> Unit): Workbook =
  WorkbookBuilder().apply(block).build()
