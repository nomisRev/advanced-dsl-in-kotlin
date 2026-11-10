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

<DrawnAnnotation text="fun Formulas.withVat" on="0" label="`Formulas` has to be the receiver"  :geometry="{ label: { x: 0.4343, y: 0.3709 }, connector: { type: 'quadratic', start: { x: 0.2766, y: 0.4535 }, control: { x: 0.2739, y: 0.4083 }, end: { x: 0.2817, y: 0.3850 } } }"/>
<DrawnAnnotation text="withVat(total)" on="1" label="Reads backwards; goal: `total.withVat()`" color="red" />

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

```kotlin
import presentation.support.delegated.Formulas
import presentation.support.delegated.formula
import presentation.support.delegated.total

interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
  operator fun Column<Number>.times(factor: Double): Cell
}

context(formulas: Formulas)
// [!error * Unresolved reference 'times' for operator '*' on receiver of type 'Column<Number>'.]
fun Column<Number>.withVat(): Cell = this * 1.21

val gross by formula { total.withVat() }
```

---
magic-move
---

# A context parameter is not a receiver

<DrawnAnnotation text="with(formulas)" label="Turn it into a receiver by hand, in every helper" color="red"  :geometry="{ label: { x: 0.6634, y: 0.5578 } }"/>

```kotlin
import presentation.support.delegated.Formulas
import presentation.support.delegated.formula
import presentation.support.delegated.total

interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
  operator fun Column<Number>.times(factor: Double): Cell
}

context(formulas: Formulas)
fun Column<Number>.withVat(): Cell = with(formulas) { // [!this Formulas]
  this@withVat * 1.21
}

val gross by formula { total.withVat() }
```

---

# Declare the operators with a context

<DrawnAnnotation text="fun multiply" label="The interface keeps plain functions" on="0" :geometry="{ label: { x: 0.2626, y: 0.3667 }, connector: { type: 'quadratic', start: { x: 0.1111, y: 0.2750 }, control: { x: 0.1135, y: 0.3090 }, end: { x: 0.1111, y: 0.3439 } } }"/>
<DrawnAnnotation text="formulas.multiply(this, factor)" label="Still dynamic dispatch, through the context" on="1" :geometry="{ label: { x: 0.6711, y: 0.5520 }, connector: { type: 'quadratic', start: { x: 0.4266, y: 0.5487 }, control: { x: 0.4423, y: 0.5557 }, end: { x: 0.4579, y: 0.5487 } } }"/>
<DrawnAnnotation text="*" label="Resolves, `formulas` is in context" on="2" />

```kotlin{1-4|6-8|10-11}
interface Formulas {
  fun multiply(column: Column<Number>, other: Column<Number>): Cell
  fun multiply(column: Column<Number>, factor: Double): Cell
}

context(formulas: Formulas)
operator fun Column<Number>.times(factor: Double): Cell =
  formulas.multiply(this, factor)

context(_: Formulas)
fun Column<Number>.withVat(): Cell = this * 1.21
```

---

# A receiver satisfies a context

<DrawnAnnotation text="*" />
<DrawnAnnotation text="withVat()" label="`this: Formulas` satisfies `context(formulas: Formulas)`"  :geometry="{ label: { x: 0.7478, y: 0.5376, width: 0.3037 }, connector: { type: 'quadratic', start: { x: 0.5954, y: 0.4182 }, control: { x: 0.6334, y: 0.4493 }, end: { x: 0.6429, y: 0.4925 } } }"/>

