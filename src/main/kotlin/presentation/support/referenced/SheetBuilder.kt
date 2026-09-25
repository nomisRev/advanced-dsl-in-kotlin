@file:OptIn(ExperimentalTypeInference::class)

package presentation.support.referenced

import java.time.LocalDate
import kotlin.experimental.ExperimentalTypeInference
import presentation.support.Cell
import presentation.support.Column
import presentation.support.ExcelDsl
import presentation.support.SheetColumns
import presentation.support.delegated.ExcelFormulas
import presentation.support.delegated.Formulas
import presentation.support.writeWorkbook

/*
 * Part 3 before delegates: `column("Hours") { }` returns its `Column`, so
 * `formula("Total") { hours * rate }` can reference it. Every column is named
 * twice, once by the `val` and once by the header.
 */

@ExcelDsl
class SheetBuilder<T> : SheetColumns<T>() {
  @OverloadResolutionByLambdaReturnType
  @JvmName("textColumn")
  fun column(name: String, value: (T) -> String): Column<String> =
    register(name, value)

  @OverloadResolutionByLambdaReturnType
  @JvmName("numberColumn")
  fun column(name: String, value: (T) -> Number): Column<Number> =
    register(name, value)

  @OverloadResolutionByLambdaReturnType
  @JvmName("dateColumn")
  fun column(name: String, value: (T) -> LocalDate): Column<LocalDate> =
    register(name, value)

  /** A column of formulas, [block] runs for every row with its Excel row. */
  fun formula(name: String, block: Formulas.() -> Cell): Column<Number> {
    val index = addColumn(name) { _, excelRow -> ExcelFormulas(excelRow).block() }
    return Column(name, index)
  }

  private fun <V> register(name: String, value: (T) -> V): Column<V> =
    Column(name, addColumn(name) { row, _ -> value(row) })
}

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) = writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
