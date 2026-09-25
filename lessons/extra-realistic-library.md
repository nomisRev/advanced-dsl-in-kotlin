---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Extra</div>

# A realistic library

## What users ask for after the demo

<!--
For Simon. Every slide here runs against the finished library in `excel-dsl/`
(package `excel.dsl`, tests in `excel-dsl/src/test`). The lede on a slide names
the slide of the main deck it would follow if it moved into a lesson; slides
without a lede have no natural home there.
-->

---

# Every type Excel stores has an overload

> Part 2, after “Every column has a cell type”

<DrawnAnnotation text="{ it.amount }" label="`BigDecimal` is a `Number`" />
<DrawnAnnotation text="{ it.paid }" label="`Boolean`: `TRUE` or `FALSE`, not text" />
<DrawnAnnotation text="{ it.sentAt }" label="`Instant`, shown in `ExcelConfig.zone`" color="var(--fundamentals-blue)" />
<DrawnAnnotation text="{ it.status }" label="An `Enum` writes its `name`" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val amount by column { it.amount }
  val paid by column { it.paid }
  val issuedOn by column { it.issuedOn }
  val sentAt by column { it.sentAt }
  val status by column { it.status }
}
```
```console
| Customer | Amount | Paid  | IssuedOn   | SentAt              | Status |
| Ada      | 1080   | FALSE | 2026-09-01 | 2026-09-01 09:30:00 | Open   |
```

</TypeHint>

<!--
Seven overloads now, one per return type: String, Number, Enum, Boolean, LocalDate,
LocalDateTime, Instant, each with its own @JvmName. The lambdas may return null, which
leaves the cell empty. Dates and date-times get a format by default, so Excel shows
2026-09-01 instead of 46266.
-->

---

# `ExcelConfig` also picks the time zone

> Part 3, after “Columns become values we can reference”

<DrawnAnnotation text="ZoneId.of(&quot;Europe/Brussels&quot;)" label="Excel dates have no zone, so the schema picks one" />
<DrawnAnnotation text="11:30:00" label="09:30 UTC, in summer time" color="var(--fundamentals-blue)" />

```kotlin
import excel.dsl.*
import java.time.ZoneId

generateExcel(
  "invoices.xlsx",
  invoices,
  ExcelConfig(zone = ZoneId.of("Europe/Brussels")),
) {
  val customer by column { it.customer }
  val sentAt by column { it.sentAt }
}
```
```console
| Customer | SentAt              |
| Ada      | 2026-09-01 11:30:00 |
```

<!--
The zone defaults to UTC. `columnNameStyle` moved to the last parameter, so
`ExcelConfig { it.uppercase() }` still reads as in Part 3.
-->

---

# A member overload beats a user's extension

> Part 2, after “Users add a cell type with an extension”

<InlineCompilerError :line="8" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(format: CellFormat?, width: Int?, value: (Invoice) -> String?)`\n`fun column(format: CellFormat?, width: Int?, value: (Invoice) -> Number?)`\n…and 5 more" style="--inline-compiler-error-message-size: 1.2rem">

```kotlin
import excel.dsl.ColumnDelegate
import excel.dsl.SheetBuilder
import excel.dsl.generateExcel
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun SheetBuilder<Invoice>.column(
  value: (Invoice) -> Money,
): ColumnDelegate<Number> = column { value(it).cents / 100.0 }

generateExcel("invoices.xlsx", invoices) {
  val price by column { it.price }
}
```

</InlineCompilerError>

<!--
The compiler lists all seven member candidates, with their return types; the slide
cuts it to two.

Members are resolved before extensions. All seven `column` members accept a lambda,
so they are all candidates, and the lambda's return type only chooses *among* them.
`Money` fits none, and the compiler reports an ambiguity instead of moving on to
the extension. The more types the library supports, the less room users have to
add their own under the same name.
-->

---

# An extension with its own name adds a type

<DrawnAnnotation text="money" label="No member to compete with" />
<DrawnAnnotation text="CellFormat.currency(&quot;€&quot;)" label="The extension picks the format too" color="var(--fundamentals-blue)" />

<TypeHint :line="4" receiver="SheetBuilder<Invoice>">

```kotlin
import excel.dsl.*
import excel.dsl.ColumnDelegate

fun <T> SheetBuilder<T>.money(value: (T) -> Money): ColumnDelegate<Number> =
  column(CellFormat.currency("€")) { value(it).cents / 100.0 }

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val price by money { it.price }
}
```
```console
| Customer | Price     |
| Ada      | €1,080.00 |
```

