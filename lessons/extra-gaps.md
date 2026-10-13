---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Extra</div>

# What the talk leaves out

## And where each piece would go

<!--
For Simon. A review of Parts 0-6 against what a DSL author runs into, plus the
KEEPs that change how DSLs are written. Every lede names the slide of the main
deck it would follow. Diagnostics and ABI output are verbatim from Kotlin
2.5.0-Beta1 (this repo's build). The last two slides are the placement table
and the KEEPs to watch.
-->

---
zoom: 0.72
---

# A marked type alias marks the receiver

> Part 1, after “Mark receivers you don't own”

<DrawnAnnotation text="AnnotationTarget.TYPEALIAS" label="Allow the marker on an alias" />
<DrawnAnnotation text="@ExcelDsl typealias HeaderStyle = CellStyle" label="Marked once, not at every function type" color="var(--fundamentals-pink)" />

<InlineCompilerError :line="15" text="column" message="'fun column(name: String): Unit' cannot be called in this context with an implicit receiver.\nUse an explicit receiver if necessary." style="--inline-compiler-error-message-size: 1.2rem">

```kotlin
import org.apache.poi.ss.usermodel.CellStyle

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
annotation class ExcelDsl

@ExcelDsl typealias HeaderStyle = CellStyle

@ExcelDsl
class SheetBuilder {
  fun column(name: String)
  fun header(block: HeaderStyle.() -> Unit)
}

fun SheetBuilder.invoice() = header {
  wrapText = true
  column("Customer")
}
```

</InlineCompilerError>

<!--
Part 1 marks `CellStyle` with `@ExcelDsl CellStyle.() -> Unit` at every
function type. KEEP notes/0005 (the first written spec of @DslMarker, against
2.2.0) lists four sources of marking: the class and its supertypes, type
aliases, type annotations (also on context parameters), and propagation
through function types. Checked on 2.5.0-Beta1: the alias alone is enough.
-->

---

# A `Column<Int>` is not a `Column<Number>`

> Part 3, after “The lambda still picks the type”

<InlineCompilerError :line="4" text="*" message="Candidate 'context(formulas: Formulas) fun Column<Number>.times(other: Column<Number>): Cell'\nis inapplicable for operator '*' because of a receiver type mismatch.\nActual receiver type is 'Column<Int>'." style="--inline-compiler-error-message-size: 1.2rem">

```kotlin
import presentation.support.covariant.times

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
}
```

</InlineCompilerError>

<!--
Why keep `Column<Int>` at all: a typed read, `row[hours]`, should give back an
`Int`, not a `Number`. The deck's overloads return `Column<Number>` and never
hit this. As soon as a column keeps its precise type, every operator written
against `Column<Number>` stops applying, because `class Column<V>` is invariant.
Verified by compiling `presentation.support.covariant` with `class Column<V>`.
-->

---

# `out V` makes it one

> Part 3, after “The lambda still picks the type”

<DrawnAnnotation text="out V" label="Only produces `V`: a `Column<Int>` is a `Column<Number>`" />
<DrawnAnnotation text="Column<Number>" :line="4" color="var(--fundamentals-pink)" />

```kotlin no-compile
class Column<out V>(val name: String, val index: Int)

context(formulas: Formulas)
operator fun Column<Number>.times(other: Column<Number>): Cell
```

---

# `out V` makes it one

> Part 3, after “The lambda still picks the type”

<DrawnAnnotation text="{ it.hours }" label="`Column<Int>`" :connect="false" />
<DrawnAnnotation text="{ it.rate }" label="`Column<Int>`" :connect="false" />
<DrawnAnnotation text="hours * rate" label="Accepted as `Column<Number>`" color="var(--fundamentals-pink)" />

<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.covariant.generateExcel
import presentation.support.covariant.times

generateExcel("invoices.xlsx", invoices) {
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
}
```
```console
| hours | rate | total  |
| 12    | 90   | =A2*B2 |
```

</TypeHint>

<!--
The support code has `Int` and `Double` overloads next to `String`. A single
generic `fun <V : Number> column(value: (T) -> V)` does not work next to the
`String` overload: with @OverloadResolutionByLambdaReturnType the compiler
picks the non-generic candidate and reports "Return type mismatch: expected
'String', actual 'Int'" (checked on 2.5.0-Beta1).
-->

---

# The builder hands out a read-only view

> Part 3, after “`PropertyDelegateProvider` runs at the `by`”
>
> Stable since Kotlin 2.4.0

<DrawnAnnotation text="List<Column<*>>" label="What users of the builder see" />
<DrawnAnnotation text="field = mutableListOf()" label="What the class itself sees" color="var(--fundamentals-pink)" />
<DrawnAnnotation text="columns.add" color="var(--fundamentals-pink)" />

```kotlin
@ExcelDsl
class SheetBuilder<T> {
  val columns: List<Column<*>>
    field = mutableListOf()

  fun register(column: Column<*>) {
    columns.add(column)
  }
}
```

<!--
Explicit backing fields (KEEP-0430, experimental in 2.3.0, stable in 2.4.0). Replaces the `private val _columns` /
`val columns: List<...> get() = _columns` pair every builder has. Outside the
file, `builder.columns.add(...)` fails with "Unresolved reference 'add' on
receiver of type 'List<Column<*>>'". Inside the same file the smart cast still
applies, so the demo of the error has to live in another file.
Compiles on 2.5.0-Beta1 without a flag.
-->

---

# A context can be passed by name

> Part 5, after “Context parameter or receiver?”
>
> Experimental in Kotlin 2.4, `-Xexplicit-context-arguments`

<InlineValue :line="1" text="total.withVat(formulas = Evaluate(mapOf(total to 1080)))" value="Value(number=1306.8)">

```kotlin
import presentation.support.contexts.Evaluate
import presentation.support.contexts.withVat
import presentation.support.delegated.total

total.withVat(formulas = Evaluate(mapOf(total to 1080)))
```

</InlineValue>

<!--
KEEP-0448. The context parameter's name is the argument name, other
contexts still resolve implicitly. Useful in tests: evaluate a user's formula
without a `formula { }` block or `with(...)`. It also disambiguates two
overloads that differ only in `context(...)`, which 2.3.20 made an ambiguity
error (Part 6's fallback slide works around it with `Column<*>`).
Not stable in 2.4.0: stable context parameters exclude explicit arguments and
callable references. `_` parameters cannot be passed by name.
Check the InlineValue: 1080 * 1.21 = 1306.8 as a Double.
-->

---
zoom: 0.8
---

# `return` in an inline block leaves the caller

> Part 6, after “A stored lambda cannot be inlined”

<DrawnAnnotation text="return" label="Returns from `export`: `invoices.xlsx` is never written" color="red"  :geometry="{ label: { x: 0.6787, y: 0.3895 }, connector: { type: 'quadratic', start: { x: 0.3769, y: 0.3950 }, control: { x: 0.4257, y: 0.4159 }, end: { x: 0.4644, y: 0.3937 } } }"/>

<TypeHint :line="2" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.tricks.generateExcel

fun export(includeRates: Boolean) {
  generateExcel("invoices.xlsx", invoices) {
    val customer by column { it.customer }
    if (!includeRates) return
    val rate by column { it.rate }
  }
}
```

</TypeHint>

<!--
`inline` from Part 6 has a second consequence: the block is pasted into
`export`, so a bare `return` is a non-local return. It skips `writeWorkbook`
without a warning. This is the most common surprise with inline builders
(`buildString`, `transaction { }` in Exposed has the same shape).
-->

---
zoom: 0.8
---

# `crossinline` forbids the non-local `return`

> Part 6, after “A stored lambda cannot be inlined”

<DrawnAnnotation text="crossinline" label="Inline, but the block may not leave the caller"  :geometry="{ label: { x: 0.4469, y: 0.4391 }, connector: { type: 'quadratic', start: { x: 0.1941, y: 0.4200 }, control: { x: 0.2316, y: 0.4444 }, end: { x: 0.2713, y: 0.4372 } } }"/>

<InlineCompilerError :line="13" text="return" message="'return' is prohibited here.">

```kotlin
import presentation.support.tricks.SheetBuilder

inline fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  crossinline block: SheetBuilder<T>.() -> Unit,
) {
  val builder = SheetBuilder<T>().apply { block() }
  writeWorkbook(path, rows, builder)
}

fun export(includeRates: Boolean) {
  generateExcel("invoices.xlsx", invoices) {
    val customer by column { it.customer }
    if (!includeRates) return
    val rate by column { it.rate }
  }
}
```

</InlineCompilerError>

<!--
`crossinline` is also what `formula { }` would need if it were inline: its
block runs inside another lambda, once per row, and without `crossinline` the
compiler says "Cannot inline 'block: Formulas.() -> Cell' here: it might
contain non-local returns. Add 'crossinline' modifier to parameter
declaration". `noinline` (the fix the previous slide's error suggests) stores
the lambda as an object; `crossinline` keeps it inlined.
-->

---
magicMove: true
zoom: 0.8
---

# `crossinline` forbids the non-local `return`

> Part 6, after “A stored lambda cannot be inlined”

<DrawnAnnotation text="return@generateExcel" label="Ends the block, the file is still written" />

```kotlin
import presentation.support.tricks.SheetBuilder

inline fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  crossinline block: SheetBuilder<T>.() -> Unit,
) {
  val builder = SheetBuilder<T>().apply { block() }
  writeWorkbook(path, rows, builder)
}

fun export(includeRates: Boolean) {
  generateExcel("invoices.xlsx", invoices) {
    val customer by column { it.customer }
    if (!includeRates) return@generateExcel
    val rate by column { it.rate }
  }
}
```

---
zoom: 0.8
---

# Implementing `Formulas` is an opt-in

> Part 6, after “New formulas extend the interface”

<DrawnAnnotation text="@SubclassOptInRequired(FormulasImplementation::class)" label="Calling is free, implementing needs an opt-in"  :geometry="{ label: { x: 0.6725, y: 0.4213 } }"/>

<InlineCompilerError :line="11" text="Formulas" occurrence="2" message="This class or interface requires opt-in to be implemented:\nFormulas grows with every release. Implement it at your own risk." style="--inline-compiler-error-message-size: 1.2rem">

```kotlin
@RequiresOptIn(
  "Formulas grows with every release. Implement it at your own risk.",
)
annotation class FormulasImplementation

@SubclassOptInRequired(FormulasImplementation::class)
interface Formulas {
  fun multiply(column: Column<Number>, other: Column<Number>): Cell
}

class AuditFormulas : Formulas {
  override fun multiply(column: Column<Number>, other: Column<Number>) =
    TODO()
}
```

</InlineCompilerError>

<!--
Part 6 adds `sum` to `Formulas` by extending the interface. That is source
compatible for callers, and breaks every class a user wrote that implements
`Formulas`, like Part 4's `Evaluate`. `sealed` forbids implementations
outright; `@SubclassOptInRequired` (stable since Kotlin 2.1) lets advanced
users implement it and makes them sign for it. kotlinx.coroutines 1.11 marks
`Job` and `SharedFlow` this way.
-->

---
zoom: 0.72
---

# A default argument is part of the signature

> Part 6, after “Hide the old API, keep the binary”

<DrawnAnnotation text="wrapText: Boolean = false" label="Source compatible, not binary compatible" color="red"  :geometry="{ label: { x: 0.5968, y: 0.5165 } }"/>
<DrawnAnnotation text="ZZ" label="One more `boolean` in the JVM signature" color="red" />

```kotlin no-compile
class SheetBuilder<T> {
  fun column(
    name: String,
    bold: Boolean = false,
    wrapText: Boolean = false,
    value: (T) -> Any?,
  )
}
```
```console
$ ./gradlew checkKotlinAbi
-  public final fun column (Ljava/lang/String;ZLkotlin/…/Function1;)V
+  public final fun column (Ljava/lang/String;ZZLkotlin/…/Function1;)V
```

<!--
DSL entry points collect options over time (extra "A realistic library":
options go before the trailing lambda). Every new defaulted parameter is a new JVM
method, and code compiled against the old one fails with NoSuchMethodError.
Output trimmed from a real run on 2.5.0-Beta1; the `column$default` lines
change the same way.
-->

---
magicMove: true
zoom: 0.72
---

# A default argument is part of the signature

> Part 6, after “Hide the old API, keep the binary”

<DrawnAnnotation text="DeprecationLevel.HIDDEN" label="Gone from Kotlin, still in the class file"  :geometry="{ label: { x: 0.7405, y: 0.1291 } }"/>
<DrawnAnnotation text="synthetic" label="Only compiled callers can reach it" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.5233, y: 0.7462 } }"/>

