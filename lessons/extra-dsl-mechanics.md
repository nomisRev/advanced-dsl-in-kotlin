---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Extra</div>

# Advanced DSL mechanics

## Contracts, context resolution, and phase safety

<!--
For Simon. A systematic review of language mechanics and emerging Kotlin KEEPs
critical to explaining Kotlin DSLs thoroughly. Each slide includes a lede indicating
where it naturally fits in the main talk (Parts 0 through 6).
-->

---

# A non-inline builder cannot initialize a local val

> Part 0, after “SheetBuilder.() -> Unit brings members into scope”

<InlineCompilerError :line="6" text="total = 1000.0" message="Captured variable can't be assigned." style="--inline-compiler-error-message-size: 1.1rem">

```kotlin
import presentation.support.delegated.SheetBuilder
import presentation.support.Invoices

val total: Double
sheet<Invoice> {
  val hours by column { it.hours }
  total = 1000.0
}
```

</InlineCompilerError>

<!--
In Part 0, `block: SheetBuilder<T>.() -> Unit` brings members into scope.
When a builder is non-inline (for example, an interface member or virtual builder),
the compiler cannot tell whether or when the lambda runs. Assigning an outer `val`
inside the lambda fails compilation because an escaping lambda could invoke it zero,
one, or many times.
-->

---

# `callsInPlace` contracts guarantee in-place execution

> Part 0, after “A non-inline builder cannot initialize a local val”

<DrawnAnnotation text="callsInPlace" label="Guarantees the block runs before the function returns" />
<DrawnAnnotation text="EXACTLY_ONCE" label="Enables definite assignment of outer `val` variables" color="var(--fundamentals-blue)" />

```kotlin no-compile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <T> sheet(block: SheetBuilder<T>.() -> Unit): Sheet<T> {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  return SheetBuilder<T>().apply(block).build()
}
```

<!--
`contract { callsInPlace(block, EXACTLY_ONCE) }` informs compiler data-flow analysis
that the block runs synchronously exactly once before the function returns.
This enables definite assignment of local `val`s and allows smart casts from inside
the builder to persist after the builder finishes.
-->

---

# `local once` replaces the contract ceremony

> Experimental proposal KEEP-0464

<DrawnAnnotation text="local once" label="Declares in-place execution directly in the parameter signature" />

```kotlin no-compile
fun <T> sheet(
  local once block: SheetBuilder<T>.() -> Unit,
): Sheet<T> {
  val builder = SheetBuilder<T>()
  builder.block()
  return builder.build()
}
```

<!--
KEEP-0464 proposes `local` and `local once` modifiers on lambda parameters.
Contracts today are experimental, verbose, and cannot be attached to abstract or
interface methods. `local once` moves execution-frequency guarantees into the type
system, making non-inline DSL builders first-class citizens.
-->

---

# Row extractors escape and must not capture mutable state

> KEEP-0463: Warn on capture of mutable variables by escaping lambdas

<Warning :line="6" text="runningTotal" message="Capture of mutable variable 'runningTotal' in escaping lambda.">

```kotlin no-compile
var runningTotal = 0.0

sheet<Invoice> {
  val customer by column { it.customer }
  val total by column {
    runningTotal += it.amount
    runningTotal
  }
}
```

</Warning>

<!--
A DSL contains two distinct classes of lambdas: configuration lambdas (`local once`),
which execute synchronously during schema setup, and row extraction lambdas (`column { ... }`),
which escape and execute per-row when streaming data. Capturing a mutable `var` in an
escaping lambda introduces state bugs and race conditions. KEEP-0463 warns on capturing
mutable variables in escaping lambdas.
-->

---

# `@DslMarker` stops nesting, but not temporal ordering

> Part 1, after “@DslMarker restricts the scope”

<DrawnAnnotation text="totals {" label="Configures summary row after columns" />
<DrawnAnnotation text="val lateColumn" label="Adding a column after totals breaks row layout at runtime" color="red" />

```kotlin no-compile
sheet<Invoice> {
  val hours by column { it.hours }
  totals {
    sum(hours)
  }
  // Compiles, but throws IllegalStateException at runtime:
  val lateColumn by column { it.rate }
}
```

