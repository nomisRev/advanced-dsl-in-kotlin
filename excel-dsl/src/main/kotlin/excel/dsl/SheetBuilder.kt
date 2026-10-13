@file:OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)

package excel.dsl

import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

/** What `by column { }` and `by formula { }` return: the column registers when `by` runs. */
typealias ColumnDelegate<V> =
  PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Column<V>>>

/**
 * How the DSL turns a property name into a header, and the time zone an
 * `Instant` is shown in, since Excel dates have none.
 */
class ExcelConfig(
  val zone: ZoneId = ZoneOffset.UTC,
  val columnNameStyle: (propertyName: String) -> String = { it.replaceFirstChar(Char::uppercase) },
) {
  companion object {
    /** Capitalizes the first letter, `issuedOn` becomes `IssuedOn`, and shows instants in UTC. */
    val Default: ExcelConfig = ExcelConfig()

    /** Keeps the property name as it is. */
    val PropertyNames: ExcelConfig = ExcelConfig { it }
  }
}

/**
 * The receiver of [sheet]: every `column`, `formula` or `"Header" { }` adds a
 * column, in the order they run. The column type follows the lambda's
 * return type; a `null` leaves the cell empty. For a type of your own, map
 * it to one of these in an extension with its own name:
 *
 * ```kotlin
 * fun <T> SheetBuilder<T>.money(value: (T) -> Money?) =
 *   column(CellFormat.currency("€")) { value(it)?.amount }
 * ```
 *
 * Every column takes an optional [CellFormat], the type's default otherwise,
 * and a width in characters, fitted to the header otherwise.
 */
@ExcelDsl
class SheetBuilder<T> @PublishedApi internal constructor(private val config: ExcelConfig) {
  private val columns = mutableListOf<ColumnSpec<T>>()
  private val totals = mutableMapOf<Column<*>, TotalSpec>()

  /** Keeps the header row visible while scrolling. */
  var freezePanes: Boolean = true

  /** Adds Excel's filter buttons to the header, over the data rows. */
  var autoFilter: Boolean = false

  @OverloadResolutionByLambdaReturnType
  @JvmName("textColumn")
  fun column(format: CellFormat? = null, width: Int? = null, value: (T) -> String?): ColumnDelegate<String> =
    delegate { data(it, format, width, ValueKind.Text, value) }

  /** Written as a double, which Excel stores with 15 significant digits. */
  @OverloadResolutionByLambdaReturnType
  @JvmName("numberColumn")
  fun column(format: CellFormat? = null, width: Int? = null, value: (T) -> Number?): ColumnDelegate<Number> =
    delegate { data(it, format, width, ValueKind.Number, value) }

  /** Checked against Excel's numeric limits; use text to preserve arbitrary precision. */
  @OverloadResolutionByLambdaReturnType
  @JvmName("decimalColumn")
  fun column(format: CellFormat? = null, width: Int? = null, value: (T) -> BigDecimal?): ColumnDelegate<BigDecimal> =
    delegate { data(it, format, width, ValueKind.Number, value) }

  /** Checked against Excel's numeric limits; use text to preserve arbitrary precision. */
  @OverloadResolutionByLambdaReturnType
  @JvmName("bigIntegerColumn")
  fun column(format: CellFormat? = null, width: Int? = null, value: (T) -> BigInteger?): ColumnDelegate<BigInteger> =
    delegate { data(it, format, width, ValueKind.Number, value) }

  /** Written as the constant's [Enum.name]. */
  @OverloadResolutionByLambdaReturnType
  @JvmName("enumColumn")
  fun column(format: CellFormat? = null, width: Int? = null, value: (T) -> Enum<*>?): ColumnDelegate<String> =
    delegate { data(it, format, width, ValueKind.Text) { row -> value(row)?.name } }

