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

internal object FormulaScope : Formulas
internal object TotalsScope : Totals

context(_: Arithmetic)
fun round(value: Expr<Number>, digits: Int = 0): Expr<Double> = Expr.Round(value, digits)

context(_: Arithmetic)
fun literal(value: Number): Expr<Double> = constant(value)

context(_: Arithmetic)
fun literal(value: String): Expr<String> = Expr.Text(value)

context(_: Arithmetic)
fun literal(value: Boolean): Expr<Boolean> = Expr.Bool(value)

context(_: Formulas)
fun sum(columns: List<Column<Number>>): Expr<Double> =
  Expr.Aggregate(AggregateFunction.Sum, columns.map { Expr.Ref(it) })

context(_: Formulas)
fun average(columns: List<Column<Number>>): Expr<Double> =
  Expr.Aggregate(AggregateFunction.Average, columns.map { Expr.Ref(it) })

context(_: Formulas)
fun min(columns: List<Column<Number>>): Expr<Double> =
  Expr.Aggregate(AggregateFunction.Min, columns.map { Expr.Ref(it) })

context(_: Formulas)
fun max(columns: List<Column<Number>>): Expr<Double> =
  Expr.Aggregate(AggregateFunction.Max, columns.map { Expr.Ref(it) })

context(_: Formulas)
fun round(column: Column<Number>, digits: Int = 0): Expr<Double> =
  Expr.Round(Expr.Ref(column), digits)

context(_: Totals)
fun sum(column: Column<Number>): Expr<Double> =
  Expr.Aggregate(AggregateFunction.Sum, listOf(Expr.Range(column)))

context(_: Totals)
fun average(column: Column<Number>): Expr<Double> =
  Expr.Aggregate(AggregateFunction.Average, listOf(Expr.Range(column)))

context(_: Totals)
fun min(column: Column<Number>): Expr<Double> =
  Expr.Aggregate(AggregateFunction.Min, listOf(Expr.Range(column)))

context(_: Totals)
fun max(column: Column<Number>): Expr<Double> =
  Expr.Aggregate(AggregateFunction.Max, listOf(Expr.Range(column)))

/** COUNT counts numeric cells, excluding blanks and errors. */
context(_: Totals)
fun count(column: Column<Number>): Expr<Double> =
  Expr.Aggregate(AggregateFunction.Count, listOf(Expr.Range(column)))

/** COUNTA counts non-blank cells, including text, booleans and errors. */
context(_: Totals)
fun countNonBlank(column: Column<*>): Expr<Double> =
  Expr.Aggregate(AggregateFunction.CountNonBlank, listOf(Expr.Range(column)))

/** Explicit reference for an alias formula: `formula { amount.ref() }`. */
context(_: Formulas)
@JvmName("numberRef")
fun Column<Number>.ref(): Expr<Number> = Expr.Ref(this)
context(_: Formulas)
@JvmName("textRef")
fun Column<String>.ref(): Expr<String> = Expr.Ref(this)
context(_: Formulas)
@JvmName("booleanRef")
fun Column<Boolean>.ref(): Expr<Boolean> = Expr.Ref(this)

/** Explicit conversion: Boolean cells are otherwise excluded from numeric operations. */
context(_: Arithmetic)
fun Expr<Boolean>.asNumber(): Expr<Double> = Expr.If(this, Expr.Constant(1.0), Expr.Constant(0.0))
context(_: Formulas)
fun Column<Boolean>.asNumber(): Expr<Double> = ref().asNumber()
context(_: Arithmetic)
fun Expr<Number>.asBoolean(): Expr<Boolean> = Expr.Compare(this, Comparison.NotEqual, Expr.Constant(0.0))
context(_: Formulas)
fun Column<Number>.asBoolean(): Expr<Boolean> = ref().asBoolean()

