@file:OptIn(ExperimentalTypeInference::class)

package presentation.support.contexts

import java.time.LocalDate
import kotlin.experimental.ExperimentalTypeInference
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import presentation.support.Cell
import presentation.support.Column
import presentation.support.ColumnDelegate
import presentation.support.CustomType
import presentation.support.ExcelDsl
import presentation.support.SheetColumns
import presentation.support.writeWorkbook

@ExcelDsl
class SheetBuilder<T> : SheetColumns<T>() {
  private val header: (propertyName: String) -> String = { it }

  fun <V> register(value: (T) -> V): ColumnDelegate<V> =
    delegate { name -> addColumn(name) { row, _ -> value(row) } }

  private fun <V> delegate(add: (name: String) -> Int): ColumnDelegate<V> =
    PropertyDelegateProvider { _, property ->
      val name = header(property.name)
      val column = Column<V>(name, add(name))
      ReadOnlyProperty { _, _ -> column }
    }

  fun formulaColumn(cell: (excelRow: Int) -> Cell): ColumnDelegate<Number> =
    delegate { name -> addColumn(name) { _, excelRow -> cell(excelRow) } }

  fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> =
    formulaColumn { row -> ExcelFormulas(row).block() }
}

@OverloadResolutionByLambdaReturnType
@JvmName("textColumn")
context(builder: SheetBuilder<T>)
fun <T> column(value: (T) -> String): ColumnDelegate<String> = builder.register(value)

@OverloadResolutionByLambdaReturnType
@JvmName("numberColumn")
context(builder: SheetBuilder<T>)
fun <T> column(value: (T) -> Number): ColumnDelegate<Number> = builder.register(value)

@OverloadResolutionByLambdaReturnType
@JvmName("dateColumn")
context(builder: SheetBuilder<T>)
fun <T> column(value: (T) -> LocalDate): ColumnDelegate<LocalDate> = builder.register(value)

@OverloadResolutionByLambdaReturnType
@JvmName("customColumn")
context(builder: SheetBuilder<T>)
fun <T> column(value: (T) -> CustomType): ColumnDelegate<String> =
  column { value(it).toColumnText() }

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) = writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
