package excel.dsl

import java.math.RoundingMode
import java.util.Collections

private const val MAX_FORMULA_TEXT_LENGTH = 255
private const val MAX_FUNCTION_ARGUMENTS = 255

/** A typed formula tree, built once and rendered for each row. */
sealed class Formula<out V> private constructor() {
  @ConsistentCopyVisibility
  internal data class Ref<V> internal constructor(val column: Column<V>) : Formula<V>()

  /** A range is only valid as an aggregate operand in a totals formula. */
  internal data class Range(val column: Column<*>) : Formula<Nothing>()
  internal data class Constant(val value: Double) : Formula<Double>() {
    init {
      require(value.isExcelNumber()) { "Excel has no literal for $value" }
    }
  }

  internal data class Text(val value: String) : Formula<String>() {
    init {
      require(value.length <= MAX_FORMULA_TEXT_LENGTH) {
        "Excel formula text literals hold at most $MAX_FORMULA_TEXT_LENGTH characters; got ${value.length}"
      }
    }
  }
  internal data class Bool(val value: Boolean) : Formula<Boolean>()
  internal data class Plus(val left: Formula<Number>, val right: Formula<Number>) : Formula<Double>()
  internal data class Minus(val left: Formula<Number>, val right: Formula<Number>) : Formula<Double>()
  internal data class Times(val left: Formula<Number>, val right: Formula<Number>) : Formula<Double>()
  internal data class Div(val left: Formula<Number>, val right: Formula<Number>) : Formula<Double>()

  @ConsistentCopyVisibility
  internal data class Compare internal constructor(val left: Formula<*>, val comparison: Comparison, val right: Formula<*>) :
    Formula<Boolean>()

  internal data class And(val left: Formula<Boolean>, val right: Formula<Boolean>) : Formula<Boolean>()
  internal data class Or(val left: Formula<Boolean>, val right: Formula<Boolean>) : Formula<Boolean>()
  internal data class Not(val value: Formula<Boolean>) : Formula<Boolean>()

  /** Owns a defensive snapshot: later changes to the input cannot change a sheet. */
  internal class Aggregate(val function: AggregateFunction, terms: List<Formula<*>>) : Formula<Double>() {
    val terms: List<Formula<*>> = Collections.unmodifiableList(terms.toList())

    init {
      require(this.terms.isNotEmpty()) { "${function.excel} needs at least one term" }
      require(this.terms.size <= MAX_FUNCTION_ARGUMENTS) {
        "${function.excel} accepts at most $MAX_FUNCTION_ARGUMENTS arguments; got ${this.terms.size}"
      }
    }

    override fun equals(other: Any?): Boolean = other is Aggregate && function == other.function && terms == other.terms
    override fun hashCode(): Int = 31 * function.hashCode() + terms.hashCode()
    override fun toString(): String = "Aggregate($function, $terms)"
  }

  internal data class Round(val value: Formula<Number>, val digits: Int) : Formula<Double>()
  internal class If<V> internal constructor(val condition: Formula<Boolean>, val then: Formula<V>, val otherwise: Formula<V>) :
    Formula<V>() {
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
internal fun Formula<*>.toExcel(row: Int, data: IntRange? = null): String = when (this) {
  is Formula.Ref -> column.reference(row).let {
    when (column.kind) {
      ValueKind.Boolean -> "IF($it,TRUE(),FALSE())"
      ValueKind.Text -> "T($it)"
      ValueKind.Number, ValueKind.Date, ValueKind.DateTime, ValueKind.Instant -> it
    }
  }

  is Formula.Range -> {
    requireNotNull(data) { "A range only exists in a totals row: $column" }
    "${column.reference(data.first)}:${column.reference(data.last)}"
  }

  is Formula.Constant -> literal(value)
  is Formula.Text -> "\"${value.replace("\"", "\"\"")}\""
  is Formula.Bool -> if (value) "TRUE()" else "FALSE()"
  is Formula.Plus -> binary("+", left, right, row, data)
  is Formula.Minus -> binary("-", left, right, row, data)
  is Formula.Times -> binary("*", left, right, row, data)
  is Formula.Div -> binary("/", left, right, row, data)
  is Formula.Compare -> binary(comparison.excel, left, right, row, data)
  is Formula.And -> listOf(left, right).map { it.toExcel(row, data) }.call("AND")
  is Formula.Or -> listOf(left, right).map { it.toExcel(row, data) }.call("OR")
  is Formula.Not -> listOf(value.toExcel(row, data)).call("NOT")
  is Formula.Aggregate -> terms.map {
    // References passed to an aggregate must stay references: SUM skips Boolean cells.
    if (it is Formula.Ref) it.column.reference(row) else it.toExcel(row, data)
  }.call(function.excel)

