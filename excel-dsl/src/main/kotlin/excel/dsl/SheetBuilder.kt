@file:OptIn(ExperimentalContracts::class)

package excel.dsl

import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
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
 * Collects column specifications for [sheet]. DSL operations require this builder
 * as a context and register every data or formula column through [add].
 */
@ExcelDsl
class SheetBuilder<T> @PublishedApi internal constructor(internal val config: ExcelConfig) {
  private val columns = mutableListOf<ColumnSpec<T>>()
  private val totals = mutableMapOf<Column<*>, TotalSpec>()
  internal var freezePanes: Boolean = true
  internal var autoFilter: Boolean = false

  @PublishedApi
  internal fun build(): Sheet<T> =
    Sheet(columns.toList(), totals.mapKeys { it.key.index }, freezePanes, autoFilter, config.zone)

  internal fun <V> add(
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

  internal fun checkFormula(formula: Formula<*>) {
    fun validateRanges(node: Formula<*>, aggregateOperand: Boolean = false) {
      require(node !is Formula.Range || aggregateOperand) { "A range must be an aggregate operand: $node" }
      node.children().forEach { validateRanges(it, node is Formula.Aggregate) }
    }
    validateRanges(formula)
    val foreign = formula.references().mapNotNull { it.referencedColumn }.filter { it.owner !== this }
    require(foreign.isEmpty()) { "Formula references columns of another sheet: $foreign" }
  }

  internal fun addTotal(column: Column<*>, spec: TotalSpec) {
    require(column.owner === this) { "$column belongs to another sheet" }
    require(column !in totals) { "${column.name} already has a total" }
    totals[column] = spec
  }
}

/** Keeps the header row visible while scrolling. */
context(sheet: SheetBuilder<*>)
var freezePanes: Boolean
  get() = sheet.freezePanes
  set(value) { sheet.freezePanes = value }

/** Adds Excel's filter buttons to the header, over the data rows. */
context(sheet: SheetBuilder<*>)
var autoFilter: Boolean
  get() = sheet.autoFilter
  set(value) { sheet.autoFilter = value }

internal sealed interface ColumnSpec<in T> {
  val layout: Layout
  val column: Column<*> get() = layout.column

  class Layout(val column: Column<*>, val format: CellFormat?, val width: Int?)

  class Data<T>(override val layout: Layout, val value: (T) -> Any?) : ColumnSpec<T>
  class Formula(override val layout: Layout, val expr: excel.dsl.Formula<*>) : ColumnSpec<Any?>
}

internal sealed interface TotalSpec {
  class Label(val text: String) : TotalSpec
  class Formula(val expr: excel.dsl.Formula<*>) : TotalSpec
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
  block: context(SheetBuilder<T>) () -> Unit,
): Sheet<T> {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
  val builder = SheetBuilder<T>(config)
  block(builder)
  return builder.build()
}
