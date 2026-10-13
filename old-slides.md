---
theme: kotlin
layout: cover
canvasWidth: 1440
aspectRatio: 16/9
highlighter: shiki
themeConfig:
  snippets:
    imports:
      - presentation.support.*
---

# Earlier versions of the slides

## Preserved before the 50-minute rehearsal edit

<!--
Every updated or promoted slide below retains its original source, annotations,
frontmatter, and notes. Original locations, in archive order:
1. lessons/extra-still-missing.md: A more specific receiver wins before the lambda — Promoted into the talk; original retained here
2. lessons/lesson-2.md: Users add a cell type with an extension — Updated in the talk
3. lessons/lesson-3.md: Columns become values we can reference — Reduced zoom so the output fits
4. lessons/extra-realistic-library.md: `full` exports the return-value check — Promoted into the talk; original retained here
5. lessons/extra-realistic-library.md: A discarded delegate gets a warning — Promoted into the talk; original retained here
6. lessons/extra-realistic-library.md: `by` consumes the delegate — Promoted into the talk; original retained here
7. lessons/extra-gaps.md: A context lambda brings only the context — Promoted into the talk; original retained here
8. lessons/extra-gaps.md: A context lambda brings only the context — Promoted into the talk; original retained here
9. lessons/extra-gaps.md: A context lambda brings only the context — Promoted into the talk; original retained here
10. lessons/extra-gaps.md: `@DslMarker` closes contexts too — Promoted into the talk; original retained here
11. lessons/extra-sheets-as-values.md: A sheet is a value — Promoted into the talk; original retained here
12. lessons/extra-sheets-as-values.md: One schema, many row sets — Promoted into the talk; original retained here
13. lessons/extra-sheets-as-values.md: The schema is testable without a file — Promoted into the talk; original retained here
14. lessons/lesson-6.md: Recap — Updated in the talk
15. slides.md: Advanced DSL in Kotlin — Added the 50-minute rehearsal timer
The nonexistent extra-ordered-state.md import was removed from extras.md;
there was no slide content to archive for that import.
-->

---

# A more specific receiver wins before the lambda

> Part 2, replacing “Users add a cell type with an extension”

<DrawnAnnotation text="column(&quot;Customer&quot;)" label="Resolves to the user's `SheetBuilder<Invoice>.column`, not the library's" color="red" />
<DrawnAnnotation text="column(&quot;custom&quot;)" label="The only call it was written for" />
<InlineCompilerError :line="2" text="{ it.customer }" message="Return type mismatch: expected 'CustomType', actual 'String'.">
<InlineCompilerError :line="3" text="{ it.hours }" message="Return type mismatch: expected 'CustomType', actual 'Int'.">

```kotlin
import presentation.support.typed.column
import presentation.support.typed.generateExcel
import invoices.custom.column

generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  column("Hours") { it.hours }
  column("custom") { it.custom }
}
```

</InlineCompilerError>
</InlineCompilerError>

<!--
The Part 2 slide declares `fun SheetBuilder<Invoice>.column(name: String,
block: (Invoice) -> CustomType)` and shows it next to the library's three
overloads. That does not compile, in any arrangement (2.5.0-Beta1):

- Declared in another file and imported explicitly, as here: `SheetBuilder<Invoice>`
  is more specific than `SheetBuilder<T>`, so the user's overload wins every
  `column` call before any lambda is analysed, and the other three columns fail
  with the errors above. `@OverloadResolutionByLambdaReturnType` on the user's
  function changes nothing: it only breaks ties between equally specific
  candidates.
- Declared in the same file as the calls, with the library's `column`
  imported: explicit imports outrank the file's package (row 3 of the table),
  so the user's overload is never a candidate and `column("custom") { it.custom }`
  is an "Overload resolution ambiguity" between the three library overloads.
- Against member overloads: the realistic library's "A member overload beats a
  user's extension", the same ambiguity.

The fix is the realistic library's next slide: an extension under its own name,
`money { it.price }`. The Part 2 slide would be right as "An extension adds a
type under its own name".
-->

