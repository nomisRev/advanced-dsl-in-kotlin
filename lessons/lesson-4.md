---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 4</div>

# Extensions with dynamic dispatch

## Not every extension is static

---

# The total is a number, not a formula

<DrawnAnnotation text="it.hours * it.rate" label="Computed in Kotlin, the sheet never recalculates" color="red"  :geometry="{ label: { x: 0.7015, y: 0.4439 }, connector: { type: 'quadratic', start: { x: 0.3755, y: 0.4214 }, control: { x: 0.4046, y: 0.4528 }, end: { x: 0.4576, y: 0.4443 } } }"/>

```kotlin
import presentation.support.typed.generateExcel
import presentation.support.typed.column

generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  column("Hours") { it.hours }
  column("Rate") { it.rate }
  column("Total") { it.hours * it.rate }
}
```
```console
| Customer | Hours | Rate | Total |
| Ada      | 12    | 90   | 1080  |
```

---
magic-move
---

# The total is a number, not a formula

<DrawnAnnotation text="hours * rate" label="`hours` & `rate` are `Column<Number>`, not an `Int`" on="0" :geometry="{ label: { x: 0.7082, y: 0.4433 }, connector: { type: 'quadratic', start: { x: 0.3950, y: 0.4240 }, control: { x: 0.4231, y: 0.4535 }, end: { x: 0.4793, y: 0.4467 } } }"/>

```kotlin no-compile
import presentation.support.referenced.generateExcel

generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  val hours = column("Hours") { it.hours }
  val rate = column("Rate") { it.rate }
  column("Total") { hours * rate }
}
```
```console
| Customer | Hours | Rate | Total |
| Ada      | 12    | 90   | 1080  |
```

---
magic-move
---

# The total is a number, not a formula

<DrawnAnnotation text="formula" on="0" />
<DrawnAnnotation text="*" on="0" label="Sub DSL `FormulaScope`"  :geometry="{ label: { x: 0.7029, y: 0.4449 }, connector: { type: 'quadratic', start: { x: 0.4963, y: 0.4232 }, control: { x: 0.5375, y: 0.4573 }, end: { x: 0.5858, y: 0.4483 } } }"/>
<DrawnAnnotation text="=B2*C2" />

<DrawnAnnotation text="hours" on="1" />
<DrawnAnnotation text="&quot;Hours&quot;" on="1" />
<DrawnAnnotation text="rate" on="1" occurrence="2" />
<DrawnAnnotation text="&quot;Rate&quot;" />
<DrawnAnnotation text="total" on="1" />
<DrawnAnnotation text="&quot;Total&quot;" label="Redundant redefinition of column name" on="1" :geometry="{ label: { x: 0.6013, y: 0.4441 }, connector: { type: 'quadratic', start: { x: 0.3437, y: 0.4195 }, control: { x: 0.3569, y: 0.4647 }, end: { x: 0.4002, y: 0.4468 } } }"/>

```kotlin
import presentation.support.referenced.generateExcel

generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  val hours = column("Hours") { it.hours }
  val rate = column("Rate") { it.rate }
  val total = formula("Total") { hours * rate }
}
```
```console
| Customer | Hours | Rate | Total |
| Ada      | 12    | 90   | =B2*C2 |
```

---
magic-move
---

# The total is a number, not a formula

<DrawnAnnotation text="formula" on="0" />
<DrawnAnnotation text="*" on="0" label="Sub DSL `FormulaScope`"  :geometry="{ label: { x: 0.6171, y: 0.4457 }, connector: { type: 'quadratic', start: { x: 0.4056, y: 0.4244 }, control: { x: 0.4468, y: 0.4585 }, end: { x: 0.4951, y: 0.4495 } } }"/>

```kotlin
import presentation.support.delegated.generateExcel

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
}
```
```console
| customer | hours | rate | total  |
| Ada      | 12    | 90   | =B2*C2 |
```

---

# `*` only exists inside `formula { }`


