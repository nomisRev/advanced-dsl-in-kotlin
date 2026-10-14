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

```kotlin{1-2,4-5,7-8}
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

# Nested scopes leak the outer receiver

<DrawnAnnotation text="sheet(&quot;Q2&quot;, q2)" label="Resolves to `this@workbook.sheet`, and compiles" color="red"  :geometry="{ label: { x: 0.3719, y: 0.4511 }, connector: { type: 'quadratic', start: { x: 0.1101, y: 0.3760 }, control: { x: 0.1115, y: 0.4353 }, end: { x: 0.1345, y: 0.4495 } } }"/>

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<TypeHint :line="4" receiver="SheetBuilder<Invoice>">

```kotlin{1-2,4,6-8}
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

<DrawnAnnotation text="@DslMarker" label="An annotation for annotations"  :geometry="{ label: { x: 0.3185, y: 0.1965 } }" :connect="false"/>

```kotlin
@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class ExcelDsl
```

---
magic-move
---

# `@DslMarker` closes the scope

<DrawnAnnotation text="@ExcelDsl" :line="5" label="Mark every builder that belongs to the DSL" :geometry="{ label: { x: 0.3933, y: 0.3738 }, connector: { type: 'quadratic', start: { x: 0.1612, y: 0.4098 }, control: { x: 0.1819, y: 0.4175 }, end: { x: 0.1902, y: 0.3878 } } }"/>
<DrawnAnnotation text="@ExcelDsl" :line="14" />

```kotlin no-compile
@DslMarker
@Target(AnnotationTarget.CLASS)
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
<InlineCompilerError :line="5" text="sheet" message="Cannot be called in this context with an implicit receiver\nUse an explicit receiver if necessary.">

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

<DrawnAnnotation text="this@workbook" label="Still possible, but explicit at the call site"  :geometry="{ label: { x: 0.3229, y: 0.5587 }, connector: { type: 'quadratic', start: { x: 0.1067, y: 0.4778 }, control: { x: 0.1060, y: 0.5293 }, end: { x: 0.1297, y: 0.5509 } } }"/>

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

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

</TypeHint>
</TypeHint>

---

# Mark receivers you don't own

<DrawnAnnotation text="AnnotationTarget.TYPE" />
<DrawnAnnotation text="@ExcelDsl CellStyle.() -> Unit" label="Apache POI (Excel) `CellStyle` joins the DSL"  :geometry="{ label: { x: 0.6344, y: 0.6830 }, connector: { type: 'quadratic', start: { x: 0.4357, y: 0.5609 }, control: { x: 0.4238, y: 0.6029 }, end: { x: 0.4332, y: 0.6507 } } }"/>

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

<InlineCompilerError :line="5" text="column" message="cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary." style="--inline-compiler-error-message-size: 1.5rem">

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<TypeHint :line="3" receiver="CellStyle">

```kotlin
import presentation.support.marked.workbook

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    header {
      wrapText = true
      column("Customer") { it.customer }
    }
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>
</InlineCompilerError>
