---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Extra</div>

# Still missing

## Resolution rules, the JVM surface, evolution, and the KEEP roadmap

<!--
For Simon. A second pass over Parts 0-6 and the six extras, looking for what a
DSL author still runs into and for what the Kotlin KEEP repository (open and
merged pull requests, as of 25 September 2026) changes. Nothing here repeats
the other extras: contracts, `local once`, var capture, phases and phantom
types, context-sensitive resolution, typed delegate access, explicit context
arguments, companion extensions and `@SignatureDiscriminator` are in "Advanced
DSL mechanics"; non-local return, variance, explicit backing fields, context
lambdas, `@SubclassOptInRequired`, default-argument ABI and ABI validation are
in "What the talk leaves out"; infix, `invoke`, and the toolbox table are in
"DSL tricks".

Every lede names the slide of the main deck it would follow. Diagnostics,
`javap` output and runtime output are verbatim from Kotlin 2.5.0-Beta1, this
repository's compiler, unless a note says otherwise. The last slide is the
placement table.
-->

---

# An inner extension beats an outer member

> Part 1, after “Nested scopes leak the outer receiver”

<DrawnAnnotation text="fun SheetBuilder.sheet" label="An extension on the inner receiver" />
<DrawnAnnotation text="fun sheet" :line="2" label="A member of the outer receiver" color="var(--fundamentals-blue)" />

```kotlin
class WorkbookBuilder {
  fun sheet(name: String): String = "WorkbookBuilder.sheet, a member"
}
class SheetBuilder

fun SheetBuilder.sheet(name: String): String =
  "SheetBuilder.sheet, an extension"

with(WorkbookBuilder()) {
  with(SheetBuilder()) {
    println(sheet("Q2"))
  }
}
```
```console
SheetBuilder.sheet, an extension
```

<!--
Resolution walks the implicit receivers from the innermost outwards, and at
each receiver it looks at members first, then at extensions on that receiver.
An extension on the closest receiver therefore beats a member of the one
around it. Part 1 shows the leak only because `SheetBuilder` has no `sheet` at
all; Part 6's "`ERROR` finds it, and forbids it" is this rule used on purpose:
the deprecated `SheetBuilder<*>.sheet` extension wins over
`WorkbookBuilder.sheet` before `@DslMarker` even gets a say.
-->

---

# How a name in a block is resolved

> Part 1, after “Implicit receivers stay in their own scope”

| Step | The compiler looks at | Shown in |
| --- | --- | --- |
| 1 | Locals of the block, then of the enclosing functions | Part 3, `val hours by column` |
| 2 | Each implicit receiver, innermost first: members, then extensions on it | “An inner extension beats an outer member” |
| 3 | Extensions by scope: explicit imports, the file's package, star imports, `kotlin.*` | “A more specific receiver wins before the lambda” |
| 4 | The first level with an applicable candidate wins, the most specific one if several | Part 6, “A fallback names the missing scope” |
| 5 | A `@DslMarker` receiver hides marked outer ones, and the search stops there | Part 1, “`@DslMarker` closes the scope” |
| 6 | Context parameters are filled after that, from the same scopes, and never break a tie | Part 5, “A receiver satisfies a context” |

<style>
.slidev-layout table { font-size: 1.35rem; }
td, th { padding-top: 0.4rem; padding-bottom: 0.4rem; }
</style>

<!--
The talk shows every rule in isolation; this is the one place they are lined
up. Sources: the Kotlin specification, "Call without an explicit receiver";
KEEP design note 0005 (DslMarker), "No further search"; KEEP-0367 §7.8,
context parameters play no role in choosing the most specific candidate.
Row 3 checked on 2.5.0-Beta1 with three `String.hello()` extensions: the
explicitly imported one wins over the one in the file's own package, which
wins over a star import.
-->

---

# `@JvmSynthetic` hides an overload from Java

> Part 2, after “`@JvmName` gives each overload its own name”

<DrawnAnnotation text="@JvmSynthetic" label="`javac` skips synthetic members: no `Function1`, no `Unit.INSTANCE`" />
<DrawnAnnotation text="Function<T, String>" label="The overload Java sees, a lambda there too" color="var(--fundamentals-blue)" />