```kotlin no-compile
class SheetBuilder<T> {
  @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
  fun column(name: String, bold: Boolean = false, value: (T) -> Any?) =
    column(name, bold, wrapText = false, value)

  fun column(
    name: String,
    bold: Boolean = false,
    wrapText: Boolean = false,
    value: (T) -> Any?,
  )
}
```
```console
-  public final fun column (Ljava/lang/String;ZLkotlin/…/Function1;)V
+  public final synthetic fun column (Ljava/lang/String;ZLkotlin/…/Function1;)V
+  public final fun column (Ljava/lang/String;ZZLkotlin/…/Function1;)V
```

---
zoom: 0.72
---

# `@IntroducedAt` writes the old overload

> Part 6, after “A default argument is part of the signature”
>
> `kotlin.IntroducedAt`: `@SinceKotlin("2.5")`, experimental in 2.4

<DrawnAnnotation text="@IntroducedAt(&quot;1.1&quot;)" label="Parameters before the trailing lambda may be added later" />
<DrawnAnnotation text="ZLkotlin" label="The 1.0 signature stays, not even `synthetic`" color="var(--fundamentals-pink)" />

```kotlin no-compile
class SheetBuilder<T> {
  fun column(
    name: String,
    bold: Boolean = false,
    @IntroducedAt("1.1") wrapText: Boolean = false,
    value: (T) -> Any?,
  )
}
```
```console
$ ./gradlew checkKotlinAbi
   public final fun column (Ljava/lang/String;ZLkotlin/…/Function1;)V
+  public final fun column (Ljava/lang/String;ZZLkotlin/…/Function1;)V
```

