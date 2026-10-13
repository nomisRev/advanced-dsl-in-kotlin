---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 6</div>

# Cost and evolution

## What a DSL costs, and how it changes

---

# Varargs take any number of columns

<DrawnAnnotation text="vararg" label="`columns` is an `Array<Column<Number>>`"  :geometry="{ label: { x: 0.5032, y: 0.3037 }, connector: { type: 'quadratic', start: { x: 0.2355, y: 0.2723 }, control: { x: 0.2749, y: 0.3044 }, end: { x: 0.3182, y: 0.3051 } } }"/>
<DrawnAnnotation text="sum(hours, overtime)" label="Allocates a new array on every call" color="var(--fundamentals-pink)" />

```kotlin
import presentation.support.varargs.*

interface Formulas {
  fun sum(vararg columns: Column<Number>): Cell
}

val subtotal by formula { sum(hours, overtime) }
```

---
magic-move
---

# Spread copies the array

<DrawnAnnotation text="*billable" label="Copies `billable` into a new array" color="red" />

```kotlin
import presentation.support.varargs.*

interface Formulas {
  fun sum(vararg columns: Column<Number>): Cell
}

val subtotal by formula { sum(hours, overtime) }

val billable = arrayOf(hours, overtime)
val total by formula { sum(*billable) }
```

---
magic-move
---

# Accept a `List` instead

<DrawnAnnotation text="columns: Iterable<Column<Number>>" label="No array, no copy"  :connect="false" :geometry="{ label: { x: 0.4393, y: 0.3021 } }"/>

```kotlin
import presentation.support.lists.*

interface Formulas {
  fun sum(columns: Iterable<Column<Number>>): Cell
}

val subtotal by formula { sum(listOf(hours, overtime)) }

val billable = listOf(hours, overtime)
val total by formula { sum(billable) }
```

---
magic-move
---

# Collection literals keep the call short

> Experimental in Kotlin 2.4, `-Xcollection-literals`

<DrawnAnnotation text="[hours, overtime]" label="A `List<Column<Number>>`, from the expected type"  :connect="false" :geometry="{ label: { x: 0.7142, y: 0.5607 } }"/>

```kotlin
import presentation.support.lists.*

interface Formulas {
  fun sum(columns: Iterable<Column<Number>>): Cell
}

val subtotal by formula { sum([hours, overtime]) }

val billable = [hours, overtime]
val total by formula { sum(billable) }
```
```console
| hours | overtime | subtotal     |
| 12    | 3        | =SUM(B2, C2) |
```

---

# `inline` removes the builder lambda

<DrawnAnnotation text="inline" label="The body is copied to the call site, no `Function1` object"  :geometry="{ label: { x: 0.6311, y: 0.2633 }, connector: { type: 'quadratic', start: { x: 0.1247, y: 0.2140 }, control: { x: 0.2460, y: 0.2274 }, end: { x: 0.3595, y: 0.2591 } } }"/>
<DrawnAnnotation text="rows: Sequence<T>" label="Pulled one row at a time, never all in memory" color="var(--fundamentals-blue)" :geometry="{ label: { x: 0.665, y: 0.336 }, connector: { type: 'quadratic', start: { x: 0.33, y: 0.29 }, control: { x: 0.42, y: 0.30 }, end: { x: 0.49, y: 0.33 } } }" />

```kotlin no-compile
inline fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
)
```

---

# A stored lambda cannot be inlined

<InlineCompilerError :line="5" text="value" occurrence="2" message="Illegal usage of inline parameter 'value: (T) -> Any?'.\nAdd 'noinline' modifier to the parameter declaration.">

```kotlin
class SheetBuilder<T> {
  val values: MutableList<(T) -> Any?> = mutableListOf()

  inline fun column(name: String, value: (T) -> Any?) {
    values.add(value)
  }
}
```

</InlineCompilerError>

<!--
Column lambdas run once per row, so they have to be stored. Only the outer builder block benefits from inline.
-->

---

# Hide the old API, keep the binary

<DrawnAnnotation text="DeprecationLevel.HIDDEN" label="Invisible to new code, still linked by compiled callers"  :connect="false" :geometry="{ label: { x: 0.7030, y: 0.4225 } }"/>
<DrawnAnnotation text="fun column(value: (T) -> Number)" label="The delegated replacement" color="var(--fundamentals-pink)"  :connect="false" :geometry="{ label: { x: 0.5824, y: 0.5632 } }"/>

