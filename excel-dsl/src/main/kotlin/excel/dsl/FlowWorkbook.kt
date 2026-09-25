package excel.dsl

import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.outputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext

/** Named sheets backed by flows, collected sequentially on each write. */
class FlowWorkbook internal constructor(private val sheets: List<Entry<*>>) {
  val names: List<String> = sheets.map { it.name }

  /** Streams on Dispatchers.IO and leaves [output] open. Cancellation closes POI's temporary files. */
  suspend fun write(output: OutputStream) {
    withContext(Dispatchers.IO) {
      streamWorkbook(output) {
        for (entry in sheets) {
          currentCoroutineContext().ensureActive()
          entry.writeTo(this)
        }
        currentCoroutineContext().ensureActive()
      }
    }
  }

  internal class Entry<T>(val name: String, val sheet: Sheet<T>, val rows: Flow<T>) {
    suspend fun writeTo(workbook: StreamingWorkbook) {
      workbook.sheet(name, sheet) { writer ->
        rows.collect {
          currentCoroutineContext().ensureActive()
          writer.write(it)
        }
      }
    }
  }
}

suspend fun FlowWorkbook.write(path: Path) {
  withContext(Dispatchers.IO) { path.outputStream().use { write(it) } }
}

@ExcelDsl
class FlowWorkbookBuilder @PublishedApi internal constructor() {
  private val sheets = mutableListOf<FlowWorkbook.Entry<*>>()
  fun <T> sheet(name: String, sheet: Sheet<T>, rows: Flow<T>) {
    checkSheetName(name)
    require(sheets.none { it.name.equals(name, ignoreCase = true) }) { "Duplicate sheet name: $name" }
    sheets += FlowWorkbook.Entry(name, sheet, rows)
  }
  fun <T> sheet(name: String, sheet: Sheet<T>, rows: Sequence<T>) = sheet(name, sheet, rows.asFlow())
  fun <T> sheet(name: String, sheet: Sheet<T>, rows: Iterable<T>) = sheet(name, sheet, rows.asFlow())
  @PublishedApi internal fun build(): FlowWorkbook = FlowWorkbook(sheets.toList())
}

/** Composes sheets of different row types, using flows, sequences or lists. */
inline fun flowWorkbook(block: FlowWorkbookBuilder.() -> Unit): FlowWorkbook =
  FlowWorkbookBuilder().apply(block).build()
