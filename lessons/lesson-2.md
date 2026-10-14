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

```kotlin no-compile
class FormulaScope internal constructor() {
  internal fun times(first: Column<Number>, second: Column<Number>): Expr
}
```

---
magic-move
---

```kotlin no-compile
class FormulaScope internal constructor() {
  internal fun times(first: Column<Number>, second: Column<Number>): Expr
}

context(formulas: FormulaScope)
operator fun Column<Number>.times(second: Column<Number>): Expr =
  formulas.times(this, second)
```

---

# The total is a number, not a formula

<TypeHint :line="1" receiver="WorkbookBuilder">
<TypeHint :line="2" receiver="SheetBuilder<Invoice>">
<TypeHint :line="6" receiver="FormulaScope">

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

# The total is a number, not a formula

<TypeHint :line="1" context="WorkbookBuilder">
<TypeHint :line="2" context="SheetBuilder<Invoice>">
<TypeHint :line="6" context="FormulaScope">

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

# The total is a number, not a formula

<TypeHint :line="1" context="WorkbookBuilder">
<TypeHint :line="2" context="SheetBuilder<Invoice>">
<TypeHint :line="6" context="FormulaScope">
<InlineCompilerError text="times" message="TODO">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    formula("Total") {
      times(hours, rate)
    }
  }
}
```

</InlineCompilerError>
</TypeHint>
</TypeHint>
</TypeHint>

```console
| Customer | Hours | Rate | Total  |
| Ada      | 12    | 90   | =B2*C2 |
```

---

# The total is a number, not a formula

<TypeHint :line="1" context="WorkbookBuilder">
<TypeHint :line="2" context="SheetBuilder<Invoice>">
<TypeHint :line="6" context="FormulaScope">
<DrawnAnnotation text="contextOf<FormulaScope>().times" label="Explicitly get instance of `context(FormulaScope)`, and call a method on it"  :geometry="{ label: { x: 0.5107, y: 0.5793 } }"/>

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    formula("Total") {
      contextOf<FormulaScope>().times(hours, rate)
    }
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

- Receiver can resolve context
- Context cannot be used as receiver, or `this`

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

val ctx: context(CoroutineScope) () -> Job = {
  val scope = contextOf<CoroutineScope>()
  scope.launch { }
}

val receiver: CoroutineScope.() -> Job = {
  val scope = this
  launch { }
}
```

---

# Fluent DSL vs Mutable DSL

```kotlin no-compile
fun formula(block: context(FormulaScope) () -> Expr): Formula

fun worksheet(block: context(WorkbookBuilder) () -> Unit): Workbook
```

---

<TypeHint :line="1" context="WorkbookBuilder">
<TypeHint :line="2" context="SheetBuilder<Invoice>">
<TypeHint :line="6" context="FormulaScope">

```kotlin
import presentation.support.trees.*

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    column("Customer") { it.customer }
    val hours = column("Hours") { it.hours }
    val rate = column("Rate") { it.rate }
    formula("Total") { hours * rate * 1.21 }
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

```console
| Customer | Hours | Rate | Total  |
| Ada      | 12    | 90   | =B2*C2*1.21 |
```

---

# Formulas build a tree

<DrawnAnnotation text="sealed interface Expr" label="The formula as data"  :geometry="{ label: { x: 0.4684, y: 0.1929 }, connector: { type: 'quadratic', start: { x: 0.2919, y: 0.2222 }, control: { x: 0.3385, y: 0.2275 }, end: { x: 0.3712, y: 0.2032 } } }"/>

```kotlin
import presentation.support.expressions.Formulas

