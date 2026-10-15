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
<DrawnAnnotation text="{ it.hours }" label="`Number`"  :geometry="{ label: { x: 0.5315, y: 0.3517 }, connector: { type: 'quadratic', start: { x: 0.3943, y: 0.3131 }, control: { x: 0.4503, y: 0.3218 }, end: { x: 0.5074, y: 0.2965 } } }" :connect="false"/>
<DrawnAnnotation text="{ it.issuedOn }" label="`LocalDate`"  :geometry="{ label: { x: 0.5467, y: 0.3976 }, connector: { type: 'quadratic', start: { x: 0.4379, y: 0.3606 }, control: { x: 0.4773, y: 0.3674 }, end: { x: 0.5167, y: 0.3493 } } }" :connect="false"/>
<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.typed.workbook
import presentation.support.typed.column

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

# An extension groups the columns

<DrawnAnnotation text="fun SheetBuilder<Invoice>.invoice()" label="Named once, called in every sheet"  :geometry="{ label: { x: 0.6672, y: 0.5269 }, connector: { type: 'quadratic', start: { x: 0.4527, y: 0.4570 }, control: { x: 0.4872, y: 0.4588 }, end: { x: 0.4995, y: 0.5048 } } }"/>

```kotlin
import presentation.support.typed.SheetBuilder
import presentation.support.typed.workbook
import presentation.support.typed.column

workbook("invoices.xlsx") {
  sheet("Q1", q1) { invoice() }
  sheet("Q2", q2) { invoice() }
}

fun SheetBuilder<Invoice>.invoice() {
  column("Customer") { it.customer }
  column("Hours") { it.hours }
  column("Issued") { it.issuedOn }
}
```

---
magic-move
---

# Every column has a cell type

<InlineCompilerError :line="6" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(name: String, value: (Invoice) -> String)`\n`fun column(name: String, value: (Invoice) -> Number)`\n`fun column(name: String, value: (Invoice) -> LocalDate)`" style="--inline-compiler-error-message-size: 1.35rem">
<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.typed.workbook

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("Hours") { it.hours }
    column("Issued") { it.issuedOn }
    column("custom") { it.customCell }
  }  
}

@OverloadResolutionByLambdaReturnType
fun <A> SheetBuilder<A>.column(name: String, transform: (A) -> CellValue): Column<String> = TODO()
```

</TypeHint>
</TypeHint>
</InlineCompilerError>

---
magic-move
---

# Every column has a cell type

<InlineCompilerError :line="8" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(name: String, value: (Invoice) -> String)`\n`fun column(name: String, value: (Invoice) -> Number)`\n`fun column(name: String, value: (Invoice) -> LocalDate)`" style="--inline-compiler-error-message-size: 1.35rem">
<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<DrawnAnnotation text="this@sheet.column" />
<DrawnAnnotation text="this@sheet.column" :occurrence="2" />
<DrawnAnnotation text="this@sheet.column" :occurrence="3" label="Class method always resolve over extensions"  :geometry="{ label: { x: 0.2716, y: 0.4633 } }"/>

```kotlin
import presentation.support.typed.workbook

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    this@sheet.column("Customer") { it.customer }
    this@sheet.column("Hours") { it.hours }
    this@sheet.column("Issued") { it.issuedOn }
    
    
    column("custom") { it.customCell }
  }  
}

@OverloadResolutionByLambdaReturnType
fun <A> SheetBuilder<A>.column(name: String, transform: (A) -> CellValue): Column<String> = TODO()
```

</TypeHint>
</TypeHint>
</InlineCompilerError>

---
magic-move
---

# An extension with its own name adds a type

<DrawnAnnotation text="cellColumn" label="Disambiguate using function name"  :geometry="{ label: { x: 0.3719, y: 0.5203 }, connector: { type: 'quadratic', start: { x: 0.1774, y: 0.4697 }, control: { x: 0.1759, y: 0.5044 }, end: { x: 0.2005, y: 0.5166 } } }"/>
<DrawnAnnotation text="cellColumn" :occurrence="2" />
<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

```kotlin no-compile
import presentation.support.typed.SheetBuilder
import presentation.support.typed.workbook
import presentation.support.typed.column

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("Hours") { it.hours }
    column("Issued") { it.issuedOn }
    cellColumn("custom") { it.customCell }
  }
}

@OverloadResolutionByLambdaReturnType
fun <A> SheetBuilder<A>.cellColumn(name: String, transform: (A) -> CellValue): Column<String> =
  column(name) { transform(it).formatted() }
