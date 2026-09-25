package presentation.support.expressions

import presentation.support.Column

/**
 * The scope of `formula { }`. It no longer renders anything, it only makes
 * the operators below available, as a context like in Part 5.
 */
interface Formulas {
  fun sum(columns: List<Column<Number>>): Expr =
    Expr.Sum(columns.map(Expr::Ref))
}

context(_: Formulas)
operator fun Expr.times(other: Expr): Expr = Expr.Times(this, other)

context(_: Formulas)
operator fun Expr.times(factor: Double): Expr = this * Expr.Constant(factor)

context(_: Formulas)
operator fun Expr.times(other: Column<Number>): Expr = this * Expr.Ref(other)

context(_: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Expr =
  Expr.Ref(this) * Expr.Ref(other)

context(_: Formulas)
operator fun Column<Number>.times(factor: Double): Expr = Expr.Ref(this) * factor

context(_: Formulas)
operator fun Column<Number>.times(other: Expr): Expr = Expr.Ref(this) * other

context(_: Formulas)
fun Column<Number>.withVat(): Expr = this * 1.21