---
magic-move
---

# Users add a cell type with an extension

<DrawnAnnotation text="fun SheetBuilder<Invoice>.column" label="An overload outside the library, on the same receiver" :geometry="{ label: { x: 0.31, y: 0.745 } }" />
<DrawnAnnotation text="CustomType" label="A `@JvmInline value class` over the invoice reference" color="var(--fundamentals-blue)" :connect="false" :geometry="{ label: { x: 0.40, y: 0.665 } }" />

```kotlin no-compile
import presentation.support.typed.SheetBuilder
import presentation.support.typed.generateExcel

generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  column("Hours") { it.hours }
  column("Issued") { it.issuedOn }
  
  column("custom") { it.custom }
}

fun SheetBuilder<Invoice>.column(name: String, block: (Invoice) -> CustomType) {
  column(name) { block(it).toColumnText() }
}
```

---
magic-move
---

# Columns become values we can reference

```kotlin
import presentation.support.delegated.SheetBuilder
import presentation.support.writeWorkbook

data class ExcelConfig(val columnNameStyle: (String) -> String)

companion val ExcelConfig.Default =
  ExcelConfig { it.replaceFirstChar(Char::uppercase) }

// Example
inline fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  config: ExcelConfig = ExcelConfig.Default,
  block: SheetBuilder<T>.() -> Unit,
) {
  val builder = SheetBuilder<T>(config.columnNameStyle).apply(block)
  writeWorkbook(path, rows, builder)
}

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
}
```
```console
| Customer | Hours | Rate | Total  |
| Ada      | 12    | 90   | =B2*C2 |
```

---

# `full` exports the return-value check

> `excel-dsl/build.gradle.kts`

<DrawnAnnotation text='"-Xreturn-value-checker=full"' label="The library exports the checking metadata" />

```kotlin gradle
kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xreturn-value-checker=full")
  }
}
```

<!--
Full mode marks the library's declarations in the compiled Kotlin metadata.
Consumers enable check or full to report unused results. Unit-returning operations
need no annotation. Immediate registration functions opt out with
@IgnorableReturnValue; delegate factories do not.
-->

---

# A discarded delegate gets a warning

> Consumer: `-Xreturn-value-checker=check`

<Warning :line="2" text="column" message="Unused return value of 'column'.">
<Warning :line="3" text="formula" message="Unused return value of 'formula'.">
<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import excel.dsl.*

val report = sheet<Invoice> {
  column { it.customer }
  formula { literal(1) }
  column("Hours") { it.hours }
}
```

</TypeHint>
</Warning>
</Warning>

<!--
The two unnamed calls discard a delegate, so neither registers a column. The
named call does register and intentionally permits ignoring the returned handle.
The compiler says "warning: unused return value of 'column'." and likewise for
formula. This is a warning, not an error, unless the consumer promotes it.
Assigning the delegate to `val unused = column { ... }` counts as using the result;
the return-value checker cannot prove that this delegate was never bound with by.
-->

---
magic-move
---

# `by` consumes the delegate

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import excel.dsl.*

val report = sheet<Invoice> {
  val customer by column { it.customer }
  val one by formula { literal(1) }
  column("Hours") { it.hours }
}
```

</TypeHint>

---

# A context lambda brings only the context

> Part 5, after “A receiver satisfies a context”
>
> Function types with `context(...)`: stable since Kotlin 2.4.0

<DrawnAnnotation text="context(Formulas) () -> Cell" label="A function type with a context, no receiver"  :geometry="{ label: { x: 0.6262, y: 0.3743 }, connector: { type: 'quadratic', start: { x: 0.4770, y: 0.4664 }, control: { x: 0.5024, y: 0.4279 }, end: { x: 0.5017, y: 0.3948 } } }"/>
<DrawnAnnotation text="block(ExcelFormulas(row))" label="The context is passed like an argument" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.4921, y: 0.6203 } }"/>

```kotlin no-compile
@ExcelDsl
class SheetBuilder<T> {
  fun formula(block: context(Formulas) () -> Cell): ColumnDelegate<Number> =
    formulaColumn { row -> block(ExcelFormulas(row)) }
}
```

