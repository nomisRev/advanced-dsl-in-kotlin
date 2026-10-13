---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 2</div>

# Context parameters

## Let users extend the DSL

---

# Every column has a cell type

<InlineCompilerError :line="6" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(name: String, value: (Invoice) -> String)`\n`fun column(name: String, value: (Invoice) -> Number)`\n`fun column(name: String, value: (Invoice) -> LocalDate)`" style="--inline-compiler-error-message-size: 1.35rem">
<TypeHint :line="3" receiver="WorkbookBuilder">
<TypeHint :line="4" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.typed.generateExcel

fun <A> SheetBuilder<A>.column(name: String, block: (A) -> CellValue) = TODO()

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("custom") { it.customCell }
  }  
}
```

</TypeHint>
</TypeHint>
</InlineCompilerError>

---

# Every column has a cell type

<InlineCompilerError :line="6" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(name: String, value: (Invoice) -> String)`\n`fun column(name: String, value: (Invoice) -> Number)`\n`fun column(name: String, value: (Invoice) -> LocalDate)`" style="--inline-compiler-error-message-size: 1.35rem">
<TypeHint :line="3" receiver="WorkbookBuilder">
<TypeHint :line="4" receiver="SheetBuilder<Invoice>">
<DrawnAnnotation text="column" :occurrence="4" label="Class method has highest resolution priority">

```kotlin
import presentation.support.typed.generateExcel

fun <A> SheetBuilder<A>.column(name: String, block: (A) -> CellValue) = TODO()

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("custom") { it.customCell }
  }  
}

class SheetBuilder<T> {
  @OverloadResolutionByLambdaReturnType
  @JvmName("textColumn")
  fun column(name: String, value: (T) -> String)
}
```

</DrawnAnnotation>
</TypeHint>
</TypeHint>
</InlineCompilerError>

---

# Every column has a cell type

```kotlin
class SheetBuilder<T> {
  fun <V> add(
    name: String,
    value: (T) -> ColumnSpec<V>
  ): Column<V>
}
```


---
magic-move
---

# Every column has a cell type

```kotlin
class SheetBuilder<T> {
  fun <V> add(
    name: String,
    value: (T) -> ColumnSpec<V>
  ): Column<V>
}

@OverloadResolutionByLambdaReturnType
context(builder: SheetBuilder<T>)
fun <T> column(name: String, value: (T) -> String) =
  builder.add(name) { ColumnSpec.string(value(it)) }
```

---

# Every column has a cell type

<InlineCompilerError :line="6" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(name: String, value: (Invoice) -> String)`\n`fun column(name: String, value: (Invoice) -> Number)`\n`fun column(name: String, value: (Invoice) -> LocalDate)`" style="--inline-compiler-error-message-size: 1.35rem">
<TypeHint :line="3" receiver="WorkbookBuilder">
<TypeHint :line="4" receiver="SheetBuilder<Invoice>">

```kotlin
package my.application

import excel.dsl.*

context(builder: SheetBuilder<A>)
fun <A> column(name: String, block: (A) -> CellValue) = TODO()

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("custom") { it.customCell }
  }  
}
```

</TypeHint>
</TypeHint>
</InlineCompilerError>

---