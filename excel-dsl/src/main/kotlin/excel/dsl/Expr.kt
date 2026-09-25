package excel.dsl

import java.math.RoundingMode

/**
 * A formula as a tree, built once per column by the operators of [Formulas]
 * and [Totals], then rendered for every row with [toExcel] or computed with
 * [evaluate].
 */
sealed interface Expr {
  data class Ref(val column: Column<Number>) : Expr
  data class Range(val column: Column<*>) : Expr

  data class Constant(val value: Double) : Expr {
    init {
      require(value.isFinite()) { "Excel has no literal for $value" }
    }
  }

  data class Plus(val left: Expr, val right: Expr) : Expr
  data class Minus(val left: Expr, val right: Expr) : Expr
  data class Times(val left: Expr, val right: Expr) : Expr
  data class Div(val left: Expr, val right: Expr) : Expr

  /** `left>right`: `TRUE` or `FALSE` in Excel, `1.0` or `0.0` in [evaluate]. */
  data class Compare(val left: Expr, val comparison: Comparison, val right: Expr) : Expr

  /** `SUM(...)` and friends, over at least one term. */
  data class Aggregate(val function: AggregateFunction, val terms: List<Expr>) : Expr {
    init {
      require(terms.isNotEmpty()) { "${function.excel} needs at least one term" }
    }
  }

  /** `ROUND(value, digits)`, half away from zero like Excel; negative [digits] round left of the point. */
  data class Round(val value: Expr, val digits: Int) : Expr

  /** `IF(condition, then, otherwise)`: any non-zero [condition] is true. */
  data class If(val condition: Expr, val then: Expr, val otherwise: Expr) : Expr
}

enum class Comparison(internal val excel: String) {
  Greater(">"), GreaterOrEqual(">="), Less("<"), LessOrEqual("<="), Equal("="), NotEqual("<>"),
}

/** Like Excel, every function skips empty cells, and all but [Count] skip text and booleans. */
enum class AggregateFunction(internal val excel: String) {
  Sum("SUM"), Average("AVERAGE"), Min("MIN"), Max("MAX"),

  /** `COUNTA`: the cells that are not empty, of any type. */
  Count("COUNTA"),
}

/**
 * The formula text for [row], the 1-based row number Excel shows, without
 * the leading `=`. [data] is the rows a [Expr.Range] spans, only known to a
 * totals row. Parentheses are added only where precedence needs them.
 *
 * @throws IllegalArgumentException for a [Expr.Range] without [data].
 */
fun Expr.toExcel(row: Int, data: IntRange? = null): String = when (this) {
  is Ref -> column.reference(row)
  is Range -> {
    requireNotNull(data) { "A range only exists in a totals row: $column" }
    "${column.reference(data.first)}:${column.reference(data.last)}"
  }

  is Constant -> literal(value)
  is Plus -> binary("+", left, right, row, data)
  is Minus -> binary("-", left, right, row, data)
  is Times -> binary("*", left, right, row, data)
  is Div -> binary("/", left, right, row, data)
  is Compare -> binary(comparison.excel, left, right, row, data)
  is Aggregate -> terms.map { it.toExcel(row, data) }.call(function.excel)
  is Round -> listOf(value.toExcel(row, data), "$digits").call("ROUND")
  is If -> listOf(condition, then, otherwise).map { it.toExcel(row, data) }.call("IF")
}

/**
 * The value of this formula for one row, without Excel. [row] maps each
 * referenced column to its value; in arithmetic a `null` counts as `0.0`,
 * like an empty cell, and aggregates skip it. Division follows IEEE 754,
 * so `x / 0.0` is infinite and `AVERAGE` of nothing is `NaN`, not `#DIV/0!`.
 *
 * @throws NoSuchElementException when a referenced column is missing from [row].
 * @throws IllegalArgumentException for a [Expr.Range], which only a totals row has.
 */
fun Expr.evaluate(row: Map<out Column<*>, Number?>): Double =
  evaluate(
    cell = { column ->
      if (column !in row) throw NoSuchElementException("No value for $column")
      row[column]
    },
    range = { throw IllegalArgumentException("A range only exists in a totals row: $it") },
  ).let { number(it) ?: 0.0 }