</TypeHint>

<!--
It still returns a `ColumnDelegate`, so `by`, the header naming and `formula { }`
references all keep working: `val gross by formula { price * 1.21 }`.
-->

---

# Excel keeps 15 digits, the library checks

> Part 2, after “`Any?` accepts anything”

<DrawnAnnotation text="{ it.billingId }" label="16 digits, one more than Excel keeps" color="red" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val billingId by column { it.billingId }
}
```
```console
Exception in thread "main" java.lang.IllegalArgumentException: BillingId, row 2:
2026000000000244 has more than 15 significant digits, which Excel cannot store;
write it as text
```

</TypeHint>

<!--
`Any?` in Part 2 failed silently, a `toString()` in the cell. This fails loudly, at
the row. The same check covers `BigDecimal` and `BigInteger`; doubles are already
doubles and pass. `NaN` and infinity fail too: Excel has no literal for them.
-->

---
magic-move
---

# Excel keeps 15 digits, the library checks

> Part 2, after “`Any?` accepts anything”

<DrawnAnnotation text="toString()" label="Text keeps every digit" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val billingId by column { it.billingId.toString() }
}
```
```console
| Customer | BillingId        |
| Ada      | 2026000000000244 |
```

</TypeHint>

---

# Options go before the trailing lambda

> Part 3, after “The lambda still picks the type”

<DrawnAnnotation text="width = 20" label="A named argument, and still a trailing lambda" />
<DrawnAnnotation text="CellFormat.currency(&quot;€&quot;)" label="A `@JvmInline value class` over Excel's pattern" color="var(--fundamentals-blue)" />
<DrawnAnnotation text="formula(euro)" label="Formulas take the same options" />

<TypeHint :line="3" receiver="SheetBuilder<Invoice>">
<TypeHint :line="7" text="formula(euro) {" receiver="Formulas">

```kotlin
import excel.dsl.*

val euro = CellFormat.currency("€")

generateExcel("invoices.xlsx", invoices) {
  val customer by column(width = 20) { it.customer }
  val hours by column { it.hours }
  val rate by column(euro) { it.rate }
  val amount by formula(euro) { hours * rate }
}
```
```console
| Customer | Hours | Rate   | Amount    |
| Ada      | 12    | €90.00 | €1,080.00 |
```

</TypeHint>
</TypeHint>

<!--
Every overload gained `format: CellFormat? = null, width: Int? = null` in front of the
lambda. Defaults: General for numbers and text, yyyy-mm-dd for dates, and a width that
fits the header, at least 10 characters. The format also applies to the column's total.
-->

---

# Sheet settings are properties of the receiver

> Part 1, after “One file, many sheets”

<DrawnAnnotation text="autoFilter = true" label="A `var` on `SheetBuilder`, set like a local" />
<DrawnAnnotation text="freezePanes = false" label="On by default" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  autoFilter = true
  freezePanes = false
  val customer by column { it.customer }
  val hours by column { it.hours }
}
```

</TypeHint>

<!--
The filter covers the header and the data rows, not the totals row, so it is set after
the last row is written: streaming only knows the range at the end.
-->

---
magic-move
---

# `@DslMarker` keeps settings out of formulas

> Part 1, after “`@DslMarker` closes the scope”

<InlineCompilerError :line="5" text="autoFilter" message="'var autoFilter: Boolean' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary." style="--inline-compiler-error-message-size: 1.2rem">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val amount by formula {
    autoFilter = true
    hours * rate
  }
}
```

</InlineCompilerError>

---

# Every comparison is an `infix` function

> Extra “DSL tricks”, after “`infix` reads like the formula”

