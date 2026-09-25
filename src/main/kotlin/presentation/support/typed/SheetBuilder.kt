@file:OptIn(ExperimentalTypeInference::class)

package presentation.support.typed

import java.time.LocalDate
import kotlin.experimental.ExperimentalTypeInference
import presentation.support.ExcelDsl
import presentation.support.SheetColumns
import presentation.support.writeWorkbook

/*
 * Part 2: one `column` overload per cell type, picked by the lambda's return
 * type, with a `@JvmName` each so they don't clash on the JVM.
 */

@ExcelDsl
class SheetBuilder<T> : SheetColumns<T>() {
  @OverloadResolutionByLambdaReturnType
  @JvmName("textColumn")
  fun column(name: String, value: (T) -> String) {
    addColumn(name) { row, _ -> value(row) }
  }

  @OverloadResolutionByLambdaReturnType
  @JvmName("numberColumn")
  fun column(name: String, value: (T) -> Number) {
    addColumn(name) { row, _ -> value(row) }
  }

  @OverloadResolutionByLambdaReturnType
  @JvmName("dateColumn")
  fun column(name: String, value: (T) -> LocalDate) {
    addColumn(name) { row, _ -> value(row) }
  }
}

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) = writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