<!--
Part 5 turns the operators into context functions but keeps the block a
receiver lambda, `Formulas.() -> Cell`. Once every operator takes `Formulas`
as a context, the block no longer needs `this` at all.
-->

---

# A context lambda brings only the context

> Part 5, after “A receiver satisfies a context”

<DrawnAnnotation text="hours * rate" label="`context(formulas: Formulas)` operators resolve"  :geometry="{ label: { x: 0.6811, y: 0.3790 } }"/>
<DrawnAnnotation text="total.withVat()" label="So does every user extension from Part 5" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.7123, y: 0.5465 } }"/>

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" text="formula {" context="Formulas">
<TypeHint :line="5" text="formula {" context="Formulas">

```kotlin
import presentation.support.contextual.generateExcel
import presentation.support.contexts.times
import presentation.support.contexts.withVat

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
  val gross by formula { total.withVat() }
}
```
```console
| hours | rate | total  | gross    |
| 12    | 90   | =A2*B2 | =C2*1.21 |
```

</TypeHint>
</TypeHint>
</TypeHint>

---
magic-move
---

# A context lambda brings only the context

> Part 5, after “A receiver satisfies a context”

<InlineCompilerError :line="4" text="multiply" message="Unresolved reference 'multiply'.">

```kotlin
import presentation.support.contextual.generateExcel
import presentation.support.contexts.times
import presentation.support.contexts.withVat

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { multiply(hours, rate) }
  val gross by formula { total.withVat() }
}
```

</InlineCompilerError>

<!--
The members of `Formulas` are not in scope: the context is not `this`. The
interface becomes the implementation detail, the context functions the API.
That is the "extension receiver or context parameter" table from Part 5, at
the level of the block.
-->

---

# `@DslMarker` closes contexts too

> Part 5, after “A receiver satisfies a context”

<DrawnAnnotation text="context(_: WorkbookBuilder)" label="Satisfied by the marked `workbook { }` receiver"  :geometry="{ label: { x: 0.6277, y: 0.3352 } }"/>
<DrawnAnnotation text="stamp()" :line="5" label="The closest marked scope is the workbook" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.4166, y: 0.4931 } }"/>

<TypeHint :line="4" receiver="WorkbookBuilder">
<TypeHint :line="6" receiver="SheetBuilder<Invoice>">
<InlineCompilerError :line="7" text="stamp" message="'context(_: WorkbookBuilder) fun stamp(): Unit' cannot be called in this context\nwith an implicit receiver. Use an explicit receiver if necessary." style="--inline-compiler-error-message-size: 1.2rem">

```kotlin
import presentation.support.marked.WorkbookBuilder
import presentation.support.marked.workbook

context(_: WorkbookBuilder)
fun stamp() {}

workbook("invoices.xlsx") {
  stamp()
  sheet("Q1", q1) {
    stamp()
  }
}
```

</InlineCompilerError>
</TypeHint>
</TypeHint>

<!--
Context parameters (stable since 2.4.0) resolve from the same implicit
receivers @DslMarker guards, so a user's context function cannot leak the
outer scope either. The spec (KEEP notes/0005) adds one rule for contexts:
two implicit values with the same marker in one scope forbid every implicit
call. Explicit context arguments (next slides) are exempt.
Verified on 2.5.0-Beta1; the same text is in Part 6's notes from 2.4.20.
-->

---
magic-move
---

# A sheet is a value

<DrawnAnnotation text="sheet<Invoice>" label="No path, no rows: only the schema" />
<DrawnAnnotation text="invoiceSheet.write" label="The I/O, as often as needed" color="var(--fundamentals-blue)" />
<DrawnAnnotation text="output" :line="8" label="An `OutputStream`, not a `String` path" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" receiver="Formulas">

```kotlin
import kotlin.io.path.Path
import kotlin.io.path.outputStream
import presentation.support.contexts.times
import presentation.support.sheets.sheet

val invoiceSheet = sheet<Invoice> {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
}

Path("invoices.xlsx").outputStream().use { output ->
  invoiceSheet.write(output, invoices)
}
```
```console
| customer | hours | rate | total  |
| Ada      | 12    | 90   | =B2*C2 |
```

