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

# One file, many sheets

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

---
magic-move
---

# Nested scopes leak the outer receiver

<DrawnAnnotation text="sheet(&quot;Q2&quot;, q2)" label="Resolves to `this@workbook.sheet`, and compiles" color="red"  :geometry="{ label: { x: 0.3719, y: 0.4511 }, connector: { type: 'quadratic', start: { x: 0.1101, y: 0.3760 }, control: { x: 0.1115, y: 0.4353 }, end: { x: 0.1345, y: 0.4495 } } }"/>

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.leaky.workbook

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

```kotlin no-compile
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

<DrawnAnnotation text="@DslMarker" label="An annotation for annotations"  :geometry="{ label: { x: 0.3085, y: 0.1932 } }" :connect="false"/>
<DrawnAnnotation text="@ExcelDsl" :line="4" label="Mark every builder that belongs to the DSL"  :geometry="{ label: { x: 0.3642, y: 0.3393 }, connector: { type: 'quadratic', start: { x: 0.1588, y: 0.3673 }, control: { x: 0.1949, y: 0.3761 }, end: { x: 0.2095, y: 0.3507 } } }" :connect="false"/>
<DrawnAnnotation text="@ExcelDsl" :line="13" />

```kotlin no-compile
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

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<InlineCompilerError :line="4" text="sheet" message="`fun <T> sheet(name: String, rows: Sequence<T>, block: SheetBuilder<T>.() -> Unit)`\ncannot be called in this context with an implicit receiver. Use an explicit receiver if necessary." style="--inline-compiler-error-message-size: 1.3rem">

```kotlin
import presentation.support.marked.workbook

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
</TypeHint>
</TypeHint>

---
magic-move
---

# Implicit receivers stay in their own scope

> Explicit receiver is always valid although strange here

<DrawnAnnotation text="this@workbook" label="Still possible, but visible at the call site"  :geometry="{ label: { x: 0.3131, y: 0.5671 }, connector: { type: 'quadratic', start: { x: 0.1121, y: 0.4781 }, control: { x: 0.1060, y: 0.5293 }, end: { x: 0.1297, y: 0.5509 } } }"/>

```kotlin
import presentation.support.marked.workbook

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

<DrawnAnnotation text="AnnotationTarget.TYPE" label="Allow the marker on a type"  :geometry="{ label: { x: 0.7417, y: 0.3089 }, connector: { type: 'quadratic', start: { x: 0.6007, y: 0.2752 }, control: { x: 0.6071, y: 0.2904 }, end: { x: 0.6150, y: 0.3049 } } }"/>
<DrawnAnnotation text="@ExcelDsl CellStyle.() -> Unit" label="Apache &quot;Poor Obfuscation Implementation&quot; `CellStyle` joins the DSL" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.6334, y: 0.6695 }, connector: { type: 'quadratic', start: { x: 0.4357, y: 0.5573 }, control: { x: 0.4238, y: 0.5993 }, end: { x: 0.4332, y: 0.6471 } } }"/>

```kotlin no-compile
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
import presentation.support.marked.generateExcel

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