```kotlin
import presentation.support.contexts.*

generateExcel("invoices.xlsx", invoices) { // [!this SheetBuilder<Invoice>]
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate } // [!this text=`formula {` Formulas]
  val gross by formula { total.withVat() } // [!this text=`formula {` Formulas]
}
```
```console
| hours | rate | total  | gross       |
| 12    | 90   | =A2*B2 | =C2*1.21    |
```

---
magic-move
---

# A context lambda brings only the context

```kotlin
import presentation.support.contexts.generateExcel
import presentation.support.contexts.column
import presentation.support.contexts.times
import presentation.support.contexts.withVat

generateExcel("invoices.xlsx", invoices) { // [!this SheetBuilder<Invoice>]
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate } // [!context text=`formula {` Formulas]
  val gross by formula { total.withVat() } // [!context text=`formula {` Formulas]
}
```

---
magic-move
---

# A context lambda brings only the context

```kotlin
import presentation.support.contexts.generateExcel
import presentation.support.contexts.column
import presentation.support.contexts.times
import presentation.support.contexts.withVat
import presentation.support.contexts.Formulas

generateExcel("invoices.xlsx", invoices) { // [!this SheetBuilder<Invoice>]
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { // [!context text=`formula {` Formulas]
    multiply(hours, rate) // [!error multiply Unresolved reference `multiply`]
  }
  val gross by formula { total.withVat() } // [!context text=`formula {` Formulas]
}
```

---
magic-move
---

# A context lambda brings only the context

<DrawnAnnotation text="contextOf" label="Explicitly calling is always allowed"  :geometry="{ label: { x: 0.3438, y: 0.6038 } }"/>

```kotlin
import presentation.support.contexts.generateExcel
import presentation.support.contexts.column
import presentation.support.contexts.withVat
import presentation.support.contexts.contextOf
import presentation.support.contexts.Formulas

generateExcel("invoices.xlsx", invoices) { // [!this SheetBuilder<Invoice>]
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { // [!context text=`formula {` Formulas]
    contextOf<Formulas>().multiply(hours, rate)
  }
  val gross by formula { total.withVat() } // [!context text=`formula {` Formulas]
}
```

---

# Users want their own column types too

<DrawnAnnotation text="context(builder: SheetBuilder<T>)" label="Requires the sheet builder in context" />
<DrawnAnnotation text="fun <T> column" label="Same function name as the library" color="var(--fundamentals-blue)" />

```kotlin
@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference
import presentation.support.CustomType
import presentation.support.ColumnDelegate
import presentation.support.contexts.SheetBuilder
import presentation.support.contexts.column

@OverloadResolutionByLambdaReturnType
@JvmName("customColumn")
context(builder: SheetBuilder<T>)
fun <T> column(value: (T) -> CustomType): ColumnDelegate<String> =
  column { value(it).toColumnText() }
```

<!--
In Part 2, adding a custom type overload forced us to give it another name (`cellColumn`)
because member overloads take priority and extension receivers clash.
With context parameters, `column` is defined as a context function rather than a member.
Downstream code can declare a `column` overload for custom types without collision.
-->

---

# Context overloads resolve without renaming

<DrawnAnnotation text="{ it.custom }" label="`CustomType`, picks the custom overload" :geometry="{ label: { x: 0.63, y: 0.44 } }" />

```kotlin
import presentation.support.contexts.*
import presentation.support.custom

generateExcel("invoices.xlsx", invoices) { // [!this SheetBuilder<Invoice>]
  val customer by column { it.customer }
  val hours by column { it.hours }
  val custom by column { it.custom }
}
```
```console
| customer | hours | custom          |
| Ada      | 12    | ADA-2026-09-01  |
```

<!--
Both library and downstream overloads exist in the same candidate space.
The compiler uses `@OverloadResolutionByLambdaReturnType` to inspect the return
type of `{ it.custom }`, finds `CustomType`, and selects our custom overload—no
renaming or import aliases required.
-->

---

# `@DslMarker` closes contexts too (Example..)

<DrawnAnnotation text="context(builder: WorkbookBuilder)" label="Satisfied by the marked `workbook { }` receiver"  :geometry="{ label: { x: 0.5439, y: 0.2599 }, connector: { type: 'quadratic', start: { x: 0.2883, y: 0.2271 }, control: { x: 0.2950, y: 0.2514 }, end: { x: 0.3114, y: 0.2586 } } }"/>
<DrawnAnnotation text="stamp()" :line="5" label="The closest marked scope is the workbook" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.3993, y: 0.3850 }, connector: { type: 'quadratic', start: { x: 0.1583, y: 0.3878 }, control: { x: 0.1732, y: 0.3942 }, end: { x: 0.1880, y: 0.3872 } } }"/>

```kotlin
import presentation.support.marked.WorkbookBuilder
import presentation.support.marked.workbook

context(builder: WorkbookBuilder)
fun stamp() {}

workbook("invoices.xlsx") { // [!this WorkbookBuilder]
  stamp()
  sheet("Q1", q1) { // [!this SheetBuilder<Invoice>]
    
    // [!error stamp 'context(_: WorkbookBuilder) fun stamp(): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.]
    stamp()
  }
}
```

---

# `@DslMarker` closes contexts too (Example..)

> Kotlin 2.4.0: `-Xexplicit-context-arguments`

<DrawnAnnotation text="builder = this@workbook" label="Explicitly specifying is always allowed"/>

```kotlin
import presentation.support.marked.WorkbookBuilder
import presentation.support.marked.workbook

context(builder: WorkbookBuilder)
fun stamp() {}

workbook("invoices.xlsx") { // [!this WorkbookBuilder]
  stamp()
  sheet("Q1", q1) { // [!this SheetBuilder<Invoice>]
    stamp(builder = this@workbook)
  }
}
```

---
class: versus-slide
---

# Context parameter or receiver?

| | Extension receiver | Context parameter |
| --- | --- | --- |
| How many | one | any number |
| Inside the body | `this`, implicit member calls | by name only |
| At the call site | `total.withVat()` | resolved from scope |
| Reads as | the subject of the sentence | the ambient environment |

> Is the code about it? Make it a <span class="versus-receiver">receiver</span>.
> 
> Does it only need it? Make it a <span class="versus-context">context</span>.