<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" text="formula {" receiver="Formulas">
<InlineCompilerError :line="8" text="*" message="Unresolved reference `times` for operator `*` on receiver `Column<Number>`" style="--inline-compiler-error-message-size: 1.53rem">

```kotlin
import presentation.support.delegated.generateExcel

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula {
    hours * rate
  }

  val cost = hours * rate
}
```

</InlineCompilerError>
</TypeHint>
</TypeHint>

---

# An extension declared inside an interface

```kotlin no-compile
typealias FormulaBuilder =
  PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Column<Number>>>

//Example
interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
}

@ExcelDsl
class SheetBuilder<T> {
  fun formula(block: Formulas.() -> Cell): FormulaDelegate
}
```

---

# One receiver, two strategies

<DrawnAnnotation text="$letter" label="`this: Column<Number>`"  :geometry="{ label: { x: 0.2630, y: 0.6459 } }"/>
<DrawnAnnotation text="$row" label="`this@ExcelFormulas`" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.4871, y: 0.6430 }, connector: { type: 'quadratic', start: { x: 0.3759, y: 0.5579 }, control: { x: 0.3706, y: 0.5939 }, end: { x: 0.3932, y: 0.6222 } } }"/>

```kotlin
interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
}

// Example
sealed interface Cell {
  data class Formula(val text: String) : Cell
  data class Value(val number: Double) : Cell
}

class ExcelFormulas(private val row: Int) : Formulas {
  override fun Column<Number>.times(other: Column<Number>): Cell =
    Cell.Formula("=$letter$row*${other.letter}$row")
}
```

---
magic-move
---

# Same call, another implementation

<DrawnAnnotation text="class Evaluate" label="Computes the value: CSV export, previews, tests"  :connect="false" :geometry="{ label: { x: 0.4197, y: 0.3771 } }"/>

```kotlin
interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
}

// Example
sealed interface Cell {
  data class Formula(val text: String) : Cell
  data class Value(val number: Double) : Cell
}

class Evaluate(private val row: Map<Column<*>, Number>) : Formulas {
  override fun Column<Number>.times(other: Column<Number>): Cell =
    Cell.Value(
      row.getValue(this).toDouble() * row.getValue(other).toDouble(),
    )
}
```

---

# Only top-level extensions are static

<DrawnAnnotation text="fun Column<*>.reference" label="Top-level: a `static` method"  :connect="false" :geometry="{ label: { x: 0.1899, y: 0.2488 } }"/>
<DrawnAnnotation text="public static String reference" />
<DrawnAnnotation text="operator fun Column<Number>.times" label="Member: a virtual call on `Formulas`" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.5223, y: 0.2786 } }" :connect="false"/>
<DrawnAnnotation text="Cell times" color="var(--fundamentals-pink)" />

```kotlin
fun Column<*>.reference(row: Int): String = "$letter$row"

interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
}
```

```java no-compile
public final class FormulasKt {
  public static String reference(Column<?> $this$reference, int row);
}

public interface Formulas {
  Cell times(Column<Number> $this$times, Column<Number> other);
}
```

<!--
The extension receiver is just the first parameter in both cases.
What decides static vs virtual is where the function is declared, not that it is an extension.
Real world: KtMongo's filter DSL declares its operators this way.
-->

---

# The receiver in scope picks the implementation

<InlineValue :line="3" text="ExcelFormulas(row = 2).total()" value="Formula(text==B2*C2)">
<InlineValue :line="4" text="Evaluate(mapOf(hours to 12, rate to 90)).total()" value="Value(number=1080.0)">

```kotlin
import presentation.support.delegated.*

val total: Formulas.() -> Cell = { hours * rate }

ExcelFormulas(row = 2).total()
Evaluate(mapOf(hours to 12, rate to 90)).total()
```

</InlineValue>
</InlineValue>

---

# The receiver type still checks

<InlineCompilerError :line="5" text="*" message="None of the following candidates is applicable:\n`fun Column<Number>.times(other: Column<Number>): Cell` is inapplicable because of a receiver type mismatch.\n`fun Column<Number>.times(factor: Double): Cell` is inapplicable because of a receiver type mismatch." style="--inline-compiler-error-message-size: 1.1rem">

```kotlin
import presentation.support.delegated.generateExcel

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { customer * hours }
}
```

</InlineCompilerError>

<!--
`customer` is a `Column<String>`. Dynamic dispatch picks the implementation at runtime,
but the receiver type is still checked at compile time: `times` is only declared on
`Column<Number>`.
-->

---

#  Explain double receiver problem!

