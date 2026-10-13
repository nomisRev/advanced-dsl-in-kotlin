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

<DrawnAnnotation text="{ it.amount }" label="`BigDecimal` is a `Number`"  :geometry="{ label: { x: 0.6941, y: 0.3865 } }"/>
<DrawnAnnotation text="{ it.paid }" label="`Boolean`: `TRUE` or `FALSE`, not text"  :geometry="{ label: { x: 0.7004, y: 0.4660 } }"/>
<DrawnAnnotation text="{ it.sentAt }" label="`Instant`, shown in `ExcelConfig.zone`" color="var(--fundamentals-blue)"  :geometry="{ label: { x: 0.7003, y: 0.5362 } }"/>
<DrawnAnnotation text="{ it.status }" label="An `Enum` writes its `name`"  :geometry="{ label: { x: 0.6858, y: 0.5916 } }"/>

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
Nine overloads now: String, Number, BigDecimal, BigInteger, Enum, Boolean,
LocalDate, LocalDateTime, Instant, each with its own @JvmName. BigDecimal and
BigInteger retain their input types; their values still have to fit Excel. The lambdas may return null, which
leaves the cell empty. Dates and date-times get a format by default, so Excel shows
2026-09-01 instead of 46266.
-->

---

# A named column registers immediately

> Part 3, after “Columns become values we can reference”

<DrawnAnnotation text="column" :line="2" :on="0" :wait="false" label="The header is an argument"  :geometry="{ label: { x: 0.7576, y: 0.3520 } }"/>
<DrawnAnnotation text="formula" :line="5" :on="0" :wait="false" label="The same shape for a formula" color="var(--fundamentals-blue)"  :geometry="{ label: { x: 0.7768, y: 0.6567 } }"/>

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" text='formula("Invoice total") {' context="Formulas">

```kotlin
import excel.dsl.*

val report = sheet<Invoice> {
  column("Customer") { it.customer }
  val hours = column("Hours") { it.hours }
  val rate = column("Rate") { it.rate }
  formula("Invoice total") { hours * rate }
}
```

</TypeHint>
</TypeHint>

<!--
Named columns, String.invoke and named formulas register immediately. Each carries
@IgnorableReturnValue: retaining the handle is optional. The unnamed overloads
return delegates and still need `by` to discover the property name.
-->

---

# `BigDecimal` keeps its type during evaluation

> The value still has to fit Excel's 15 significant digits

<TypeHint :line="2" receiver="SheetBuilder<BigDecimal>">

```kotlin
import excel.dsl.*
import excel.dsl.Column
import java.math.BigDecimal

val amount: Column<BigDecimal>
val report = sheet<BigDecimal> {
  amount = column("Amount") { it }
}
val result: CellResult<BigDecimal> =
  report.evaluateTyped(BigDecimal("1080.25"))[amount]
println(result)
```
```console
Value(value=1080.25)
```

</TypeHint>

<!--
The BigDecimal overload preserves the data value; writing checks its precision
and range before converting to Excel's numeric representation. evaluateTyped and
evaluate perform the same checks, so an invalid value cannot pass a schema test
and fail only on export. Formulas still compute with Excel's numeric semantics.
Use a String column for an identifier or a decimal whose every digit must survive.
-->

---

# `ExcelConfig` also picks the time zone

> Part 3, after “Columns become values we can reference”

<DrawnAnnotation text="ZoneId.of(&quot;Europe/Brussels&quot;)" label="Excel dates have no zone, so the schema picks one"  :geometry="{ label: { x: 0.7571, y: 0.5313, width: 0.3978 }, connector: { type: 'quadratic', start: { x: 0.5556, y: 0.4775 }, control: { x: 0.5722, y: 0.5332 }, end: { x: 0.6209, y: 0.5291 } } }"/>
<DrawnAnnotation text="11:30:00" label="09:30 UTC, in summer time" color="var(--fundamentals-blue)"  :geometry="{ label: { x: 0.4535, y: 0.8584 } }"/>

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

