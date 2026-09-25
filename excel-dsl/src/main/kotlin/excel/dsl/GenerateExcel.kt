@file:OptIn(ExperimentalContracts::class)

package excel.dsl

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension
import kotlinx.coroutines.flow.Flow

/**
 * Declares a [sheet] and writes [rows] to [path] in one call, naming the
 * sheet after the file: `invoices.xlsx` gets `invoices`.
 *
 * ```kotlin
 * generateExcel("invoices.xlsx", invoices) {
 *   val hours by column { it.hours }
 *   val rate by column { it.rate }
 *   val total by formula { hours * rate }
 * }
 * ```
 */
inline fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  config: ExcelConfig = ExcelConfig.Default,
  name: String = Path(path).nameWithoutExtension,
  block: SheetBuilder<T>.() -> Unit,
) {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
  sheet(config, block).write(Path(path), rows, name)
}

/** Like the `Sequence` overload, suspending while [rows] arrive. The block and column lambdas do not suspend. */
suspend inline fun <T> generateExcel(
  path: String,
  rows: Flow<T>,
  config: ExcelConfig = ExcelConfig.Default,
  name: String = Path(path).nameWithoutExtension,
  block: SheetBuilder<T>.() -> Unit,
) {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
  sheet(config, block).write(Path(path), rows, name)
}

/** Convenience overload for ordinary lists and other iterables. */
inline fun <T> generateExcel(
  path: String,
  rows: Iterable<T>,
  config: ExcelConfig = ExcelConfig.Default,
  name: String = Path(path).nameWithoutExtension,
  block: SheetBuilder<T>.() -> Unit,
) {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
  generateExcel(path, rows.asSequence(), config, name, block)
}