</TypeHint>
</TypeHint>

<!--
The block is byte-for-byte the same as before; the audience should see that
the columns do not change, only the first line and the last three.

`sheet<Invoice>` needs the explicit type argument: nothing else in the call
fixes `T`, and `it.customer` needs it. `generateExcel` inferred it from
`invoices`. That is the one line of noise this design adds.

`sheet { }` returns a `Sheet<Invoice>`. `write(output, rows)` is a member of
it and takes a `java.io.OutputStream`, which it leaves open (the caller owns
it, hence `use`). A `Path` overload exists as an extension for the common case.
Streaming is unchanged: `SXSSFWorkbook` keeps 100 rows in memory.
-->

---

# One schema, many row sets

<DrawnAnnotation text="q1" label="Same schema, other rows" />
<DrawnAnnotation text="q2" color="var(--fundamentals-blue)" />

```kotlin
import kotlin.io.path.Path
import presentation.support.sheets.invoiceSheet
import presentation.support.sheets.write

invoiceSheet.write(Path("q1.xlsx"), q1)
invoiceSheet.write(Path("q2.xlsx"), q2)
```
```console
q1.xlsx
| customer | hours | rate | total  |
| Linus    | 8     | 80   | =B2*C2 |
| Grace    | 20    | 110  | =B3*C3 |

q2.xlsx
| customer | hours | rate | total  |
| Olivia   | 6     | 95   | =B2*C2 |
| Linus    | 14    | 80   | =B3*C3 |
```

<!--
`invoiceSheet` here is the one in support/sheets/Invoices.kt: a top-level
`val` of an immutable `Sheet<Invoice>`. Sharing a *value* globally is fine;
what was wrong in the old support code is sharing a *builder* (see the end).

`write(Path, rows)` is an extension over the member `write(OutputStream, ...)`:
it opens and closes the file and names the sheet after it (`q1`).
With `generateExcel` this slide is two copies of the block.
-->

---

# The schema is testable without a file

<InlineValue :line="1" text="invoiceSheet.headers" value="[customer, hours, rate, total]">
<InlineValue :line="2" text="invoiceSheet.evaluate(invoices.first())" value="{customer=Ada, hours=12, rate=90, total=1080.0}">

```kotlin
import presentation.support.sheets.invoiceSheet

invoiceSheet.headers
invoiceSheet.evaluate(invoices.first())
```

</InlineValue>
</InlineValue>

<!--
This is the Part 4 payoff: `evaluate` runs every `formula { }` block again
with `Evaluate` instead of `ExcelFormulas`, so `total` is `1080.0`, not
`=B2*C2`. No POI, no file, no bytes: a unit test is two `check`s:
  check(invoiceSheet.headers == listOf("customer", "hours", "rate", "total"))
  check(invoiceSheet.evaluate(invoice)["total"] == 1080.0)
Before, the only way to test a formula was to write an .xlsx and read the
cell back, because the schema only existed inside `generateExcel`.

`Evaluate` in support/sheets implements Part 6's `Formulas` (multiply from
Part 5 plus `sum` over a `List`) by reading the values of the columns to its
left, by `Column.index`.
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

---
theme: kotlin
transition: view-transition
layout: cover
class: fundamentals-cover
canvasWidth: 1440
aspectRatio: 16/9
colorSchema: both
highlighter: shiki
info: |
  Advanced DSL in Kotlin. Scope control, overloads by lambda return type, delegates, member extensions and context parameters.
themeConfig:
  kodee: greeting
  snippets:
    imports:
      - presentation.support.*
  siteUrl: https://nomisrev.github.io/advanced-dsl-in-kotlin/
  analytics:
    goatcounter: nomsrev
kodee: welcome
---

<!-- @formatter:off -->

# Advanced DSL in Kotlin

## Beyond the builder

Simon Vergauwen · Developer Advocate, JetBrains
