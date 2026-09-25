---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Extra</div>

# Sheets as values

## Build the schema once, write it anywhere

<!--
For Simon, not the audience (yet). The critique in one sentence: every stage of
the DSL is `generateExcel(path, rows) { columns }`, so the *schema* (which
columns, which formulas) is only ever built inside the call that does the
*I/O* (open a file, pull the rows, write). Split the two: `sheet<T> { }`
returns a value, `write(output, rows)` does the I/O.

Implementation: src/main/kotlin/presentation/support/sheets/. It reuses the
core: `DelegatedColumns` for `by column`, Part 6's `lists.Formulas` for
`formula { }`, and `writeWorkbook`, which got an `OutputStream` overload
(the `String` path overload behaves as before).
-->

---

# `generateExcel` ties the schema to one file

<DrawnAnnotation text="&quot;invoices.xlsx&quot;" label="Where: a path, opened inside the call" color="red" />
<DrawnAnnotation text="invoices" :line="1" occurrence="2" label="What: the rows" color="var(--fundamentals-blue)" />
<DrawnAnnotation text="val total by formula" label="How: the schema, rebuilt on every call" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" receiver="Formulas">

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

</TypeHint>
</TypeHint>

<!--
What is wrong with this, concretely:
- Want the same sheet for Q1 and Q2? Copy the block, or wrap the call in a
  function that takes the rows. Either way the schema is not something you
  can hold, name, or pass around.
- Want to send it as an HTTP response? The only destination is a `String`
  path, so you write a temp file and read it back.
- Want to test that `total` is `hours * rate`? You have to write a real
  .xlsx and open it with POI again. There is no object to inspect.
- The support code needed a hack for this (last slides of this extra).
Nothing about the block itself is wrong. Only *where* it runs.
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

# The schema is built once, rows at every `write`

<DrawnAnnotation text="println(&quot;schema&quot;)" label="Runs when `sheet { }` is called" />
<DrawnAnnotation text="q1.onEach" label="Lazy: nothing is pulled yet" color="var(--fundamentals-blue)" />

```kotlin
import java.io.OutputStream
import presentation.support.sheets.sheet

val customers = sheet<Invoice> {
  println("schema")
  val customer by column { it.customer }
}

val rows = q1.onEach { println("row ${it.customer}") }

customers.write(OutputStream.nullOutputStream(), rows)
customers.write(OutputStream.nullOutputStream(), rows)
```
```console
schema
row Linus
row Grace
row Linus
row Grace
```

<!--
The two phases, made visible:
1. Build: the builder block runs exactly once, at `sheet { }`. The `by`
   delegates register the columns (PropertyDelegateProvider, Part 3), then
   `sheet` copies the builder's lists into an immutable `Sheet`. The builder
   is gone after that.
2. Write: every `write` pulls the `Sequence` from the start, row by row, and
   calls each column lambda per row. Rows are never collected into a list.

Before, both phases happened in one call, so "build once, write many" could
not even be expressed. The `println` in the block is only for the demo.

Caveat worth knowing: a `Sequence` that can only be iterated once (e.g. from
`generateSequence` over a JDBC cursor, or `constrainOnce()`) can only be
written once. That is a property of the rows, not of the sheet.
-->

---

# `Sheet<T>` is the whole API

<DrawnAnnotation text="internal constructor" label="Only `sheet { }` builds one" />
<DrawnAnnotation text="List<ColumnSpec<T>>" label="Copied when the block returns, never mutated" />
<DrawnAnnotation text="fun evaluate" label="A row without Excel: tests, previews" color="var(--fundamentals-blue)" />
<DrawnAnnotation text="OutputStream" :line="9" label="File, HTTP body, memory" color="var(--fundamentals-blue)" />