  is Formula.Round -> listOf(value.toExcel(row, data), "$digits").call("ROUND")
  is Formula.If -> listOf(condition, then, otherwise).map { it.toExcel(row, data) }.call("IF")
}

/** Evaluate without Excel. Missing columns throw; blanks and spreadsheet errors are preserved. */
internal fun <V> Formula<V>.evaluate(row: Map<out Column<*>, Any?>): CellResult<V> = cellResult(
  evaluateRaw(
    cell = { column ->
      if (column !in row) throw NoSuchElementException("No value for $column")
      row[column]
    },
    range = { throw IllegalArgumentException("A range only exists in a totals row: $it") },
  )
)

internal fun Formula<*>.evaluateRaw(cell: (Column<*>) -> Any?, range: (Column<*>) -> RangeSummary): Any? {
  fun Formula<*>.eval(): Any? = evaluateRaw(cell, range)
  fun pair(left: Formula<*>, right: Formula<*>, apply: (Any?, Any?) -> Any?): Any? {
    val l = left.eval()
    val r = right.eval()
    return if (l is ExcelError) l else if (r is ExcelError) r else apply(l, r)
  }

  fun numeric(left: Formula<Number>, right: Formula<Number>, apply: (Double, Double) -> Double): Any? =
    pair(left, right) { l, r -> finite(apply(number(l), number(r))) }
  return when (this) {
    is Formula.Ref -> when (val value = cell(column)) {
      is ExcelError -> value
      is Number -> finite(value.toDouble())
      null -> when (column.kind) {
        ValueKind.Boolean -> false
        ValueKind.Text -> ""
        ValueKind.Number, ValueKind.Date, ValueKind.DateTime, ValueKind.Instant -> 0.0
      }
      else -> value
    }

    is Formula.Range -> error("A range must be an aggregate operand: $column")
    is Formula.Constant -> value
    is Formula.Text -> value
    is Formula.Bool -> value
    is Formula.Plus -> numeric(left, right, Double::plus)
    is Formula.Minus -> numeric(left, right, Double::minus)
    is Formula.Times -> numeric(left, right, Double::times)
    is Formula.Div -> pair(left, right) { l, r ->
      if (number(r) == 0.0) ExcelError.DivisionByZero else finite(number(l) / number(r))
    }

    is Formula.Compare -> pair(left, right) { l, r -> comparison.test(compare(l, r)) }
    is Formula.And -> pair(left, right) { l, r -> l == true && r == true }
    is Formula.Or -> pair(left, right) { l, r -> l == true || r == true }
    is Formula.Not -> value.eval().let { if (it is ExcelError) it else it != true }
    is Formula.Aggregate -> RangeSummary().also { summary ->
      terms.forEach { term ->
        when (term) {
          is Formula.Range -> summary.merge(range(term.column))
          is Formula.Ref -> summary.add(cell(term.column))
          is Formula.Constant, is Formula.Text, is Formula.Bool, is Formula.Plus, is Formula.Minus, is Formula.Times, is Formula.Div,
          is Formula.Compare, is Formula.And, is Formula.Or, is Formula.Not, is Formula.Aggregate, is Formula.Round, is Formula.If ->
            summary.add(term.eval(), direct = true)
        }
      }
    }.result(function)

    is Formula.Round -> when (val evaluated = value.eval()) {
      is ExcelError -> evaluated
      // Excel's useful precision is far below these bounds; avoid huge BigDecimals.
      else if digits > 308 -> number(evaluated)
      else if digits < -308 -> 0.0
      else -> finite(number(evaluated).toBigDecimal().setScale(digits, RoundingMode.HALF_UP).toDouble())
    }

    is Formula.If -> when (val evaluated = condition.eval()) {
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
    sum = (sum + n).flushUnderflow()
    min = minOf(min, n)
    max = maxOf(max, n)
  }

  fun merge(other: RangeSummary) {
    count += other.count
    numericCount += other.numericCount
    sum = (sum + other.sum).flushUnderflow()
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

internal fun Formula<*>.children(): List<Formula<*>> = when (this) {
  is Formula.Ref, is Formula.Range, is Formula.Constant, is Formula.Text, is Formula.Bool -> emptyList()
  is Formula.Plus -> listOf(left, right)
  is Formula.Minus -> listOf(left, right)
  is Formula.Times -> listOf(left, right)
  is Formula.Div -> listOf(left, right)
  is Formula.Compare -> listOf(left, right)
  is Formula.And -> listOf(left, right)
  is Formula.Or -> listOf(left, right)
  is Formula.Not -> listOf(value)
  is Formula.Aggregate -> terms
  is Formula.Round -> listOf(value)
  is Formula.If -> listOf(condition, then, otherwise)
}

internal fun Formula<*>.references(): List<Formula<*>> =
  if (this is Formula.Ref || this is Formula.Range) listOf(this) else children().flatMap { it.references() }

internal val Formula<*>.referencedColumn: Column<*>?
  get() = when (this) {
    is Formula.Ref -> column
    is Formula.Range -> column
    is Formula.Constant, is Formula.Text, is Formula.Bool, is Formula.Plus, is Formula.Minus, is Formula.Times, is Formula.Div,
    is Formula.Compare, is Formula.And, is Formula.Or, is Formula.Not, is Formula.Aggregate, is Formula.Round, is Formula.If -> null
  }

internal val Formula<*>.kind: ValueKind
  get() = when (this) {
    is Formula.Ref -> column.kind
    is Formula.Text -> ValueKind.Text
    is Formula.Bool, is Formula.Compare, is Formula.And, is Formula.Or, is Formula.Not -> ValueKind.Boolean
    is Formula.If -> then.kind
    is Formula.Range, is Formula.Constant, is Formula.Plus, is Formula.Minus, is Formula.Times, is Formula.Div,
    is Formula.Aggregate, is Formula.Round -> ValueKind.Number
  }

private fun finite(value: Double): Any = if (value.isFinite()) value.flushUnderflow() else ExcelError.Number
private fun number(value: Any?): Double = (value as? Number)?.toDouble() ?: 0.0
private fun compare(left: Any?, right: Any?): Int = when (left) {
  is String if right is String -> left.compareTo(right, ignoreCase = true)
  is Boolean if right is Boolean -> left.compareTo(right)
  is Boolean -> 1
  else if right is Boolean -> -1
  is String -> 1
  else if right is String -> -1
  else -> compareExcelNumbers(number(left), number(right))
}

private fun Comparison.test(order: Int): Boolean = when (this) {
  Comparison.Greater -> order > 0
  Comparison.GreaterOrEqual -> order >= 0
  Comparison.Less -> order < 0
  Comparison.LessOrEqual -> order <= 0
  Comparison.Equal -> order == 0
  Comparison.NotEqual -> order != 0
}

private val Formula<*>.precedence: Int
  get() = when (this) {
    is Formula.Compare -> 0
    is Formula.Plus, is Formula.Minus -> 1
    is Formula.Times, is Formula.Div -> 2
    is Formula.Ref, is Formula.Range, is Formula.Constant, is Formula.Text, is Formula.Bool, is Formula.And, is Formula.Or, is Formula.Not,
    is Formula.Aggregate, is Formula.Round, is Formula.If -> 3
  }

private fun Formula<*>.binary(op: String, left: Formula<*>, right: Formula<*>, row: Int, data: IntRange?): String {
  val l = left.toExcel(row, data).let { if (left.precedence < precedence) "($it)" else it }
  val r = right.toExcel(row, data).let { if (right.precedence <= precedence) "($it)" else it }
  return "$l$op$r"
}

private fun List<String>.call(name: String): String = joinToString(", ", "$name(", ")")
private fun literal(value: Double): String {
  val plain = value.toBigDecimal().stripTrailingZeros().toPlainString()
  return if (value < 0) "($plain)" else plain
}
