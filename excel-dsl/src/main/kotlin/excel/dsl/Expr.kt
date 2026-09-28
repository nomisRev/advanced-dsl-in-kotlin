package excel.dsl

import java.math.RoundingMode
import java.util.Collections

/** A typed formula tree, built once and rendered for each row. */
sealed class Expr<out V> private constructor() {
  @ConsistentCopyVisibility
  internal data class Ref<V> internal constructor(val column: Column<V>) : Expr<V>()

  /** A range is only valid as an aggregate operand in a totals formula. */
  internal data class Range(val column: Column<*>) : Expr<Nothing>()
  internal data class Constant(val value: Double) : Expr<Double>() {
    init {
      require(value.isFinite()) { "Excel has no literal for $value" }
    }
  }

  internal data class Text(val value: String) : Expr<String>()
  internal data class Bool(val value: Boolean) : Expr<Boolean>()
  internal data class Plus(val left: Expr<Number>, val right: Expr<Number>) : Expr<Double>()
  internal data class Minus(val left: Expr<Number>, val right: Expr<Number>) : Expr<Double>()
  internal data class Times(val left: Expr<Number>, val right: Expr<Number>) : Expr<Double>()
  internal data class Div(val left: Expr<Number>, val right: Expr<Number>) : Expr<Double>()

  @ConsistentCopyVisibility
  internal data class Compare internal constructor(val left: Expr<*>, val comparison: Comparison, val right: Expr<*>) :
    Expr<Boolean>()

  internal data class And(val left: Expr<Boolean>, val right: Expr<Boolean>) : Expr<Boolean>()
  internal data class Or(val left: Expr<Boolean>, val right: Expr<Boolean>) : Expr<Boolean>()
  internal data class Not(val value: Expr<Boolean>) : Expr<Boolean>()

  /** Owns a defensive snapshot: later changes to the input cannot change a sheet. */
  internal class Aggregate(val function: AggregateFunction, terms: List<Expr<*>>) : Expr<Double>() {
    val terms: List<Expr<*>> = Collections.unmodifiableList(terms.toList())

    init {
      require(this.terms.isNotEmpty()) { "${function.excel} needs at least one term" }
    }

    override fun equals(other: Any?): Boolean = other is Aggregate && function == other.function && terms == other.terms
    override fun hashCode(): Int = 31 * function.hashCode() + terms.hashCode()
    override fun toString(): String = "Aggregate($function, $terms)"
  }

  internal data class Round(val value: Expr<Number>, val digits: Int) : Expr<Double>()
  internal class If<V> internal constructor(val condition: Expr<Boolean>, val then: Expr<V>, val otherwise: Expr<V>) :
    Expr<V>() {
    override fun equals(other: Any?): Boolean =
      other is If<*> && condition == other.condition && then == other.then && otherwise == other.otherwise

    override fun hashCode(): Int = 31 * (31 * condition.hashCode() + then.hashCode()) + otherwise.hashCode()
  }
}

internal enum class Comparison(internal val excel: String) {
  Greater(">"), GreaterOrEqual(">="), Less("<"), LessOrEqual("<="), Equal("="), NotEqual("<>"),
}

internal enum class AggregateFunction(internal val excel: String) {
  Sum("SUM"), Average("AVERAGE"), Min("MIN"), Max("MAX"), Count("COUNT"), CountNonBlank("COUNTA"),
}

/** Spreadsheet errors remain errors through arithmetic, comparisons and conditions. */
enum class ExcelError(val text: String) {
  DivisionByZero("#DIV/0!"), Number("#NUM!"), Value("#VALUE!"),
}

/** A cell can contain a typed value, a blank, or a spreadsheet error. */
sealed interface CellResult<out V> {
  data class Value<V>(val value: V?) : CellResult<V>
  data class Error(val error: ExcelError) : CellResult<Nothing>
}