```kotlin no-compile
@ExcelDsl
class SheetBuilder<T> {
  @Deprecated(
    message = "Use a delegated column: val name by column { }",
    level = DeprecationLevel.HIDDEN,
  )
  fun column(name: String, bold: Boolean = false, value: (T) -> Any?)

  fun column(value: (T) -> Number):
    PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Column<Number>>>
}
```

---

# New formulas extend the interface

<DrawnAnnotation text="interface Formulas : Multiply" label="Part 5's operators stay, `sum` joins them" :connect="false" :geometry="{ label: { x: 0.60, y: 0.33 } }" />
<DrawnAnnotation text="Multiply by ExcelMultiply(row)" label="Delegation: the Part 5 interpreter, unchanged" color="var(--fundamentals-blue)" :connect="false" :geometry="{ label: { x: 0.6633, y: 0.6156 } }" />

```kotlin show-imports
import presentation.support.contexts.ExcelFormulas as ExcelMultiply
import presentation.support.contexts.Formulas as Multiply

interface Formulas : Multiply {
  fun sum(columns: List<Column<Number>>): Cell
}

class ExcelFormulas(private val row: Int) :
  Formulas,
  Multiply by ExcelMultiply(row) {
  override fun sum(columns: List<Column<Number>>): Cell =
    sumFormula(row, columns)
}
```

<!--
This is how the support code really grows from Part 5 to Part 6: `sum` is added by
extending the interface, and each interpreter delegates the old operators to the old
interpreter with `by`. A receiver can be several interfaces at once, so a `formula { }`
block written against Part 5's `Formulas` still runs against Part 6's.
-->

---

# `ERROR` finds it, and forbids it

> `HIDDEN`: doesn't exist, keep looking. `ERROR`: found it, and forbidden

<DrawnAnnotation text="sheet" :line="2" label="Shadows `WorkbookBuilder.sheet` on the closest receiver" color="var(--fundamentals-blue)" :connect="false" :geometry="{ label: { x: 0.52, y: 0.457 } }" />
<TypeHint :line="8" receiver="WorkbookBuilder">
<InlineCompilerError :line="10" text="sheet" message="'fun <T> SheetBuilder<*>.sheet(name: String, rows: Sequence<T>,\nblock: SheetBuilder<T>.() -> Unit): Nothing' is deprecated.\nA sheet cannot contain a sheet." style="--inline-compiler-error-message-size: 1.1rem">

```kotlin
import presentation.support.marked.SheetBuilder
import presentation.support.marked.workbook

@Deprecated("A sheet cannot contain a sheet", level = DeprecationLevel.ERROR)
fun <T> SheetBuilder<*>.sheet(
  name: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
): Nothing = error("unreachable")

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    sheet("Q2", q2) { }
  }
}
```

</InlineCompilerError>
</TypeHint>

<!--
Part 1 already stops this with `@DslMarker`, but the message is the compiler's. An
`ERROR`-level shadow on the closest receiver wins resolution and fails with a message in
the DSL's own words. Use it when only some outer calls should be blocked, not all of them.

Tested on 2.4.20: `@DslMarker` restricts context parameters too. A
`context(_: WorkbookBuilder) fun stamp()` called inside `sheet { }` fails with
"'context(_: WorkbookBuilder) fun stamp(): Unit' cannot be called in this context with an
implicit receiver."
-->

---

# A fallback names the missing scope

<DrawnAnnotation text="Column<*>.withVat" label="Less specific, so it only wins outside `formula { }`" :connect="false" :geometry="{ label: { x: 0.40, y: 0.575 } }" />
<DrawnAnnotation text="total.withVat()" :line="14" label="Both apply, `Column<Number>` is more specific" color="var(--fundamentals-blue)" :connect="false" :geometry="{ label: { x: 0.70, y: 0.76 } }" />
<TypeHint :line="10" receiver="SheetBuilder<Invoice>">
<TypeHint :line="14" text="formula {" receiver="Formulas">
<InlineCompilerError :line="15" text="withVat" message="'fun Column<*>.withVat(): Cell' is deprecated. withVat() only exists inside formula { }." style="--inline-compiler-error-message-size: 1.3rem">

```kotlin
import presentation.support.contexts.Formulas
import presentation.support.contexts.generateExcel
import presentation.support.contexts.times

context(formulas: Formulas)
fun Column<Number>.withVat(): Cell = formulas.multiply(this, 1.21)

@Deprecated(
  "withVat() only exists inside formula { }",
  level = DeprecationLevel.ERROR,
)
fun Column<*>.withVat(): Cell = error("unreachable")

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
  val gross by formula { total.withVat() }
  val outside = total.withVat()
}
```