<DrawnAnnotation text="gt" :line="5" />
<DrawnAnnotation text="lt" :line="6" />
<DrawnAnnotation text="eq" :line="7" label="Six of them, one per Excel operator" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" text="formula {" receiver="Formulas">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val amount by formula { hours * rate }
  val large by formula { amount gt 1000 }
  val short by formula { hours lt 10 }
  val standard by formula { rate eq 80 }
}
```
```console
| Hours | Rate | Amount | Large    | Short  | Standard |
| 12    | 90   | =A2*B2 | =C2>1000 | =A2<10 | =B2=80   |
```

</TypeHint>
</TypeHint>

---

# Six comparisons, six Excel operators

| Kotlin | Excel | `evaluate` |
| --- | --- | --- |
| `a gt b` | `A2>B2` | `true` / `false` |
| `a ge b` | `A2>=B2` | `true` / `false` |
| `a lt b` | `A2<B2` | `true` / `false` |
| `a le b` | `A2<=B2` | `true` / `false` |
| `a eq b` | `A2=B2` | `true` / `false` |
| `a ne b` | `A2<>B2` | `true` / `false` |

<style>
.slidev-layout table { font-size: 1.6rem; }
</style>

<!--
`==` has to return Boolean and `compareTo` has to return Int, so none of these can be
an operator. In arithmetic a comparison counts as 1 or 0, like TRUE and FALSE in Excel.
-->

---

# `if` is a keyword, `then` is not

> Extra “DSL tricks”, after “`infix` reads like the formula”

<DrawnAnnotation text="then" label="Returns a `Branch`, not an `Expr`" />
<DrawnAnnotation text="otherwise" label="Only `otherwise` finishes the `IF`" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" text="formula {" receiver="Formulas">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val amount by formula { hours * rate }
  val due by formula {
    amount gt 1000 then amount * 0.9 otherwise amount
  }
}
```
```console
| Amount | Due                      |
| =A2*B2 | =IF(C2>1000, C2*0.9, C2) |
```

</TypeHint>
</TypeHint>

<!--
Infix calls share one precedence and associate to the left:
((amount gt 1000) then (amount * 0.9)) otherwise amount. `*` binds tighter than any
infix call, so the branches need no parentheses.
-->

---
magic-move
---

# A `Branch` is not an `Expr`

> Extra “DSL tricks”, after “`infix` reads like the formula”

<InlineCompilerError :line="6" text="amount gt 1000 then amount * 0.9" message="Return type mismatch: expected 'Expr<Number>', actual 'Branch<Number>'.">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val amount by formula { hours * rate }
  val due by formula<Number> {
    amount gt 1000 then amount * 0.9
  }
}
```

</InlineCompilerError>

<!--
A forgotten `otherwise` is a compile error, not an `IF` without an else branch, which
Excel would fill with FALSE.
-->

---

# New formulas are members of the scope

> Part 6, after “New formulas extend the interface”

<DrawnAnnotation text="round" label="`ROUND`, half away from zero like Excel" />
<DrawnAnnotation text="average" label="`AVERAGE`, `MIN` and `MAX` take a `List` like `sum`" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" text="formula {" receiver="Formulas">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val gross by formula { round(hours * rate * 1.21, 2) }
  val average by formula { average([hours, rate]) }
}
```
```console
| Gross                 | Average          |
| =ROUND(A2*B2*1.21, 2) | =AVERAGE(A2, B2) |
```

</TypeHint>
</TypeHint>

---

# `sum()` only exists inside `totals { }`

> Part 4, after “`*` only exists inside `formula { }`”

<DrawnAnnotation text="hours.sum()" label="A member extension of `TotalsBuilder`" />
<DrawnAnnotation text="customer.label(&quot;Total&quot;)" label="Any column, any type" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="7" text="totals {" receiver="SheetBuilder<Invoice>.TotalsBuilder">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val amount by formula { hours * rate }

  totals {
    customer.label("Total")
    hours.sum()
    amount.sum()
  }
}
```
```console
| Customer | Hours       | Rate | Amount      |
| Ada      | 12          | 90   | =B2*C2      |
| Total    | =SUM(B2:B6) |      | =SUM(D2:D6) |
```

</TypeHint>
</TypeHint>

<!--
A bold row under the data, not written when there are no rows. `sum`, `average`, `min`,
`max` and `count` exist as members of `TotalsBuilder`, so `hours.sum()` means nothing
outside `totals { }`, just like `*` outside `formula { }` in Part 4.
-->

---
magic-move
---

# A total is a formula over whole columns

> Part 4, after “`*` only exists inside `formula { }`”

<DrawnAnnotation text="amount.total" label="Any formula, not only the shorthands" />
<DrawnAnnotation text="sum(amount)" label="The whole column: `SUM(D2:D6)`" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="7" text="totals {" receiver="SheetBuilder<Invoice>.TotalsBuilder">
<TypeHint :line="10" text="total {" receiver="Totals">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val amount by formula { hours * rate }

  totals {
    customer.label("Total")
    hours.sum()
    amount.total { round(sum(amount) * 1.21, 2) }
  }
}
```
```console
| Customer | Hours       | Rate | Amount                     |
| Ada      | 12          | 90   | =B2*C2                     |
| Total    | =SUM(B2:B6) |      | =ROUND(SUM(D2:D6)*1.21, 2) |
```

</TypeHint>
</TypeHint>
</TypeHint>

---

# The context decides which operands exist

