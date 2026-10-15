package excel.dsl

/** Operations shared by row formulas and totals. */
@ExcelDsl
sealed interface Arithmetic

/** Row formulas may reference columns from their sheet. */
@ExcelDsl
sealed interface Formulas : Arithmetic

/** Totals may aggregate whole columns, but cannot reference a single row. */
@ExcelDsl
sealed interface Totals : Arithmetic

internal object FormulaBuilder : Formulas
internal object TotalsScope : Totals

context(_: Arithmetic)
fun round(value: Formula<Number>, digits: Int = 0): Formula<Double> = Formula.Round(value, digits)

context(_: Arithmetic)
fun literal(value: Number): Formula<Double> = constant(value)

context(_: Arithmetic)
fun literal(value: String): Formula<String> = Formula.Text(value)

context(_: Arithmetic)
fun literal(value: Boolean): Formula<Boolean> = Formula.Bool(value)

context(_: Formulas)
fun sum(columns: List<Column<Number>>): Formula<Double> =
  Formula.Aggregate(AggregateFunction.Sum, columns.map { Formula.Ref(it) })

context(_: Formulas)
fun average(columns: List<Column<Number>>): Formula<Double> =
  Formula.Aggregate(AggregateFunction.Average, columns.map { Formula.Ref(it) })

context(_: Formulas)
fun min(columns: List<Column<Number>>): Formula<Double> =
  Formula.Aggregate(AggregateFunction.Min, columns.map { Formula.Ref(it) })

context(_: Formulas)
fun max(columns: List<Column<Number>>): Formula<Double> =
  Formula.Aggregate(AggregateFunction.Max, columns.map { Formula.Ref(it) })

context(_: Formulas)
fun round(column: Column<Number>, digits: Int = 0): Formula<Double> =
  Formula.Round(Formula.Ref(column), digits)

context(_: Totals)
fun sum(column: Column<Number>): Formula<Double> =
  Formula.Aggregate(AggregateFunction.Sum, listOf(Formula.Range(column)))

context(_: Totals)
fun average(column: Column<Number>): Formula<Double> =
  Formula.Aggregate(AggregateFunction.Average, listOf(Formula.Range(column)))

context(_: Totals)
fun min(column: Column<Number>): Formula<Double> =
  Formula.Aggregate(AggregateFunction.Min, listOf(Formula.Range(column)))

context(_: Totals)
fun max(column: Column<Number>): Formula<Double> =
  Formula.Aggregate(AggregateFunction.Max, listOf(Formula.Range(column)))

/** COUNT counts numeric cells, excluding blanks and errors. */
context(_: Totals)
fun count(column: Column<Number>): Formula<Double> =
  Formula.Aggregate(AggregateFunction.Count, listOf(Formula.Range(column)))

/** COUNTA counts non-blank cells, including text, booleans and errors. */
context(_: Totals)
fun countNonBlank(column: Column<*>): Formula<Double> =
  Formula.Aggregate(AggregateFunction.CountNonBlank, listOf(Formula.Range(column)))

/** Explicit reference for an alias formula: `formula { amount.ref() }`. */
context(_: Formulas)
@JvmName("numberRef")
fun Column<Number>.ref(): Formula<Number> = Formula.Ref(this)
context(_: Formulas)
@JvmName("textRef")
fun Column<String>.ref(): Formula<String> = Formula.Ref(this)
context(_: Formulas)
@JvmName("booleanRef")
fun Column<Boolean>.ref(): Formula<Boolean> = Formula.Ref(this)

/** Explicit conversion: Boolean cells are otherwise excluded from numeric operations. */
context(_: Arithmetic)
fun Formula<Boolean>.asNumber(): Formula<Double> = Formula.If(this, Formula.Constant(1.0), Formula.Constant(0.0))
context(_: Formulas)
fun Column<Boolean>.asNumber(): Formula<Double> = ref().asNumber()
context(_: Arithmetic)
fun Formula<Number>.asBoolean(): Formula<Boolean> = Formula.Compare(this, Comparison.NotEqual, Formula.Constant(0.0))
context(_: Formulas)
fun Column<Number>.asBoolean(): Formula<Boolean> = ref().asBoolean()