</InlineCompilerError>
</TypeHint>
</TypeHint>

<!--
Without the fallback the error is the compiler's: "No context argument for 'formulas:
Formulas' found." Two overloads that differ only in `context(...)` are ambiguous, so the
fallback needs a lever: a less specific receiver (`Column<*>`) or parameter type. Inside
`formula { }` both apply and the more specific one wins; outside, only the fallback does.
-->

---

# A context parameter is a receiver on the JVM

<DrawnAnnotation text="fun Formulas.withVat(column" label="The Part 5 helper, before context parameters" :connect="false" :geometry="{ label: { x: 0.57, y: 0.243 } }" />
<DrawnAnnotation text="context(formulas: Formulas)" label="Compiles to the same first parameter" color="var(--fundamentals-blue)" :geometry="{ label: { x: 0.33, y: 0.55 }, connector: { type: 'quadratic', start: { x: 0.30, y: 0.355 }, control: { x: 0.32, y: 0.45 }, end: { x: 0.33, y: 0.525 } } }" />
<InlineCompilerError :line="5" text="withVat" message="Platform declaration clash:\nThe following declarations have the same JVM signature\n(withVat(Lpresentation/support/contexts/Formulas;\nLpresentation/support/Column;)Lpresentation/support/Cell;)" style="--inline-compiler-error-message-size: 1rem">

```kotlin
import presentation.support.contexts.Formulas

fun Formulas.withVat(column: Column<Number>): Cell =
  multiply(column, 1.21)

context(formulas: Formulas)
fun Column<Number>.withVat(): Cell =
  formulas.multiply(this, 1.21)
```

</InlineCompilerError>

---
magic-move
---

# `@JvmName` keeps the old binary

<DrawnAnnotation text="DeprecationLevel.HIDDEN" label="Old binaries keep linking to `withVat(Formulas, Column)`" :connect="false" :geometry="{ label: { x: 0.60, y: 0.29 } }" />
<DrawnAnnotation text="@JvmName(&quot;withVatColumn&quot;)" label="New source only sees this one, under another JVM name" color="var(--fundamentals-blue)" :connect="false" :geometry="{ label: { x: 0.61, y: 0.385 } }" />

```kotlin
import presentation.support.contexts.Formulas
import presentation.support.lists.formula
import presentation.support.lists.hours

@Deprecated("Use total.withVat()", level = DeprecationLevel.HIDDEN)
fun Formulas.withVat(column: Column<Number>): Cell =
  multiply(column, 1.21)

@JvmName("withVatColumn")
context(formulas: Formulas)
fun Column<Number>.withVat(): Cell =
  formulas.multiply(this, 1.21)

val gross by formula { hours.withVat() }
```

<!--
The same move as "Hide the old API", one step further: `HIDDEN` takes the receiver version
out of resolution, `@JvmName` gives the context version its own JVM name so both fit in
the class file. Migrating a receiver API to context parameters never breaks a compiled
caller. `javap` on 2.4.20: `withVat(Formulas, Column)` and `withVatColumn(Formulas, Column)`.
-->

---

# A `Cell` does not compose

<InlineCompilerError :line="4" text="*" occurrence="2" message="None of the following candidates is applicable:\n`context(formulas: Formulas) fun Column<Number>.times(other: Column<Number>): Cell`\n`context(formulas: Formulas) fun Column<Number>.times(factor: Double): Cell`" style="--inline-compiler-error-message-size: 1.1rem">

```kotlin
import presentation.support.contexts.times
import presentation.support.lists.generateExcel

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val gross by formula { hours * rate * 1.21 }
}
```

</InlineCompilerError>

<!--
`hours * rate` already rendered itself to "=B2*C2". A string cannot be multiplied again,
so every intermediate result needs its own column.
-->

---

# Operators build a tree

<DrawnAnnotation text="sealed interface Expr" label="The formula as data, not text"  :geometry="{ label: { x: 0.6083, y: 0.1995 }, connector: { type: 'quadratic', start: { x: 0.2919, y: 0.2187 }, control: { x: 0.3780, y: 0.2364 }, end: { x: 0.4634, y: 0.1993 } } }"/>
<DrawnAnnotation text="context(_: Formulas)" :line="7" label="`_`: nothing reads it, it only gates the call to `formula { }`" :connect="false" :geometry="{ label: { x: 0.585, y: 0.478 } }" />
<DrawnAnnotation text="Expr.Times(this, Expr.Constant(factor))" label="An `Expr` composes with the next operator" color="var(--fundamentals-pink)" />