sealed interface Expr {
  data class Ref(val column: Column<Number>) : Expr
  data class Constant(val value: Double) : Expr
  data class Times(val left: Expr, val right: Expr) : Expr
}
```

---

# Formulas build a tree

<DrawnAnnotation text="context(_: FormulaScope)" :line="7" label="`_:` nothing reads it, it requires `formula { }` to call it" on="0" :geometry="{ label: { x: 0.6449, y: 0.4447 }, connector: { type: 'quadratic', start: { x: 0.3271, y: 0.5048 }, control: { x: 0.3700, y: 0.5085 }, end: { x: 0.3881, y: 0.4748 } } }" />
<DrawnAnnotation text="Times(Ref(this), Ref(other))" />

```kotlin
import presentation.support.trees.FormulaScope

import presentation.support.expressions.Formulas

sealed interface Expr {
  data class Ref(val column: Column<Number>) : Expr
  data class Constant(val value: Double) : Expr
  data class Times(val left: Expr, val right: Expr) : Expr
}

context(_: FormulaScope)
operator fun Column<Number>.times(other: Column<Number>): Expr =
  Expr.Times(Expr.Ref(this), Expr.Ref(other))
```

---

# Formulas build a tree

<DrawnAnnotation text="FormulaScope" occurrence="2" label="Purely a marker type for scope control, has no functionality"  :geometry="{ label: { x: 0.4037, y: 0.8100 }, connector: { type: 'quadratic', start: { x: 0.2229, y: 0.7491 }, control: { x: 0.2239, y: 0.7680 }, end: { x: 0.2229, y: 0.7912 } } }"/>

```kotlin
import presentation.support.expressions.Formulas

sealed interface Expr {
  data class Ref(val column: Column<Number>) : Expr
  data class Constant(val value: Double) : Expr
  data class Times(val left: Expr, val right: Expr) : Expr
}

context(_: FormulaScope)
operator fun Column<Number>.times(other: Column<Number>): Expr =
  Expr.Times(Expr.Ref(this), Expr.Ref(other))

@ExcelDsl
class FormulaScope internal constructor()
```

---

# Formulas build a tree

```kotlin
sealed interface Expr<out T> {
  data class Ref<T>(val column: Column<T>) : Expr<T>
  data class Times(val left: Expr<Number>, val right: Expr<Number>) : Expr<Double>
  data class In<T>(val target: Expr<T>, val values: Iterable<T>) : Expr<Boolean>
  data class Contains(val target: Expr<String>, val substring: String) : Expr<Boolean>
}
```

---

# Formulas build a tree

<InlineCompilerError text="operator" message="`operator` modifier is not applicable to function: must return `Boolean`.">

```kotlin
import presentation.support.trees.FormulaScope

sealed interface Expr<out T> {
  data class Ref<T>(val column: Column<T>) : Expr<T>
  data class Times(val left: Expr<Number>, val right: Expr<Number>) : Expr<Double>
  data class In<T>(val target: Expr<T>, val values: Iterable<T>) : Expr<Boolean>
  data class Contains(val target: Expr<String>, val substring: String) : Expr<Boolean>
}

context(_: FormulaScope)
operator fun Column<String>.contains(other: String): Expr<Boolean> =
  Expr.Contains(Expr.Ref(this), other)
```

</InlineCompilerError>

---

# Formulas build a tree

```kotlin
import presentation.support.trees.FormulaScope

sealed interface Expr<out T> {
  data class Ref<T>(val column: Column<T>) : Expr<T>
  data class Times(val left: Expr<Number>, val right: Expr<Number>) : Expr<Double>
  data class In<T>(val target: Expr<T>, val values: Iterable<T>) : Expr<Boolean>
  data class Contains(val target: Expr<String>, val substring: String) : Expr<Boolean>
}

context(_: FormulaScope)
infix fun Column<String>.contains(other: String): Expr<Boolean> =
  Expr.Contains(Expr.Ref(this), other)
```

---

# Formulas build a tree

```kotlin
import presentation.support.trees.FormulaScope

sealed interface Expr<out T> {
  data class Ref<T>(val column: Column<T>) : Expr<T>
  data class Times(val left: Expr<Number>, val right: Expr<Number>) : Expr<Double>
  data class In<T>(val target: Expr<T>, val values: Iterable<T>) : Expr<Boolean>
  data class Contains(val target: Expr<String>, val substring: String) : Expr<Boolean>
}

