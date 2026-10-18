@file:OptIn(ExperimentalTypeInference::class)

package excel.dsl

import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.experimental.ExperimentalTypeInference
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

/** Adds a data column immediately, with an explicit header. */
@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("textNamedColumn")
context(_: SheetBuilder<T>)
fun <T> column(
  name: String,
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> String?,
): Column<String> = data(name, format, width, ValueKind.Text, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("numberNamedColumn")
context(_: SheetBuilder<T>)
fun <T> column(
  name: String,
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> Number?,
): Column<Number> = data(name, format, width, ValueKind.Number, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("decimalNamedColumn")
context(_: SheetBuilder<T>)
fun <T> column(
  name: String,
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> BigDecimal?,
): Column<BigDecimal> = data(name, format, width, ValueKind.Number, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("bigIntegerNamedColumn")
context(_: SheetBuilder<T>)
fun <T> column(
  name: String,
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> BigInteger?,
): Column<BigInteger> = data(name, format, width, ValueKind.Number, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("enumNamedColumn")
context(_: SheetBuilder<T>)
fun <T> column(
  name: String,
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> Enum<*>?,
): Column<String> = data(name, format, width, ValueKind.Text) { row -> value(row)?.name }

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("booleanNamedColumn")
context(_: SheetBuilder<T>)
fun <T> column(
  name: String,
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> Boolean?,
): Column<Boolean> = data(name, format, width, ValueKind.Boolean, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("dateNamedColumn")
context(_: SheetBuilder<T>)
fun <T> column(
  name: String,
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> LocalDate?,
): Column<LocalDate> = data(name, format, width, ValueKind.Date, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("dateTimeNamedColumn")
context(_: SheetBuilder<T>)
fun <T> column(
  name: String,
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> LocalDateTime?,
): Column<LocalDateTime> = data(name, format, width, ValueKind.DateTime, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("instantNamedColumn")
context(_: SheetBuilder<T>)
fun <T> column(
  name: String,
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> Instant?,
): Column<Instant> = data(name, format, width, ValueKind.Instant, value)

/** `"Issued on" { it.issuedOn }`: the string is the header, used as is. */
@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("textInvoke")
context(_: SheetBuilder<T>)
operator fun <T> String.invoke(format: CellFormat? = null, width: Int? = null, value: (T) -> String?): Column<String> =
  data(this, format, width, ValueKind.Text, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("numberInvoke")
context(_: SheetBuilder<T>)
operator fun <T> String.invoke(format: CellFormat? = null, width: Int? = null, value: (T) -> Number?): Column<Number> =
  data(this, format, width, ValueKind.Number, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("enumInvoke")
context(_: SheetBuilder<T>)
operator fun <T> String.invoke(format: CellFormat? = null, width: Int? = null, value: (T) -> Enum<*>?): Column<String> =
  data(this, format, width, ValueKind.Text) { row -> value(row)?.name }

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("booleanInvoke")
context(_: SheetBuilder<T>)
operator fun <T> String.invoke(
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> Boolean?,
): Column<Boolean> = data(this, format, width, ValueKind.Boolean, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("dateInvoke")
context(_: SheetBuilder<T>)
operator fun <T> String.invoke(
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> LocalDate?,
): Column<LocalDate> = data(this, format, width, ValueKind.Date, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("dateTimeInvoke")
context(_: SheetBuilder<T>)
operator fun <T> String.invoke(
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> LocalDateTime?,
): Column<LocalDateTime> = data(this, format, width, ValueKind.DateTime, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("instantInvoke")
context(_: SheetBuilder<T>)
operator fun <T> String.invoke(
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> Instant?,
): Column<Instant> = data(this, format, width, ValueKind.Instant, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("decimalInvoke")
context(_: SheetBuilder<T>)
operator fun <T> String.invoke(format: CellFormat? = null, width: Int? = null, value: (T) -> BigDecimal?): Column<BigDecimal> =
  data(this, format, width, ValueKind.Number, value)

@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
@JvmName("bigIntegerInvoke")
context(_: SheetBuilder<T>)
operator fun <T> String.invoke(format: CellFormat? = null, width: Int? = null, value: (T) -> BigInteger?): Column<BigInteger> =
  data(this, format, width, ValueKind.Number, value)


context(sheet: SheetBuilder<*>)
private fun <V> delegate(add: (header: String) -> Column<V>): ColumnDelegate<V> =
  PropertyDelegateProvider { _, property ->
    val column = add(sheet.config.columnNameStyle(property.name))
    ReadOnlyProperty { _, _ -> column }
  }

context(sheet: SheetBuilder<T>)
private fun <T, V> data(header: String, format: CellFormat?, width: Int?, kind: ValueKind, value: (T) -> V?): Column<V> =
  sheet.add(header, format, width, kind) { ColumnSpec.Data(it, value) }

@OverloadResolutionByLambdaReturnType
@JvmName("textColumn")
context(_: SheetBuilder<T>)
fun <T> column(format: CellFormat? = null, width: Int? = null, value: (T) -> String?): ColumnDelegate<String> =
  delegate { data(it, format, width, ValueKind.Text, value) }

/** Written as a double, which Excel stores with 15 significant digits. */
@OverloadResolutionByLambdaReturnType
@JvmName("numberColumn")
context(_: SheetBuilder<T>)
fun <T> column(format: CellFormat? = null, width: Int? = null, value: (T) -> Number?): ColumnDelegate<Number> =
  delegate { data(it, format, width, ValueKind.Number, value) }

/** Checked against Excel's numeric limits; use text to preserve arbitrary precision. */
@OverloadResolutionByLambdaReturnType
@JvmName("decimalColumn")
context(_: SheetBuilder<T>)
fun <T> column(format: CellFormat? = null, width: Int? = null, value: (T) -> BigDecimal?): ColumnDelegate<BigDecimal> =
  delegate { data(it, format, width, ValueKind.Number, value) }

/** Checked against Excel's numeric limits; use text to preserve arbitrary precision. */
@OverloadResolutionByLambdaReturnType
@JvmName("bigIntegerColumn")
context(_: SheetBuilder<T>)
fun <T> column(format: CellFormat? = null, width: Int? = null, value: (T) -> BigInteger?): ColumnDelegate<BigInteger> =
  delegate { data(it, format, width, ValueKind.Number, value) }

/** Written as the constant's [Enum.name]. */
@OverloadResolutionByLambdaReturnType
@JvmName("enumColumn")
context(_: SheetBuilder<T>)
fun <T> column(format: CellFormat? = null, width: Int? = null, value: (T) -> Enum<*>?): ColumnDelegate<String> =
  delegate { data(it, format, width, ValueKind.Text) { row -> value(row)?.name } }

@OverloadResolutionByLambdaReturnType
@JvmName("booleanColumn")
context(_: SheetBuilder<T>)
fun <T> column(format: CellFormat? = null, width: Int? = null, value: (T) -> Boolean?): ColumnDelegate<Boolean> =
  delegate { data(it, format, width, ValueKind.Boolean, value) }

@OverloadResolutionByLambdaReturnType
@JvmName("dateColumn")
context(_: SheetBuilder<T>)
fun <T> column(format: CellFormat? = null, width: Int? = null, value: (T) -> LocalDate?): ColumnDelegate<LocalDate> =
  delegate { data(it, format, width, ValueKind.Date, value) }

@OverloadResolutionByLambdaReturnType
@JvmName("dateTimeColumn")
context(_: SheetBuilder<T>)
fun <T> column(
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> LocalDateTime?,
): ColumnDelegate<LocalDateTime> = delegate { data(it, format, width, ValueKind.DateTime, value) }

/** Shown as a date and time in [ExcelConfig.zone]. */
@OverloadResolutionByLambdaReturnType
@JvmName("instantColumn")
context(_: SheetBuilder<T>)
fun <T> column(format: CellFormat? = null, width: Int? = null, value: (T) -> Instant?): ColumnDelegate<Instant> =
  delegate { data(it, format, width, ValueKind.Instant, value) }

/**
 * A column Excel computes. [block] runs once, here, and builds a [Formula]
 * that is rendered for every row. It may only reference columns of this sheet.
 */
context(sheet: SheetBuilder<*>)
fun <V> formula(
  format: CellFormat? = null,
  width: Int? = null,
  block: context(Formulas) () -> Formula<V>,
): ColumnDelegate<V> {
  val expr = block(FormulaBuilder)
  sheet.checkFormula(expr)
  require(expr.references().none { it is Formula.Range }) { "A row formula cannot hold a range: $expr" }
  return delegate { header -> sheet.add(header, format, width, expr.kind) { ColumnSpec.Formula(it, expr) } }
}

/** Adds a formula with an explicit header and returns its typed column. */
@IgnorableReturnValue
context(sheet: SheetBuilder<*>)
fun <V> formula(
  name: String,
  format: CellFormat? = null,
  width: Int? = null,
  block: context(Formulas) () -> Formula<V>,
): Column<V> {
  val expr = block(FormulaBuilder)
  sheet.checkFormula(expr)
  require(expr.references().none { it is Formula.Range }) { "A row formula cannot hold a range: $expr" }
  return sheet.add(name, format, width, expr.kind) { ColumnSpec.Formula(it, expr) }
}

