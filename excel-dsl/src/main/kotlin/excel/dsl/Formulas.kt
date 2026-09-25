package excel.dsl

/*
 * The scopes of the formula DSL. Which operands exist depends on the context:
 * a row formula can reference the columns of its row, a totals formula only
 * whole columns through `sum(total)` and friends. Plain `Expr` arithmetic,
 * `round`, comparisons and `then`/`otherwise` work in both.
 */

/** What every formula scope has: arithmetic over [Expr], [round], comparisons and `then`/`otherwise`. */
@ExcelDsl
sealed interface Arithmetic {
  /** `ROUND(value, digits)`. */
  fun round(value: Expr, digits: Int = 0): Expr = Expr.Round(value, digits)
}

/**
 * The scope of `formula { }`: one row. The operators below take it as a
 * context, so users write helpers the same way:
 *
 * ```kotlin
 * context(_: Formulas)
 * fun Column<Number>.withVat(): Expr = this * 1.21
 * ```
 */
@ExcelDsl
sealed interface Formulas : Arithmetic {
  /** `SUM(B2, C2)` over [columns], at least one. */
  fun sum(columns: List<Column<Number>>): Expr = aggregate(AggregateFunction.Sum, columns)

  /** `AVERAGE(B2, C2)`, skipping empty cells. */
  fun average(columns: List<Column<Number>>): Expr = aggregate(AggregateFunction.Average, columns)

  fun min(columns: List<Column<Number>>): Expr = aggregate(AggregateFunction.Min, columns)

  fun max(columns: List<Column<Number>>): Expr = aggregate(AggregateFunction.Max, columns)

  /** `ROUND(B2, digits)`. */
  fun round(column: Column<Number>, digits: Int = 0): Expr = Expr.Round(Expr.Ref(column), digits)

  private fun aggregate(function: AggregateFunction, columns: List<Column<Number>>): Expr =
    Expr.Aggregate(function, columns.map(Expr::Ref))
}

/** The scope of a totals cell: every data row of a column at once, `SUM(E2:E4)`. */
@ExcelDsl
sealed interface Totals : Arithmetic {
  fun sum(column: Column<Number>): Expr = aggregate(AggregateFunction.Sum, column)

  /** Skips empty cells; `#DIV/0!` in Excel, `NaN` in `evaluate`, when all are empty. */
  fun average(column: Column<Number>): Expr = aggregate(AggregateFunction.Average, column)

  /** `0` when every cell is empty, like Excel. */
  fun min(column: Column<Number>): Expr = aggregate(AggregateFunction.Min, column)

  /** `0` when every cell is empty, like Excel. */
  fun max(column: Column<Number>): Expr = aggregate(AggregateFunction.Max, column)

  /** `COUNTA`: the non-empty cells of a column of any type. */
  fun count(column: Column<*>): Expr = aggregate(AggregateFunction.Count, column)

  private fun aggregate(function: AggregateFunction, column: Column<*>): Expr =
    Expr.Aggregate(function, listOf(Expr.Range(column)))
}

internal object FormulaScope : Formulas

internal object TotalsScope : Totals

/** Half of `condition then a otherwise b`, finished by [otherwise]. */
class Branch internal constructor(internal val condition: Expr, internal val then: Expr)

context(_: Arithmetic)
operator fun Expr.plus(other: Expr): Expr = Expr.Plus(this, other)

context(_: Arithmetic)
operator fun Expr.plus(other: Number): Expr = Expr.Plus(this, constant(other))