/** Preserves Boolean results; [cell] reads the current row, [range] every data row of a column. */
internal fun Expr.evaluate(cell: (Column<*>) -> Any?, range: (Column<*>) -> List<Any?>): Any {
  fun Expr.eval(): Any = evaluate(cell, range)
  fun Expr.numeric(): Double = number(eval()) ?: 0.0
  fun Expr.operand(): Any? = if (this is Expr.Ref) cell(column) else eval()

  return when (this) {
    is Ref -> cell(column).let { if (it is Boolean) it else number(it) ?: 0.0 }
    is Range -> throw IllegalArgumentException("A range is not a number: $column")
    is Constant -> value
    is Plus -> left.numeric() + right.numeric()
    is Minus -> left.numeric() - right.numeric()
    is Times -> left.numeric() * right.numeric()
    is Div -> left.numeric() / right.numeric()
    is Compare -> comparison.test(compare(left.operand(), right.operand()))
    is Aggregate -> {
      val values = terms.flatMap { term ->
        when (term) {
          is Range -> range(term.column)
          is Ref -> listOf(cell(term.column))
          // A Boolean passed directly to an aggregate counts as a number;
          // a Boolean read from a cell or range is skipped.
          else -> listOf(term.numeric())
        }
      }
      function.apply(values)
    }

    is Round -> value.numeric().let {
      if (it.isFinite()) it.toBigDecimal().setScale(digits, RoundingMode.HALF_UP).toDouble() else it
    }

    is If -> if (condition.numeric() != 0.0) then.eval() else otherwise.eval()
  }
}

/** The [Expr.Ref] and [Expr.Range] leaves of the tree, in reading order. */
internal fun Expr.references(): List<Expr> = when (this) {
  is Ref, is Range -> listOf(this)
  is Constant -> emptyList()
  is Plus -> left.references() + right.references()
  is Minus -> left.references() + right.references()
  is Times -> left.references() + right.references()
  is Div -> left.references() + right.references()
  is Compare -> left.references() + right.references()
  is Aggregate -> terms.flatMap { it.references() }
  is Round -> value.references()
  is If -> condition.references() + then.references() + otherwise.references()
}

internal val Expr.referencedColumn: Column<*>?
  get() = when (this) {
    is Ref -> column
    is Range -> column
    else -> null
  }

/** Excel reads `TRUE` as `1` in arithmetic, and a missing value as `0`. */
private fun number(value: Any?): Double? = when (value) {
  is Number -> value.toDouble()
  is Boolean -> if (value) 1.0 else 0.0
  else -> null
}

/** Booleans sort above numbers; a blank compares as false or zero, depending on the other operand. */
private fun compare(left: Any?, right: Any?): Int = when {
  left is Boolean && (right is Boolean || right == null) -> left.compareTo(right ?: false)
  right is Boolean && left == null -> false.compareTo(right)
  left is Boolean -> 1
  right is Boolean -> -1
  else -> {
    val l = number(left) ?: 0.0
    val r = number(right) ?: 0.0
    if (l == r) 0 else l.compareTo(r)
  }
}

private fun Comparison.test(order: Int): Boolean = when (this) {
  Comparison.Greater -> order > 0
  Comparison.GreaterOrEqual -> order >= 0
  Comparison.Less -> order < 0
  Comparison.LessOrEqual -> order <= 0
  Comparison.Equal -> order == 0
  Comparison.NotEqual -> order != 0
}

private fun AggregateFunction.apply(values: List<Any?>): Double {
  if (this == AggregateFunction.Count) return values.count { it != null }.toDouble()
  val numbers = values.filterIsInstance<Number>().map { it.toDouble() }
  return when (this) {
    AggregateFunction.Sum -> numbers.sum()
    AggregateFunction.Average -> numbers.sum() / numbers.size
    AggregateFunction.Min -> numbers.minOrNull() ?: 0.0
    AggregateFunction.Max -> numbers.maxOrNull() ?: 0.0
    AggregateFunction.Count -> error("handled above")
  }
}

private val Expr.precedence: Int
  get() = when (this) {
    is Compare -> 0
    is Plus, is Minus -> 1
    is Times, is Div -> 2
    is Ref, is Range, is Constant, is Aggregate, is Round, is If -> 3
  }

/** Left-associative: `a-b-c` stays bare, `a-(b-c)` keeps its parentheses. */
private fun Expr.binary(op: String, left: Expr, right: Expr, row: Int, data: IntRange?): String {
  val l = left.toExcel(row, data).let { if (left.precedence < precedence) "($it)" else it }
  val r = right.toExcel(row, data).let { if (right.precedence <= precedence) "($it)" else it }
  return "$l$op$r"
}

private fun List<String>.call(name: String): String = joinToString(", ", "$name(", ")")

/** `2` instead of `2.0`, `0.00001` instead of `1.0E-5`, `(-1)` so `B2*(-1)` parses. */
private fun literal(value: Double): String {
  val plain = value.toBigDecimal().stripTrailingZeros().toPlainString()
  return if (value < 0) "($plain)" else plain
}