<!--
KEEP-0431, version overloading. The compiler generates one hidden overload per
earlier version. The stdlib KDoc: "If the last parameter has a function type,
this rule may be disregarded. This makes it possible to introduce optional
parameters before the trailing lambda position", exactly the DSL shape.
Not allowed on non-final or abstract members (like @JvmName), nor together with
@JvmOverloads. Output from a real run on 2.5.0-Beta1, no opt-in needed there;
2.4 needs @OptIn(ExperimentalVersionOverloading::class).
-->

---

# The Gradle plugin checks the binary

> Part 6, after “`@JvmName` keeps the old binary”

<DrawnAnnotation text="abiValidation()" label="Dumps the public ABI to `api/excel-dsl.api`"  :geometry="{ label: { x: 0.5377, y: 0.4488 } }"/>
<DrawnAnnotation text="checkKotlinAbi" label="Fails when the dump changed, runs in `check`" color="var(--fundamentals-pink)" />

```kotlin no-compile
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

kotlin {
  @OptIn(ExperimentalAbiValidation::class)
  abiValidation()
}
```
```bash
./gradlew updateKotlinAbi   # commit api/excel-dsl.api
./gradlew checkKotlinAbi    # fails on any ABI change
```

<!--
Built into the Kotlin Gradle plugin since 2.2 (the successor of the
binary-compatibility-validator plugin), still behind ExperimentalAbiValidation
in 2.5.0-Beta1. Every `@JvmName`, `HIDDEN` and default-argument slide of Part 6
is a claim this task verifies. The task names were `checkLegacyAbi` /
`updateLegacyAbi` in 2.2.
-->

