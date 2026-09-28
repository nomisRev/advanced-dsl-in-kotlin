@file:OptIn(ExperimentalTypeInference::class)

package presentation.support.covariant

import kotlin.experimental.ExperimentalTypeInference
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import presentation.support.Cell
import presentation.support.ExcelDsl
import presentation.support.SheetColumns
import presentation.support.reference
import presentation.support.writeWorkbook

/*
 * Extra "What the talk leaves out": a number column keeps its precise type,
 * `Column<Int>`, and `out V` lets it stand in for a `Column<Number>`.
 */

class Column<out V>(val name: String, val index: Int)

typealias ColumnDelegate<V> =
  PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Column<V>>>

interface Formulas {
  fun multiply(column: Column<Number>, other: Column<Number>): Cell
}

context(formulas: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Cell =
  formulas.multiply(this, other)

class ExcelFormulas(private val row: Int) : Formulas {
  override fun multiply(column: Column<Number>, other: Column<Number>): Cell =
    Cell.Formula("=${column.reference(row)}*${other.reference(row)}")

  private fun Column<*>.reference(row: Int): String =
    presentation.support.Column<Any?>(name, index).reference(row)
}

@ExcelDsl
class SheetBuilder<T> : SheetColumns<T>() {
  @OverloadResolutionByLambdaReturnType
  @JvmName("textColumn")
  fun column(value: (T) -> String): ColumnDelegate<String> =
    delegate { name -> addColumn(name) { row, _ -> value(row) } }

  @OverloadResolutionByLambdaReturnType
  @JvmName("intColumn")
  fun column(value: (T) -> Int): ColumnDelegate<Int> =
    delegate { name -> addColumn(name) { row, _ -> value(row) } }

  @OverloadResolutionByLambdaReturnType
  @JvmName("doubleColumn")
  fun column(value: (T) -> Double): ColumnDelegate<Double> =
    delegate { name -> addColumn(name) { row, _ -> value(row) } }

  fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> =
    delegate { name ->
      addColumn(name) { _, excelRow -> ExcelFormulas(excelRow).block() }
    }

  private fun <V> delegate(add: (name: String) -> Int): ColumnDelegate<V> =
    PropertyDelegateProvider { _, property ->
      val column = Column<V>(property.name, add(property.name))
      ReadOnlyProperty { _, _ -> column }
    }
}

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) = writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