<InlineCompilerError :line="8" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(format: CellFormat?, width: Int?, value: (Invoice) -> String?)`\n`fun column(format: CellFormat?, width: Int?, value: (Invoice) -> Number?)`\n…and 7 more" style="--inline-compiler-error-message-size: 1.2rem">

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
The compiler lists all nine member candidates, with their return types; the slide
cuts it to two.

Members are resolved before extensions. All nine unnamed `column` members accept a lambda,
so they are all candidates, and the lambda's return type only chooses *among* them.
`Money` fits none, and the compiler reports an ambiguity instead of moving on to
the extension. The more types the library supports, the less room users have to
add their own under the same name.
-->

---

# An extension with its own name adds a type

<DrawnAnnotation text="money" label="No member to compete with"  :geometry="{ label: { x: 0.5551, y: 0.1476 } }"/>
<DrawnAnnotation text="CellFormat.currency(&quot;€&quot;)" label="The extension picks the format too" color="var(--fundamentals-blue)"  :connect="false" :geometry="{ label: { x: 0.4980, y: 0.2915 } }"/>

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

<DrawnAnnotation text="{ it.billingId }" label="16 digits, one more than Excel keeps" color="red"  :geometry="{ label: { x: 0.7100, y: 0.4570 }, connector: { type: 'quadratic', start: { x: 0.5008, y: 0.4361 }, control: { x: 0.5073, y: 0.4633 }, end: { x: 0.5307, y: 0.4651 } } }"/>

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
the row. The same check covers `BigDecimal` and `BigInteger`. Their overloads preserve the
input type, but a type cannot prove how many digits a value contains. Evaluation
and writing share the validation. Doubles are already doubles and pass. `NaN` and infinity fail too: Excel has no literal for them.
-->

---
magic-move
---

# Excel keeps 15 digits, the library checks

> Part 2, after “`Any?` accepts anything”

<DrawnAnnotation text="toString()" label="Text keeps every digit"  :geometry="{ label: { x: 0.7352, y: 0.4542 }, connector: { type: 'quadratic', start: { x: 0.5851, y: 0.4278 }, control: { x: 0.5875, y: 0.4647 }, end: { x: 0.6251, y: 0.4565 } } }"/>

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
<TypeHint :line="7" text="formula(euro) {" context="Formulas">

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

<DrawnAnnotation text="autoFilter = true" label="A `var` on `SheetBuilder`, set like a local"  :geometry="{ label: { x: 0.5770, y: 0.3836 } }"/>
<DrawnAnnotation text="freezePanes = false" label="On by default" color="var(--fundamentals-blue)"  :geometry="{ label: { x: 0.6123, y: 0.4396 } }"/>

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
<DrawnAnnotation text="eq" :line="7" label="Six of them, one per Excel operator"  :geometry="{ label: { x: 0.7470, y: 0.6143 }, connector: { type: 'quadratic', start: { x: 0.4494, y: 0.6163 }, control: { x: 0.5107, y: 0.6420 }, end: { x: 0.5721, y: 0.6163 } } }"/>

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" text="formula {" context="Formulas">

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
an operator. Comparisons return `Expr<Boolean>`. Arithmetic requires the explicit
`asNumber()` conversion, which writes TRUE as 1 and FALSE as 0.
-->

---

# `if` is a keyword, `then` is not

> Extra “DSL tricks”, after “`infix` reads like the formula”

<DrawnAnnotation text="then" label="Returns a `Branch`, not an `Expr`"  :geometry="{ label: { x: 0.4720, y: 0.6289 }, connector: { type: 'quadratic', start: { x: 0.2950, y: 0.5764 }, control: { x: 0.2967, y: 0.6197 }, end: { x: 0.3156, y: 0.6280 } } }"/>
<DrawnAnnotation text="otherwise" label="Only `otherwise` finishes the `IF`" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" text="formula {" context="Formulas">

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

<DrawnAnnotation text="round" label="`ROUND`, half away from zero like Excel"  :geometry="{ label: { x: 0.7626, y: 0.5412 }, connector: { type: 'quadratic', start: { x: 0.3954, y: 0.4691 }, control: { x: 0.6872, y: 0.4559 }, end: { x: 0.6732, y: 0.5106 } } }"/>
<DrawnAnnotation text="average" label="`AVERAGE`, `MIN` and `MAX` take a `List` like `sum`" color="var(--fundamentals-blue)"  :geometry="{ label: { x: 0.4878, y: 0.7703 } }"/>

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" text="formula {" context="Formulas">

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

<DrawnAnnotation text="hours.sum()" label="A member extension of `TotalsBuilder`"  :geometry="{ label: { x: 0.4582, y: 0.7189 } }"/>
<DrawnAnnotation text="customer.label(&quot;Total&quot;)" label="Any column, any type" color="var(--fundamentals-blue)"  :geometry="{ label: { x: 0.4294, y: 0.7897 } }"/>

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
`max`, `count` and `countNonBlank` exist as members of `TotalsBuilder`, so `hours.sum()` means nothing
outside `totals { }`, just like `*` outside `formula { }` in Part 4.
-->

---
magic-move
---

# A total is a formula over whole columns

> Part 4, after “`*` only exists inside `formula { }`”

<DrawnAnnotation text="amount.total" label="Any formula, not only the shorthands"  :geometry="{ label: { x: 0.2826, y: 0.8108 } }"/>
<DrawnAnnotation text="sum(amount)" label="The whole column: `SUM(D2:D6)`" color="var(--fundamentals-blue)"  :geometry="{ label: { x: 0.6613, y: 0.6576 } }"/>

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="7" text="totals {" receiver="SheetBuilder<Invoice>.TotalsBuilder">
<TypeHint :line="10" text="total {" context="Totals">

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

# `count` and `countNonBlank` count different cells

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" text="totals {" receiver="SheetBuilder<Invoice>.TotalsBuilder">

```kotlin
import excel.dsl.*