---

# `local once` would replace the contract

> Extra “DSL tricks”, after “A contract promises exactly once”
>
> Public discussion, KEEP-0464

<DrawnAnnotation text="local once" label="`callsInPlace(block, EXACTLY_ONCE)` as a modifier"  :geometry="{ label: { x: 0.4518, y: 0.5595 } }"/>

```kotlin no-compile
inline fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  local once block: SheetBuilder<T>.() -> Unit,
) {
  writeWorkbook(path, rows, SheetBuilder<T>().apply(block))
}
```

<!--
KEEP-0464, "local lambda parameters", opened 2026-06-03. `local` means
callsInPlace UNKNOWN, `local once` EXACTLY_ONCE. Unlike a contract, escaping
the parameter is a compile error, it is stable syntax instead of an
@OptIn(ExperimentalContracts::class) block, and it works on functions without
a body, such as interface members. Open question: whether `once` alone means
`local once`. Nothing shipped; do not present as available.
-->

---

# Where each slide goes

| Slide | After | Part |
| --- | --- | --- |
| A marked type alias marks the receiver | Mark receivers you don't own | 1 |
| A `Column<Int>` is not a `Column<Number>` | The lambda still picks the type | 3 |
| The builder hands out a read-only view | `PropertyDelegateProvider` runs at the `by` | 3 |
| `@DslMarker` closes contexts too | A receiver satisfies a context | 5 |
| A context lambda brings only the context | A receiver satisfies a context | 5 |
| A context can be passed by name | Context parameter or receiver? | 5 |
| `return` in an inline block leaves the caller | A stored lambda cannot be inlined | 6 |
| Implementing `Formulas` is an opt-in | New formulas extend the interface | 6 |
| A default argument is part of the signature | Hide the old API, keep the binary | 6 |
| `@IntroducedAt` writes the old overload | A default argument is part of the signature | 6 |
| The Gradle plugin checks the binary | `@JvmName` keeps the old binary | 6 |