<!--
In Part 1, `@DslMarker` prevents spatial scope leakage: an inner block cannot access
an outer receiver. However, `@DslMarker` cannot enforce temporal ordering: calling
`totals` before columns, or appending columns after a sheet layout is finalized.
Basic DSLs fall back to runtime exceptions.
-->

---

# Scope swapping transitions the builder across phases

> Part 1, after “@DslMarker stops nesting, but not temporal ordering”

<DrawnAnnotation text="columns {" label="Phase 1: returns `ColumnsStage`" />
<DrawnAnnotation text="totals {" label="Phase 2: only available after `columns` completes" color="var(--fundamentals-blue)" />

```kotlin no-compile
sheet<Invoice> {
  columns {
    val hours by column { it.hours }
    val rate by column { it.rate }
  }.totals {
    sum(hours)
  }
}
```

<!--
By designing the DSL with phased receivers (scope swapping), methods transition the caller
to a new builder interface. In this structure, `totals` is not a member of the root sheet builder;
it is only exposed on the object returned by `columns`. You cannot add columns after `totals`
because `column` does not exist on `TotalsBuilder`.
-->

---

# Phantom type parameters verify state at compile time

> Part 1, after “Scope swapping transitions the builder across phases”

<DrawnAnnotation text="SheetBuilder<Phase.Configured>" label="State tracked in type parameter" />
<DrawnAnnotation text="fun SheetBuilder<Phase.Configured>.totals" label="Extension only exists when columns are registered" color="var(--fundamentals-blue)" />

```kotlin no-compile
sealed interface Phase {
  interface Empty : Phase
  interface Configured : Phase
}

class SheetBuilder<P : Phase>

fun <T> sheet(block: SheetBuilder<Phase.Empty>.() -> SheetBuilder<Phase.Configured>): Sheet<T>

fun <T> SheetBuilder<Phase.Configured>.totals(block: TotalsScope.() -> Unit)
```

<!--
When a flat builder syntax is preferred over nesting, phantom types track builder lifecycle
state directly in the type system. Extensions like `totals` or `write` can require
`SheetBuilder<Phase.Configured>`, converting out-of-order calls into immediate compile errors.
-->

---

# Enum qualifications clutter configuration calls

> Part 2, after “Every column has a cell type”

<DrawnAnnotation text="Alignment.RIGHT" label="Repetitive type name qualification" color="red" />
<DrawnAnnotation text="NumberFormat.CURRENCY" label="Noise across every column configuration" color="red" />

```kotlin no-compile
sheet<Invoice> {
  val hours by column(
    align = Alignment.RIGHT,
    format = NumberFormat.DECIMAL,
  ) { it.hours }

  val amount by column(
    align = Alignment.RIGHT,
    format = NumberFormat.CURRENCY,
  ) { it.amount }
}
```

<!--
In Part 2, columns gain types and formats. In real DSLs, options are enums or sealed types.
Writing `Alignment.RIGHT` or `NumberFormat.CURRENCY` on every column clutters the DSL.
Library authors often work around this by importing everything or polluting the builder
with constants (`val RIGHT = Alignment.RIGHT`), which clutters autocomplete.
-->

---

# Context-sensitive resolution infers the entry from the expected type

> Experimental in Kotlin 2.5, -Xcontext-sensitive-resolution

<DrawnAnnotation text="RIGHT" label="Resolved from expected `Alignment` without receiver pollution"  :geometry="{ label: { x: 0.4804, y: 0.4344, width: 0.4859 }, connector: { type: 'quadratic', start: { x: 0.2466, y: 0.4908 }, control: { x: 0.2924, y: 0.4875 }, end: { x: 0.3143, y: 0.4529 } } }"/>
<DrawnAnnotation text="CURRENCY" label="Resolved from expected `NumberFormat`" color="var(--fundamentals-blue)" />

```kotlin no-compile
sheet<Invoice> {
  val hours by column(
    align = RIGHT,
    format = DECIMAL,
  ) { it.hours }

  val amount by column(
    align = RIGHT,
    format = CURRENCY,
  ) { it.amount }
}
```