/** Invariant V prevents otherwise from widening incompatible branches to Any. */
class Branch<V> internal constructor(internal val condition: Formula<Boolean>, internal val then: Formula<V>) {
  context(_: Arithmetic)
  infix fun otherwise(value: Formula<V>): Formula<V> = Formula.If(condition, then, value)
  context(_: Formulas)
  infix fun otherwise(value: Column<V>): Formula<V> = Formula.If(condition, then, Formula.Ref(value))
}

context(_: Arithmetic)
operator fun Formula<Number>.plus(other: Formula<Number>): Formula<Double> = Formula.Plus(this, other)

context(_: Arithmetic)
operator fun Formula<Number>.plus(other: Number): Formula<Double> = Formula.Plus(this, constant(other))

context(_: Formulas)
operator fun Formula<Number>.plus(other: Column<Number>): Formula<Double> = Formula.Plus(this, Formula.Ref(other))

context(_: Formulas)
operator fun Column<Number>.plus(other: Formula<Number>): Formula<Double> = Formula.Plus(Formula.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.plus(other: Column<Number>): Formula<Double> = Formula.Plus(Formula.Ref(this), Formula.Ref(other))

context(_: Formulas)
operator fun Column<Number>.plus(other: Number): Formula<Double> = Formula.Plus(Formula.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Number.plus(other: Formula<Number>): Formula<Double> = Formula.Plus(constant(this), other)

context(_: Formulas)
operator fun Number.plus(other: Column<Number>): Formula<Double> = Formula.Plus(constant(this), Formula.Ref(other))

context(_: Arithmetic)
operator fun Formula<Number>.minus(other: Formula<Number>): Formula<Double> = Formula.Minus(this, other)

context(_: Arithmetic)
operator fun Formula<Number>.minus(other: Number): Formula<Double> = Formula.Minus(this, constant(other))

context(_: Formulas)
operator fun Formula<Number>.minus(other: Column<Number>): Formula<Double> = Formula.Minus(this, Formula.Ref(other))

context(_: Formulas)
operator fun Column<Number>.minus(other: Formula<Number>): Formula<Double> = Formula.Minus(Formula.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.minus(other: Column<Number>): Formula<Double> = Formula.Minus(Formula.Ref(this), Formula.Ref(other))

context(_: Formulas)
operator fun Column<Number>.minus(other: Number): Formula<Double> = Formula.Minus(Formula.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Number.minus(other: Formula<Number>): Formula<Double> = Formula.Minus(constant(this), other)

context(_: Formulas)
operator fun Number.minus(other: Column<Number>): Formula<Double> = Formula.Minus(constant(this), Formula.Ref(other))

context(_: Arithmetic)
operator fun Formula<Number>.times(other: Formula<Number>): Formula<Double> = Formula.Times(this, other)

context(_: Arithmetic)
operator fun Formula<Number>.times(other: Number): Formula<Double> = Formula.Times(this, constant(other))

context(_: Formulas)
operator fun Formula<Number>.times(other: Column<Number>): Formula<Double> = Formula.Times(this, Formula.Ref(other))

context(_: Formulas)
operator fun Column<Number>.times(other: Formula<Number>): Formula<Double> = Formula.Times(Formula.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Formula<Double> = Formula.Times(Formula.Ref(this), Formula.Ref(other))

context(_: Formulas)
operator fun Column<Number>.times(other: Number): Formula<Double> = Formula.Times(Formula.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Number.times(other: Formula<Number>): Formula<Double> = Formula.Times(constant(this), other)

context(_: Formulas)
operator fun Number.times(other: Column<Number>): Formula<Double> = Formula.Times(constant(this), Formula.Ref(other))

context(_: Arithmetic)
operator fun Formula<Number>.div(other: Formula<Number>): Formula<Double> = Formula.Div(this, other)

context(_: Arithmetic)
operator fun Formula<Number>.div(other: Number): Formula<Double> = Formula.Div(this, constant(other))

context(_: Formulas)
operator fun Formula<Number>.div(other: Column<Number>): Formula<Double> = Formula.Div(this, Formula.Ref(other))

context(_: Formulas)
operator fun Column<Number>.div(other: Formula<Number>): Formula<Double> = Formula.Div(Formula.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.div(other: Column<Number>): Formula<Double> = Formula.Div(Formula.Ref(this), Formula.Ref(other))

context(_: Formulas)
operator fun Column<Number>.div(other: Number): Formula<Double> = Formula.Div(Formula.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Number.div(other: Formula<Number>): Formula<Double> = Formula.Div(constant(this), other)

context(_: Formulas)
operator fun Number.div(other: Column<Number>): Formula<Double> = Formula.Div(constant(this), Formula.Ref(other))

context(_: Arithmetic)
operator fun Formula<Number>.unaryMinus(): Formula<Double> = Formula.Minus(Formula.Constant(0.0), this)

context(_: Formulas)
operator fun Column<Number>.unaryMinus(): Formula<Double> = Formula.Minus(Formula.Constant(0.0), Formula.Ref(this))

context(_: Arithmetic)
@JvmName("gtNumber0")
infix fun Formula<Number>.gt(other: Formula<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.Greater, other)

context(_: Arithmetic)
@JvmName("gtNumber1")
infix fun Formula<Number>.gt(other: Number): Formula<Boolean> = Formula.Compare(this, Comparison.Greater, constant(other))

context(_: Formulas)
@JvmName("gtNumber2")
infix fun Formula<Number>.gt(other: Column<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.Greater, Formula.Ref(other))

context(_: Formulas)
@JvmName("gtNumber3")
infix fun Column<Number>.gt(other: Formula<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Greater, other)

context(_: Formulas)
@JvmName("gtNumber4")
infix fun Column<Number>.gt(other: Column<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Greater, Formula.Ref(other))

context(_: Formulas)
@JvmName("gtNumber5")
infix fun Column<Number>.gt(other: Number): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Greater, constant(other))

context(_: Arithmetic)
@JvmName("geNumber0")
infix fun Formula<Number>.ge(other: Formula<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.GreaterOrEqual, other)

context(_: Arithmetic)
@JvmName("geNumber1")
infix fun Formula<Number>.ge(other: Number): Formula<Boolean> = Formula.Compare(this, Comparison.GreaterOrEqual, constant(other))

context(_: Formulas)
@JvmName("geNumber2")
infix fun Formula<Number>.ge(other: Column<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.GreaterOrEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("geNumber3")
infix fun Column<Number>.ge(other: Formula<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.GreaterOrEqual, other)

context(_: Formulas)
@JvmName("geNumber4")
infix fun Column<Number>.ge(other: Column<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.GreaterOrEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("geNumber5")
infix fun Column<Number>.ge(other: Number): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.GreaterOrEqual, constant(other))

context(_: Arithmetic)
@JvmName("ltNumber0")
infix fun Formula<Number>.lt(other: Formula<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.Less, other)

context(_: Arithmetic)
@JvmName("ltNumber1")
infix fun Formula<Number>.lt(other: Number): Formula<Boolean> = Formula.Compare(this, Comparison.Less, constant(other))

context(_: Formulas)
@JvmName("ltNumber2")
infix fun Formula<Number>.lt(other: Column<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.Less, Formula.Ref(other))

context(_: Formulas)
@JvmName("ltNumber3")
infix fun Column<Number>.lt(other: Formula<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Less, other)

context(_: Formulas)
@JvmName("ltNumber4")
infix fun Column<Number>.lt(other: Column<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Less, Formula.Ref(other))

context(_: Formulas)
@JvmName("ltNumber5")
infix fun Column<Number>.lt(other: Number): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Less, constant(other))

context(_: Arithmetic)
@JvmName("leNumber0")
infix fun Formula<Number>.le(other: Formula<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.LessOrEqual, other)

context(_: Arithmetic)
@JvmName("leNumber1")
infix fun Formula<Number>.le(other: Number): Formula<Boolean> = Formula.Compare(this, Comparison.LessOrEqual, constant(other))

context(_: Formulas)
@JvmName("leNumber2")
infix fun Formula<Number>.le(other: Column<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.LessOrEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("leNumber3")
infix fun Column<Number>.le(other: Formula<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.LessOrEqual, other)

context(_: Formulas)
@JvmName("leNumber4")
infix fun Column<Number>.le(other: Column<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.LessOrEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("leNumber5")
infix fun Column<Number>.le(other: Number): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.LessOrEqual, constant(other))

context(_: Arithmetic)
@JvmName("eqNumber0")
infix fun Formula<Number>.eq(other: Formula<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.Equal, other)

context(_: Arithmetic)
@JvmName("eqNumber1")
infix fun Formula<Number>.eq(other: Number): Formula<Boolean> = Formula.Compare(this, Comparison.Equal, constant(other))

context(_: Formulas)
@JvmName("eqNumber2")
infix fun Formula<Number>.eq(other: Column<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.Equal, Formula.Ref(other))

context(_: Formulas)
@JvmName("eqNumber3")
infix fun Column<Number>.eq(other: Formula<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Equal, other)

context(_: Formulas)
@JvmName("eqNumber4")
infix fun Column<Number>.eq(other: Column<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Equal, Formula.Ref(other))

context(_: Formulas)
@JvmName("eqNumber5")
infix fun Column<Number>.eq(other: Number): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Equal, constant(other))

context(_: Arithmetic)
@JvmName("neNumber0")
infix fun Formula<Number>.ne(other: Formula<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.NotEqual, other)

context(_: Arithmetic)
@JvmName("neNumber1")
infix fun Formula<Number>.ne(other: Number): Formula<Boolean> = Formula.Compare(this, Comparison.NotEqual, constant(other))

context(_: Formulas)
@JvmName("neNumber2")
infix fun Formula<Number>.ne(other: Column<Number>): Formula<Boolean> = Formula.Compare(this, Comparison.NotEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("neNumber3")
infix fun Column<Number>.ne(other: Formula<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.NotEqual, other)

context(_: Formulas)
@JvmName("neNumber4")
infix fun Column<Number>.ne(other: Column<Number>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.NotEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("neNumber5")
infix fun Column<Number>.ne(other: Number): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.NotEqual, constant(other))

context(_: Arithmetic)
@JvmName("gtString0")
infix fun Formula<String>.gt(other: Formula<String>): Formula<Boolean> = Formula.Compare(this, Comparison.Greater, other)

context(_: Arithmetic)
@JvmName("gtString1")
infix fun Formula<String>.gt(other: String): Formula<Boolean> = Formula.Compare(this, Comparison.Greater, Formula.Text(other))

context(_: Formulas)
@JvmName("gtString2")
infix fun Formula<String>.gt(other: Column<String>): Formula<Boolean> = Formula.Compare(this, Comparison.Greater, Formula.Ref(other))

context(_: Formulas)
@JvmName("gtString3")
infix fun Column<String>.gt(other: Formula<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Greater, other)

context(_: Formulas)
@JvmName("gtString4")
infix fun Column<String>.gt(other: Column<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Greater, Formula.Ref(other))

context(_: Formulas)
@JvmName("gtString5")
infix fun Column<String>.gt(other: String): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Greater, Formula.Text(other))

context(_: Arithmetic)
@JvmName("geString0")
infix fun Formula<String>.ge(other: Formula<String>): Formula<Boolean> = Formula.Compare(this, Comparison.GreaterOrEqual, other)

context(_: Arithmetic)
@JvmName("geString1")
infix fun Formula<String>.ge(other: String): Formula<Boolean> = Formula.Compare(this, Comparison.GreaterOrEqual, Formula.Text(other))

context(_: Formulas)
@JvmName("geString2")
infix fun Formula<String>.ge(other: Column<String>): Formula<Boolean> = Formula.Compare(this, Comparison.GreaterOrEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("geString3")
infix fun Column<String>.ge(other: Formula<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.GreaterOrEqual, other)

context(_: Formulas)
@JvmName("geString4")
infix fun Column<String>.ge(other: Column<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.GreaterOrEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("geString5")
infix fun Column<String>.ge(other: String): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.GreaterOrEqual, Formula.Text(other))

context(_: Arithmetic)
@JvmName("ltString0")
infix fun Formula<String>.lt(other: Formula<String>): Formula<Boolean> = Formula.Compare(this, Comparison.Less, other)

context(_: Arithmetic)
@JvmName("ltString1")
infix fun Formula<String>.lt(other: String): Formula<Boolean> = Formula.Compare(this, Comparison.Less, Formula.Text(other))

context(_: Formulas)
@JvmName("ltString2")
infix fun Formula<String>.lt(other: Column<String>): Formula<Boolean> = Formula.Compare(this, Comparison.Less, Formula.Ref(other))

context(_: Formulas)
@JvmName("ltString3")
infix fun Column<String>.lt(other: Formula<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Less, other)

context(_: Formulas)
@JvmName("ltString4")
infix fun Column<String>.lt(other: Column<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Less, Formula.Ref(other))

context(_: Formulas)
@JvmName("ltString5")
infix fun Column<String>.lt(other: String): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Less, Formula.Text(other))

context(_: Arithmetic)
@JvmName("leString0")
infix fun Formula<String>.le(other: Formula<String>): Formula<Boolean> = Formula.Compare(this, Comparison.LessOrEqual, other)

context(_: Arithmetic)
@JvmName("leString1")
infix fun Formula<String>.le(other: String): Formula<Boolean> = Formula.Compare(this, Comparison.LessOrEqual, Formula.Text(other))

context(_: Formulas)
@JvmName("leString2")
infix fun Formula<String>.le(other: Column<String>): Formula<Boolean> = Formula.Compare(this, Comparison.LessOrEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("leString3")
infix fun Column<String>.le(other: Formula<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.LessOrEqual, other)

context(_: Formulas)
@JvmName("leString4")
infix fun Column<String>.le(other: Column<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.LessOrEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("leString5")
infix fun Column<String>.le(other: String): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.LessOrEqual, Formula.Text(other))

context(_: Arithmetic)
@JvmName("eqString0")
infix fun Formula<String>.eq(other: Formula<String>): Formula<Boolean> = Formula.Compare(this, Comparison.Equal, other)

context(_: Arithmetic)
@JvmName("eqString1")
infix fun Formula<String>.eq(other: String): Formula<Boolean> = Formula.Compare(this, Comparison.Equal, Formula.Text(other))

context(_: Formulas)
@JvmName("eqString2")
infix fun Formula<String>.eq(other: Column<String>): Formula<Boolean> = Formula.Compare(this, Comparison.Equal, Formula.Ref(other))

context(_: Formulas)
@JvmName("eqString3")
infix fun Column<String>.eq(other: Formula<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Equal, other)

context(_: Formulas)
@JvmName("eqString4")
infix fun Column<String>.eq(other: Column<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Equal, Formula.Ref(other))

context(_: Formulas)
@JvmName("eqString5")
infix fun Column<String>.eq(other: String): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Equal, Formula.Text(other))

context(_: Arithmetic)
@JvmName("neString0")
infix fun Formula<String>.ne(other: Formula<String>): Formula<Boolean> = Formula.Compare(this, Comparison.NotEqual, other)

context(_: Arithmetic)
@JvmName("neString1")
infix fun Formula<String>.ne(other: String): Formula<Boolean> = Formula.Compare(this, Comparison.NotEqual, Formula.Text(other))

context(_: Formulas)
@JvmName("neString2")
infix fun Formula<String>.ne(other: Column<String>): Formula<Boolean> = Formula.Compare(this, Comparison.NotEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("neString3")
infix fun Column<String>.ne(other: Formula<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.NotEqual, other)

context(_: Formulas)
@JvmName("neString4")
infix fun Column<String>.ne(other: Column<String>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.NotEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("neString5")
infix fun Column<String>.ne(other: String): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.NotEqual, Formula.Text(other))

context(_: Arithmetic)
@JvmName("eqBoolean0")
infix fun Formula<Boolean>.eq(other: Formula<Boolean>): Formula<Boolean> = Formula.Compare(this, Comparison.Equal, other)

context(_: Arithmetic)
@JvmName("eqBoolean1")
infix fun Formula<Boolean>.eq(other: Boolean): Formula<Boolean> = Formula.Compare(this, Comparison.Equal, Formula.Bool(other))

context(_: Formulas)
@JvmName("eqBoolean2")
infix fun Formula<Boolean>.eq(other: Column<Boolean>): Formula<Boolean> = Formula.Compare(this, Comparison.Equal, Formula.Ref(other))

context(_: Formulas)
@JvmName("eqBoolean3")
infix fun Column<Boolean>.eq(other: Formula<Boolean>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Equal, other)

context(_: Formulas)
@JvmName("eqBoolean4")
infix fun Column<Boolean>.eq(other: Column<Boolean>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Equal, Formula.Ref(other))

context(_: Formulas)
@JvmName("eqBoolean5")
infix fun Column<Boolean>.eq(other: Boolean): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.Equal, Formula.Bool(other))

context(_: Arithmetic)
@JvmName("neBoolean0")
infix fun Formula<Boolean>.ne(other: Formula<Boolean>): Formula<Boolean> = Formula.Compare(this, Comparison.NotEqual, other)

context(_: Arithmetic)
@JvmName("neBoolean1")
infix fun Formula<Boolean>.ne(other: Boolean): Formula<Boolean> = Formula.Compare(this, Comparison.NotEqual, Formula.Bool(other))

context(_: Formulas)
@JvmName("neBoolean2")
infix fun Formula<Boolean>.ne(other: Column<Boolean>): Formula<Boolean> = Formula.Compare(this, Comparison.NotEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("neBoolean3")
infix fun Column<Boolean>.ne(other: Formula<Boolean>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.NotEqual, other)

context(_: Formulas)
@JvmName("neBoolean4")
infix fun Column<Boolean>.ne(other: Column<Boolean>): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.NotEqual, Formula.Ref(other))

context(_: Formulas)
@JvmName("neBoolean5")
infix fun Column<Boolean>.ne(other: Boolean): Formula<Boolean> = Formula.Compare(Formula.Ref(this), Comparison.NotEqual, Formula.Bool(other))

context(_: Arithmetic)
infix fun Formula<Boolean>.and(other: Formula<Boolean>): Formula<Boolean> = Formula.And(this, other)

context(_: Formulas)
infix fun Formula<Boolean>.and(other: Column<Boolean>): Formula<Boolean> = Formula.And(this, Formula.Ref(other))

context(_: Formulas)
infix fun Column<Boolean>.and(other: Formula<Boolean>): Formula<Boolean> = Formula.And(Formula.Ref(this), other)

context(_: Formulas)
infix fun Column<Boolean>.and(other: Column<Boolean>): Formula<Boolean> = Formula.And(Formula.Ref(this), Formula.Ref(other))

context(_: Arithmetic)
infix fun Formula<Boolean>.and(other: Boolean): Formula<Boolean> = Formula.And(this, Formula.Bool(other))

context(_: Formulas)
infix fun Column<Boolean>.and(other: Boolean): Formula<Boolean> = Formula.And(Formula.Ref(this), Formula.Bool(other))

context(_: Arithmetic)
infix fun Formula<Boolean>.or(other: Formula<Boolean>): Formula<Boolean> = Formula.Or(this, other)

context(_: Formulas)
infix fun Formula<Boolean>.or(other: Column<Boolean>): Formula<Boolean> = Formula.Or(this, Formula.Ref(other))

context(_: Formulas)
infix fun Column<Boolean>.or(other: Formula<Boolean>): Formula<Boolean> = Formula.Or(Formula.Ref(this), other)

context(_: Formulas)
infix fun Column<Boolean>.or(other: Column<Boolean>): Formula<Boolean> = Formula.Or(Formula.Ref(this), Formula.Ref(other))

context(_: Arithmetic)
infix fun Formula<Boolean>.or(other: Boolean): Formula<Boolean> = Formula.Or(this, Formula.Bool(other))

context(_: Formulas)
infix fun Column<Boolean>.or(other: Boolean): Formula<Boolean> = Formula.Or(Formula.Ref(this), Formula.Bool(other))

context(_: Arithmetic)
operator fun Formula<Boolean>.not(): Formula<Boolean> = Formula.Not(this)

context(_: Formulas)
operator fun Column<Boolean>.not(): Formula<Boolean> = Formula.Not(Formula.Ref(this))

context(_: Arithmetic)
@JvmName("thenNumberExpr0")
infix fun Formula<Boolean>.then(other: Formula<Number>): Branch<Number> = Branch(this, other)

context(_: Formulas)
@JvmName("thenNumberExpr1")
infix fun Formula<Boolean>.then(other: Column<Number>): Branch<Number> = Branch(this, Formula.Ref(other))

context(_: Arithmetic)
@JvmName("thenNumberExpr2")
infix fun Formula<Boolean>.then(other: Number): Branch<Number> = Branch(this, constant(other))

context(_: Formulas)
@JvmName("thenNumberColumn0")
infix fun Column<Boolean>.then(other: Formula<Number>): Branch<Number> = Branch(Formula.Ref(this), other)

context(_: Formulas)
@JvmName("thenNumberColumn1")
infix fun Column<Boolean>.then(other: Column<Number>): Branch<Number> = Branch(Formula.Ref(this), Formula.Ref(other))

context(_: Formulas)
@JvmName("thenNumberColumn2")
infix fun Column<Boolean>.then(other: Number): Branch<Number> = Branch(Formula.Ref(this), constant(other))

context(_: Arithmetic)
infix fun Branch<Number>.otherwise(value: Number): Formula<Number> = Formula.If(condition, then, constant(value))

context(_: Arithmetic)
@JvmName("thenStringExpr0")
infix fun Formula<Boolean>.then(other: Formula<String>): Branch<String> = Branch(this, other)

context(_: Formulas)
@JvmName("thenStringExpr1")
infix fun Formula<Boolean>.then(other: Column<String>): Branch<String> = Branch(this, Formula.Ref(other))

context(_: Arithmetic)
@JvmName("thenStringExpr2")
infix fun Formula<Boolean>.then(other: String): Branch<String> = Branch(this, Formula.Text(other))

context(_: Formulas)
@JvmName("thenStringColumn0")
infix fun Column<Boolean>.then(other: Formula<String>): Branch<String> = Branch(Formula.Ref(this), other)

context(_: Formulas)
@JvmName("thenStringColumn1")
infix fun Column<Boolean>.then(other: Column<String>): Branch<String> = Branch(Formula.Ref(this), Formula.Ref(other))

context(_: Formulas)
@JvmName("thenStringColumn2")
infix fun Column<Boolean>.then(other: String): Branch<String> = Branch(Formula.Ref(this), Formula.Text(other))

context(_: Arithmetic)
infix fun Branch<String>.otherwise(value: String): Formula<String> = Formula.If(condition, then, Formula.Text(value))

context(_: Arithmetic)
@JvmName("thenBooleanExpr0")
infix fun Formula<Boolean>.then(other: Formula<Boolean>): Branch<Boolean> = Branch(this, other)

context(_: Formulas)
@JvmName("thenBooleanExpr1")
infix fun Formula<Boolean>.then(other: Column<Boolean>): Branch<Boolean> = Branch(this, Formula.Ref(other))

context(_: Arithmetic)
@JvmName("thenBooleanExpr2")
infix fun Formula<Boolean>.then(other: Boolean): Branch<Boolean> = Branch(this, Formula.Bool(other))

context(_: Formulas)
@JvmName("thenBooleanColumn0")
infix fun Column<Boolean>.then(other: Formula<Boolean>): Branch<Boolean> = Branch(Formula.Ref(this), other)

context(_: Formulas)
@JvmName("thenBooleanColumn1")
infix fun Column<Boolean>.then(other: Column<Boolean>): Branch<Boolean> = Branch(Formula.Ref(this), Formula.Ref(other))

context(_: Formulas)
@JvmName("thenBooleanColumn2")
infix fun Column<Boolean>.then(other: Boolean): Branch<Boolean> = Branch(Formula.Ref(this), Formula.Bool(other))

context(_: Arithmetic)
infix fun Branch<Boolean>.otherwise(value: Boolean): Formula<Boolean> = Formula.If(condition, then, Formula.Bool(value))

internal fun constant(value: Number): Formula<Double> = Formula.Constant(value.checkedExcelDouble())