```kotlin no-compile
class Sheet<T> internal constructor(
  private val columns: List<ColumnSpec<T>>,
  private val formulas: Map<Int, Formulas.() -> Cell>,
) {
  val headers: List<String>

  fun evaluate(row: T): Map<String, Any?>

  fun write(output: OutputStream, rows: Sequence<T>, name: String = "Sheet1")
}

fun <T> sheet(block: SheetBuilder<T>.() -> Unit): Sheet<T>
```

<!--
Design choices, so you can push back on them:
- `Sheet<T>` is a plain class, not an interface: one implementation, and the
  internal constructor means nobody can build an inconsistent one.
- It exposes `headers` and `evaluate(row)`, not the column lambdas. Enough to
  test, not enough to couple callers to `ColumnSpec`.
- `formulas` keeps the *block* of each `formula { }`, not only the Excel
  closure. That is what lets `evaluate` run the same block with the
  `Evaluate` interpreter of Part 4. The builder registers both: the Excel
  one through `formulaColumn`, and the block keyed by column index.
- The sheet name is a parameter of `write` (and of `workbook { sheet(...) }`),
  not of the schema: the same schema is "Q1" in one workbook and "Q2" in the
  next.
- `T` stays invariant; `ColumnSpec<in T>` already allows a
  `Sheet<Invoice>` to reuse columns written for a supertype.
Real code: support/sheets/Sheet.kt, SheetBuilder.kt.
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
magic-move
---

# Any `OutputStream` will do

<DrawnAnnotation text="exchange.responseBody" label="Streamed to the client, no temp file" color="var(--fundamentals-blue)" />
<DrawnAnnotation text="invoiceSheet.write" label="The same value as the file" />

```kotlin
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import presentation.support.sheets.invoiceSheet

val server = HttpServer.create(InetSocketAddress(8080), 0)

server.createContext("/invoices.xlsx") { exchange ->
  exchange.sendResponseHeaders(200, 0)
  exchange.responseBody.use { body ->
    invoiceSheet.write(body, invoices)
  }
}
server.start()
```

<!--
The JDK's own HttpServer so the fence compiles without a dependency; in Ktor
it is `call.respondOutputStream { invoiceSheet.write(this, invoices) }`.
`sendResponseHeaders(200, 0)` means chunked: the length is unknown because
the rows are streamed.

With a `String` path this needs a temp file, a read back, and a delete.
Taking an `OutputStream` is the smallest change that makes the destination
the caller's decision. Not run on purpose: it would keep a port open.
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

# A workbook composes sheets

<DrawnAnnotation text="sheet(&quot;Q1&quot;, invoiceSheet, q1)" label="A name, a schema, the rows" />
<DrawnAnnotation text="report.write" label="Every `Sequence` is pulled here" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="WorkbookBuilder">

```kotlin
import kotlin.io.path.Path
import kotlin.io.path.outputStream
import presentation.support.sheets.invoiceSheet
import presentation.support.sheets.workbook

val report = workbook {
  sheet("Q1", invoiceSheet, q1)
  sheet("Q2", invoiceSheet, q2)
}

Path("report.xlsx").outputStream().use { report.write(it) }
```
```console
Q1
| customer | hours | rate | total  |
| Linus    | 8     | 80   | =B2*C2 |
| Grace    | 20    | 110  | =B3*C3 |

Q2
| customer | hours | rate | total  |
| Olivia   | 6     | 95   | =B2*C2 |
| Linus    | 14    | 80   | =B3*C3 |
```

</TypeHint>

<!--
`workbook { }` follows the same rule: it returns a `Workbook` value and
`write(output)` does the I/O. It binds rows to sheets but does not read them;
the sequences are still lazy until `write`.

`Sheet.write(output, rows)` is itself `workbook { sheet(name, this, rows) }
.write(output)`, so there is one write path, the core `writeWorkbook`.

Note what disappears: in Part 1 `sheet("Q1", q1) { }` takes a block, so a
`sheet` nested in a `sheet` is the scope leak `@DslMarker` fixes. Here
`sheet` takes a value and no block, so there is nothing to leak. That is why
the "What changes" slides keep Part 1 as it is.
-->

