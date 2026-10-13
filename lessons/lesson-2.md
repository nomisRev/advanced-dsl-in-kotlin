---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 2</div>

# Overloads by lambda return type

## Let the cell type pick the function

---

# `Any?` accepts anything

<DrawnAnnotation text="{ it }" label="Compiles, and writes `toString()` into the cell" color="red"  :geometry="{ label: { x: 0.5746, y: 0.4082 }, connector: { type: 'quadratic', start: { x: 0.3202, y: 0.3761 }, control: { x: 0.3341, y: 0.4025 }, end: { x: 0.3514, y: 0.4008 } } }"/>

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.marked.workbook

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("Hours") { it }
  }
}
```

</TypeHint>
</TypeHint>

```console
| Customer | Hours                                                       |
| Ada      | Invoice(customer=Ada, hours=12, rate=90, issuedOn=2026-09-01) |
```

---

# One overload per cell type

<DrawnAnnotation text="String" occurrence="2" />
<DrawnAnnotation text="Number" />
<DrawnAnnotation text="LocalDate" label="Every type a cell can hold" />

```kotlin no-compile
import java.time.LocalDate

class SheetBuilder<T> {
  fun column(name: String, value: (T) -> String)
  fun column(name: String, value: (T) -> Number)
  fun column(name: String, value: (T) -> LocalDate)
}
```

---

# The overload is chosen before the lambda

<InlineCompilerError :line="3" text="column" message="">
<InlineCompilerError :line="5" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(name: String, value: (Invoice) -> String)`\n`fun column(name: String, value: (Invoice) -> Number)`\n`fun column(name: String, value: (Invoice) -> LocalDate)`" style="--inline-compiler-error-message-size: 1.43rem">
<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.marked.workbook

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    
    column("Hours") { it }
  }
}
```

</TypeHint>
</TypeHint>
</InlineCompilerError>
</InlineCompilerError>

<!--
The compiler needs a candidate to know the lambda's parameter types, and the lambda's
return type is the only difference between the candidates. So it gives up.
-->

---

# The lambda picks the overload

<DrawnAnnotation text="@OverloadResolutionByLambdaReturnType" label="Signals to compiler to analyse the lambda first, then choose overload" on="0" :geometry="{ label: { x: 0.6961, y: 0.2900, width: 0.5675 }, connector: { type: 'quadratic', start: { x: 0.4930, y: 0.4577 }, control: { x: 0.6056, y: 0.4634 }, end: { x: 0.6530, y: 0.3433 } } }"/>
<DrawnAnnotation text="@OverloadResolutionByLambdaReturnType" occurrence="2" on="0"/>
<DrawnAnnotation text="@OverloadResolutionByLambdaReturnType" occurrence="3" on="0"/>
<DrawnAnnotation text="@file:OptIn(ExperimentalTypeInference::class)" on="1" />

```kotlin no-compile
@file:OptIn(ExperimentalTypeInference::class)

import java.time.LocalDate
import kotlin.experimental.ExperimentalTypeInference

class SheetBuilder<T> {
  @OverloadResolutionByLambdaReturnType
  fun column(name: String, value: (T) -> String)
  @OverloadResolutionByLambdaReturnType
  fun column(name: String, value: (T) -> Number)
  @OverloadResolutionByLambdaReturnType
  fun column(name: String, value: (T) -> LocalDate)
}
```

---

# The JVM erases the lambda type

<InlineCompilerError :line="7" message="Platform declaration clash: The following declarations have the same JVM signature\n(column(Ljava/lang/String;Lkotlin/jvm/functions/Function1;)V)" >

```kotlin
class SheetBuilder<T> {
  @OverloadResolutionByLambdaReturnType
  fun column(
    name: String,
    value: (T) -> String,
    
  )
  
