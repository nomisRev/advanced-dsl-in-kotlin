@file:OptIn(kotlin.experimental.ExperimentalTypeInference::class)

package presentation.support.trees

import presentation.support.Cell
import presentation.support.Column
import presentation.support.ExcelDsl
import presentation.support.SheetColumns
import presentation.support.WorkbookSheets
import presentation.support.reference

/** The typed expression stage used by Part 2, before delegated columns. */
sealed interface Formula<out T> {
  data class Ref<T>(val column: Column<T>) : Formula<T>
  data class Constant(val value: Double) : Formula<Double>
  data class Times(val left: Formula<Number>, val right: Formula<Number>) : Formula<Double>
  data class LessThan(val left: Formula<Number>, val right: Formula<Number>) : Formula<Boolean>
  data class In<T>(val target: Formula<T>, val values: Iterable<T>) : Formula<Boolean>
  data class Contains(val target: Formula<String>, val substring: String) : Formula<Boolean>
}

@ExcelDsl
class FormulaBuilder internal constructor() {
  internal fun times(first: Column<Number>, second: Column<Number>): Formula<Double> =
    Formula.Times(Formula.Ref(first), Formula.Ref(second))
}

context(scope: FormulaBuilder)
operator fun Column<Number>.times(other: Column<Number>): Formula<Double> =
  scope.times(this, other)

context(_: FormulaBuilder)
operator fun Formula<Number>.times(other: Double): Formula<Double> =
  Formula.Times(this, Formula.Constant(other))

private fun literal(value: Any?): String = when (value) {
  is String -> "\"${value.replace("\"", "\"\"")}\""
  is Number -> value.toString()
  is Boolean -> if (value) "TRUE" else "FALSE"
  else -> error("Unsupported formula literal: $value")
}

private fun Formula<*>.render(row: Int): String = when (this) {
  is Formula.Ref -> column.reference(row)
  is Formula.Constant -> value.toString()
  is Formula.Times -> "(${left.render(row)}*${right.render(row)})"
  is Formula.LessThan -> "${left.render(row)}<${right.render(row)}"
  is Formula.Contains -> "ISNUMBER(SEARCH(${literal(substring)},${target.render(row)}))"
  is Formula.In<*> -> values.joinToString(",", "OR(", ")") { "${target.render(row)}=${literal(it)}" }
}

@ExcelDsl
class SheetBuilder<T> : SheetColumns<T>() {
  @OverloadResolutionByLambdaReturnType
  @JvmName("textColumn")
  fun column(name: String, value: (T) -> String): Column<String> =
    Column(name, addColumn(name) { row, _ -> value(row) })

  @OverloadResolutionByLambdaReturnType
  @JvmName("numberColumn")
  fun column(name: String, value: (T) -> Number): Column<Number> =
    Column(name, addColumn(name) { row, _ -> value(row) })

  fun <V> formula(name: String, block: FormulaBuilder.() -> Formula<V>): Column<V> {
    val Formula = FormulaBuilder().block()
    return Column(name, addColumn(name) { _, row -> Cell.Formula(Formula.render(row)) })
  }
}

@ExcelDsl
class WorkbookBuilder : WorkbookSheets() {
  fun <T> sheet(name: String, rows: Sequence<T>, block: SheetBuilder<T>.() -> Unit) =
    addSheet(name, rows, SheetBuilder<T>().apply(block))
}

fun workbook(path: String, block: WorkbookBuilder.() -> Unit) =
  WorkbookBuilder().apply(block).write(path)