<!--
KEEP-0379 introduces context-sensitive resolution. When the parameter has a known type
such as `Alignment` or `NumberFormat`, the compiler automatically resolves unqualified names
against the target type's entries or companion members.
The DSL syntax stays clean without polluting the builder interface.
-->

---

# Delegated properties hide the underlying delegate wrapper

> Part 3, after “Columns become values we can reference”

<DrawnAnnotation text="val hours by column" label="`hours` evaluates to `Column<Number>`" />
<DrawnAnnotation text="_hours" label="Requires a private backing field to access delegate methods" color="red" />

```kotlin no-compile
class InvoiceReport : SheetDefinition<Invoice> {
  private val _hours = column { it.hours }
  val hours by _hours

  fun configure() {
    // Need access to the ColumnDelegate, not just the Column value:
    _hours.setHeader("Billed Hours")
  }
}
```

<!--
In Part 3, `val hours by column { it.hours }` uses property delegation to extract the property
name as the header. However, delegation hides the delegate instance itself. If a DSL author
or test needs to inspect the delegate wrapper (to access metadata, validation rules, or
lifecycle state), they are forced to declare a separate private backing field.
-->

---

# `::property.delegate` exposes the delegate without boilerplate

> Experimental proposal KEEP-0450

<DrawnAnnotation text="val hours by column" label="Natural single-declaration syntax" />
<DrawnAnnotation text="::hours.delegate" label="Direct typed access to `ColumnDelegate<Invoice, Int>`" color="var(--fundamentals-blue)" />

```kotlin no-compile
class InvoiceReport : SheetDefinition<Invoice> {
  val hours by column { it.hours }

  fun configure() {
    val delegate: ColumnDelegate<Invoice, Int> = ::hours.delegate
    delegate.setHeader("Billed Hours")
  }
}
```

<!--
KEEP-0450 introduces typed delegate access via `::property.delegate`. Within the declaring scope,
the compiler provides strongly-typed access to the backing delegate instance.
For DSLs that lean heavily on property delegates for AST nodes or column definitions,
this eliminates backing property boilerplate.
-->

---

# `compareTo` cannot build expression AST trees

> Part 4, after “Member extensions keep operators scoped”

<InlineCompilerError :line="2" text="operator" message="'operator' modifier is not applicable to function: must return 'Int'." style="--inline-compiler-error-message-size: 1.1rem">

```kotlin
import presentation.support.tricks.Expr
import presentation.support.tricks.Formulas
import presentation.support.Column

context(_: Formulas)
operator fun Column<Number>.compareTo(limit: Double): Expr =
  Expr.Greater(Expr.Ref(this), Expr.Constant(limit))
```

</InlineCompilerError>

<!--
In Part 4, member extensions allowed overloading `*` and `+` for `Column<Number>` to produce
formula expressions. But why didn't we overload `>` or `<`?
In Kotlin, `a > b` compiles to `a.compareTo(b) > 0`. The language hardcodes `compareTo`
to return `Int`, and `equals` to return `Boolean`. They cannot return an expression node `Expr<Boolean>`.
-->

---

# Infix functions are the standard pattern for DSL expression trees

> Part 4, after “compareTo cannot build expression AST trees”

<DrawnAnnotation text="infix fun" label="One-argument function called without dot or parentheses" />
<DrawnAnnotation text="hours gt 40.0" label="Builds `Expr.Greater` instead of evaluating a boolean" color="var(--fundamentals-blue)" />

```kotlin
import presentation.support.tricks.*

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val overtime by formula { hours gt 40.0 }
}
```

<!--
Because Kotlin's comparison operators cannot return arbitrary types, every Kotlin expression DSL
(such as Exposed, SQLDelight, or spreadsheet formula builders) uses `infix` functions (`gt`, `lt`, `eq`).
Infix is not a visual gimmick; it is an architectural necessity in Kotlin for building type-safe AST trees.
-->

---

# Implicit context resolution can be ambiguous or rigid

> Part 5, after “Context parameters let users extend the DSL”

<DrawnAnnotation text="context(excel: Formulas)" label="Overload 1 for Excel formulas" />
<DrawnAnnotation text="context(eval: Evaluator)" label="Overload 2 for in-memory evaluation" />
<DrawnAnnotation text="total.withVat()" label="Overload resolution ambiguity when both contexts are in scope" color="red" />