```kotlin
import presentation.support.expressions.Formulas

sealed interface Expr {
  data class Ref(val column: Column<Number>) : Expr
  data class Constant(val value: Double) : Expr
  data class Times(val left: Expr, val right: Expr) : Expr
}

context(_: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Expr =
  Expr.Times(Expr.Ref(this), Expr.Ref(other))

context(_: Formulas)
operator fun Expr.times(factor: Double): Expr =
  Expr.Times(this, Expr.Constant(factor))
```

---

# Rendering is a `when` over the tree

<DrawnAnnotation text="when (this)" label="Exhaustive, the tree is sealed"  :geometry="{ label: { x: 0.7996, y: 0.2483 }, connector: { type: 'quadratic', start: { x: 0.5951, y: 0.2204 }, control: { x: 0.6192, y: 0.2450 }, end: { x: 0.6498, y: 0.2484 } } }"/>

```kotlin
import presentation.support.expressions.Expr

fun Expr.toExcel(row: Int): String = when (this) {
  is Expr.Ref -> column.reference(row)
  is Expr.Constant -> "$value"
  is Expr.Times -> "${left.toExcel(row)}*${right.toExcel(row)}"
  is Expr.Sum -> terms.joinToString(", ", "SUM(", ")") { it.toExcel(row) }
}
```

---

# Built once, rendered per row

<DrawnAnnotation text="formula { hours * rate * 1.21 }" label="Runs once, at the `by`"  :geometry="{ label: { x: 0.6406, y: 0.4430 }, connector: { type: 'quadratic', start: { x: 0.4716, y: 0.4248 }, control: { x: 0.4864, y: 0.4497 }, end: { x: 0.5289, y: 0.4466 } } }"/>

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" receiver="Formulas">

```kotlin
import presentation.support.expressions.generateExcel
import presentation.support.expressions.times

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val gross by formula { hours * rate * 1.21 }
}
```
```console
| customer | hours | rate | gross       |
| Ada      | 12    | 90   | =B2*C2*1.21 |
```

</TypeHint>
</TypeHint>

<!--
Before, the formula lambda ran again for every row with a new ExcelFormulas(row).
Now the tree is built once when the column is registered, and each row only renders it.
-->

---

# A sheet is a value

<DrawnAnnotation text="sheet<Invoice>" />
<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" receiver="Formulas">

```kotlin
import excel.dsl.sheet
import excel.dsl.times

val invoiceSheet = sheet<Invoice> {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val gross by formula { hours * rate * 1.21 }
}
```

</TypeHint>
</TypeHint>

<!--
Same columns and expression tree, now returned as a Sheet<Invoice>. There is no
file or row source at construction. Invoice must be explicit because the rows
no longer provide the type argument. This uses the finished excel.dsl library,
which snapshots the builder and keeps the expression trees from the previous slides.
-->

---

# One schema writes many reports

<DrawnAnnotation text="invoiceSheet.write" :line="1" />

```kotlin
import excel.dsl.write
import kotlin.io.path.Path
import presentation.support.finished.invoiceSheet

invoiceSheet.write(Path("q1.xlsx"), q1)
invoiceSheet.write(Path("q2.xlsx"), q2)
```

<!--
The schema is reused; each write pulls its own rows. The writer also accepts an
OutputStream for an HTTP response or an in-memory destination. Reusing the schema
does not make a one-shot row source repeatable. No file is opened until write.
-->

---

# The schema is testable without a file

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" receiver="Formulas">

```kotlin
import excel.dsl.sheet
import excel.dsl.times

val invoiceSheet = sheet<Invoice> {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val gross by formula { hours * rate * 1.21 }
}

println(invoiceSheet.headers)
println(invoiceSheet.evaluate(invoices.first())["Gross"])
```
```console
[Customer, Hours, Rate, Gross]
1306.8
```

</TypeHint>
</TypeHint>

<!--
The same expression tree has two interpreters: Excel text and values. We have
come back to Part 4's payoff, now behind a reusable schema API. The finished
library uses the capitalized header policy shown in Part 3. Neither headers
nor evaluate creates a workbook or touches a file.
-->

---
layout: default
class: recap-slide
---

# Recap

- `@DslMarker`: constrain receivers and contexts
- Typed overloads: choose by the lambda's return type
- Delegates: register at `by`, catch discarded results
- Member extensions and contexts: scope the operators
- Sheets as values: build once, render and test

> **A DSL is a set of scopes. Decide what each one can see.**