@Suppress("UNCHECKED_CAST")
internal fun <V> cellResult(value: Any?): CellResult<V> =
  if (value is ExcelError) CellResult.Error(value) else CellResult.Value(value as V?)

/** Typed lookup by column identity. A column from another schema is rejected. */
class EvaluatedRow internal constructor(private val values: Map<Column<*>, Any?>) {
  operator fun <V> get(column: Column<V>): CellResult<V> {
    require(column in values) { "$column is not part of this row" }
    return cellResult(values[column])
  }

  fun toMap(): Map<String, Any?> = values.mapKeys { it.key.name }
}

/** Formula text without the leading `=`; [data] is the range of data rows for totals. */
internal fun Expr<*>.toExcel(row: Int, data: IntRange? = null): String = when (this) {
  is Expr.Ref -> column.reference(row).let {
    when (column.kind) {
      ValueKind.Boolean -> "IF($it,TRUE(),FALSE())"
      ValueKind.Text -> "T($it)"
      ValueKind.Number, ValueKind.Date, ValueKind.DateTime, ValueKind.Instant -> it
    }
  }

  is Expr.Range -> {
    requireNotNull(data) { "A range only exists in a totals row: $column" }
    "${column.reference(data.first)}:${column.reference(data.last)}"
  }

  is Expr.Constant -> literal(value)
  is Expr.Text -> "\"${value.replace("\"", "\"\"")}\""
  is Expr.Bool -> if (value) "TRUE()" else "FALSE()"
  is Expr.Plus -> binary("+", left, right, row, data)
  is Expr.Minus -> binary("-", left, right, row, data)
  is Expr.Times -> binary("*", left, right, row, data)
  is Expr.Div -> binary("/", left, right, row, data)
  is Expr.Compare -> binary(comparison.excel, left, right, row, data)
  is Expr.And -> listOf(left, right).map { it.toExcel(row, data) }.call("AND")
  is Expr.Or -> listOf(left, right).map { it.toExcel(row, data) }.call("OR")
  is Expr.Not -> listOf(value.toExcel(row, data)).call("NOT")
  is Expr.Aggregate -> terms.map {
    // References passed to an aggregate must stay references: SUM skips Boolean cells.
    if (it is Expr.Ref) it.column.reference(row) else it.toExcel(row, data)
  }.call(function.excel)

  is Expr.Round -> listOf(value.toExcel(row, data), "$digits").call("ROUND")
  is Expr.If -> listOf(condition, then, otherwise).map { it.toExcel(row, data) }.call("IF")
}

/** Evaluate without Excel. Missing columns throw; blanks and spreadsheet errors are preserved. */
internal fun <V> Expr<V>.evaluate(row: Map<out Column<*>, Any?>): CellResult<V> = cellResult(
  evaluateRaw(
    cell = { column ->
      if (column !in row) throw NoSuchElementException("No value for $column")
      row[column]
    },
    range = { throw IllegalArgumentException("A range only exists in a totals row: $it") },
  )
)

