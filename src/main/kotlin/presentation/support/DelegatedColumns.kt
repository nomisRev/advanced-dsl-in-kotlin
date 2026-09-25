@file:OptIn(ExperimentalTypeInference::class)

package presentation.support

import java.time.LocalDate
import kotlin.experimental.ExperimentalTypeInference
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

typealias ColumnDelegate<V> =
  PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Column<V>>>

/**
 * `val name by column { }` from Part 3 on: the property names the header,
 * and the column is registered when `by` runs, not when it is read.
 *
 * Each stage adds its own `formula { }`, since the formulas it offers differ.
 */
@ExcelDsl
abstract class DelegatedColumns<T> : SheetColumns<T>() {
  @OverloadResolutionByLambdaReturnType
  @JvmName("textColumn")
  fun column(value: (T) -> String): ColumnDelegate<String> = register(value)

  @OverloadResolutionByLambdaReturnType
  @JvmName("numberColumn")
  fun column(value: (T) -> Number): ColumnDelegate<Number> = register(value)

  @OverloadResolutionByLambdaReturnType
  @JvmName("dateColumn")
  fun column(value: (T) -> LocalDate): ColumnDelegate<LocalDate> =
    register(value)

  /** Registers a column whose cells [cell] computes from the Excel row. */
  protected fun formulaColumn(cell: (excelRow: Int) -> Cell): ColumnDelegate<Number> =
    delegate { name -> addColumn(name) { _, excelRow -> cell(excelRow) } }

  private fun <V> register(value: (T) -> V): ColumnDelegate<V> =
    delegate { name -> addColumn(name) { row, _ -> value(row) } }

  private fun <V> delegate(add: (name: String) -> Int): ColumnDelegate<V> =
    PropertyDelegateProvider { _, property ->
      val column = Column<V>(property.name, add(property.name))
      ReadOnlyProperty { _, _ -> column }
    }
}
