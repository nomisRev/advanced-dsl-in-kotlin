@file:OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)

package presentation.support.tricks

import java.time.LocalDate
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import presentation.support.Cell
import presentation.support.ColumnDelegate
import presentation.support.DelegatedColumns
import presentation.support.ExcelDsl
import presentation.support.writeWorkbook

@ExcelDsl
class SheetBuilder<T> : DelegatedColumns<T>() {
  fun formula(block: Formulas.() -> Expr): ColumnDelegate<Number> {
    val expr = Scope.block()
    return formulaColumn { row -> Cell.Formula("=${expr.toExcel(row)}") }
  }

  /** `"Customer" { it.customer }`: the string is the header. */
  @OverloadResolutionByLambdaReturnType
  @JvmName("textColumn")
  operator fun String.invoke(value: (T) -> String) {
    addColumn(this) { row, _ -> value(row) }
  }

  @OverloadResolutionByLambdaReturnType
  @JvmName("numberColumn")
  operator fun String.invoke(value: (T) -> Number) {
    addColumn(this) { row, _ -> value(row) }
  }

  @OverloadResolutionByLambdaReturnType
  @JvmName("dateColumn")
  operator fun String.invoke(value: (T) -> LocalDate) {
    addColumn(this) { row, _ -> value(row) }
  }

  private object Scope : Formulas
}

/** The contract lets callers assign a `val` inside [block]. */
inline fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
  writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
}
