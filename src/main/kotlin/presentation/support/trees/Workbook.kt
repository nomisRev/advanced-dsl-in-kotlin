@file:OptIn(kotlin.experimental.ExperimentalTypeInference::class)

package presentation.support.trees

import presentation.support.Cell
import presentation.support.Column
import presentation.support.ExcelDsl
import presentation.support.SheetColumns
import presentation.support.WorkbookSheets
import presentation.support.reference

/** The typed expression stage used by Part 2, before delegated columns. */
sealed interface Expr<out T> {
  data class Ref<T>(val column: Column<T>) : Expr<T>
  data class Constant(val value: Double) : Expr<Double>
  data class Times(val left: Expr<Number>, val right: Expr<Number>) : Expr<Double>
  data class In<T>(val target: Expr<T>, val values: Iterable<T>) : Expr<Boolean>
  data class Contains(val target: Expr<String>, val substring: String) : Expr<Boolean>
}

@ExcelDsl
class FormulaScope internal constructor() {
  internal fun times(first: Column<Number>, second: Column<Number>): Expr<Double> =
    Expr.Times(Expr.Ref(first), Expr.Ref(second))
}

context(scope: FormulaScope)
operator fun Column<Number>.times(other: Column<Number>): Expr<Double> =
  scope.times(this, other)

context(_: FormulaScope)
operator fun Expr<Number>.times(other: Double): Expr<Double> =
  Expr.Times(this, Expr.Constant(other))

private fun literal(value: Any?): String = when (value) {
  is String -> "\"${value.replace("\"", "\"\"")}\""
  is Number -> value.toString()
  is Boolean -> if (value) "TRUE" else "FALSE"
  else -> error("Unsupported formula literal: $value")
}

private fun Expr<*>.render(row: Int): String = when (this) {
  is Expr.Ref -> column.reference(row)
  is Expr.Constant -> value.toString()
  is Expr.Times -> "(${left.render(row)}*${right.render(row)})"
  is Expr.Contains -> "ISNUMBER(SEARCH(${literal(substring)},${target.render(row)}))"
  is Expr.In<*> -> values.joinToString(",", "OR(", ")") { "${target.render(row)}=${literal(it)}" }
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

  fun <V> formula(name: String, block: FormulaScope.() -> Expr<V>): Column<V> {
    val expr = FormulaScope().block()
    return Column(name, addColumn(name) { _, row -> Cell.Formula(expr.render(row)) })
  }
}

@ExcelDsl
class WorkbookBuilder : WorkbookSheets() {
  fun <T> sheet(name: String, rows: Sequence<T>, block: SheetBuilder<T>.() -> Unit) =
    addSheet(name, rows, SheetBuilder<T>().apply(block))
}

fun workbook(path: String, block: WorkbookBuilder.() -> Unit) =
  WorkbookBuilder().apply(block).write(path)