```

</TypeHint>
</TypeHint>

---
magic-move
---

```kotlin no-compile
class SheetBuilder<T> {
  fun <V> add(name: String, value: (T) -> ColumnSpec<V>): Column<V>
}
```

---
magic-move
---

<DrawnAnnotation text="internal" />
<DrawnAnnotation text="internal" occurrence="2" label="Lock things down; easier to evolve."  :geometry="{ label: { x: 0.3491, y: 0.2754 }, connector: { type: 'quadratic', start: { x: 0.1444, y: 0.1931 }, control: { x: 0.1484, y: 0.2454 }, end: { x: 0.1756, y: 0.2737 } } }"/>

```kotlin no-compile
class SheetBuilder<T> internal constructor() {
  internal fun <V> add(name: String, value: (T) -> ColumnSpec<V>): Column<V>
}
```

---
magic-move
---

```kotlin no-compile
package excel.dsl

class SheetBuilder<T> internal constructor() {
  internal fun <V> add(name: String, value: (T) -> ColumnSpec<V>): Column<V>
}

@OverloadResolutionByLambdaReturnType
fun <T> SheetBuilder<T>.column(name: String, value: (T) -> String): Column<String>

@OverloadResolutionByLambdaReturnType
fun <T> SheetBuilder<T>.column(name: String, value: (T) -> Number): Column<Number>

@OverloadResolutionByLambdaReturnType
fun <T> SheetBuilder<T>.column(name: String, value: (T) -> LocalDate): Column<LocalDate>
```

---
magic-move
---

<InlineCompilerError :line="6" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(name: String, value: (Invoice) -> String)`\n`fun column(name: String, value: (Invoice) -> Number)`\n`fun column(name: String, value: (Invoice) -> LocalDate)`" style="--inline-compiler-error-message-size: 1.35rem">
<TypeHint :line="3" receiver="WorkbookBuilder">
<TypeHint :line="4" receiver="SheetBuilder<Invoice>">

```kotlin show-imports
import excel.dsl.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("custom") { it.customCell }
  }
}

fun <A> SheetBuilder<A>.column(name: String, transform: (A) -> CellValue) =
  column(name) { transform(it).formatted() }
```

</TypeHint>
</TypeHint>
</InlineCompilerError>

---
magic-move
---

<TypeHint :line="4" receiver="WorkbookBuilder">
<TypeHint :line="5" receiver="SheetBuilder<Invoice>">

```kotlin show-imports no-compile
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

<DrawnAnnotation text="SheetBuilder" on="1" label="Lose our extension receiver to DSL" color="red" :geometry="{ label: { x: 0.3180, y: 0.8144 }, connector: { type: 'quadratic', start: { x: 0.2213, y: 0.7456 }, control: { x: 0.2399, y: 0.7637 }, end: { x: 0.2340, y: 0.7921 } } }"/>

```kotlin no-compile
package my.app.dsl

@OverloadResolutionByLambdaReturnType
fun <A> SheetBuilder<A>.column(name: String, transform: (A) -> CellValue)
```

---

# Context Parameters

```kotlin no-compile
package excel.dsl

class SheetBuilder<T> internal constructor() {
  fun <V> add(name: String, value: (T) -> ColumnSpec<V>): Column<V>
}

@OverloadResolutionByLambdaReturnType
context(builder: SheetBuilder<T>)
fun <T> column(name: String, value: (T) -> String) = builder.add(...)
```
```kotlin no-compile
package my.app.dsl

@OverloadResolutionByLambdaReturnType
context(builder: SheetBuilder<A>)
fun <A> column(name: String, transform: (A) -> CellValue) = builder.add(..)
```

---

# Context Parameters

<TypeHint :line="4" receiver="WorkbookBuilder">
<TypeHint :line="5" receiver="SheetBuilder<Invoice>">

```kotlin show-imports no-compile
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

---

# Context Parameters

<TypeHint :line="4" receiver="WorkbookBuilder">
<TypeHint :line="5" receiver="SheetBuilder<Invoice>">

```kotlin show-imports no-compile
import excel.dsl.*
import my.app.dsl.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("custom") { it.customCell }
    add(...)
  }
}
```

</TypeHint>
</TypeHint>

---

# Context Parameters

<TypeHint :line="4" context="WorkbookBuilder">
<TypeHint :line="5" context="SheetBuilder<Invoice>">
<InlineCompilerError text="add" message="Unresolved reference `add`" >