> Part 5, after “Context parameter or receiver?”

<DrawnAnnotation text="context(_: Arithmetic)" label="`Expr` arithmetic: rows and totals" />
<DrawnAnnotation text="context(_: Formulas)" label="Columns as operands: rows only" color="var(--fundamentals-blue)" />
<DrawnAnnotation text="fun sum(column: Column<Number>)" label="A whole column: totals only" />

```kotlin no-compile
sealed interface Arithmetic
sealed interface Formulas : Arithmetic
sealed interface Totals : Arithmetic {
  fun sum(column: Column<Number>): Expr<Double>
}

context(_: Arithmetic)
operator fun Expr<Number>.times(other: Number): Expr<Double>

context(_: Formulas)
operator fun Column<Number>.times(other: Number): Expr<Double>
```

<!--
In a totals row, `B7` would be the totals row itself. So a total may only see whole
columns, and a row formula may only see its own row. Both share everything that works
on an `Expr`: arithmetic, `round`, comparisons, `then`/`otherwise`, and user helpers
written against `Arithmetic`.
-->

---
magic-move
---

# A total cannot see a row

> Part 5, after “Context parameter or receiver?”

<InlineCompilerError :line="5" text="hours" occurrence="2" message="Argument type mismatch: actual type is 'Column<Number>', but 'Expr<Number>' was expected.">

```kotlin
import excel.dsl.*

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }

  totals {
    hours.total { round(hours) }
  }
}
```

</InlineCompilerError>

<!--
`round(Column)` is a member of `Formulas`, not of `Totals`. Only `round(Expr)` from
`Arithmetic` is in scope, and a column is not an `Expr`.
-->

---

# A range is one more node

> Part 6, after “Rendering is a `when` over the tree”

<DrawnAnnotation text="data: IntRange?" label="Only the totals row knows the last row" />
<DrawnAnnotation text="Range" :line="3" color="var(--fundamentals-blue)" />

```kotlin no-compile
sealed interface Expr<out V> {
  data class Ref<V> internal constructor(val column: Column<V>) : Expr<V>
  data class Range(val column: Column<*>) : Expr<Nothing>
}

fun Expr<*>.toExcel(row: Int, data: IntRange? = null): String = when (this) {
  is Ref -> column.reference(row)
  is Range -> {
    requireNotNull(data) { "A range only exists in a totals row: $column" }
    "${column.reference(data.first)}:${column.reference(data.last)}"
  }
}
```

<!--
This sketch shows reference addressing. The full renderer also normalizes blank
text and Boolean references, and renders arithmetic, conditions and aggregates.
Rows are streamed, so the totals row is rendered after the last row, when `data` is
finally known.
-->

---

# Totals are testable without a file

> Extra “Sheets as values”, after “The schema is testable without a file”

<InlineValue :line="8" text="invoiceSheet.evaluate(invoices.first())" value="{Hours=12, Rate=90, Amount=1080.0}">
<InlineValue :line="9" text="invoiceSheet.totals(invoices)" value="{Amount=5610.0}">

```kotlin
import excel.dsl.*

val invoiceSheet = sheet<Invoice> {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val amount by formula { hours * rate }
  totals { amount.sum() }
}

invoiceSheet.evaluate(invoices.first())
invoiceSheet.totals(invoices)
```

</InlineValue>
</InlineValue>

<!--
The library's tests write every formula, let Apache POI's formula engine compute it as
Excel would, and check it against `evaluate` and `totals`.
-->

---

# The library fails where Excel would lose data

| Excel limit | Checked |
| --- | --- |
| 32,767 characters of text | per cell, names the column and row |
| 15 significant digits | `Long`, `BigInteger`, `BigDecimal` |
| No `NaN` or infinity | per cell |
| 16,384 columns | at `sheet { }` |
| 1,048,576 rows | while streaming |
| 31-character sheet names, no `/ \ ? * [ ] :` | at `workbook { }` |

<style>
.slidev-layout table { font-size: 1.5rem; }
</style>

<!--
Apache POI checks some of these itself, but deep inside the write and without the
column's name. Failing at `sheet { }` or at the offending row is what makes the
error actionable.
-->

---
layout: intro
class: section-slide
---

# Recap

- `column(format, width) { }`: options before the trailing lambda
- Named extensions add types, same-name overloads lose to members
- `then` / `otherwise`: `infix` builds an `IF`
- `totals { }`: member extensions, and a scope of its own
- `Formulas` / `Totals`: the context decides the operands

> **A realistic DSL is mostly scopes, and knowing the limits.**