```kotlin no-compile
context(excel: Formulas)
fun Column<Number>.withVat(): Expr = this * 1.21

context(eval: Evaluator)
fun Column<Number>.withVat(): Double = eval.compute(this) * 1.21

context(excel: Formulas, eval: Evaluator)
fun generate() {
  total.withVat() // Ambiguity: which withVat() to call?
}
```

<!--
In Part 5, context parameters allowed third-party extensions like `total.withVat()`.
However, implicit resolution has a known weakness: when multiple contexts are present,
or two overloads accept different context types, the compiler reports a resolution ambiguity.
Previously, the only workaround was defining helper dispatch functions.
-->

---

# Named context arguments resolve ambiguity without nesting

> Experimental in Kotlin 2.4/2.5, KEEP-0448

<DrawnAnnotation text="excel = excel" label="Explicit context argument resolves ambiguity by name" />

```kotlin no-compile
context(excel: Formulas, eval: Evaluator)
fun generate() {
  // Explicitly binds the context parameter by name:
  val formula = total.withVat(excel = excel)

  val inMemory = total.withVat(eval = eval)
}
```

<!--
KEEP-0448 introduces explicit context arguments. Using named parameter syntax, callers
can pass context arguments directly to functions that declare context parameters.
This eliminates ambiguous call errors and avoids wrapping calls in unnecessary `with(...)` blocks.
-->

---

# Companion extensions provide extensible type-level presets

> Experimental in Kotlin 2.5, -Xcompanion-blocks-and-extensions

<DrawnAnnotation text="companion val" label="Extension on the companion of `ColumnFormat`" />
<DrawnAnnotation text="format = Currency" label="Resolves cleanly alongside context-sensitive resolution" color="var(--fundamentals-blue)" />

```kotlin no-compile
// In a third-party finance module:
companion val ColumnFormat.Currency: ColumnFormat
  get() = DecimalFormat("$#,##0.00")

sheet<Invoice> {
  val total by column(format = Currency) { it.amount }
}
```

<!--
In Part 5, we saw how users can extend the DSL with extension functions.
KEEP-0449 introduces `companion val` and companion blocks, enabling extensions on a class's
companion namespace even if the class didn't declare one.
Coupled with context-sensitive resolution, third-party libraries can provide presets
that feel like built-in language keywords.
-->

---

# `@JvmName` cannot evolve multiplatform library signatures

> Part 6, after “@JvmName disambiguates erased signatures”

<DrawnAnnotation text="@JvmName" label="Only applies to JVM bytecode, ignored in Kotlin multiplatform" color="red" />

```kotlin no-compile
interface FormulaEngine {
  @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
  fun calculate(col: Column<Number>): Number

  // Error in common KMP or open interface: @JvmName is not enough:
  @JvmName("calculateDouble")
  fun calculate(col: Column<Number>): Double
}
```

<!--
In Part 6, `@JvmName` combined with `@Deprecated(level = HIDDEN)` allowed evolving
a library function without breaking bytecode compatibility on the JVM.
However, in modern Kotlin Multiplatform (Native, JS, Wasm), `@JvmName` does nothing.
Furthermore, `@JvmName` cannot be used on open or interface members participating in Kotlin inheritance.
-->

---

# `@SignatureDiscriminator` provides multiplatform binary identity

> KEEP proposal PR #503: @SignatureDiscriminator

<DrawnAnnotation text="@SignatureDiscriminator" label="Assigns a distinct binary identity across all Kotlin targets" />

```kotlin no-compile
interface FormulaEngine {
  @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
  fun calculate(col: Column<Number>): Number

  @SignatureDiscriminator("calculateDouble")
  fun calculate(col: Column<Number>): Double
}
```

<!--
PR #503 introduces `@SignatureDiscriminator`. It provides an additional identity component
for Kotlin callables across all compiler backends (JVM, JS, Native, Wasm).
It allows library authors to evolve return types or parameter signatures while retaining
older declarations for binary backward compatibility, without relying on JVM-only annotations.
-->