```kotlin show-imports no-compile
import excel.dsl.*
import my.app.dsl.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("custom") { it.customCell }
    add(...)
  }
}
```

</InlineCompilerError>
</TypeHint>
</TypeHint>

---

# Context Parameters

<TypeHint :line="4" context="WorkbookBuilder">
<TypeHint :line="5" context="SheetBuilder<Invoice>">
<DrawnAnnotation text="contextOf" label="Get instance of `context(FormulaBuilder)`" />

```kotlin show-imports no-compile
import excel.dsl.*
import my.app.dsl.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("custom") { it.customCell }
    contextOf<FormulaBuilder>().add(...)
  }
}
```

</TypeHint>
</TypeHint>

---

# The total is a number, not a formula

<DrawnAnnotation text="it.hours * it.rate" label="Computed in Kotlin, not really a formula" color="red"  :geometry="{ label: { x: 0.6688, y: 0.5045 }, connector: { type: 'quadratic', start: { x: 0.3916, y: 0.4796 }, control: { x: 0.4207, y: 0.5110 }, end: { x: 0.4737, y: 0.5025 } } }"/>
<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    column("Hours") { it.hours }
    column("Rate") { it.rate }
    column("Total") { it.hours * it.rate }
  }
}
```

</TypeHint>
</TypeHint>

<DrawnAnnotation text="1080" color="red" />

```console
| Customer | Hours | Rate | Total |
| Ada      | 12    | 90   | 1080  |
```

---
magic-move
---

# The total is a number, not a formula

<DrawnAnnotation text="val hours" />
<DrawnAnnotation text="val rate" label="Reference the actual columns"  :geometry="{ label: { x: 0.3015, y: 0.5300 }, connector: { type: 'quadratic', start: { x: 0.1626, y: 0.4156 }, control: { x: 0.1647, y: 0.4559 }, end: { x: 0.1638, y: 0.5042 } } }"/>
<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    column("Total") { it.hours * it.rate }
  }
}
```

</TypeHint>
</TypeHint>

```console
| Customer | Hours | Rate | Total  |
| Ada      | 12    | 90   | 1080  |
```

---
magic-move
---

# The total is a number, not a formula

<InlineCompilerError :line="7" text="*" message="Unresolved reference `times` for operator `*` on receiver of type `Column<Number>`">
<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    
    column("Total") { hours * rate }
  }
}
```

</TypeHint>
</TypeHint>
</InlineCompilerError>

```console
| Customer | Hours | Rate | Total  |
| Ada      | 12    | 90   | =B2*C2 |
```

---
magic-move
---

# The total is a number, not a formula

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<TypeHint :line="7" receiver="FormulaBuilder">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    
    formula("Total") { hours * rate }
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

```console
| Customer | Hours | Rate | Total  |
| Ada      | 12    | 90   | =B2*C2 |
```

---
magic-move
---

# The total is a number, not a formula

<InlineCompilerError :line="9" text="*" message="Unresolved reference `times` for operator `*` on receiver of type `Column<Number>`">
<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<TypeHint :line="7" receiver="FormulaBuilder">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    
    formula("Total") { hours * rate }

    hours * rate
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>
</InlineCompilerError>

```console
| Customer | Hours | Rate | Total  |
| Ada      | 12    | 90   | =B2*C2 |
```

---

# Mutable DSL vs Fluent DSL

<DrawnAnnotation text="Unit" on="0" label="No output 'mutates' `SheetBuilder`"  :geometry="{ label: { x: 0.6574, y: 0.3204 } }"/>
<DrawnAnnotation text="Formula" on="1" occurrence="2" label="Produces or computes a `Formula`" />

```kotlin no-compile
context(builder: WorkbookBuilder)
fun <A> sheet(block: context(SheetBuilder<A>) () -> Unit): Sheet<A>

context(builder: SheetBuilder<*>)
fun formula(block: context(FormulaBuilder) () -> Formula): Formula
```

---

# Formulas build a tree

<DrawnAnnotation text="sealed interface Formula" label="The formula as data"  :geometry="{ label: { x: 0.5033, y: 0.1930 }, connector: { type: 'quadratic', start: { x: 0.3258, y: 0.2230 }, control: { x: 0.3724, y: 0.2283 }, end: { x: 0.4051, y: 0.2040 } } }"/>

```kotlin
import presentation.support.expressions.Formulas

sealed interface Formula {
  data class Reference(val column: Column<Number>) : Formula
  data class Times(val left: Formula, val right: Formula) : Formula
}
```

---
magic-move
---

# Formulas build a tree

```kotlin
import presentation.support.expressions.Formulas