```kotlin
import java.util.function.Function

class SheetBuilder<T> {
  @JvmSynthetic
  fun column(value: (T) -> String) { }

  fun column(name: String, value: Function<T, String>) { }
}
```
```console
$ javap -p -v SheetBuilder.class
public final void column(kotlin.jvm.functions.Function1);
  flags: (0x1011) ACC_PUBLIC, ACC_FINAL, ACC_SYNTHETIC
```

<!--
A receiver lambda is `Function1<SheetBuilder<T>, Unit>` in Java: the block
returns `Unit.INSTANCE`, `this` is a parameter, and `@OverloadResolutionByLambdaReturnType`
means nothing. `@JvmSynthetic` takes those members out of Java's view while
Kotlin callers are unaffected, and a `java.util.function` overload gives Java
its own entry. `@JvmOverloads` goes the other way, exposing default arguments
as Java overloads, and every one of them is a signature to keep (extra "What the
talk leaves out", the default-argument slides). `javap` output trimmed to the
synthetic method; the `Function` overload follows it with
`flags: (0x0011) ACC_PUBLIC, ACC_FINAL`.
-->

---

# `sheet { }` cannot infer the row type

> Extra “Sheets as values”, after “A sheet is a value”

<InlineCompilerError :line="1" text="sheet" message="Cannot infer type for type parameter 'T'. Specify it explicitly.">

```kotlin
import presentation.support.sheets.sheet

val invoiceSheet = sheet {
  val customer by column { it.customer }
}
```

</InlineCompilerError>

<!--
`generateExcel("invoices.xlsx", invoices)` had `invoices` to infer `T` from.
`sheet { }` has only the block, and lambdas are analysed after the candidate
is chosen, so `it.customer` cannot help. Three more errors cascade from this
one on 2.5.0-Beta1 (an ambiguity between the `column` overloads, and
"Unresolved reference 'customer' on receiver of type 'TypeVariable(T)'").
`sheet<Invoice> { }` is the only spelling that works, which is what the
extra already does everywhere.
-->

---
magic-move
---

# `explicit` makes the type argument part of the DSL

> KEEP-0469, public discussion

<DrawnAnnotation text="explicit" label="Never inferred, `sheet<Invoice>` is the only call" />

```kotlin no-compile
fun <explicit T> sheet(block: SheetBuilder<T>.() -> Unit): Sheet<T>

val invoiceSheet = sheet<Invoice> {
  val customer by column { it.customer }
}
```

<!--
The KEEP names DSLs as the second use case after casts: `single<Service> { }`
in Koin, `serializer<T>()`, `sheet<Invoice>`. Today the standard library gets
the same effect from `@kotlin.internal.NoInfer` on `filterIsInstance` and
`castAll`, which is internal. `explicit` also forbids `sheet<_> { }`.
-->

---

# `reified` reads the type argument at runtime

> Extra “Sheets as values”, after “A sheet is a value”

<DrawnAnnotation text="reified" label="Only in an `inline` function: `T` survives erasure" />
<DrawnAnnotation text="T::class.simpleName!!" label="The report is named after the row type" color="var(--fundamentals-blue)" />
<DrawnAnnotation text="noinline" label="`sheet` stores the block, so it cannot be inlined" />

```kotlin
import presentation.support.sheets.Sheet
import presentation.support.sheets.SheetBuilder
import presentation.support.sheets.sheet

class Report<T>(val name: String, val sheet: Sheet<T>)

inline fun <reified T> report(
  name: String = T::class.simpleName!!,
  noinline block: SheetBuilder<T>.() -> Unit,
): Report<T> = Report(name, sheet(block))

println(report<Invoice> { val customer by column { it.customer } }.name)
println(report<Invoice>("Q1") { val customer by column { it.customer } }.name)
```
```console
Invoice
Q1
```

<!--
Part 6 gives one reason to inline the entry point, the lambda object. This is
the second: with `reified` the DSL can use the type argument as a value, for
a default name, a serializer, a class to instantiate. Ktor's `call.receive<T>()`
and kotlinx.serialization's `serializer<T>()` are this. `T::class` in a default
argument of a reified inline function compiles on 2.5.0-Beta1.
-->

---

# One marker twice in a scope: nothing is implicit

> Part 5, after “A receiver satisfies a context”

<InlineCompilerError :line="9" text="column" message="'fun Sheet.column(name: String): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary." style="--inline-compiler-error-message-size: 1.1rem">
<InlineCompilerError :line="10" text="bold" message="'context(styles: Styles) fun bold(): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary." style="--inline-compiler-error-message-size: 1.1rem">

```kotlin
@ExcelDsl class Styles
@ExcelDsl class Sheet

context(styles: Styles) fun bold() { }
fun Sheet.column(name: String) { }

context(styles: Styles)
fun Sheet.report() {
  column("Customer")
  bold()
  this.column("Hours")
  context(styles) { bold() }
}
```

</InlineCompilerError>
</InlineCompilerError>

<!--
KEEP design note 0005, "No duplicate markers on the same scope": if two
implicit values carry the same marker in one scope, neither may be bound
implicitly, whichever one a call would pick. Before context parameters this
could not happen, receivers arrive one scope at a time. Now
`context(styles: Styles) fun Sheet.report()` declares two at once. The two
last lines of the body show the ways out: an explicit receiver is always
allowed, and `context(styles) { }` opens a closer scope in which `styles` is
the only marked value.

The nested form behaves like Part 1: inside `context(styles) { }` a marked
context hides the marked outer receiver, `column("Hours")` fails there and
`bold()` works (checked on 2.5.0-Beta1). For the deck: `excel-dsl` marks
both `SheetBuilder` and `Formulas`, so a helper declared as
`context(_: Formulas) fun SheetBuilder<T>.x()` can only be called with an
explicit receiver.
-->

---

# A public `inline` function can only call public API

> Part 6, after “`inline` removes the builder lambda”

<DrawnAnnotation text="internal fun save" label="Callers in other modules cannot see it" color="red" />
<InlineCompilerError :line="5" text="save" message="Public-API inline function cannot access non-public-API function.">

```kotlin
import presentation.support.delegated.SheetBuilder

internal fun save(path: String) { }

inline fun <T> sheet(path: String, block: SheetBuilder<T>.() -> Unit) {
  val builder = SheetBuilder<T>().apply(block)
  save(path)
}
```

</InlineCompilerError>

<!--
The body of an `inline` function is copied into every caller's class file, in
other modules too, so it may only reference what those callers can reference.
Every DSL entry point that is `inline` for Part 6's reason hits this the moment
the writer behind it is `internal`.
-->

---
magic-move
---

# `@PublishedApi` lets the inlined body call it

> Part 6, after “`inline` removes the builder lambda”

<DrawnAnnotation text="@PublishedApi" label="`internal` for Kotlin callers, public in the class file: ABI now" />

```kotlin
import presentation.support.delegated.SheetBuilder

@PublishedApi
internal fun save(path: String) { }

inline fun <T> sheet(path: String, block: SheetBuilder<T>.() -> Unit) {
  val builder = SheetBuilder<T>().apply(block)
  save(path)
}
```
```console
$ javap -p SheetKt.class
public static final void save(java.lang.String);
```

<!--
The support code of this deck does exactly this: `WorkbookSheets.write` and
`SheetColumns.toSheet` are `@PublishedApi internal`. The price is in the
`javap`: `save` is a public static method, part of what `checkKotlinAbi`
(extra "What the talk leaves out") dumps and guards, even though no Kotlin
source outside the module can name it.
-->

---

# `@RequiresOptIn` marks the part that may still change

> Part 6, after “Hide the old API, keep the binary”

<DrawnAnnotation text="@RequiresOptIn" label="An opt-in marker, `ERROR` by default" />
<DrawnAnnotation text="@ExperimentalFormulas" :line="6" label="Only `formula { }` is experimental, the columns are not" color="var(--fundamentals-blue)" />

```kotlin
import presentation.support.contexts.ExcelFormulas
import presentation.support.contexts.Formulas

@RequiresOptIn("Formulas are experimental, the operators may change")
annotation class ExperimentalFormulas

@ExcelDsl
class SheetBuilder<T> : DelegatedColumns<T>() {
  @ExperimentalFormulas
  fun formula(block: Formulas.() -> Cell): ColumnDelegate<Number> =
    formulaColumn { row -> ExcelFormulas(row).block() }
}
```

<!--
Part 2 opts in to `ExperimentalTypeInference` as a consumer. This is the
author's side: the same mechanism lets a DSL ship one experimental block
while the rest is stable. The extra "What the talk leaves out" uses its
sibling `@SubclassOptInRequired` for implementing an interface.
-->

---

# `@RequiresOptIn` marks the part that may still change

> Part 6, after “Hide the old API, keep the binary”

<DrawnAnnotation text="formula" :line="4" label="`@OptIn(ExperimentalFormulas::class)` on the caller, or `-opt-in=` for a module" />
<InlineCompilerError :line="4" text="formula" message="Formulas are experimental, the operators may change">

```kotlin
import presentation.support.contexts.times

val report = SheetBuilder<Invoice>().apply {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
}
```

</InlineCompilerError>

<!--
The compiler's error is the marker's message, verbatim. The IDE adds the
quick fix that inserts `@OptIn(ExperimentalFormulas::class)` on the
declaration, or `@ExperimentalFormulas` to propagate the requirement to the
caller's own callers.
-->

---

# `ReplaceWith` rewrites the call site

> Part 6, after “`@JvmName` keeps the old binary”

<DrawnAnnotation text="ReplaceWith(&quot;column.withVat()&quot;" label="The quick fix, written with the parameter names" />
<DrawnAnnotation text="&quot;presentation.support.contexts.withVat&quot;" label="The import the fix adds" color="var(--fundamentals-blue)" />

```kotlin
import presentation.support.contexts.Formulas
import presentation.support.contexts.withVat

@Deprecated(
  "Use total.withVat()",
  ReplaceWith("column.withVat()", "presentation.support.contexts.withVat"),
)
fun Formulas.withVat(column: Column<Number>): Cell = column.withVat()
```

<!--
Part 6 jumps straight to `HIDDEN`, which keeps binaries linking but cannot
offer a fix: a hidden declaration is invisible to the IDE too. The usual
order across releases is `WARNING` with `ReplaceWith` (Alt+Enter, "Replace
with", and "Replace all usages in project"), then `ERROR`, then `HIDDEN`.
The old body delegates to the new function, so there is one implementation.
-->

---

# A default body keeps implementations compiling

> Part 6, after “New formulas extend the interface”

<style>
.slidev-layout pre { white-space: pre-wrap; word-break: break-all; }
</style>

<DrawnAnnotation text="error" :line="4" label="Part 4's `Evaluate` still compiles, and fails only when `sum` is called" />
<DrawnAnnotation text="public default" label="A JVM default method: an implementation compiled against the old interface gets it" color="var(--fundamentals-blue)" />

```kotlin
interface Formulas {
  fun multiply(column: Column<Number>, other: Column<Number>): Cell
  fun sum(columns: List<Column<Number>>): Cell =
    error("sum is not supported by ${this::class.simpleName}")
}
```
```console
$ javap -p Formulas.class
public interface Formulas {
  public default presentation.support.Cell sum(java.util.List<presentation.support.Column<java.lang.Number>>);
}
```

<!--
Part 6 adds `sum` as an abstract member. Every class implementing `Formulas`
stops compiling, and a class compiled against the old interface throws
`AbstractMethodError` the first time `sum` is called. A default body keeps
both working. Since Kotlin 2.2 the default `-jvm-default=enable` emits a real
JVM default method plus a `DefaultImpls` class for older callers; `javap`
trimmed to the `sum` line, `multiply` and the `DefaultImpls` class are still
there. The other
answer, when the member has no sensible default, is
`@SubclassOptInRequired` (extra "What the talk leaves out").
-->

---

# `@IntroducedAt` writes the old overload

> Experimental in Kotlin 2.4, KEEP-0431

<DrawnAnnotation text="@IntroducedAt(&quot;1.1&quot;)" label="The compiler generates the 1.0 signature as well" />
<DrawnAnnotation text="ZZLkotlin" label="1.1: `bold` and `wrapText`" color="var(--fundamentals-blue)" />
<DrawnAnnotation text="String;ZL" label="1.0, generated, `@Deprecated(level = ERROR)`" />

```kotlin
@OptIn(ExperimentalVersionOverloading::class)
class SheetBuilder<T> {
  fun column(
    name: String,
    bold: Boolean = false,
    @IntroducedAt("1.1") wrapText: Boolean = false,
    value: (T) -> Any?,
  ) { }
}
```
```console
$ javap -p -s SheetBuilder.class
    descriptor: (Ljava/lang/String;ZZLkotlin/jvm/functions/Function1;)V
    descriptor: (Ljava/lang/String;ZLkotlin/jvm/functions/Function1;)V
```

<!--
The extra "What the talk leaves out" writes the old `column` by hand with
`HIDDEN`. `@IntroducedAt` generates it, with its `$default` companion, and
`javap -v` shows `kotlin.Deprecated(level=ERROR)` on it. The console keeps
only the two `column` descriptors; the constructor and the two `$default`
methods are trimmed. Rules from the KEEP: versions ascending left to right,
only final functions, versioned parameters after every non-optional one
except a trailing lambda, which is why `value` may stay last. Inserting a
parameter in the middle is source incompatible for positional callers, which
is where `named` (next slide) comes in. Compiles on 2.5.0-Beta1 with only
the opt-in.
-->

---

# `named` makes an option a name, never a position

> KEEP-0439, public discussion

<DrawnAnnotation text="named format" label="Only by name, or omitted" />
<DrawnAnnotation text="column(euro, 20)" label="Rejected, the trailing lambda stays" color="red" />

```kotlin no-compile
class SheetBuilder<T> {
  fun column(
    named format: CellFormat? = null,
    named width: Int? = null,
    value: (T) -> String,
  ): ColumnDelegate<String>
}

val customer by column(width = 20) { it.customer }
val rate by column(euro, 20) { it.rate }
```

<!--
The KEEP names DSLs explicitly: a group of named-only options followed by a
trailing lambda, which is the realistic library's "Options go before the
trailing lambda". `named` does not take part in overload resolution, the check
runs after; `@SoftNamedOnlyParametersCheck` turns the errors into warnings for
a migration. Until then the same rule is an IDE inspection or a detekt rule.
-->

---

# A `$$` string keeps Excel's `$`

> Stable in Kotlin 2.2, KEEP-0375

<DrawnAnnotation text="absolute" :line="2" label="Two dollars open an interpolation, a single `$` is text" />
<DrawnAnnotation text="letter" :line="2" label="Three dollars before the brace: `$`, then `${letter}`" color="var(--fundamentals-blue)" />

```kotlin
/** `$B$1`: the cell stays put when Excel copies the formula. */
fun Column<*>.absolute(row: Int): String = $$"$$${letter}$$${row}"

val rate = Column<Number>("rate", 1)
val amount = Column<Number>("amount", 2)
println("=${amount.reference(2)}*${rate.absolute(1)}")
```
```console
=C2*$B$1
```

<!--
Part 6 renders formulas as text. Absolute references, `$B$1` for a VAT rate
kept in one cell, need a literal `$` next to an interpolation, which used to
be `${'$'}` or `\$`. A DSL that renders another language (Excel, JSON Schema,
GraphQL) meets this constantly. Placement: Part 6, after “Rendering is a
`when` over the tree”.
-->

---

# `lateinit var` only fails at `build()`

> Extra “A realistic library”, after “Sheet settings are properties of the receiver”

<style>
.slidev-layout pre { white-space: pre-wrap; word-break: break-all; }
</style>

<DrawnAnnotation text="lateinit var path" label="Required, and nothing says so until `build()`" color="red" />

```kotlin
class WorkbookBuilder {
  lateinit var path: String
  var autoFilter: Boolean = false

  fun build(): String = path
}

runCatching { WorkbookBuilder().apply { autoFilter = true }.build() }
  .onFailure(::println)
```
```console
kotlin.UninitializedPropertyAccessException: lateinit property path has not been initialized
```

---
magic-move
---

# Required is a parameter, optional is a property

> Extra “A realistic library”, after “Sheet settings are properties of the receiver”

<DrawnAnnotation text="val path: String" label="Required: a parameter of the entry point" />
<DrawnAnnotation text="autoFilter = true" label="Optional: a `var` with a default, set inside the block" color="var(--fundamentals-blue)" />

```kotlin
class WorkbookBuilder(val path: String) {
  var autoFilter: Boolean = false
}

fun workbook(path: String, block: WorkbookBuilder.() -> Unit): WorkbookBuilder =
  WorkbookBuilder(path).apply(block)

workbook("invoices.xlsx") { autoFilter = true }
```

<!--
The rule Part 1 follows without stating it: `workbook("invoices.xlsx") { }`,
`sheet("Q1", q1) { }`. A builder with `lateinit var` for a required value
fails at `build()`, with the compiler's message unless `::path.isInitialized`
is checked first. KEEP-0455 (`lateinit val`, public discussion) is for
assign-once fields and explicitly leaves builders to `lateinit var`, so it
does not change this.
-->

---

# `=` on a `val` needs a compiler plugin

> KEEP PR #308, closed in 2022: a plugin, not the language

<DrawnAnnotation text="val autoFilter" label="A `val`, and `autoFilter = true` still compiles" />
<DrawnAnnotation text="fun assign" label="What the assignment calls: `Setting.assign(true)` in the bytecode" color="var(--fundamentals-blue)" />

```kotlin no-compile
@SupportsAssignment
class Setting<T>(private var value: T) {
  fun assign(value: T) { this.value = value }
}

class SheetBuilder {
  val autoFilter = Setting(false)
}

sheet { autoFilter = true }
```
```kotlin gradle
plugins { kotlin("plugin.assignment") version "2.5.0-Beta1" }
assignment { annotation("excel.dsl.SupportsAssignment") }
```

<!--
Gradle's `Property<T>` is the reason this exists: `version = "1.0"` in a
`.gradle.kts` file is `version.assign("1.0")`. The language team rejected the
convention for Kotlin itself and shipped the compiler-plugin extension point
instead (Roman Elizarov's closing comment on #308). Checked with
`kotlin("plugin.assignment") version "2.5.0-Beta1"`: `javap -c` shows
`invokevirtual Setting.assign`. Compose's `@Composable`, kotlinx.serialization
and Power-assert are the same move: when the language says no, a plugin can
still shape the DSL. Placement: extra “A realistic library”, after “Sheet
settings are properties of the receiver”.
-->

---

# A typed value picks the `suspend` overload

> Extra “`Flow` rows”, after “`suspend` does not pick an overload”

<DrawnAnnotation text="suspend (Invoice) -> String" label="The declared type does what a lambda literal cannot" />

```kotlin
class SheetBuilder<T> {
  val kinds = mutableListOf<String>()
  fun column(value: (T) -> String) { kinds += "plain" }
  fun column(value: suspend (T) -> String) { kinds += "suspend" }
}

suspend fun fetchEmail(customer: String): String = "$customer@example.com"

val lookup: suspend (Invoice) -> String = { fetchEmail(it.customer) }

println(SheetBuilder<Invoice>().apply { column(lookup) }.kinds)
```
```console
[suspend]
```

<!--
The `Flow` extra is right on 2.5.0-Beta1: `column { it.customer }` is an
"Overload resolution ambiguity" between the two `String` overloads. What it
does not show is the way out. A value with a declared function type resolves,
and so does `suspend { }` for a lambda without parameters
(`run(suspend { })` picks `run(f: suspend () -> Unit)`); with `it` or a typed
parameter it is still ambiguous. KEEP-0445 (public discussion) records this
history and the parser change planned once the old frontend is gone.
-->

---

# A script gives the DSL its own file type

> KEEP-0075, Kotlin scripting

<DrawnAnnotation text="implicitReceivers" label="The builder is `this` for the whole file" />
<DrawnAnnotation text="defaultImports" label="No imports in the script" color="var(--fundamentals-blue)" />

```kotlin no-compile
@KotlinScript(
  fileExtension = "invoices.kts",
  compilationConfiguration = InvoiceScript::class,
)
abstract class InvoiceReport

object InvoiceScript : ScriptCompilationConfiguration({
  defaultImports("excel.dsl.*")
  implicitReceivers(InvoiceSheetBuilder::class)
})
```
```kotlin no-compile
// q1.invoices.kts
val customer by column { it.customer }
val hours by column { it.hours }
totals { hours.sum() }
```

<!--
`.gradle.kts` and `main.kts` are this: a script definition names the implicit
receiver and the default imports, and the host evaluates each file against a
real builder. The row type must be fixed by the definition, a generic
`SheetBuilder<T>` as implicit receiver loses `T`. The cost is the scripting
host, its dependency and the slow first compile. Placement: the closing, as
the answer to "can users write these without a build".
-->

---
zoom: 0.75
---

# The KEEPs the talk already stands on

| KEEP | Status, September 2026 | In the talk |
| --- | --- | --- |
| KEEP-0057 scope control (`@DslMarker`), design note 0005 | Stable in 1.1; the note adds context parameters | Part 1, Part 5, this extra |
| KEEP-0025 local delegated properties | Stable in 1.1 | Part 3, `val hours by column` inside a block |
| KEEP-0139 contracts | Experimental in 1.3 | Extra “DSL tricks” |
| KEEP-0367 context parameters | In progress, `-Xcontext-parameters` | Part 5 |
| KEEP-0448 explicit context arguments | Experimental in 2.4 | Extra “Advanced DSL mechanics” |
| KEEP-0379 context-sensitive resolution | In progress, `-Xcontext-sensitive-resolution` | Extra “DSL tricks” |

<style>
.slidev-layout table { font-size: 1.3rem; }
td, th { padding-top: 0.35rem; padding-bottom: 0.35rem; }
</style>

<!--
Statuses are copied from the proposal headers in github.com/Kotlin/KEEP on
25 September 2026. Context parameters are still "In progress" in KEEP-0367
even though the deck presents them without a lede; the build still passes
`-Xcontext-parameters`.
-->

---
zoom: 0.75
---

# The KEEPs the talk already stands on

| KEEP | Status, September 2026 | In the talk |
| --- | --- | --- |
| KEEP-0416 collection literals | Experimental since 2.4.0 | Part 6 |
| KEEP-0449 companion extensions and blocks | Experimental in 2.5.0; KEEP-0427 statics rejected for it | Part 3, `companion val ExcelConfig.Default` |
| KEEP-0430 explicit backing fields | Stable in 2.4 | Extra “What the talk leaves out” |
| KEEP-0412 unused return value checker | Experimental in 2.3 | Extra “A realistic library” |
| KEEP-0375 multi-dollar interpolation | Stable in 2.2 | This extra |
| KEEP-0431 version overloading | Experimental in 2.4 | This extra |

<style>
.slidev-layout table { font-size: 1.3rem; }
td, th { padding-top: 0.35rem; padding-bottom: 0.35rem; }
</style>

---
zoom: 0.75
---

# The KEEPs that would change it

| KEEP | Status, September 2026 | What it changes |
| --- | --- | --- |
| KEEP-0439 named-only parameters | Public discussion | Options before the trailing lambda, by name only |
| KEEP-0469 explicit type parameters | Public discussion | `sheet<Invoice>` declared as mandatory |
| KEEP-0464 local lambda parameters, KEEP-0463 var capture | Public discussion | `local once` blocks, warnings on `var` in escaping lambdas |
| KEEP-0450 typed delegate access | Public discussion | `::hours.delegate` instead of a backing property |
| KEEP-0455 `lateinit val` (supersedes 0452) | Public discussion | Assign-once fields; builders keep `lateinit var` |
| KEEP-0445 `suspend` lambdas | Public discussion | `suspend { }` with parameters, once the old frontend is gone |

<style>
.slidev-layout table { font-size: 1.3rem; }
td, th { padding-top: 0.35rem; padding-bottom: 0.35rem; }
</style>

---
zoom: 0.75
---

# The KEEPs that would change it

| KEEP | Status, September 2026 | What it changes |
| --- | --- | --- |
| KEEP-0456 more specific `equals` | In progress | `==` stays `Boolean`: `eq` stays an `infix` function |
| KEEP-0440 ABI validation in the Gradle plugin | Public discussion | `checkKotlinAbi` as the guard for Part 6 |
| KEEP-0302 binary signature name, PR #503 `@SignatureDiscriminator` | Unknown; the PR is open | A multiplatform `@JvmName` |
| KEEP-0462 rich errors | Public discussion | A builder could return `Sheet<T> \| InvalidSchema` |
| KEEP-0409 subtyping reconstruction | Declined | A `when` over `Expr<V>` keeps its casts |

<style>
.slidev-layout table { font-size: 1.3rem; }
td, th { padding-top: 0.35rem; padding-bottom: 0.35rem; }
</style>

---
zoom: 0.75
---

# Closed without merging, and what replaced them

| Pull request | Closed | Replaced by |
| --- | --- | --- |
| #308 assign operator overload | 2022 | The `kotlin("plugin.assignment")` compiler plugin (this extra) |
| #87 compile-time extension interfaces | 2020 | Multiple receivers, then context receivers, then KEEP-0367 |
| #46 override extension functions | 2026, obsolete | Member extensions with dynamic dispatch (Part 4) |
| #356 self types | 2026, no plans (KT-6494) | Fluent builders keep returning `this` by hand |
| #169 flexible delegated property convention | 2020 | `PropertyDelegateProvider` stays the hook (Part 3) |
| #112, #229 collection literals | 2020 | KEEP-0416 (Part 6) |
| #425 static members and type extensions | 2025, merged as KEEP-0427, then rejected | KEEP-0449 companions |

<style>
.slidev-layout table { font-size: 1.3rem; }
td, th { padding-top: 0.35rem; padding-bottom: 0.35rem; }
</style>

<!--
The KEEP workflow changed in July 2025: every proposal that reaches public
review is merged, so "merged" no longer means accepted, and the status line
in the document header is the thing to read. Since then new proposals are
merged directly and discussed in GitHub Discussions; the pull requests above
are the older, discussion-carrying ones.
-->

---
zoom: 0.75
---

# Where each slide goes

| Slide | After |
| --- | --- |
| An inner extension beats an outer member | Part 1, “Nested scopes leak the outer receiver” |
| How a name in a block is resolved | Part 1, “Implicit receivers stay in their own scope” |
| A more specific receiver wins before the lambda | Part 2, replaces “Users add a cell type with an extension” |
| `@JvmSynthetic` hides the Kotlin-only overload from Java | Part 2, “`@JvmName` gives each overload its own name” |
| `sheet { }` cannot infer the row type, `explicit`, `reified` | Extra “Sheets as values”, “A sheet is a value” |
| One marker twice in a scope: nothing is implicit | Part 5, “A receiver satisfies a context” |
| A public `inline` function can only call public API, `@PublishedApi` | Part 6, “`inline` removes the builder lambda” |
| `@RequiresOptIn` marks the part that may still change | Part 6, “Hide the old API, keep the binary” |

<style>
.slidev-layout table { font-size: 1.3rem; }
td, th { padding-top: 0.35rem; padding-bottom: 0.35rem; }
</style>

---
zoom: 0.75
---

# Where each slide goes

| Slide | After |
| --- | --- |
| `ReplaceWith` rewrites the call site | Part 6, “`@JvmName` keeps the old binary” |
| A default body keeps implementations compiling | Part 6, “New formulas extend the interface” |
| `@IntroducedAt` writes the old overload, `named` | Extra “What the talk leaves out”, “A default argument is part of the signature” |
| A `$$` string keeps Excel's `$` | Part 6, “Rendering is a `when` over the tree” |
| `lateinit var` only fails at `build()`, required is a parameter, `=` needs a plugin | Extra “A realistic library”, “Sheet settings are properties of the receiver” |
| A typed value picks the `suspend` overload | Extra “`Flow` rows”, “`suspend` does not pick an overload” |
| A script gives the DSL its own file type | Closing |
| The KEEP tables | Closing, or the handout only |

<style>
.slidev-layout table { font-size: 1.3rem; }
td, th { padding-top: 0.35rem; padding-bottom: 0.35rem; }
</style>
