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

```kotlin
class SheetBuilder<T> private constructor() {
  internal fun <V> add(name: String, value: (T) -> ColumnSpec<V>): Column<V>
}

@OverloadResolutionByLambdaReturnType
fun <T> SheetBuilder<T>.column(name: String, value: (T) -> String) = add(...)

@OverloadResolutionByLambdaReturnType
fun <T> SheetBuilder<T>.column(name: String, value: (T) -> Number) = add(...)

@OverloadResolutionByLambdaReturnType
fun <T> SheetBuilder<T>.column(name: String, value: (T) -> LocalDate) = add(...)
```

---
magic-move
---

<DrawnAnnotation text="builder"/>
<DrawnAnnotation text="builder" occurrence="2" label="Explicitly reference context parameter" :geometry="{ label: { x: 0.7584, y: 0.4687 } }"/>

```kotlin
class SheetBuilder<T> private constructor() {
  internal fun <V> add(name: String, value: (T) -> ColumnSpec<V>): Column<V>
}

@OverloadResolutionByLambdaReturnType
context(builder: SheetBuilder<T>)
fun <T> column(name: String, value: (T) -> String) = builder.add(...)

@OverloadResolutionByLambdaReturnType
context(builder: SheetBuilder<T>)
fun <T> SheetBuilder<T>.column(name: String, value: (T) -> Number) = builder.add(...)

@OverloadResolutionByLambdaReturnType
context(builder: SheetBuilder<T>)
fun <T> SheetBuilder<T>.column(name: String, value: (T) -> LocalDate) = builder.add(...)
```

---


<TypeHint :line="4" receiver="WorkbookBuilder">
<TypeHint :line="5" receiver="SheetBuilder<Invoice>">

```kotlin show-imports
import excel.dsl.*
import my.app.dsl.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("custom") { it.customCell }
  }
}
```

</TypeHint>
</TypeHint>

```kotlin
package my.app.dsl

@OverloadResolutionByLambdaReturnType
context(builder: SheetBuilder<A>)
fun <A> column(name: String, transform: (A) -> CellValue)
```

---