sealed interface Formula {
  data class Reference(val column: Column<Number>) : Formula
  data class Times(val left: Formula, val right: Formula) : Formula
}

operator fun Column<Number>.times(other: Column<Number>): Formula =
  Formula.Times(Formula.Reference(this), Formula.Reference(other))
```

---
magic-move
---

# Formulas build a tree

```kotlin
import presentation.support.expressions.Formulas

sealed interface Formula {
  data class Reference(val column: Column<Number>) : Formula
  data class Times(val left: Formula, val right: Formula) : Formula
}

context(_: FormulaBuilder)
operator fun Column<Number>.times(other: Column<Number>): Formula =
  Formula.Times(Formula.Reference(this), Formula.Reference(other))

class FormulaBuilder internal constructor()
```

---
magic-move
---

# Formulas build a tree

<DrawnAnnotation text="internal" />
<DrawnAnnotation text="internal" occurrence="2" label="Force users through the DSL"  :geometry="{ label: { x: 0.3404, y: 0.3595 } }"/>

```kotlin
import presentation.support.expressions.Formulas

sealed interface Formula {
  data class Reference internal constructor(val column: Column<Number>) : Formula
  data class Times internal constructor(val left: Formula, val right: Formula) : Formula
}

context(_: FormulaBuilder)
operator fun Column<Number>.times(other: Column<Number>): Formula =
  Formula.Times(Formula.Reference(this), Formula.Reference(other))

class FormulaBuilder internal constructor()
```

---

# The total is a number, not a formula

<TypeHint :line="1" context="WorkbookBuilder">
<TypeHint :line="2" context="SheetBuilder<Invoice>">
<TypeHint :line="6" context="FormulaBuilder">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    formula("Total") { hours * rate }
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

```console
| Customer | Hours | Rate | Total  |
| Ada      | 12    | 90   | =B2*C2 |
```

---

# Operator with required return types

<TypeHint :line="1" context="WorkbookBuilder">
<TypeHint :line="2" context="SheetBuilder<Invoice>">
<TypeHint :line="6" context="FormulaBuilder">
<TypeHint :line="7" context="FormulaBuilder">
<InlineCompilerError text="<" message="Unresolved reference `compareTo` on receiver of type `Column<Long>`">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    formula("Total") { hours * rate }
    formula("Full Day") { hours < 8.hours } 
  }
}
```

</InlineCompilerError>
</TypeHint>
</TypeHint>
</TypeHint>
</TypeHint>

```console
| Customer | Hours | Rate | Total  | Under 8 hours |
| Ada      | 12    | 90   | =B2*C2 | =B2<8.0       |
```

---

# Operator with required return types

<TypeHint :line="1" context="WorkbookBuilder">
<TypeHint :line="2" context="SheetBuilder<Invoice>">
<TypeHint :line="6" context="FormulaBuilder">
<TypeHint :line="7" context="FormulaBuilder">
<InlineCompilerError text="<" message="Unresolved reference `compareTo` non receiver of type `Column<Long>`" style="--inline-compiler-error-message-size: 1.5rem">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    formula("Total") { hours * rate }
    formula("Full Day") { hours < 8.hours } 
  }
}
```

</InlineCompilerError>
</TypeHint>
</TypeHint>
</TypeHint>
</TypeHint>

```console
| Customer | Hours | Rate | Total  | Under 8 hours |
| Ada      | 12    | 90   | =B2*C2 | =B2<8.0       |
```

---

# Operator with required return types

<TypeHint :line="1" context="WorkbookBuilder">
<TypeHint :line="2" context="SheetBuilder<Invoice>">
<TypeHint :line="6" context="FormulaBuilder">
<TypeHint :line="7" context="FormulaBuilder">
<InlineCompilerError text="<" message="Unresolved reference `compareTo`\non receiver of type `Column<Long>`" style="--inline-compiler-error-message-size: 1.5rem">
<InlineCompilerError text="operator" message="`operator` modifier is not applicable to function: must return `Int`">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    formula("Total") { hours * rate }
    formula("Full Day") { hours < 8.hours } 
  }
}

context(_: FormulaBuilder)
operator fun Column<Long>
  .compareTo(other: Column<Long>): Formula = TODO()
```

</InlineCompilerError>
</InlineCompilerError>
</TypeHint>
</TypeHint>
</TypeHint>
</TypeHint>