/** Invariant V prevents otherwise from widening incompatible branches to Any. */
class Branch<V> internal constructor(internal val condition: Expr<Boolean>, internal val then: Expr<V>) {
  context(_: Arithmetic)
  infix fun otherwise(value: Expr<V>): Expr<V> = Expr.If(condition, then, value)
  context(_: Formulas)
  infix fun otherwise(value: Column<V>): Expr<V> = Expr.If(condition, then, Expr.Ref(value))
}

context(_: Arithmetic)
operator fun Expr<Number>.plus(other: Expr<Number>): Expr<Double> = Expr.Plus(this, other)

context(_: Arithmetic)
operator fun Expr<Number>.plus(other: Number): Expr<Double> = Expr.Plus(this, constant(other))

context(_: Formulas)
operator fun Expr<Number>.plus(other: Column<Number>): Expr<Double> = Expr.Plus(this, Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.plus(other: Expr<Number>): Expr<Double> = Expr.Plus(Expr.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.plus(other: Column<Number>): Expr<Double> = Expr.Plus(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.plus(other: Number): Expr<Double> = Expr.Plus(Expr.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Number.plus(other: Expr<Number>): Expr<Double> = Expr.Plus(constant(this), other)

context(_: Formulas)
operator fun Number.plus(other: Column<Number>): Expr<Double> = Expr.Plus(constant(this), Expr.Ref(other))

context(_: Arithmetic)
operator fun Expr<Number>.minus(other: Expr<Number>): Expr<Double> = Expr.Minus(this, other)

context(_: Arithmetic)
operator fun Expr<Number>.minus(other: Number): Expr<Double> = Expr.Minus(this, constant(other))

context(_: Formulas)
operator fun Expr<Number>.minus(other: Column<Number>): Expr<Double> = Expr.Minus(this, Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.minus(other: Expr<Number>): Expr<Double> = Expr.Minus(Expr.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.minus(other: Column<Number>): Expr<Double> = Expr.Minus(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.minus(other: Number): Expr<Double> = Expr.Minus(Expr.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Number.minus(other: Expr<Number>): Expr<Double> = Expr.Minus(constant(this), other)

context(_: Formulas)
operator fun Number.minus(other: Column<Number>): Expr<Double> = Expr.Minus(constant(this), Expr.Ref(other))

context(_: Arithmetic)
operator fun Expr<Number>.times(other: Expr<Number>): Expr<Double> = Expr.Times(this, other)

context(_: Arithmetic)
operator fun Expr<Number>.times(other: Number): Expr<Double> = Expr.Times(this, constant(other))

context(_: Formulas)
operator fun Expr<Number>.times(other: Column<Number>): Expr<Double> = Expr.Times(this, Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.times(other: Expr<Number>): Expr<Double> = Expr.Times(Expr.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Expr<Double> = Expr.Times(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.times(other: Number): Expr<Double> = Expr.Times(Expr.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Number.times(other: Expr<Number>): Expr<Double> = Expr.Times(constant(this), other)

context(_: Formulas)
operator fun Number.times(other: Column<Number>): Expr<Double> = Expr.Times(constant(this), Expr.Ref(other))

context(_: Arithmetic)
operator fun Expr<Number>.div(other: Expr<Number>): Expr<Double> = Expr.Div(this, other)

context(_: Arithmetic)
operator fun Expr<Number>.div(other: Number): Expr<Double> = Expr.Div(this, constant(other))

context(_: Formulas)
operator fun Expr<Number>.div(other: Column<Number>): Expr<Double> = Expr.Div(this, Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.div(other: Expr<Number>): Expr<Double> = Expr.Div(Expr.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.div(other: Column<Number>): Expr<Double> = Expr.Div(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.div(other: Number): Expr<Double> = Expr.Div(Expr.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Number.div(other: Expr<Number>): Expr<Double> = Expr.Div(constant(this), other)

context(_: Formulas)
operator fun Number.div(other: Column<Number>): Expr<Double> = Expr.Div(constant(this), Expr.Ref(other))

context(_: Arithmetic)
operator fun Expr<Number>.unaryMinus(): Expr<Double> = Expr.Minus(Expr.Constant(0.0), this)

context(_: Formulas)
operator fun Column<Number>.unaryMinus(): Expr<Double> = Expr.Minus(Expr.Constant(0.0), Expr.Ref(this))

context(_: Arithmetic)
@JvmName("gtNumber0")
infix fun Expr<Number>.gt(other: Expr<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.Greater, other)

context(_: Arithmetic)
@JvmName("gtNumber1")
infix fun Expr<Number>.gt(other: Number): Expr<Boolean> = Expr.Compare(this, Comparison.Greater, constant(other))

context(_: Formulas)
@JvmName("gtNumber2")
infix fun Expr<Number>.gt(other: Column<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.Greater, Expr.Ref(other))

context(_: Formulas)
@JvmName("gtNumber3")
infix fun Column<Number>.gt(other: Expr<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Greater, other)

context(_: Formulas)
@JvmName("gtNumber4")
infix fun Column<Number>.gt(other: Column<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Greater, Expr.Ref(other))

context(_: Formulas)
@JvmName("gtNumber5")
infix fun Column<Number>.gt(other: Number): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Greater, constant(other))

context(_: Arithmetic)
@JvmName("geNumber0")
infix fun Expr<Number>.ge(other: Expr<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.GreaterOrEqual, other)

context(_: Arithmetic)
@JvmName("geNumber1")
infix fun Expr<Number>.ge(other: Number): Expr<Boolean> = Expr.Compare(this, Comparison.GreaterOrEqual, constant(other))

context(_: Formulas)
@JvmName("geNumber2")
infix fun Expr<Number>.ge(other: Column<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.GreaterOrEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("geNumber3")
infix fun Column<Number>.ge(other: Expr<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.GreaterOrEqual, other)

context(_: Formulas)
@JvmName("geNumber4")
infix fun Column<Number>.ge(other: Column<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.GreaterOrEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("geNumber5")
infix fun Column<Number>.ge(other: Number): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.GreaterOrEqual, constant(other))

context(_: Arithmetic)
@JvmName("ltNumber0")
infix fun Expr<Number>.lt(other: Expr<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.Less, other)

context(_: Arithmetic)
@JvmName("ltNumber1")
infix fun Expr<Number>.lt(other: Number): Expr<Boolean> = Expr.Compare(this, Comparison.Less, constant(other))

context(_: Formulas)
@JvmName("ltNumber2")
infix fun Expr<Number>.lt(other: Column<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.Less, Expr.Ref(other))

context(_: Formulas)
@JvmName("ltNumber3")
infix fun Column<Number>.lt(other: Expr<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Less, other)

context(_: Formulas)
@JvmName("ltNumber4")
infix fun Column<Number>.lt(other: Column<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Less, Expr.Ref(other))

context(_: Formulas)
@JvmName("ltNumber5")
infix fun Column<Number>.lt(other: Number): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Less, constant(other))

context(_: Arithmetic)
@JvmName("leNumber0")
infix fun Expr<Number>.le(other: Expr<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.LessOrEqual, other)

context(_: Arithmetic)
@JvmName("leNumber1")
infix fun Expr<Number>.le(other: Number): Expr<Boolean> = Expr.Compare(this, Comparison.LessOrEqual, constant(other))

context(_: Formulas)
@JvmName("leNumber2")
infix fun Expr<Number>.le(other: Column<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.LessOrEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("leNumber3")
infix fun Column<Number>.le(other: Expr<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.LessOrEqual, other)

context(_: Formulas)
@JvmName("leNumber4")
infix fun Column<Number>.le(other: Column<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.LessOrEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("leNumber5")
infix fun Column<Number>.le(other: Number): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.LessOrEqual, constant(other))

context(_: Arithmetic)
@JvmName("eqNumber0")
infix fun Expr<Number>.eq(other: Expr<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.Equal, other)

context(_: Arithmetic)
@JvmName("eqNumber1")
infix fun Expr<Number>.eq(other: Number): Expr<Boolean> = Expr.Compare(this, Comparison.Equal, constant(other))

context(_: Formulas)
@JvmName("eqNumber2")
infix fun Expr<Number>.eq(other: Column<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.Equal, Expr.Ref(other))

context(_: Formulas)
@JvmName("eqNumber3")
infix fun Column<Number>.eq(other: Expr<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Equal, other)

context(_: Formulas)
@JvmName("eqNumber4")
infix fun Column<Number>.eq(other: Column<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Equal, Expr.Ref(other))

context(_: Formulas)
@JvmName("eqNumber5")
infix fun Column<Number>.eq(other: Number): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Equal, constant(other))

context(_: Arithmetic)
@JvmName("neNumber0")
infix fun Expr<Number>.ne(other: Expr<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.NotEqual, other)

context(_: Arithmetic)
@JvmName("neNumber1")
infix fun Expr<Number>.ne(other: Number): Expr<Boolean> = Expr.Compare(this, Comparison.NotEqual, constant(other))

context(_: Formulas)
@JvmName("neNumber2")
infix fun Expr<Number>.ne(other: Column<Number>): Expr<Boolean> = Expr.Compare(this, Comparison.NotEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("neNumber3")
infix fun Column<Number>.ne(other: Expr<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, other)

context(_: Formulas)
@JvmName("neNumber4")
infix fun Column<Number>.ne(other: Column<Number>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("neNumber5")
infix fun Column<Number>.ne(other: Number): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, constant(other))

context(_: Arithmetic)
@JvmName("gtString0")
infix fun Expr<String>.gt(other: Expr<String>): Expr<Boolean> = Expr.Compare(this, Comparison.Greater, other)

context(_: Arithmetic)
@JvmName("gtString1")
infix fun Expr<String>.gt(other: String): Expr<Boolean> = Expr.Compare(this, Comparison.Greater, Expr.Text(other))

context(_: Formulas)
@JvmName("gtString2")
infix fun Expr<String>.gt(other: Column<String>): Expr<Boolean> = Expr.Compare(this, Comparison.Greater, Expr.Ref(other))

context(_: Formulas)
@JvmName("gtString3")
infix fun Column<String>.gt(other: Expr<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Greater, other)

context(_: Formulas)
@JvmName("gtString4")
infix fun Column<String>.gt(other: Column<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Greater, Expr.Ref(other))

context(_: Formulas)
@JvmName("gtString5")
infix fun Column<String>.gt(other: String): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Greater, Expr.Text(other))

context(_: Arithmetic)
@JvmName("geString0")
infix fun Expr<String>.ge(other: Expr<String>): Expr<Boolean> = Expr.Compare(this, Comparison.GreaterOrEqual, other)

context(_: Arithmetic)
@JvmName("geString1")
infix fun Expr<String>.ge(other: String): Expr<Boolean> = Expr.Compare(this, Comparison.GreaterOrEqual, Expr.Text(other))

context(_: Formulas)
@JvmName("geString2")
infix fun Expr<String>.ge(other: Column<String>): Expr<Boolean> = Expr.Compare(this, Comparison.GreaterOrEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("geString3")
infix fun Column<String>.ge(other: Expr<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.GreaterOrEqual, other)

context(_: Formulas)
@JvmName("geString4")
infix fun Column<String>.ge(other: Column<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.GreaterOrEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("geString5")
infix fun Column<String>.ge(other: String): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.GreaterOrEqual, Expr.Text(other))

context(_: Arithmetic)
@JvmName("ltString0")
infix fun Expr<String>.lt(other: Expr<String>): Expr<Boolean> = Expr.Compare(this, Comparison.Less, other)

context(_: Arithmetic)
@JvmName("ltString1")
infix fun Expr<String>.lt(other: String): Expr<Boolean> = Expr.Compare(this, Comparison.Less, Expr.Text(other))

context(_: Formulas)
@JvmName("ltString2")
infix fun Expr<String>.lt(other: Column<String>): Expr<Boolean> = Expr.Compare(this, Comparison.Less, Expr.Ref(other))

context(_: Formulas)
@JvmName("ltString3")
infix fun Column<String>.lt(other: Expr<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Less, other)

context(_: Formulas)
@JvmName("ltString4")
infix fun Column<String>.lt(other: Column<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Less, Expr.Ref(other))

context(_: Formulas)
@JvmName("ltString5")
infix fun Column<String>.lt(other: String): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Less, Expr.Text(other))

context(_: Arithmetic)
@JvmName("leString0")
infix fun Expr<String>.le(other: Expr<String>): Expr<Boolean> = Expr.Compare(this, Comparison.LessOrEqual, other)

context(_: Arithmetic)
@JvmName("leString1")
infix fun Expr<String>.le(other: String): Expr<Boolean> = Expr.Compare(this, Comparison.LessOrEqual, Expr.Text(other))

context(_: Formulas)
@JvmName("leString2")
infix fun Expr<String>.le(other: Column<String>): Expr<Boolean> = Expr.Compare(this, Comparison.LessOrEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("leString3")
infix fun Column<String>.le(other: Expr<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.LessOrEqual, other)

context(_: Formulas)
@JvmName("leString4")
infix fun Column<String>.le(other: Column<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.LessOrEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("leString5")
infix fun Column<String>.le(other: String): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.LessOrEqual, Expr.Text(other))

context(_: Arithmetic)
@JvmName("eqString0")
infix fun Expr<String>.eq(other: Expr<String>): Expr<Boolean> = Expr.Compare(this, Comparison.Equal, other)

context(_: Arithmetic)
@JvmName("eqString1")
infix fun Expr<String>.eq(other: String): Expr<Boolean> = Expr.Compare(this, Comparison.Equal, Expr.Text(other))

context(_: Formulas)
@JvmName("eqString2")
infix fun Expr<String>.eq(other: Column<String>): Expr<Boolean> = Expr.Compare(this, Comparison.Equal, Expr.Ref(other))

context(_: Formulas)
@JvmName("eqString3")
infix fun Column<String>.eq(other: Expr<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Equal, other)

context(_: Formulas)
@JvmName("eqString4")
infix fun Column<String>.eq(other: Column<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Equal, Expr.Ref(other))

context(_: Formulas)
@JvmName("eqString5")
infix fun Column<String>.eq(other: String): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Equal, Expr.Text(other))

context(_: Arithmetic)
@JvmName("neString0")
infix fun Expr<String>.ne(other: Expr<String>): Expr<Boolean> = Expr.Compare(this, Comparison.NotEqual, other)

context(_: Arithmetic)
@JvmName("neString1")
infix fun Expr<String>.ne(other: String): Expr<Boolean> = Expr.Compare(this, Comparison.NotEqual, Expr.Text(other))

context(_: Formulas)
@JvmName("neString2")
infix fun Expr<String>.ne(other: Column<String>): Expr<Boolean> = Expr.Compare(this, Comparison.NotEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("neString3")
infix fun Column<String>.ne(other: Expr<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, other)

context(_: Formulas)
@JvmName("neString4")
infix fun Column<String>.ne(other: Column<String>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("neString5")
infix fun Column<String>.ne(other: String): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, Expr.Text(other))

context(_: Arithmetic)
@JvmName("eqBoolean0")
infix fun Expr<Boolean>.eq(other: Expr<Boolean>): Expr<Boolean> = Expr.Compare(this, Comparison.Equal, other)

context(_: Arithmetic)
@JvmName("eqBoolean1")
infix fun Expr<Boolean>.eq(other: Boolean): Expr<Boolean> = Expr.Compare(this, Comparison.Equal, Expr.Bool(other))

context(_: Formulas)
@JvmName("eqBoolean2")
infix fun Expr<Boolean>.eq(other: Column<Boolean>): Expr<Boolean> = Expr.Compare(this, Comparison.Equal, Expr.Ref(other))

context(_: Formulas)
@JvmName("eqBoolean3")
infix fun Column<Boolean>.eq(other: Expr<Boolean>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Equal, other)

context(_: Formulas)
@JvmName("eqBoolean4")
infix fun Column<Boolean>.eq(other: Column<Boolean>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Equal, Expr.Ref(other))

context(_: Formulas)
@JvmName("eqBoolean5")
infix fun Column<Boolean>.eq(other: Boolean): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.Equal, Expr.Bool(other))

context(_: Arithmetic)
@JvmName("neBoolean0")
infix fun Expr<Boolean>.ne(other: Expr<Boolean>): Expr<Boolean> = Expr.Compare(this, Comparison.NotEqual, other)

context(_: Arithmetic)
@JvmName("neBoolean1")
infix fun Expr<Boolean>.ne(other: Boolean): Expr<Boolean> = Expr.Compare(this, Comparison.NotEqual, Expr.Bool(other))

context(_: Formulas)
@JvmName("neBoolean2")
infix fun Expr<Boolean>.ne(other: Column<Boolean>): Expr<Boolean> = Expr.Compare(this, Comparison.NotEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("neBoolean3")
infix fun Column<Boolean>.ne(other: Expr<Boolean>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, other)

context(_: Formulas)
@JvmName("neBoolean4")
infix fun Column<Boolean>.ne(other: Column<Boolean>): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, Expr.Ref(other))

context(_: Formulas)
@JvmName("neBoolean5")
infix fun Column<Boolean>.ne(other: Boolean): Expr<Boolean> = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, Expr.Bool(other))

context(_: Arithmetic)
infix fun Expr<Boolean>.and(other: Expr<Boolean>): Expr<Boolean> = Expr.And(this, other)

context(_: Formulas)
infix fun Expr<Boolean>.and(other: Column<Boolean>): Expr<Boolean> = Expr.And(this, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Boolean>.and(other: Expr<Boolean>): Expr<Boolean> = Expr.And(Expr.Ref(this), other)

context(_: Formulas)
infix fun Column<Boolean>.and(other: Column<Boolean>): Expr<Boolean> = Expr.And(Expr.Ref(this), Expr.Ref(other))

context(_: Arithmetic)
infix fun Expr<Boolean>.and(other: Boolean): Expr<Boolean> = Expr.And(this, Expr.Bool(other))

context(_: Formulas)
infix fun Column<Boolean>.and(other: Boolean): Expr<Boolean> = Expr.And(Expr.Ref(this), Expr.Bool(other))

context(_: Arithmetic)
infix fun Expr<Boolean>.or(other: Expr<Boolean>): Expr<Boolean> = Expr.Or(this, other)

context(_: Formulas)
infix fun Expr<Boolean>.or(other: Column<Boolean>): Expr<Boolean> = Expr.Or(this, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Boolean>.or(other: Expr<Boolean>): Expr<Boolean> = Expr.Or(Expr.Ref(this), other)

context(_: Formulas)
infix fun Column<Boolean>.or(other: Column<Boolean>): Expr<Boolean> = Expr.Or(Expr.Ref(this), Expr.Ref(other))

context(_: Arithmetic)
infix fun Expr<Boolean>.or(other: Boolean): Expr<Boolean> = Expr.Or(this, Expr.Bool(other))

context(_: Formulas)
infix fun Column<Boolean>.or(other: Boolean): Expr<Boolean> = Expr.Or(Expr.Ref(this), Expr.Bool(other))

context(_: Arithmetic)
operator fun Expr<Boolean>.not(): Expr<Boolean> = Expr.Not(this)

context(_: Formulas)
operator fun Column<Boolean>.not(): Expr<Boolean> = Expr.Not(Expr.Ref(this))

context(_: Arithmetic)
@JvmName("thenNumberExpr0")
infix fun Expr<Boolean>.then(other: Expr<Number>): Branch<Number> = Branch(this, other)

context(_: Formulas)
@JvmName("thenNumberExpr1")
infix fun Expr<Boolean>.then(other: Column<Number>): Branch<Number> = Branch(this, Expr.Ref(other))

context(_: Arithmetic)
@JvmName("thenNumberExpr2")
infix fun Expr<Boolean>.then(other: Number): Branch<Number> = Branch(this, constant(other))

context(_: Formulas)
@JvmName("thenNumberColumn0")
infix fun Column<Boolean>.then(other: Expr<Number>): Branch<Number> = Branch(Expr.Ref(this), other)

context(_: Formulas)
@JvmName("thenNumberColumn1")
infix fun Column<Boolean>.then(other: Column<Number>): Branch<Number> = Branch(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
@JvmName("thenNumberColumn2")
infix fun Column<Boolean>.then(other: Number): Branch<Number> = Branch(Expr.Ref(this), constant(other))

context(_: Arithmetic)
infix fun Branch<Number>.otherwise(value: Number): Expr<Number> = Expr.If(condition, then, constant(value))

context(_: Arithmetic)
@JvmName("thenStringExpr0")
infix fun Expr<Boolean>.then(other: Expr<String>): Branch<String> = Branch(this, other)

context(_: Formulas)
@JvmName("thenStringExpr1")
infix fun Expr<Boolean>.then(other: Column<String>): Branch<String> = Branch(this, Expr.Ref(other))

context(_: Arithmetic)
@JvmName("thenStringExpr2")
infix fun Expr<Boolean>.then(other: String): Branch<String> = Branch(this, Expr.Text(other))

context(_: Formulas)
@JvmName("thenStringColumn0")
infix fun Column<Boolean>.then(other: Expr<String>): Branch<String> = Branch(Expr.Ref(this), other)

context(_: Formulas)
@JvmName("thenStringColumn1")
infix fun Column<Boolean>.then(other: Column<String>): Branch<String> = Branch(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
@JvmName("thenStringColumn2")
infix fun Column<Boolean>.then(other: String): Branch<String> = Branch(Expr.Ref(this), Expr.Text(other))

context(_: Arithmetic)
infix fun Branch<String>.otherwise(value: String): Expr<String> = Expr.If(condition, then, Expr.Text(value))

context(_: Arithmetic)
@JvmName("thenBooleanExpr0")
infix fun Expr<Boolean>.then(other: Expr<Boolean>): Branch<Boolean> = Branch(this, other)

context(_: Formulas)
@JvmName("thenBooleanExpr1")
infix fun Expr<Boolean>.then(other: Column<Boolean>): Branch<Boolean> = Branch(this, Expr.Ref(other))

context(_: Arithmetic)
@JvmName("thenBooleanExpr2")
infix fun Expr<Boolean>.then(other: Boolean): Branch<Boolean> = Branch(this, Expr.Bool(other))

context(_: Formulas)
@JvmName("thenBooleanColumn0")
infix fun Column<Boolean>.then(other: Expr<Boolean>): Branch<Boolean> = Branch(Expr.Ref(this), other)

context(_: Formulas)
@JvmName("thenBooleanColumn1")
infix fun Column<Boolean>.then(other: Column<Boolean>): Branch<Boolean> = Branch(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
@JvmName("thenBooleanColumn2")
infix fun Column<Boolean>.then(other: Boolean): Branch<Boolean> = Branch(Expr.Ref(this), Expr.Bool(other))

context(_: Arithmetic)
infix fun Branch<Boolean>.otherwise(value: Boolean): Expr<Boolean> = Expr.If(condition, then, Expr.Bool(value))

internal fun constant(value: Number): Expr<Double> = Expr.Constant(value.checkedExcelDouble())
