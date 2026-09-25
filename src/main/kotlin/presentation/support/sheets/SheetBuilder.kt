package presentation.support.sheets

import kotlin.properties.PropertyDelegateProvider
import presentation.support.Cell
import presentation.support.ColumnDelegate
import presentation.support.DelegatedColumns
import presentation.support.ExcelDsl
import presentation.support.lists.ExcelFormulas

/*
 * Extra lesson "Sheets as values": the block of `sheet<T> { }` only builds
 * the schema. It has no path and no rows, so it runs once and returns an
 * immutable `Sheet<T>` that can be written any number of times.
 */

/** The formulas of Part 6: `*` by context from Part 5, and `sum` over a `List`. */
typealias Formulas = presentation.support.lists.Formulas

@ExcelDsl
class SheetBuilder<T> internal constructor() : DelegatedColumns<T>() {
  private val formulas = mutableMapOf<Int, Formulas.() -> Cell>()

  /**
   * Written as an Excel formula, and kept as a block too, so
   * [Sheet.evaluate] can run it again with [Evaluate].
   */
  fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> {
    val excel = formulaColumn { row -> ExcelFormulas(row).block() }
    return PropertyDelegateProvider { thisRef, property ->
      excel.provideDelegate(thisRef, property).also { delegate ->
        formulas[delegate.getValue(thisRef, property).index] = block
      }
    }
  }

  internal fun build(): Sheet<T> = Sheet(columnSpecs(), formulas.toMap())
}

/** Runs [block] once and returns the schema it declared. */
fun <T> sheet(block: SheetBuilder<T>.() -> Unit): Sheet<T> =
  SheetBuilder<T>().apply(block).build()