```console
| Customer | Hours | Rate | Total  | Under 8 hours |
| Ada      | 12    | 90   | =B2*C2 | =B2<8.0       |
```

---

# Operator with required return types

<TypeHint :line="1" context="WorkbookBuilder">
<TypeHint :line="2" context="SheetBuilder<Invoice>">
<TypeHint :line="6" context="FormulaBuilder">
<TypeHint :line="7" context="FormulaBuilder">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    formula("Total") { hours * rate }
    formula("Under 8 hours") { hours lessThan 8 }
  }
}

context(_: FormulaBuilder)
infix fun Column<Number>.lessThan(other: Number): Formula<Boolean> =
  Formula.LessThan(Formula.Ref(this), Formula.Constant(other.toDouble()))
```

</TypeHint>
</TypeHint>
</TypeHint>
</TypeHint>

```console
| Customer | Hours | Rate | Total  | Under 8 hours |
| Ada      | 12    | 90   | =B2*C2 | =B2<8.0       |
```

---

# Operators with strict return type

 - `operator fun <A> A.compareTo(other: A): Int`
   - `<`, `>`, `<=`, `=>`, `==`, `!==`
 - `operator fun <A> A.contains(other: A): Boolean`
   - `in`
 
---

# Formulas build a tree

<DrawnAnnotation text="<out T>" label="Cover different types of formulas"  :geometry="{ label: { x: 0.6306, y: 0.1292 }, connector: { type: 'quadratic', start: { x: 0.4010, y: 0.2208 }, control: { x: 0.4515, y: 0.2230 }, end: { x: 0.4791, y: 0.1502 } } }"/>

```kotlin
import presentation.support.expressions.Formulas

sealed interface Formula<out T> {
  data class Reference<T>(val column: Column<T>) : Formula<T>
  data class Times(val left: Formula<Number>, val right: Formula<Number>) : Formula<Double>
  data class Contains(val target: Formula<String>, val substring: String) : Formula<Boolean>
  data class Has<T>(val target: Formula<T>, val values: Iterable<T>) : Formula<Boolean>
}
```

---

# Formulas build a tree

```kotlin
import presentation.support.trees.FormulaBuilder

sealed interface Formula<out T> {
  data class Reference<T>(val column: Column<T>) : Formula<T>
  data class Times(val left: Formula<Number>, val right: Formula<Number>) : Formula<Double>
  data class Contains(val target: Formula<String>, val substring: String) : Formula<Boolean>
  data class Has<T>(val target: Formula<T>, val values: Iterable<T>) : Formula<Boolean>
}

context(_: FormulaBuilder)
infix fun <A> Iterable<A>.contains(other: Column<A>): Formula<Boolean> =
  Formula.Has(Formula.Reference(other), this)
```

---

# Formulas build a tree

<DrawnAnnotation text="<A>" color="red"/>
<DrawnAnnotation text="<A>" occurrence="2" color="red"/>
<DrawnAnnotation text="<A>" occurrence="3" color="red"/>
<DrawnAnnotation text="[1, 2] contains customer" label="Infers `Any` for `A`" color="red"/>

```kotlin
import presentation.support.trees.*

context(_: FormulaBuilder)
infix fun <A> Iterable<A>.contains(other: Column<A>): Formula<Boolean> =
  Formula.In(Formula.Ref(other), this)

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    val customer = column("Customer") { it.customer }
    formula("one or two") { [1, 2] contains customer }
  }
}
```

---

# Formulas build a tree

<InlineCompilerError text="company" :line="11" message="Argument type mismatch: actual type is 'Column<String>', but 'Column<Int>' was expected.">

```kotlin
import presentation.support.trees.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "ERROR_SUPPRESSION")
context(_: FormulaBuilder)
infix fun <@kotlin.internal.Exact A> Iterable<A>.contains(
  other: Column<A>
): Formula<Boolean> = Formula.In(Formula.Ref(other), this)

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    val company = column("Customer") { it.customer }
    formula("one or two") {
      [1, 2] contains company
    }
  }
}
```

</InlineCompilerError>

---

# Formulas build a tree

```kotlin
import presentation.support.trees.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "ERROR_SUPPRESSION")
context(_: FormulaBuilder)
infix fun <@kotlin.internal.Exact A> Iterable<A>.contains(
  other: Column<A>
): Formula<Boolean> = Formula.In(Formula.Ref(other), this)

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    val company = column("Customer") { it.customer }
    formula("one or two") {
      ["one", "two"] contains company
    }
  }
}
```
