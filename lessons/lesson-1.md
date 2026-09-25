---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 1</div>

# Scope control

## What `@DslMarker` is for

---

# A sheet is a lambda with receiver

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
generateExcel("invoices.xlsx", invoices) {
  column("Customer", bold = true) { it.customer }
  column("Hours") { it.hours }
  column("Rate") { it.rate }
  column("Total") { it.hours * it.rate }
}
```

</TypeHint>

<!--
The lightning talk's example, renamed to one domain type: Invoice(customer, hours, rate, issuedOn).
Adapt the call to his final API once we have it.
-->

---

# One file, many sheets

<DrawnAnnotation text="workbook(&quot;invoices.xlsx&quot;) {" label="A new outer scope"  :geometry="{ label: { x: 0.7153, y: 0.1965 }, connector: { type: 'quadratic', start: { x: 0.3565, y: 0.2243 }, control: { x: 0.4814, y: 0.2321 }, end: { x: 0.6032, y: 0.1988 } } }"/>

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<TypeHint :line="5" receiver="SheetBuilder<Invoice>">

```kotlin
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

---
magic-move
---

# Nested scopes leak the outer receiver

<DrawnAnnotation text="sheet(&quot;Q2&quot;, q2)" label="Resolves to `this@workbook.sheet`, and compiles" color="red" />

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" receiver="SheetBuilder<Invoice>">

```kotlin
workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer", bold = true) { it.customer }
    sheet("Q2", q2) {
      column("Customer", bold = true) { it.customer }
    }
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

---

# `@DslMarker` closes the scope

```kotlin
class WorkbookBuilder {
  fun <T> sheet(
    name: String,
    rows: Sequence<T>,
    block: SheetBuilder<T>.() -> Unit,
  )
}

class SheetBuilder<T> {
  fun column(name: String, bold: Boolean = false, value: (T) -> Any?)
}
```

---
magic-move
---

# `@DslMarker` closes the scope

<DrawnAnnotation text="@DslMarker" label="An annotation for annotations"  :geometry="{ label: { x: 0.3673, y: 0.1890 }, connector: { type: 'quadratic', start: { x: 0.1709, y: 0.2186 }, control: { x: 0.2017, y: 0.2199 }, end: { x: 0.2230, y: 0.1991 } } }"/>
<DrawnAnnotation text="@ExcelDsl" :line="4" label="Mark every builder that belongs to the DSL"  :geometry="{ label: { x: 0.4204, y: 0.3447 }, connector: { type: 'quadratic', start: { x: 0.1589, y: 0.3673 }, control: { x: 0.1950, y: 0.3761 }, end: { x: 0.2096, y: 0.3507 } } }"/>
<DrawnAnnotation text="@ExcelDsl" :line="13" />

```kotlin
@DslMarker
annotation class ExcelDsl

@ExcelDsl
class WorkbookBuilder {
  fun <T> sheet(
    name: String,
    rows: Sequence<T>,
    block: SheetBuilder<T>.() -> Unit,
  )
}

@ExcelDsl
class SheetBuilder<T> {
  fun column(name: String, bold: Boolean = false, value: (T) -> Any?)
}
```

---

# Implicit receivers stay in their own scope

> Only the _closest_ marked receiver is implicit

<InlineCompilerError :line="4" text="sheet" message="'fun <T> sheet(name: String, rows: Sequence<T>, block: SheetBuilder<T>.() -> Unit): Unit'\ncannot be called in this context with an implicit receiver. Use an explicit receiver if necessary." style="--inline-compiler-error-message-size: 1.1rem">

```kotlin
workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer", bold = true) { it.customer }
    sheet("Q2", q2) {
      column("Customer", bold = true) { it.customer }
    }
  }
}
```

</InlineCompilerError>

---
magic-move
---

# Implicit receivers stay in their own scope

<DrawnAnnotation text="this@workbook" label="Still possible, but visible at the call site"  :geometry="{ label: { x: 0.2984, y: 0.4977 }, connector: { type: 'quadratic', start: { x: 0.1138, y: 0.3653 }, control: { x: 0.1077, y: 0.4165 }, end: { x: 0.1213, y: 0.4709 } } }"/>

```kotlin
workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer", bold = true) { it.customer }
    this@workbook.sheet("Q2", q2) {
      column("Customer", bold = true) { it.customer }
    }
  }
}
```

---

# Mark receivers you don't own

<DrawnAnnotation text="AnnotationTarget.TYPE" label="Allow the marker on a type"  :geometry="{ label: { x: 0.7668, y: 0.3239 } }"/>
<DrawnAnnotation text="@ExcelDsl CellStyle.() -> Unit" label="Apache &quot;Poor Obfuscation Implementation&quot; `CellStyle` joins the DSL" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.4516, y: 0.7243 }, connector: { type: 'quadratic', start: { x: 0.4357, y: 0.5573 }, control: { x: 0.4277, y: 0.6234 }, end: { x: 0.4357, y: 0.6897 } } }"/>

```kotlin
import org.apache.poi.ss.usermodel.CellStyle

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ExcelDsl

@ExcelDsl
class SheetBuilder<T> {
  fun column(name: String, bold: Boolean = false, value: (T) -> Any?)
  fun header(block: @ExcelDsl CellStyle.() -> Unit)
}
```

---

# Mark receivers you don't own

<InlineCompilerError :line="4" text="column" message="'fun column(name: String, bold: Boolean, value: (Invoice) -> Any?): Unit'\ncannot be called in this context with an implicit receiver. Use an explicit receiver if necessary." style="--inline-compiler-error-message-size: 1.1rem">

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">
<TypeHint :line="2" receiver="CellStyle">

```kotlin
generateExcel("invoices.xlsx", invoices) {
  header {
    wrapText = true
    column("Customer") { it.customer }
  }
}
```

</TypeHint>
</TypeHint>
</InlineCompilerError>
