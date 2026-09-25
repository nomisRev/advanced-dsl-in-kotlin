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

<DrawnAnnotation text="formula" on="0" />
<DrawnAnnotation text="*" on="0" label="Sub DSL `FormulaScope`"  :geometry="{ label: { x: 0.6171, y: 0.4457 }, connector: { type: 'quadratic', start: { x: 0.4056, y: 0.4244 }, control: { x: 0.4468, y: 0.4585 }, end: { x: 0.4951, y: 0.4495 } } }"/>

```kotlin
import presentation.support.typed.generateExcel

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
}
```
```console
| Customer | Hours | Rate | Total |
| Ada      | 12    | 90   | =B2*C2 |
```

---

# `*` only exists inside `formula { }`


<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" text="formula {" receiver="Formulas">
<InlineCompilerError :line="8" text="*" message="Unresolved reference `times` for operator `*` on receiver of type `Column<Number>`" style="--inline-compiler-error-message-size: 1.4rem">

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

# `*` only exists inside `formula { }`


<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" text="formula {" receiver="Formulas">
<InlineCompilerError :line="8" text="*" message="Unresolved reference `times` for operator `*` on receiver of type `Column<Number>`" style="--inline-compiler-error-message-size: 1.4rem">

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

interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
}
```

</InlineCompilerError>
</TypeHint>
</TypeHint>

---

# An extension declared inside an interface

```kotlin no-compile
interface Formulas {
  operator fun Column<Number>.times(other: Column<Number>): Cell
}

@ExcelDsl
class SheetBuilder<T> {
  fun formula(block: Formulas.() -> Cell):
    PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Column<Number>>>
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


```kotlin
import presentation.support.delegated.generateExcel

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { rate * hours }
}
```