---

# A global builder grows as classes load

<DrawnAnnotation text="SheetBuilder<Invoice>()" label="Mutable and shared by every slide" color="red" />
<DrawnAnnotation text="invoiceSheet.formula(block)" label="Any top-level `by formula` adds a column" color="red" />

```kotlin
import presentation.support.delegated.Formulas
import presentation.support.delegated.SheetBuilder

val invoiceSheet = SheetBuilder<Invoice>()

val hours by invoiceSheet.column { it.hours }
val rate by invoiceSheet.column { it.rate }
val total by invoiceSheet.formula { hours * rate }

fun formula(block: Formulas.() -> Cell) = invoiceSheet.formula(block)
```

<!--
This is (a trimmed copy of) the real support code in
support/delegated/SheetBuilder.kt, and the same in varargs/ and lists/ with
`timesheetSheet`. Why it exists: Part 5 and 6 slides write a lone top-level
`val gross by formula { withVat(total) }` or `val subtotal by formula { }`.
`by formula` needs a builder, and outside a `generateExcel { }` block there
is none, so the support code offers a global one.

Why it is a hack:
- `val gross by formula { }` at top level runs when that snippet's file class
  is initialised, and appends a column to the shared builder as a side effect.
  Which columns the sheet has, and at which index, depends on which classes
  the JVM happened to load first.
- The builder is never written, so it hides the problem instead of showing it.
With sheets as values there is always a `sheet { }` block to put the line
in, so the global and its `formula` function can be deleted.
-->

---
magic-move
---

# A value needs no global

<DrawnAnnotation text="val gross by formula" label="The Part 5 line, now inside the block" />
<DrawnAnnotation text="invoiceSheet.evaluate" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" receiver="Formulas">
<TypeHint :line="5" receiver="Formulas">
<InlineValue :line="8" text="invoiceSheet.evaluate(invoices.first())[&quot;gross&quot;]" value="1306.8">

```kotlin
import presentation.support.contexts.times
import presentation.support.contexts.withVat
import presentation.support.sheets.sheet

val invoiceSheet = sheet<Invoice> {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
  val gross by formula { total.withVat() }
}

invoiceSheet.evaluate(invoices.first())["gross"]
```

</InlineValue>
</TypeHint>
</TypeHint>
</TypeHint>

<!--
`withVat` is Part 5's `context(formulas: Formulas) fun Column<Number>.withVat()`.
It works unchanged: the `formula { }` receiver here is Part 6's `Formulas`,
which extends Part 5's, so the receiver satisfies the context.

And the user helper is tested without Excel as well: `evaluate` gives
1080 * 1.21.
-->

---
zoom: 0.8
---

# Most slides only swap the first line

| Slide (Part) | Before | After |
| --- | --- | --- |
| Parts 2–5: every `generateExcel` slide | `generateExcel("invoices.xlsx", invoices) {` | `val invoiceSheet = sheet<Invoice> {` |
| Columns become values we can reference (3), the `ExcelConfig` step | `generateExcel(path, rows, config, block)` | `sheet(config, block)`: header naming is schema |
| `inline` removes the builder lambda (6) | `inline fun generateExcel(path, rows, block)` | `inline fun sheet(block)` |
| One file, many sheets … Mark receivers you don't own (1) | `workbook("invoices.xlsx") { sheet("Q1", q1) { } }` | unchanged |

<!--
Recommendation, per slide, so you can decide:

Part 1 (all slides): keep. The `@DslMarker` story needs a `sheet { }` block
nested inside a `sheet { }` block; a `workbook { sheet(name, value, rows) }`
has no nested block to leak. "Mark receivers you don't own" uses
`generateExcel { header { } }`, keep it too (or `sheet<Invoice> { header { } }`
if each stage gets a `sheet`, same TypeHints).

