package excel.dsl

/** The cells of the totals row, one per column at most. */
@ExcelDsl
class TotalsBuilder internal constructor(internal val sheet: SheetBuilder<*>) {
  internal fun add(column: Column<*>, spec: TotalSpec) {
    sheet.addTotal(column, spec)
  }
}

/** A bold row under the data. Not written when there are no rows. */
context(sheet: SheetBuilder<*>)
fun totals(block: context(TotalsBuilder) () -> Unit) {
  block(TotalsBuilder(sheet))
}

/** A text cell, such as `Total`. */
context(builder: TotalsBuilder)
fun Column<*>.label(text: String) {
  builder.add(this, TotalSpec.Label(text))
}

/** Numeric totals return numbers, text totals text, and Boolean totals booleans. */
@JvmName("numberTotal")
context(_: TotalsBuilder)
fun Column<Number>.total(block: context(Totals) () -> Formula<Number>): Unit = formulaTotal(this, block)

@JvmName("textTotal")
context(_: TotalsBuilder)
fun Column<String>.total(block: context(Totals) () -> Formula<String>): Unit = formulaTotal(this, block)

@JvmName("booleanTotal")
context(_: TotalsBuilder)
fun Column<Boolean>.total(block: context(Totals) () -> Formula<Boolean>): Unit = formulaTotal(this, block)

context(builder: TotalsBuilder)
private fun formulaTotal(column: Column<*>, block: context(Totals) () -> Formula<*>) {
  val expr = block(TotalsScope)
  builder.sheet.checkFormula(expr)
  require(expr.references().none { it is Formula.Ref }) { "A total cannot reference a single row: $expr" }
  builder.add(column, TotalSpec.Formula(expr))
}

/** `SUM(E2:E4)` */
context(_: TotalsBuilder)
fun Column<Number>.sum(): Unit = total { sum(this@sum) }

/** `AVERAGE(E2:E4)` */
context(_: TotalsBuilder)
fun Column<Number>.average(): Unit = total { average(this@average) }

/** `MIN(E2:E4)` */
context(_: TotalsBuilder)
fun Column<Number>.min(): Unit = total { min(this@min) }

/** `MAX(E2:E4)` */
context(_: TotalsBuilder)
fun Column<Number>.max(): Unit = total { max(this@max) }

/** `COUNT(A2:A4)`: numeric cells only; blanks and errors do not count. */
context(_: TotalsBuilder)
fun Column<Number>.count(): Unit = total { count(this@count) }

/** `COUNTA(A2:A4)`: non-blank cells, including text, booleans and errors. */
context(_: TotalsBuilder)
fun Column<*>.countNonBlank(): Unit = formulaTotal(this) { countNonBlank(this@countNonBlank) }