val report = sheet<Invoice> {
  val customer by column { it.customer }
  val hours by column { it.hours }

  totals {
    customer.countNonBlank()
    hours.count()
  }
}
```
```console
| Customer       | Hours        |
| =COUNTA(A2:A6) | =COUNT(B2:B6) |
```

</TypeHint>
</TypeHint>

<!--
count takes a numeric column and ignores blanks and errors. countNonBlank takes
any column and includes text, booleans, empty strings and errors. Its name is
explicit when a count is placed under a text or Boolean data column.
-->

---

# Each total overload names its result type

```kotlin no-compile
@JvmName("numberTotal")
fun Column<Number>.total(block: context(Totals) () -> Expr<Number>)

@JvmName("textTotal")
fun Column<String>.total(block: context(Totals) () -> Expr<String>)

@JvmName("booleanTotal")
fun Column<Boolean>.total(block: context(Totals) () -> Expr<Boolean>)
```

<!--
These are members of TotalsBuilder. A number column cannot accept a Boolean total,
and a text column cannot accept a numeric formula through total. The deliberately
named label and countNonBlank operations still allow a label or count under any
column. There is no public Column<*>.total returning Expr<*> escape hatch.
-->

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
sealed class Expr<out V> private constructor() {
  internal data class Ref<V>(val column: Column<V>) : Expr<V>()
  internal data class Range(val column: Column<*>) : Expr<Nothing>()
}

internal fun Expr<*>.toExcel(row: Int, data: IntRange? = null): String = when (this) {
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

# An Excel formula stores its result too

<TypeHint :line="1" receiver="SheetBuilder<Int>">
<TypeHint :line="3" text='formula("Double") {' context="Formulas">

```kotlin
import excel.dsl.*
import java.nio.file.Path

val report = sheet<Int> {
  val n = column("N") { it }
  formula("Double") { n * 2 }
}
report.write(Path.of("report.xlsx"), listOf(3))
```
```xml
<c r="B2" t="n"><f>A2*2</f><v>6.0</v></c>
```

</TypeHint>
</TypeHint>

<!--
The cached result is a field in the xlsx file, not a memoization cache in the DSL.
Readers can display 6 without a formula engine. Excel can recalculate after an edit.
The immutable expression is evaluated per row; each data extractor runs once.
The writer holds one evaluated row and fixed-size accumulators for totals, then
lets POI write the formula and its numeric, text, Boolean or error result.
-->

---

# File options decide whether to replace a report

<DrawnAnnotation text="CREATE_NEW" label="Fail if the destination already exists" />

<TypeHint :line="1" receiver="SheetBuilder<Int>">

```kotlin
import excel.dsl.*
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW

val report = sheet<Int> {
  column("Number") { it }
}
report.write(
  Path.of("report.xlsx"),
  listOf(1, 2, 3),
  options = arrayOf(CREATE_NEW),
)
```

</TypeHint>

<!--
The default matches Files.newOutputStream: create the file or truncate it. A write
failure may leave it empty or partial. CREATE_NEW lets the caller refuse replacement.
For atomic replacement, write to a sibling temporary file, then move it with
ATOMIC_MOVE and REPLACE_EXISTING when supported. That policy belongs to the caller.
Caller-provided OutputStreams keep control of their own opening and lifetime.
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

- `column("Header") { }` registers; `by column { }` names a delegate
- `full` exports return-value checks; immediate registration opts out
- `then` / `otherwise`: `infix` builds an `IF`
- `total { }` has typed overloads; `countNonBlank()` says what it counts
- `Formulas` / `Totals`: the context decides the operands

> **A realistic DSL is mostly scopes, and knowing the limits.**
