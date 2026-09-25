---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 5</div>

# Context parameters

## Let users extend the DSL

---

# Users want their own formulas

<DrawnAnnotation text="fun Formulas.withVat" label="`Formulas` has to be the receiver" />
<DrawnAnnotation text="withVat(total)" label="Reads backwards, we want `total.withVat()`" color="red" />

```kotlin
import presentation.support.delegated.Formulas
import presentation.support.delegated.formula
import presentation.support.delegated.total

interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
  operator fun Column<Number>.times(factor: Double): Cell
}

fun Formulas.withVat(column: Column<Number>): Cell = column * 1.21

val gross by formula { withVat(total) }
```

---
magic-move
---

# A context parameter is not a receiver

> Stable since Kotlin 2.2.0

<InlineCompilerError :line="7" text="*" message="Unresolved reference 'times' for operator '*' on receiver of type 'Column<Number>'.">

```kotlin
import presentation.support.delegated.Formulas
import presentation.support.delegated.formula
import presentation.support.delegated.total

interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
  operator fun Column<Number>.times(factor: Double): Cell
}

context(formulas: Formulas)
fun Column<Number>.withVat(): Cell = this * 1.21

val gross by formula { total.withVat() }
```

</InlineCompilerError>

---
magic-move
---

# A context parameter is not a receiver

<DrawnAnnotation text="with(formulas)" label="Turn it into a receiver by hand, in every helper" color="red" />

```kotlin
import presentation.support.delegated.Formulas
import presentation.support.delegated.formula
import presentation.support.delegated.total

interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
  operator fun Column<Number>.times(factor: Double): Cell
}

context(formulas: Formulas)
fun Column<Number>.withVat(): Cell = with(formulas) { this@withVat * 1.21 }

val gross by formula { total.withVat() }
```

---

# Declare the operators with a context

<DrawnAnnotation text="fun multiply" label="The interface keeps plain functions" />
<DrawnAnnotation text="formulas.multiply(this, factor)" label="Still dynamic dispatch, through the context" color="var(--fundamentals-pink)" />
<DrawnAnnotation text="this * 1.21" label="Resolves, `formulas` is in context" />

```kotlin
interface Formulas {
  fun multiply(column: Column<Number>, other: Column<Number>): Cell
  fun multiply(column: Column<Number>, factor: Double): Cell
}

context(formulas: Formulas)
operator fun Column<Number>.times(factor: Double): Cell =
  formulas.multiply(this, factor)

context(formulas: Formulas)
fun Column<Number>.withVat(): Cell = this * 1.21
```

---

# A receiver satisfies a context

<DrawnAnnotation text="total.withVat()" label="`this: Formulas` fills `context(formulas: Formulas)`" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" receiver="Formulas">

```kotlin
import presentation.support.contexts.*

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
  val gross by formula { total.withVat() }
}
```
```console
| hours | rate | total  | gross       |
| 12    | 90   | =A2*B2 | =C2*1.21    |
```

</TypeHint>
</TypeHint>

---

# Context parameter or receiver?

| | Extension receiver | Context parameter |
| --- | --- | --- |
| How many | one | any number |
| Inside the body | `this`, implicit member calls | by name only |
| At the call site | `total.withVat()` | resolved from scope |
| Reads as | the subject of the sentence | the ambient environment |

> **The receiver is what the sentence is about, the context is where it is said.**
>
> `serranofp.com/blog/context-params.html`
