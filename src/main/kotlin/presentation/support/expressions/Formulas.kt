package presentation.support.expressions

import presentation.support.Column

/**
 * The scope of `formula { }`. It no longer renders anything, it only makes
 * the operators below available, as a context like in Part 5.
 */
interface Formulas {
  fun sum(columns: List<Column<Number>>): Formula =
    Formula.Sum(columns.map(Formula::Ref))
}

context(_: Formulas)
operator fun Formula.times(other: Formula): Formula = Formula.Times(this, other)

context(_: Formulas)
operator fun Formula.times(factor: Double): Formula = this * Formula.Constant(factor)

context(_: Formulas)
operator fun Formula.times(other: Column<Number>): Formula = this * Formula.Ref(other)

context(_: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Formula =
  Formula.Ref(this) * Formula.Ref(other)

context(_: Formulas)
operator fun Column<Number>.times(factor: Double): Formula = Formula.Ref(this) * factor

context(_: Formulas)
operator fun Column<Number>.times(other: Formula): Formula = Formula.Ref(this) * other

context(_: Formulas)
fun Column<Number>.withVat(): Formula = this * 1.21