  @OverloadResolutionByLambdaReturnType
  fun column(name: String, value: (T) -> Number)
  @OverloadResolutionByLambdaReturnType
  fun column(name: String, value: (T) -> LocalDate)
}
```

</InlineCompilerError>

---
magic-move
---

# `@JvmName` gives each overload its own name

<DrawnAnnotation text="@JvmName(&quot;textColumn&quot;)" label="Only Java and the bytecode see this name"  :geometry="{ label: { x: 0.7009, y: 0.2800 }, connector: { type: 'quadratic', start: { x: 0.3285, y: 0.3166 }, control: { x: 0.4276, y: 0.3131 }, end: { x: 0.5262, y: 0.2584 } } }" :connect="false"/>
<DrawnAnnotation text="@JvmName(&quot;numberColumn&quot;)" />
<DrawnAnnotation text="@JvmName(&quot;dateColumn&quot;)" />

```kotlin no-compile
class SheetBuilder<T> {
  @OverloadResolutionByLambdaReturnType
  @JvmName("textColumn")
  fun column(name: String, value: (T) -> String)
  @OverloadResolutionByLambdaReturnType
  @JvmName("numberColumn")
  fun column(name: String, value: (T) -> Number)
  @OverloadResolutionByLambdaReturnType
  @JvmName("dateColumn")
  fun column(name: String, value: (T) -> LocalDate)
}
```

---

# Every column has a cell type

<DrawnAnnotation text="{ it.customer }" label="`String`"  :geometry="{ label: { x: 0.5305, y: 0.3001 }, connector: { type: 'quadratic', start: { x: 0.4603, y: 0.2636 }, control: { x: 0.4861, y: 0.2703 }, end: { x: 0.5028, y: 0.2439 } } }" :connect="false"/>
<DrawnAnnotation text="{ it.hours }" label="`Number`"  :geometry="{ label: { x: 0.5311, y: 0.3449 }, connector: { type: 'quadratic', start: { x: 0.3943, y: 0.3131 }, control: { x: 0.4503, y: 0.3218 }, end: { x: 0.5074, y: 0.2965 } } }" :connect="false"/>
<DrawnAnnotation text="{ it.issuedOn }" label="`LocalDate`"  :geometry="{ label: { x: 0.5467, y: 0.3976 }, connector: { type: 'quadratic', start: { x: 0.4379, y: 0.3606 }, control: { x: 0.4773, y: 0.3674 }, end: { x: 0.5167, y: 0.3493 } } }" :connect="false"/>
<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.typed.generateExcel
import presentation.support.typed.column

import presentation.support.marked.workbook

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("Hours") { it.hours }
    column("Issued") { it.issuedOn }
  }
}
```

</TypeHint>
</TypeHint>

---
magic-move
---

# Every column has a cell type

<InlineCompilerError :line="8" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(name: String, value: (Invoice) -> String)`\n`fun column(name: String, value: (Invoice) -> Number)`\n`fun column(name: String, value: (Invoice) -> LocalDate)`" style="--inline-compiler-error-message-size: 1.35rem">
<TypeHint :line="3" receiver="WorkbookBuilder">
<TypeHint :line="4" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.typed.generateExcel

fun <A> SheetBuilder<A>.column(name: String, block: (A) -> CellValue) = TODO()

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("Hours") { it.hours }
    column("Issued") { it.issuedOn }
    column("custom") { it.customCell }
  }  
}
```

</TypeHint>
</TypeHint>
</InlineCompilerError>

---
magic-move
---

# An extension with its own name adds a type

<DrawnAnnotation text="cellColumn" :line="1" />
<DrawnAnnotation text="formatted()" />
<TypeHint :line="4" receiver="WorkbookBuilder">
<TypeHint :line="5" receiver="SheetBuilder<Invoice>">

```kotlin no-compile
import presentation.support.typed.SheetBuilder
import presentation.support.typed.generateExcel
import presentation.support.typed.column

fun <A> SheetBuilder<Invoice>.cellColumn(name: String) =
  column(name) { it.customCell.formatted() }

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("Hours") { it.hours }
    column("Issued") { it.issuedOn }


    cellColumn("custom")
  }
}
```
```console
| Customer | Reference      |
| Ada      | ADA-2026-09-01 |
```

</TypeHint>
</TypeHint>

---
magic-move
---

# An extension groups the columns

<DrawnAnnotation text="fun SheetBuilder<Invoice>.invoice()" label="Named once, called in every sheet"  :geometry="{ label: { x: 0.6672, y: 0.5269 }, connector: { type: 'quadratic', start: { x: 0.4527, y: 0.4570 }, control: { x: 0.4872, y: 0.4588 }, end: { x: 0.4995, y: 0.5048 } } }"/>

```kotlin
import presentation.support.typed.SheetBuilder
import presentation.support.typed.generateExcel
import presentation.support.typed.column

generateExcel("invoices.xlsx", invoices) {
  invoice()
}

fun SheetBuilder<Invoice>.invoice() {
  column("Customer") { it.customer }
  column("Hours") { it.hours }
  column("Issued") { it.issuedOn }
}
```
