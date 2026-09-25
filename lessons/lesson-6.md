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

<DrawnAnnotation text="vararg" label="`columns` is an `Array<Column<Number>>`" />
<DrawnAnnotation text="sum(hours, overtime)" label="Allocates a new array on every call" color="var(--fundamentals-pink)" />

```kotlin
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

<DrawnAnnotation text="columns: List<Column<Number>>" label="No array, no copy"  :connect="false" :geometry="{ label: { x: 0.4490, y: 0.3208 } }"/>

```kotlin
interface Formulas {
  fun sum(columns: List<Column<Number>>): Cell
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
interface Formulas {
  fun sum(columns: List<Column<Number>>): Cell
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

```kotlin
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

```kotlin
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
