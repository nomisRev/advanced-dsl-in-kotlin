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

<DrawnAnnotation text="sealed interface Expr" label="The formula as data, not text" />
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

<DrawnAnnotation text="when (this)" label="Exhaustive, the tree is sealed" />
<DrawnAnnotation text="fun Expr.toExcel" label="`evaluate` is just another `when`" color="var(--fundamentals-pink)" />

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

<DrawnAnnotation text="formula { hours * rate * 1.21 }" label="Runs once, at the `by`" />

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
layout: intro
class: section-slide
---

# Recap

- `@DslMarker`: scopes don't leak, also on types you don't own
- `@OverloadResolutionByLambdaReturnType`: the lambda picks the overload
- `PropertyDelegateProvider`: name and register at the `by`
- Member extensions: dynamic dispatch, only where the receiver is in scope
- `context(...)`: let users extend the DSL

> **A DSL is a set of scopes. Decide what each one can see.**
