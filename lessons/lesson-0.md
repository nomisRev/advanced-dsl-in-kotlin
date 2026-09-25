---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 0 · Optional recap</div>

# Your first Kotlin DSL

## From a builder to a lambda with receiver

<!--
Backup material for the handover from the 15-minute lightning talk. Normally
skip this and start the main deck at Part 1. This is our invoice version of
the export example, not a claim about the other speaker's exact API.

Recovered from the function-shapes progression in 428b8ab (September 3),
before the home-automation example became the Excel example in a4fa3af.
The next slide restores the recap removed by 120468a on September 25.
Keep delegates and scope control in the main talk; this only recalls the
builder, function parameter, receiver, apply, and nested-builder mechanics.
-->

---

# A sheet is a lambda with receiver

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.leaky.generateExcel

generateExcel("invoices.xlsx", invoices) {
  column("Customer", bold = true) { it.customer }
  column("Hours") { it.hours }
  column("Rate") { it.rate }
  column("Total") { it.hours * it.rate }
}
```

</TypeHint>

<!--
This is the point we are continuing from. The export describes which columns
we want; the writer handles rows, cell types, styles, and the file. Use Excel
throughout this recap because that is the domain of the advanced deck.
If everyone has just seen the lightning talk, this is the only recap needed.
-->

---

# A builder collects the columns

<DrawnAnnotation text="SheetBuilder<Invoice>()" />
<DrawnAnnotation text="writeWorkbook" />

```kotlin
import presentation.support.leaky.SheetBuilder

val builder = SheetBuilder<Invoice>()
builder.column("Customer", bold = true) { it.customer }
builder.column("Hours") { it.hours }
builder.column("Rate") { it.rate }
builder.column("Total") { it.hours * it.rate }

writeWorkbook("invoices.xlsx", invoices, builder)
```

<!--
Unwrap the DSL. These are ordinary method calls on one object, followed by
the same workbook writer. The lambdas are stored when we register a column;
the writer calls them later for each invoice.
-->

---

# A function owns the builder's lifetime

<DrawnAnnotation text="(SheetBuilder<T>) -> Unit" />
<DrawnAnnotation text="block(builder)" />

```kotlin
import presentation.support.leaky.SheetBuilder

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: (SheetBuilder<T>) -> Unit,
) {
  val builder = SheetBuilder<T>()
  block(builder)
  writeWorkbook(path, rows, builder)
}

generateExcel("invoices.xlsx", invoices) { builder ->
  builder.column("Customer", bold = true) { it.customer }
  builder.column("Hours") { it.hours }
}
```

<!--
Create, configure, write. The caller gets the builder as a regular lambda
parameter. The lambda is the last argument, so we can put it outside the
parentheses. Nothing about the column implementation has changed.
-->

---
magic-move
---

# A receiver makes the builder `this`

<DrawnAnnotation text="SheetBuilder<T>.() -> Unit" />
<DrawnAnnotation text="builder.block()" />

<TypeHint :line="11" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.leaky.SheetBuilder

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) {
  val builder = SheetBuilder<T>()
  builder.block()
  writeWorkbook(path, rows, builder)
}

generateExcel("invoices.xlsx", invoices) {
  this.column("Customer", bold = true) { it.customer }
  this.column("Hours") { it.hours }
}
```

</TypeHint>

<!--
Move SheetBuilder from a parameter to the receiver of the function type.
Invoke block with builder as its receiver. Inside the lambda, this is now
that builder. The lambda no longer declares a builder parameter.
-->

---
magic-move
---

# A receiver makes the builder `this`

<DrawnAnnotation text="column(&quot;Customer&quot;, bold = true)" />

<TypeHint :line="11" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.leaky.SheetBuilder

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) {
  val builder = SheetBuilder<T>()
  builder.block()
  writeWorkbook(path, rows, builder)
}

generateExcel("invoices.xlsx", invoices) {
  column("Customer", bold = true) { it.customer }
  column("Hours") { it.hours }
}
```

</TypeHint>

<!--
The receiver is implicit, so this.column can be written as column.
This is the small syntax change that makes the builder read like a DSL.
-->

---
magic-move
---

# `apply` configures and returns the builder

<DrawnAnnotation text="apply(block)" />

<TypeHint :line="10" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.leaky.SheetBuilder

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) {
  val builder = SheetBuilder<T>().apply(block)
  writeWorkbook(path, rows, builder)
}

generateExcel("invoices.xlsx", invoices) {
  column("Customer", bold = true) { it.customer }
  column("Hours") { it.hours }
}
```

</TypeHint>

<!--
This is the apply step from the old function-shapes slides. apply invokes
the receiver lambda and returns the same builder. The block returns Unit;
it is apply that returns the object. The writer still runs afterwards.
-->

---

# `this` is the builder, `it` is a row

<DrawnAnnotation text="this.column" />
<DrawnAnnotation text="invoice.customer" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.leaky.generateExcel

generateExcel("invoices.xlsx", invoices) {
  this.column("Customer", bold = true) { invoice: Invoice ->
    invoice.customer
  }
  this.column("Total") { invoice: Invoice ->
    invoice.hours * invoice.rate
  }
}
```

</TypeHint>

<!--
Two function shapes: SheetBuilder<Invoice>.() -> Unit configures the sheet;
(Invoice) -> Any? computes a cell. The inner lambda has a regular parameter,
so it does not replace this. Name that parameter invoice to make the two
roles visible. The outer block runs once; column values run for each row.
-->

---
magic-move
---

# `this` is the builder, `it` is a row

<DrawnAnnotation text="it.customer" color="var(--fundamentals-blue)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.leaky.generateExcel

generateExcel("invoices.xlsx", invoices) {
  column("Customer", bold = true) { it.customer }
  column("Total") { it.hours * it.rate }
}
```

</TypeHint>

<!--
Omit this and use the implicit single parameter it. Type inference gets
Invoice from invoices, so neither the builder type nor the row type needs
to be written here. We are back at the lightning talk's style of call site.
-->

---

# Each nested builder adds a receiver

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.leaky.workbook

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer", bold = true) { it.customer }
  }
  sheet("Q2", q2) {
    column("Customer", bold = true) { it.customer }
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

<!--
Use the same recipe again: workbook configures a WorkbookBuilder; each
sheet configures a SheetBuilder. This is the exact starting example of
Part 1. Continue in the main deck: what happens if we move Q2 inside Q1?
The outer receiver is still available. That is why scope control comes next.
-->