context(_: FormulaScope)
infix fun Column<String>.contains(other: String): Expr<Boolean> =
  Expr.Contains(Expr.Ref(this), other)

context(_: FormulaScope)
infix fun <A> Iterable<A>.contains(other: Column<A>): Expr<Boolean> =
  Expr.In(Expr.Ref(other), this)
```

---

# Formulas build a tree

```kotlin
import presentation.support.trees.*

context(_: FormulaScope)
infix fun Column<String>.contains(other: String): Expr<Boolean> =
  Expr.Contains(Expr.Ref(this), other)

context(_: FormulaScope)
infix fun <A> Iterable<A>.contains(other: Column<A>): Expr<Boolean> =
  Expr.In(Expr.Ref(other), this)

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    val customer = column("Customer") { it.customer }
    formula("GOV") { customer contains "-GOV" }
  }
}
```

---

# Formulas build a tree

<DrawnAnnotation text="[1, 2] contains company" label="Infers `Any` for `A`" color="red"/>

```kotlin
import presentation.support.trees.*

context(_: FormulaScope)
infix fun Column<String>.contains(other: String): Expr<Boolean> =
  Expr.Contains(Expr.Ref(this), other)

context(_: FormulaScope)
infix fun <A> Iterable<A>.contains(other: Column<A>): Expr<Boolean> =
  Expr.In(Expr.Ref(other), this)

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    val company = column("Customer") { it.customer }
    formula("GOV") { [1, 2] contains company }
  }
}
```

---

# Formulas build a tree

<InlineCompilerError text="contains" :line="12" message="Type inference failed. The value of the type parameter 'A' must be mentioned in input types (argument types, receiver type, or expected type). Try to specify it explicitly.">

```kotlin
import presentation.support.trees.*

context(_: FormulaScope)
infix fun Column<String>.contains(other: String): Expr<Boolean> =
  Expr.Contains(Expr.Ref(this), other)

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "ERROR_SUPPRESSION")
context(_: FormulaScope)
infix fun <@kotlin.internal.OnlyInputTypes A> Iterable<A>.contains(other: Column<A>): Expr<Boolean> =
  Expr.In(Expr.Ref(other), this)

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    val company = column("Customer") { it.customer }
    formula("GOV") { [1, 2] contains company }
  }
}
```

</InlineCompilerError>

---

# Formulas build a tree

```kotlin
import presentation.support.trees.*

context(_: FormulaScope)
infix fun Column<String>.contains(other: String): Expr<Boolean> =
  Expr.Contains(Expr.Ref(this), other)

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "ERROR_SUPPRESSION")
context(_: FormulaScope)
infix fun <@kotlin.internal.OnlyInputTypes A> Iterable<A>.contains(other: Column<A>): Expr<Boolean> =
  Expr.In(Expr.Ref(other), this)

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    val company = column("Customer") { it.customer }
    formula("GOV") { ["VRT", "VUB"] contains company }
  }
}
```

---

# Formulas build a tree

<InlineCompilerError text="contains" occurrence="2" message="Type inference failed. The value of the type parameter A must be mentioned in input types.">

```kotlin
import presentation.support.trees.*

context(_: FormulaScope)
infix fun Column<String>.contains(other: String): Expr<Boolean> =
  Expr.Contains(Expr.Ref(this), other)

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "ERROR_SUPPRESSION")
context(_: FormulaScope)
infix fun <@kotlin.internal.OnlyInputTypes A> Iterable<A>.contains(other: Column<A>): Expr<Boolean> =
  Expr.In(Expr.Ref(other), this)

workbook("invoices.xlsx") {
  sheet("Q1", q1) {
    val company = column("Customer") { it.customer }
    formula("GOV") { [1, 2] contains company }
  }
}
```

</InlineCompilerError>

---