  @OverloadResolutionByLambdaReturnType
  @JvmName("booleanColumn")
  fun column(format: CellFormat? = null, width: Int? = null, value: (T) -> Boolean?): ColumnDelegate<Boolean> =
    delegate { data(it, format, width, ValueKind.Boolean, value) }

  @OverloadResolutionByLambdaReturnType
  @JvmName("dateColumn")
  fun column(format: CellFormat? = null, width: Int? = null, value: (T) -> LocalDate?): ColumnDelegate<LocalDate> =
    delegate { data(it, format, width, ValueKind.Date, value) }

  @OverloadResolutionByLambdaReturnType
  @JvmName("dateTimeColumn")
  fun column(
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> LocalDateTime?,
  ): ColumnDelegate<LocalDateTime> = delegate { data(it, format, width, ValueKind.DateTime, value) }

  /** Shown as a date and time in [ExcelConfig.zone]. */
  @OverloadResolutionByLambdaReturnType
  @JvmName("instantColumn")
  fun column(format: CellFormat? = null, width: Int? = null, value: (T) -> Instant?): ColumnDelegate<Instant> =
    delegate { data(it, format, width, ValueKind.Instant, value) }

  /**
   * A column Excel computes. [block] runs once, here, and builds an [Expr]
   * that is rendered for every row. It may only reference columns of this sheet.
   */
  fun <V> formula(
    format: CellFormat? = null,
    width: Int? = null,
    block: context(Formulas) () -> Expr<V>,
  ): ColumnDelegate<V> {
    val expr = block(FormulaScope)
    checkOwned(expr)
    require(expr.references().none { it is Expr.Range }) { "A row formula cannot hold a range: $expr" }
    return delegate { header -> add(header, format, width, expr.kind) { ColumnSpec.Formula(it, expr) } }
  }

  /** Adds a formula with an explicit header and returns its typed column. */
  @IgnorableReturnValue
  fun <V> formula(
    name: String,
    format: CellFormat? = null,
    width: Int? = null,
    block: context(Formulas) () -> Expr<V>,
  ): Column<V> {
    val expr = block(FormulaScope)
    checkOwned(expr)
    require(expr.references().none { it is Expr.Range }) { "A row formula cannot hold a range: $expr" }
    return add(name, format, width, expr.kind) { ColumnSpec.Formula(it, expr) }
  }