Parts 2 to 5, every slide with `generateExcel("invoices.xlsx", invoices) {`:
`Any?` accepts anything, The overload is chosen before the lambda, Every
column has a cell type (x4), The total is a number, not a formula (x3 in
Part 3, x1 in Part 4), Columns become values we can reference, The lambda
still picks the type, `*` only exists inside `formula { }`, The receiver type
still checks, A receiver satisfies a context. Only line 1 changes; the block,
the `console` fence and the TypeHint receivers stay. Each stage package
(typed, delegated, contexts, lists) gets a `sheet` next to (or instead of)
`generateExcel`. Line numbers of annotations do not move.

Where to introduce it (my pick): one slide at the end of Part 3, after
"The lambda still picks the type": the "A sheet is a value" magic move of
this extra. The chapter is already called "Columns as values". Then Part 2
may keep `generateExcel` (it is about overloads, the I/O is noise either
way), and Parts 3 to 6 use `sheet { }`. The alternative, `sheet { }` from
Part 2 on, is also fine; it only costs the `<Invoice>` type argument earlier.

The `ExcelConfig` step: `columnNameStyle` shapes headers, so it belongs to
the schema (`sheet(config) { }`), not to `write`.

`inline`: still valid, but the saving is now once per schema, not per write,
so the argument is weaker; the "A stored lambda cannot be inlined" slide is
unchanged.
-->

---
zoom: 0.8
---

# Top-level `by formula` moves into the block

| Slide (Part) | Before | After |
| --- | --- | --- |
| Users want their own formulas (5), A context parameter is not a receiver (5, ×2) | `val gross by formula { total.withVat() }` at top level | same line inside `sheet<Invoice> { }` |
| Varargs take any number of columns … Collection literals keep the call short (6) | `val subtotal by formula { sum([hours, overtime]) }` at top level | same line inside `sheet<Timesheet> { }` |
| The receiver in scope picks the implementation (4) | `Evaluate(mapOf(hours to 12, rate to 90)).total()` | unchanged, then `invoiceSheet.evaluate(invoice)` |
| Support: `delegated`, `varargs`, `lists` | global `invoiceSheet` / `timesheetSheet` + top-level `formula` | deleted |

<!--
Part 5, "Users want their own formulas" and both "A context parameter is not
a receiver" slides: the snippet ends with a top-level
`val gross by formula { ... }`, which only compiles thanks to the global
builder. After: wrap it as on the "A value needs no global" slide. The
`withVat` declaration and the InlineCompilerError on `*` stay on the same
lines; the block adds four lines (hours, rate, total, gross) below them.
`// Example` cannot hide lines in the middle of a block, so either accept
the four lines, or show only the `withVat` declaration (the
InlineCompilerError is inside `withVat` anyway) and move the `sheet { }`
that uses it to the next slide, as "A receiver satisfies a context" does.

Part 6, the varargs/list chain: same, the `val subtotal`/`val total by
formula` lines go into `sheet<Timesheet> { }`, and `val billable = [...]`
becomes a local in the block. "Spread copies the array" and "Accept a `List`"
keep their point; the column `hours`/`overtime` declarations become visible
(they are hidden in support today).

Part 4, "The receiver in scope picks the implementation": keep; its natural
follow-up is the "testable without a file" slide of this extra.

Support code: delete `invoiceSheet` + `formula` in delegated/SheetBuilder.kt
and `timesheetSheet` + `formula` in varargs/ and lists/. Everything else in
the stage packages stays.
-->

---
layout: intro
class: section-slide
---

# Recap

- `sheet<T> { }`: the block builds the schema, once
- `Sheet<T>`: immutable, `headers` and `evaluate` without I/O
- `write(output, rows)`: any `OutputStream`, rows pulled lazily
- `workbook { sheet(name, sheet, rows) }`: a value too

> **Build the schema as a value, do the I/O at the edge.**
