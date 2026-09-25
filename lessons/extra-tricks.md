---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Extra</div>

# DSL tricks

## What the spreadsheet did not need yet

---

# `>` cannot build a tree

<InlineCompilerError :line="2" text="operator" message="'operator' modifier is not applicable to function: must return 'Int'." style="--inline-compiler-error-message-size: 1.2rem">

```kotlin
import presentation.support.tricks.Expr
import presentation.support.tricks.Formulas

context(_: Formulas)
operator fun Column<Number>.compareTo(limit: Double): Expr =
  Expr.Greater(Expr.Ref(this), Expr.Constant(limit))
```

</InlineCompilerError>

<!--
`a > b` is compiled to `a.compareTo(b) > 0`, so `compareTo` has to return `Int`.
The same goes for `equals`, which must return `Boolean`. Comparisons cannot be data.
-->

---
magic-move
---

# `infix` builds it instead

<DrawnAnnotation text="infix" label="Any one-argument member or extension" :geometry="{ label: { x: 0.22, y: 0.40 } }" />
<DrawnAnnotation text="gt" label="A name instead of a symbol" color="var(--fundamentals-blue)" :geometry="{ label: { x: 0.68, y: 0.40 } }" />

```kotlin
import presentation.support.tricks.Expr
import presentation.support.tricks.Formulas

context(_: Formulas)
infix fun Column<Number>.gt(limit: Double): Expr =
  Expr.Greater(Expr.Ref(this), Expr.Constant(limit))
```

---

# `infix` reads like the formula

<DrawnAnnotation text="total gt 1000.0" label="`total.gt(1000.0)`, without the dot and the parentheses" :geometry="{ label: { x: 0.40, y: 0.68 } }" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" text="formula {" receiver="Formulas">

```kotlin
import presentation.support.tricks.*

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
  val large by formula { total gt 1000.0 }
}
```
```console
| hours | rate | total  | large       |
| 12    | 90   | =A2*B2 | =C2>1000.0  |
```

</TypeHint>
</TypeHint>

---

# `invoke` turns a `String` into a column

<DrawnAnnotation text="operator fun String.invoke" label="A member extension, only inside `SheetBuilder`" :geometry="{ label: { x: 0.40, y: 0.71 } }" />
<DrawnAnnotation text="@OverloadResolutionByLambdaReturnType" label="Part 2 still picks the cell type" color="var(--fundamentals-blue)" :geometry="{ label: { x: 0.70, y: 0.29 } }" />

```kotlin no-compile
@ExcelDsl
class SheetBuilder<T> {
  @OverloadResolutionByLambdaReturnType
  @JvmName("textColumn")
  operator fun String.invoke(value: (T) -> String)

  @OverloadResolutionByLambdaReturnType
  @JvmName("numberColumn")
  operator fun String.invoke(value: (T) -> Number)
}
```

---

# `invoke` turns a `String` into a column

<DrawnAnnotation text="&quot;Customer&quot; {" label="`&quot;Customer&quot;.invoke { }`" :geometry="{ label: { x: 0.52, y: 0.24 } }" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.tricks.generateExcel

generateExcel("invoices.xlsx", invoices) {
  "Customer" { it.customer }
  "Hours" { it.hours }
  "Issued" { it.issuedOn }
}
```

</TypeHint>

---

# A lambda may run twice

<InlineCompilerError :line="4" text="customers" message="Captured values cannot be initialized because of possible reassignments.">

```kotlin
import presentation.support.delegated.generateExcel

fun exportCustomers(): Column<String> {
  val customers: Column<String>
  generateExcel("invoices.xlsx", invoices) {
    val customer by column { it.customer }
    customers = customer
  }
  return customers
}
```

</InlineCompilerError>

---

# A contract promises exactly once

<DrawnAnnotation text="callsInPlace(block, InvocationKind.EXACTLY_ONCE)" label="Not checked, the compiler takes our word for it" color="red" :geometry="{ label: { x: 0.45, y: 0.67 } }" />
<DrawnAnnotation text="inline" label="The block cannot escape" color="var(--fundamentals-blue)" :geometry="{ label: { x: 0.56, y: 0.24 } }" />

```kotlin
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import presentation.support.tricks.SheetBuilder

@OptIn(ExperimentalContracts::class)
inline fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
  writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
}
```

---

# A contract promises exactly once

<DrawnAnnotation text="customers = customer" label="Definitely assigned, once" :geometry="{ label: { x: 0.55, y: 0.38 } }" />

```kotlin
import presentation.support.tricks.generateExcel

fun exportCustomers(): Column<String> {
  val customers: Column<String>
  generateExcel("invoices.xlsx", invoices) {
    val customer by column { it.customer }
    customers = customer
  }
  return customers
}
```

---

# The expected type names the subtype

> `-Xcontext-sensitive-resolution`

<DrawnAnnotation text="is Ref" label="`Expr.Ref`, found through the sealed type of `this`" :geometry="{ label: { x: 0.66, y: 0.35 } }" />
<DrawnAnnotation text="is Greater" />

```kotlin
import presentation.support.tricks.Expr

fun Expr.toExcel(row: Int): String = when (this) {
  is Ref -> column.reference(row)
  is Constant -> "$value"
  is Times -> "${left.toExcel(row)}*${right.toExcel(row)}"
  is Greater -> "${left.toExcel(row)}>${right.toExcel(row)}"
}
```

<!--
Works for enum entries too: `align(CENTER)` where a `HorizontalAlignment` is expected,
no `HorizontalAlignment.` prefix and no import.
-->

---

# The rest of the toolbox

| Trick | In a DSL | Why |
| --- | --- | --- |
| `String.unaryPlus()` | `+"Invoices 2026"` | Text as a statement, like kotlinx.html |
| `operator fun get` | `row[hours]` | Read a typed cell by its `Column` |
| `operator fun rangeTo` | `sum(hours..overtime)` | `=SUM(B2:C2)` from a Kotlin range |
| Extension property | `21.percent` | Units without a constructor |
| `@JvmInline value class` | `Row(2)` | A typed `Int`, no allocation |
| `typealias` | `Columns<T>` | Name the block type once |
| Builder inference | `buildList { add(hours) }` | Type argument from the calls |
| `@RestrictsSuspension` | `sequence { yield(row) }` | Only the scope's `suspend` calls |

<style>
table { font-size: 1.45rem; }
td, th { padding-top: 0.45rem; padding-bottom: 0.45rem; }
</style>