  /** Adds a data column immediately, with an explicit header. */
  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("textNamedColumn")
  fun column(
    name: String,
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> String?,
  ): Column<String> = data(name, format, width, ValueKind.Text, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("numberNamedColumn")
  fun column(
    name: String,
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> Number?,
  ): Column<Number> = data(name, format, width, ValueKind.Number, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("decimalNamedColumn")
  fun column(
    name: String,
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> BigDecimal?,
  ): Column<BigDecimal> = data(name, format, width, ValueKind.Number, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("bigIntegerNamedColumn")
  fun column(
    name: String,
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> BigInteger?,
  ): Column<BigInteger> = data(name, format, width, ValueKind.Number, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("enumNamedColumn")
  fun column(
    name: String,
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> Enum<*>?,
  ): Column<String> = data(name, format, width, ValueKind.Text) { row -> value(row)?.name }

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("booleanNamedColumn")
  fun column(
    name: String,
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> Boolean?,
  ): Column<Boolean> = data(name, format, width, ValueKind.Boolean, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("dateNamedColumn")
  fun column(
    name: String,
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> LocalDate?,
  ): Column<LocalDate> = data(name, format, width, ValueKind.Date, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("dateTimeNamedColumn")
  fun column(
    name: String,
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> LocalDateTime?,
  ): Column<LocalDateTime> = data(name, format, width, ValueKind.DateTime, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("instantNamedColumn")
  fun column(
    name: String,
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> Instant?,
  ): Column<Instant> = data(name, format, width, ValueKind.Instant, value)

  /** `"Issued on" { it.issuedOn }`: the string is the header, used as is. */
  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("textInvoke")
  operator fun String.invoke(format: CellFormat? = null, width: Int? = null, value: (T) -> String?): Column<String> =
    data(this, format, width, ValueKind.Text, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("numberInvoke")
  operator fun String.invoke(format: CellFormat? = null, width: Int? = null, value: (T) -> Number?): Column<Number> =
    data(this, format, width, ValueKind.Number, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("enumInvoke")
  operator fun String.invoke(format: CellFormat? = null, width: Int? = null, value: (T) -> Enum<*>?): Column<String> =
    data(this, format, width, ValueKind.Text) { row -> value(row)?.name }

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("booleanInvoke")
  operator fun String.invoke(
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> Boolean?,
  ): Column<Boolean> = data(this, format, width, ValueKind.Boolean, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("dateInvoke")
  operator fun String.invoke(
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> LocalDate?,
  ): Column<LocalDate> = data(this, format, width, ValueKind.Date, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("dateTimeInvoke")
  operator fun String.invoke(
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> LocalDateTime?,
  ): Column<LocalDateTime> = data(this, format, width, ValueKind.DateTime, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("instantInvoke")
  operator fun String.invoke(
    format: CellFormat? = null,
    width: Int? = null,
    value: (T) -> Instant?,
  ): Column<Instant> = data(this, format, width, ValueKind.Instant, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("decimalInvoke")
  operator fun String.invoke(format: CellFormat? = null, width: Int? = null, value: (T) -> BigDecimal?): Column<BigDecimal> =
    data(this, format, width, ValueKind.Number, value)

  @IgnorableReturnValue
  @OverloadResolutionByLambdaReturnType
  @JvmName("bigIntegerInvoke")
  operator fun String.invoke(format: CellFormat? = null, width: Int? = null, value: (T) -> BigInteger?): Column<BigInteger> =
    data(this, format, width, ValueKind.Number, value)

  /**
   * A bold row under the data, one cell per column that [block] fills.
   * Not written when there are no rows.
   *
   * ```kotlin
   * totals {
   *   customer.label("Total")
   *   total.sum()
   * }
   * ```
   */
  fun totals(block: TotalsBuilder.() -> Unit) {
    TotalsBuilder().block()
  }

  /** The cells of the totals row, one per column at most. */
  @ExcelDsl
  inner class TotalsBuilder internal constructor() {
    /** A text cell, such as `Total`. */
    fun Column<*>.label(text: String) {
      put(this, TotalSpec.Label(text))
    }

    /** Numeric totals return numbers, text totals text, and Boolean totals booleans. */
    @JvmName("numberTotal")
    fun Column<Number>.total(block: context(Totals) () -> Expr<Number>): Unit = formulaTotal(this, block)

    @JvmName("textTotal")
    fun Column<String>.total(block: context(Totals) () -> Expr<String>): Unit = formulaTotal(this, block)

    @JvmName("booleanTotal")
    fun Column<Boolean>.total(block: context(Totals) () -> Expr<Boolean>): Unit = formulaTotal(this, block)

    private fun formulaTotal(column: Column<*>, block: context(Totals) () -> Expr<*>) {
      val expr = block(TotalsScope)
      this@SheetBuilder.checkOwned(expr)
      require(expr.references().none { it is Expr.Ref }) { "A total cannot reference a single row: $expr" }
      put(column, TotalSpec.Formula(expr))
    }

    /** `SUM(E2:E4)` */
    fun Column<Number>.sum(): Unit = total { sum(this@sum) }

    /** `AVERAGE(E2:E4)` */
    fun Column<Number>.average(): Unit = total { average(this@average) }

    /** `MIN(E2:E4)` */
    fun Column<Number>.min(): Unit = total { min(this@min) }

    /** `MAX(E2:E4)` */
    fun Column<Number>.max(): Unit = total { max(this@max) }

    /** `COUNT(A2:A4)`: numeric cells only; blanks and errors do not count. */
    fun Column<Number>.count(): Unit = total { count(this@count) }

    /** `COUNTA(A2:A4)`: non-blank cells, including text, booleans and errors. */
    fun Column<*>.countNonBlank(): Unit = formulaTotal(this) { countNonBlank(this@countNonBlank) }

    private fun put(column: Column<*>, spec: TotalSpec) {
      val sheet = this@SheetBuilder
      sheet.checkOwned(column)
      require(column !in sheet.totals) { "${column.name} already has a total" }
      sheet.totals[column] = spec
    }
  }

  @PublishedApi
  internal fun build(): Sheet<T> =
    Sheet(columns.toList(), totals.mapKeys { it.key.index }, freezePanes, autoFilter, config.zone)

  private fun <V> data(header: String, format: CellFormat?, width: Int?, kind: ValueKind, value: (T) -> V?): Column<V> =
    add(header, format, width, kind) { ColumnSpec.Data(it, value) }

  private fun <V> add(
    header: String,
    format: CellFormat?,
    width: Int?,
    kind: ValueKind,
    spec: (ColumnSpec.Layout) -> ColumnSpec<T>,
  ): Column<V> {
    require(columns.none { it.column.name == header }) { "Duplicate header: $header" }
    require(columns.size < MAX_COLUMNS) { "Excel sheets hold at most $MAX_COLUMNS columns" }
    require(width == null || width in 1..MAX_WIDTH) { "Width of $header must be in 1..$MAX_WIDTH, got $width" }
    val column = Column<V>(header, columns.size, this, kind)
    columns += spec(ColumnSpec.Layout(column, format, width))
    return column
  }

  private fun checkOwned(expr: Expr<*>) {
    fun validateRanges(node: Expr<*>, aggregateOperand: Boolean = false) {
      require(node !is Expr.Range || aggregateOperand) { "A range must be an aggregate operand: $node" }
      node.children().forEach { validateRanges(it, node is Expr.Aggregate) }
    }
    validateRanges(expr)
    val foreign = expr.references().mapNotNull { it.referencedColumn }.filter { it.owner !== this }
    require(foreign.isEmpty()) { "Formula references columns of another sheet: $foreign" }
  }

  private fun checkOwned(column: Column<*>) {
    require(column.owner === this) { "$column belongs to another sheet" }
  }

  private fun <V> delegate(add: (header: String) -> Column<V>): ColumnDelegate<V> =
    PropertyDelegateProvider { _, property ->
      val column = add(config.columnNameStyle(property.name))
      ReadOnlyProperty { _, _ -> column }
    }
}

internal sealed interface ColumnSpec<in T> {
  val layout: Layout
  val column: Column<*> get() = layout.column

  class Layout(val column: Column<*>, val format: CellFormat?, val width: Int?)

  class Data<T>(override val layout: Layout, val value: (T) -> Any?) : ColumnSpec<T>
  class Formula(override val layout: Layout, val expr: Expr<*>) : ColumnSpec<Any?>
}

internal sealed interface TotalSpec {
  class Label(val text: String) : TotalSpec
  class Formula(val expr: Expr<*>) : TotalSpec
}

/** Excel 2007 and later: 16,384 columns, 1,048,576 rows, 255 characters wide. */
internal const val MAX_COLUMNS = 16_384
internal const val MAX_ROWS = 1_048_576
internal const val MAX_WIDTH = 255

/**
 * Runs [block] once, in place, and returns the schema it declared.
 *
 * ```kotlin
 * val invoices = sheet<Invoice> {
 *   val hours by column { it.hours }
 *   val rate by column(CellFormat.currency("€")) { it.rate }
 *   val total by formula(CellFormat.currency("€")) { hours * rate }
 *   totals { total.sum() }
 * }
 * ```
 */
inline fun <T> sheet(
  config: ExcelConfig = ExcelConfig.Default,
  block: SheetBuilder<T>.() -> Unit,
): Sheet<T> {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
  return SheetBuilder<T>(config).apply(block).build()
}