context(_: Formulas)
operator fun Expr.plus(other: Column<Number>): Expr = Expr.Plus(this, Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.plus(other: Expr): Expr = Expr.Plus(Expr.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.plus(other: Column<Number>): Expr =
  Expr.Plus(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.plus(other: Number): Expr = Expr.Plus(Expr.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Expr.minus(other: Expr): Expr = Expr.Minus(this, other)

context(_: Arithmetic)
operator fun Expr.minus(other: Number): Expr = Expr.Minus(this, constant(other))

context(_: Formulas)
operator fun Expr.minus(other: Column<Number>): Expr = Expr.Minus(this, Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.minus(other: Expr): Expr = Expr.Minus(Expr.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.minus(other: Column<Number>): Expr =
  Expr.Minus(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.minus(other: Number): Expr = Expr.Minus(Expr.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Expr.times(other: Expr): Expr = Expr.Times(this, other)

context(_: Arithmetic)
operator fun Expr.times(other: Number): Expr = Expr.Times(this, constant(other))

context(_: Formulas)
operator fun Expr.times(other: Column<Number>): Expr = Expr.Times(this, Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.times(other: Expr): Expr = Expr.Times(Expr.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Expr =
  Expr.Times(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.times(other: Number): Expr = Expr.Times(Expr.Ref(this), constant(other))

context(_: Arithmetic)
operator fun Expr.div(other: Expr): Expr = Expr.Div(this, other)

context(_: Arithmetic)
operator fun Expr.div(other: Number): Expr = Expr.Div(this, constant(other))

context(_: Formulas)
operator fun Expr.div(other: Column<Number>): Expr = Expr.Div(this, Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.div(other: Expr): Expr = Expr.Div(Expr.Ref(this), other)

context(_: Formulas)
operator fun Column<Number>.div(other: Column<Number>): Expr =
  Expr.Div(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
operator fun Column<Number>.div(other: Number): Expr = Expr.Div(Expr.Ref(this), constant(other))

/** Comparisons are infix functions, since `>` has to return `Boolean`: `total gt 1000`. */
context(_: Arithmetic)
infix fun Expr.gt(other: Expr): Expr = Expr.Compare(this, Comparison.Greater, other)

context(_: Arithmetic)
infix fun Expr.gt(other: Number): Expr = Expr.Compare(this, Comparison.Greater, constant(other))

context(_: Formulas)
infix fun Expr.gt(other: Column<Number>): Expr = Expr.Compare(this, Comparison.Greater, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.gt(other: Expr): Expr = Expr.Compare(Expr.Ref(this), Comparison.Greater, other)

context(_: Formulas)
infix fun Column<Number>.gt(other: Column<Number>): Expr =
  Expr.Compare(Expr.Ref(this), Comparison.Greater, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.gt(other: Number): Expr = Expr.Compare(Expr.Ref(this), Comparison.Greater, constant(other))

context(_: Arithmetic)
infix fun Expr.ge(other: Expr): Expr = Expr.Compare(this, Comparison.GreaterOrEqual, other)

context(_: Arithmetic)
infix fun Expr.ge(other: Number): Expr = Expr.Compare(this, Comparison.GreaterOrEqual, constant(other))

context(_: Formulas)
infix fun Expr.ge(other: Column<Number>): Expr = Expr.Compare(this, Comparison.GreaterOrEqual, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.ge(other: Expr): Expr = Expr.Compare(Expr.Ref(this), Comparison.GreaterOrEqual, other)

context(_: Formulas)
infix fun Column<Number>.ge(other: Column<Number>): Expr =
  Expr.Compare(Expr.Ref(this), Comparison.GreaterOrEqual, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.ge(other: Number): Expr = Expr.Compare(Expr.Ref(this), Comparison.GreaterOrEqual, constant(other))

context(_: Arithmetic)
infix fun Expr.lt(other: Expr): Expr = Expr.Compare(this, Comparison.Less, other)

context(_: Arithmetic)
infix fun Expr.lt(other: Number): Expr = Expr.Compare(this, Comparison.Less, constant(other))

context(_: Formulas)
infix fun Expr.lt(other: Column<Number>): Expr = Expr.Compare(this, Comparison.Less, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.lt(other: Expr): Expr = Expr.Compare(Expr.Ref(this), Comparison.Less, other)

context(_: Formulas)
infix fun Column<Number>.lt(other: Column<Number>): Expr =
  Expr.Compare(Expr.Ref(this), Comparison.Less, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.lt(other: Number): Expr = Expr.Compare(Expr.Ref(this), Comparison.Less, constant(other))

context(_: Arithmetic)
infix fun Expr.le(other: Expr): Expr = Expr.Compare(this, Comparison.LessOrEqual, other)

context(_: Arithmetic)
infix fun Expr.le(other: Number): Expr = Expr.Compare(this, Comparison.LessOrEqual, constant(other))

context(_: Formulas)
infix fun Expr.le(other: Column<Number>): Expr = Expr.Compare(this, Comparison.LessOrEqual, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.le(other: Expr): Expr = Expr.Compare(Expr.Ref(this), Comparison.LessOrEqual, other)

context(_: Formulas)
infix fun Column<Number>.le(other: Column<Number>): Expr =
  Expr.Compare(Expr.Ref(this), Comparison.LessOrEqual, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.le(other: Number): Expr = Expr.Compare(Expr.Ref(this), Comparison.LessOrEqual, constant(other))

context(_: Arithmetic)
infix fun Expr.eq(other: Expr): Expr = Expr.Compare(this, Comparison.Equal, other)

context(_: Arithmetic)
infix fun Expr.eq(other: Number): Expr = Expr.Compare(this, Comparison.Equal, constant(other))

context(_: Formulas)
infix fun Expr.eq(other: Column<Number>): Expr = Expr.Compare(this, Comparison.Equal, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.eq(other: Expr): Expr = Expr.Compare(Expr.Ref(this), Comparison.Equal, other)

context(_: Formulas)
infix fun Column<Number>.eq(other: Column<Number>): Expr =
  Expr.Compare(Expr.Ref(this), Comparison.Equal, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.eq(other: Number): Expr = Expr.Compare(Expr.Ref(this), Comparison.Equal, constant(other))

context(_: Arithmetic)
infix fun Expr.ne(other: Expr): Expr = Expr.Compare(this, Comparison.NotEqual, other)

context(_: Arithmetic)
infix fun Expr.ne(other: Number): Expr = Expr.Compare(this, Comparison.NotEqual, constant(other))

context(_: Formulas)
infix fun Expr.ne(other: Column<Number>): Expr = Expr.Compare(this, Comparison.NotEqual, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.ne(other: Expr): Expr = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, other)

context(_: Formulas)
infix fun Column<Number>.ne(other: Column<Number>): Expr =
  Expr.Compare(Expr.Ref(this), Comparison.NotEqual, Expr.Ref(other))

context(_: Formulas)
infix fun Column<Number>.ne(other: Number): Expr = Expr.Compare(Expr.Ref(this), Comparison.NotEqual, constant(other))

/** `total gt 1000 then total * 0.9 otherwise total`: `IF(E2>1000, E2*0.9, E2)`. */
context(_: Arithmetic)
infix fun Expr.then(value: Expr): Branch = Branch(this, value)

context(_: Arithmetic)
infix fun Expr.then(value: Number): Branch = Branch(this, constant(value))

context(_: Formulas)
infix fun Expr.then(value: Column<Number>): Branch = Branch(this, Expr.Ref(value))

context(_: Formulas)
infix fun Column<Number>.then(value: Expr): Branch = Branch(Expr.Ref(this), value)

context(_: Formulas)
infix fun Column<Number>.then(value: Number): Branch = Branch(Expr.Ref(this), constant(value))

context(_: Formulas)
infix fun Column<Number>.then(value: Column<Number>): Branch = Branch(Expr.Ref(this), Expr.Ref(value))

context(_: Arithmetic)
infix fun Branch.otherwise(value: Expr): Expr = Expr.If(condition, then, value)

context(_: Arithmetic)
infix fun Branch.otherwise(value: Number): Expr = Expr.If(condition, then, constant(value))

context(_: Formulas)
infix fun Branch.otherwise(value: Column<Number>): Expr = Expr.If(condition, then, Expr.Ref(value))

private fun constant(value: Number): Expr = Expr.Constant(value.toDouble())