internal fun Expr<*>.evaluateRaw(cell: (Column<*>) -> Any?, range: (Column<*>) -> RangeSummary): Any? {
  fun Expr<*>.eval(): Any? = evaluateRaw(cell, range)
  fun pair(left: Expr<*>, right: Expr<*>, apply: (Any?, Any?) -> Any?): Any? {
    val l = left.eval()
    val r = right.eval()
    return if (l is ExcelError) l else if (r is ExcelError) r else apply(l, r)
  }

  fun numeric(left: Expr<Number>, right: Expr<Number>, apply: (Double, Double) -> Double): Any? =
    pair(left, right) { l, r -> finite(apply(number(l), number(r))) }
  return when (this) {
    is Expr.Ref -> when (val value = cell(column)) {
      is ExcelError -> value
      is Number -> finite(value.toDouble())
      null -> when (column.kind) {
        ValueKind.Boolean -> false
        ValueKind.Text -> ""
        ValueKind.Number, ValueKind.Date, ValueKind.DateTime, ValueKind.Instant -> 0.0
      }
      else -> value
    }

    is Expr.Range -> error("A range must be an aggregate operand: $column")
    is Expr.Constant -> value
    is Expr.Text -> value
    is Expr.Bool -> value
    is Expr.Plus -> numeric(left, right, Double::plus)
    is Expr.Minus -> numeric(left, right, Double::minus)
    is Expr.Times -> numeric(left, right, Double::times)
    is Expr.Div -> pair(left, right) { l, r ->
      if (number(r) == 0.0) ExcelError.DivisionByZero else finite(number(l) / number(r))
    }

    is Expr.Compare -> pair(left, right) { l, r -> comparison.test(compare(l, r)) }
    is Expr.And -> pair(left, right) { l, r -> l == true && r == true }
    is Expr.Or -> pair(left, right) { l, r -> l == true || r == true }
    is Expr.Not -> value.eval().let { if (it is ExcelError) it else it != true }
    is Expr.Aggregate -> RangeSummary().also { summary ->
      terms.forEach { term ->
        when (term) {
          is Expr.Range -> summary.merge(range(term.column))
          is Expr.Ref -> summary.add(cell(term.column))
          is Expr.Constant, is Expr.Text, is Expr.Bool, is Expr.Plus, is Expr.Minus, is Expr.Times, is Expr.Div,
          is Expr.Compare, is Expr.And, is Expr.Or, is Expr.Not, is Expr.Aggregate, is Expr.Round, is Expr.If ->
            summary.add(term.eval(), direct = true)
        }
      }
    }.result(function)

    is Expr.Round -> when (val evaluated = value.eval()) {
      is ExcelError -> evaluated
      // Excel's useful precision is far below these bounds; avoid huge BigDecimals.
      else if digits > 308 -> number(evaluated)
      else if digits < -308 -> 0.0
      else -> finite(number(evaluated).toBigDecimal().setScale(digits, RoundingMode.HALF_UP).toDouble())
    }

    is Expr.If -> when (val evaluated = condition.eval()) {
      is ExcelError -> evaluated
      true -> then.eval()
      else -> otherwise.eval()
    }
  }
}

/** Constant space per referenced range, independent of the number of rows. */
internal class RangeSummary {
  private var count = 0L
  private var numericCount = 0L
  private var sum = 0.0
  private var min = Double.POSITIVE_INFINITY
  private var max = Double.NEGATIVE_INFINITY
  private var error: ExcelError? = null
  fun add(value: Any?, direct: Boolean = false) {
    if (value == null) return
    count++
    if (value is ExcelError) {
      if (error == null) error = value; return
    }
    val n = when (value) {
      is Number -> value.toDouble()
      is Boolean if direct -> if (value) 1.0 else 0.0
      is String if direct -> {
        val double = value.toDoubleOrNull()
        if (double == null) {
          error = ExcelError.Value
          return
        } else {
          double
        }
      }
      else -> return
    }
    if (!n.isFinite()) {
      error = ExcelError.Number; return
    }
    numericCount++
    sum += n
    min = minOf(min, n)
    max = maxOf(max, n)
  }

  fun merge(other: RangeSummary) {
    count += other.count
    numericCount += other.numericCount
    sum += other.sum
    min = minOf(min, other.min)
    max = maxOf(max, other.max)
    if (error == null) error = other.error
  }

  fun result(function: AggregateFunction): Any {
    val failure = error
    return when (function) {
      AggregateFunction.CountNonBlank -> count.toDouble()
      AggregateFunction.Count -> numericCount.toDouble()
      else if failure != null -> failure
      AggregateFunction.Sum -> finite(sum)
      AggregateFunction.Average -> if (numericCount == 0L) ExcelError.DivisionByZero else finite(sum / numericCount)
      AggregateFunction.Min -> if (numericCount == 0L) 0.0 else min
      AggregateFunction.Max -> if (numericCount == 0L) 0.0 else max
    }
  }
}

