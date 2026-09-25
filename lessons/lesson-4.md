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

# `*` only exists inside `formula { }`

<InlineCompilerError :line="6" text="*" message="Unresolved reference 'times' for operator '*' on receiver of type 'Column<Number>'.">

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" receiver="Formulas">

```kotlin
generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }

  val cost = hours * rate
}
```

</TypeHint>
</TypeHint>
</InlineCompilerError>

---

# An extension declared inside an interface

<DrawnAnnotation text="interface Formulas" label="The _dispatch receiver_" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.4074, y: 0.1840 }, connector: { type: 'quadratic', start: { x: 0.2584, y: 0.2225 }, control: { x: 0.2868, y: 0.2310 }, end: { x: 0.3068, y: 0.1968 } } }"/>
<DrawnAnnotation text="Column<Number>.times" label="The _extension receiver_"  :geometry="{ label: { x: 0.4762, y: 0.3317 } }"/>
<DrawnAnnotation text="Formulas.() -> Cell" label="The builder brings `Formulas` into scope"  :geometry="{ label: { x: 0.7482, y: 0.4564 }, connector: { type: 'quadratic', start: { x: 0.5047, y: 0.5009 }, control: { x: 0.5472, y: 0.5051 }, end: { x: 0.5642, y: 0.4753 } } }"/>

```kotlin
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

# Two receivers, two `this`

<DrawnAnnotation text="$letter" label="`this: Column<Number>`"  :geometry="{ label: { x: 0.2630, y: 0.6459 } }"/>
<DrawnAnnotation text="$row" label="`this@ExcelFormulas`" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.4871, y: 0.6430 }, connector: { type: 'quadratic', start: { x: 0.3759, y: 0.5579 }, control: { x: 0.3706, y: 0.5939 }, end: { x: 0.3932, y: 0.6222 } } }"/>

```kotlin
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
val total: Formulas.() -> Cell = { hours * rate }

ExcelFormulas(row = 2).total()
Evaluate(mapOf(hours to 12, rate to 90)).total()
```

</InlineValue>
</InlineValue>

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

```java
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

# The receiver type still checks

<InlineCompilerError :line="5" text="*" message="Candidate 'fun Column<Number>.times(other: Column<Number>): Cell' is inapplicable because of a receiver type mismatch.">

```kotlin
generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { customer * hours }
}
```

</InlineCompilerError>