<style>
table { font-size: 1.25rem; }
td, th { padding-top: 0.35rem; padding-bottom: 0.35rem; }
</style>

<!--
If only three make the cut: `return` in an inline block (every user hits it),
the context lambda (finishes Part 5's story), and the default argument with
@IntroducedAt (finishes Part 6's). `local once` belongs to extra "DSL tricks".
-->

---

# KEEPs to watch

| KEEP | Status | In a DSL |
| --- | --- | --- |
| 0469 explicit type parameters | Discussion | `<explicit T>`: `single<Service> { }` |
| 0439 named-only parameters | Discussion | Options that cannot turn into the trailing lambda |
| 0464 `local` lambda parameters | Discussion | `local once block`, no contract |
| 0450 typed delegate access | Discussion | `::hours.delegate` |
| 0463 captured `var` warning | Discussion | Row lambdas escape the builder |
| 0462 rich errors | Discussion | `Column<V> \| DuplicateName` |
| 0449 companion extensions | Experimental 2.5 | `companion val ExcelConfig.Default` |
| 0416 `operator fun of` | Experimental 2.4 | `[hours, overtime]` as your own type |

<style>
table { font-size: 1.25rem; }
td, th { padding-top: 0.35rem; padding-bottom: 0.35rem; }
</style>

<!--
Statuses from the KEEP repo and the 2.4.0 / 2.4.20 / 2.5.0-EAP release notes
as of 2026-09-25. 0449 is already used in Part 3 (`companion val
ExcelConfig.Default`, -Xcompanion-blocks-and-extensions) without saying so.
0416's `of` must be a member of the companion (no extension `of`).
-->