internal fun Expr<*>.children(): List<Expr<*>> = when (this) {
  is Expr.Ref, is Expr.Range, is Expr.Constant, is Expr.Text, is Expr.Bool -> emptyList()
  is Expr.Plus -> listOf(left, right)
  is Expr.Minus -> listOf(left, right)
  is Expr.Times -> listOf(left, right)
  is Expr.Div -> listOf(left, right)
  is Expr.Compare -> listOf(left, right)
  is Expr.And -> listOf(left, right)
  is Expr.Or -> listOf(left, right)
  is Expr.Not -> listOf(value)
  is Expr.Aggregate -> terms
  is Expr.Round -> listOf(value)
  is Expr.If -> listOf(condition, then, otherwise)
}

internal fun Expr<*>.references(): List<Expr<*>> =
  if (this is Expr.Ref || this is Expr.Range) listOf(this) else children().flatMap { it.references() }

internal val Expr<*>.referencedColumn: Column<*>?
  get() = when (this) {
    is Expr.Ref -> column
    is Expr.Range -> column
    is Expr.Constant, is Expr.Text, is Expr.Bool, is Expr.Plus, is Expr.Minus, is Expr.Times, is Expr.Div,
    is Expr.Compare, is Expr.And, is Expr.Or, is Expr.Not, is Expr.Aggregate, is Expr.Round, is Expr.If -> null
  }

internal val Expr<*>.kind: ValueKind
  get() = when (this) {
    is Expr.Ref -> column.kind
    is Expr.Text -> ValueKind.Text
    is Expr.Bool, is Expr.Compare, is Expr.And, is Expr.Or, is Expr.Not -> ValueKind.Boolean
    is Expr.If -> then.kind
    is Expr.Range, is Expr.Constant, is Expr.Plus, is Expr.Minus, is Expr.Times, is Expr.Div,
    is Expr.Aggregate, is Expr.Round -> ValueKind.Number
  }

private fun finite(value: Double): Any = if (value.isFinite()) value else ExcelError.Number
private fun number(value: Any?): Double = (value as? Number)?.toDouble() ?: 0.0
private fun compare(left: Any?, right: Any?): Int = when (left) {
  is String if right is String -> left.compareTo(right, ignoreCase = true)
  is Boolean if right is Boolean -> left.compareTo(right)
  is Boolean -> 1
  else if right is Boolean -> -1
  is String -> 1
  else if right is String -> -1
  else -> if (number(left) == number(right)) 0 else number(left).compareTo(number(right))
}

private fun Comparison.test(order: Int): Boolean = when (this) {
  Comparison.Greater -> order > 0
  Comparison.GreaterOrEqual -> order >= 0
  Comparison.Less -> order < 0
  Comparison.LessOrEqual -> order <= 0
  Comparison.Equal -> order == 0
  Comparison.NotEqual -> order != 0
}

private val Expr<*>.precedence: Int
  get() = when (this) {
    is Expr.Compare -> 0
    is Expr.Plus, is Expr.Minus -> 1
    is Expr.Times, is Expr.Div -> 2
    is Expr.Ref, is Expr.Range, is Expr.Constant, is Expr.Text, is Expr.Bool, is Expr.And, is Expr.Or, is Expr.Not,
    is Expr.Aggregate, is Expr.Round, is Expr.If -> 3
  }

private fun Expr<*>.binary(op: String, left: Expr<*>, right: Expr<*>, row: Int, data: IntRange?): String {
  val l = left.toExcel(row, data).let { if (left.precedence < precedence) "($it)" else it }
  val r = right.toExcel(row, data).let { if (right.precedence <= precedence) "($it)" else it }
  return "$l$op$r"
}

private fun List<String>.call(name: String): String = joinToString(", ", "$name(", ")")
private fun literal(value: Double): String {
  val plain = value.toBigDecimal().stripTrailingZeros().toPlainString()
  return if (value < 0) "($plain)" else plain
}
