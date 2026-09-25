package presentation.support.flow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import presentation.support.Cell
import presentation.support.ColumnDelegate
import presentation.support.DelegatedColumns
import presentation.support.ExcelDsl
import presentation.support.contexts.ExcelFormulas
import presentation.support.contexts.Formulas
import presentation.support.streamSheet

/*
 * Extra: the Part 5 DSL fed by a `Flow`. The builder block and the column
 * lambdas stay non-suspending; only pulling the rows suspends.
 */

@ExcelDsl
class SheetBuilder<T> : DelegatedColumns<T>() {
  fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> =
    formulaColumn { row -> ExcelFormulas(row).block() }
}

/** Blocking: pulls the rows from the `Sequence` on the calling thread. */
fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) {
  val columns = SheetBuilder<T>().apply(block)
  streamSheet(path, columns) { sheet -> rows.forEach(sheet::write) }
}

/**
 * Suspends while the rows arrive; `collect` asks for the next row only after
 * the previous one is written. Apache POI blocks, so it runs on
 * `Dispatchers.IO`, and so does the upstream of [rows] unless it has a
 * `flowOn` of its own.
 */
suspend fun <T> generateExcel(
  path: String,
  rows: Flow<T>,
  block: SheetBuilder<T>.() -> Unit,
) {
  val columns = SheetBuilder<T>().apply(block)
  withContext(Dispatchers.IO) {
    streamSheet(path, columns) { sheet -> rows.collect(sheet::write) }
  }
}
