---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Extra</div>

# `Flow` rows

## Suspending sources, streaming sheets

<!--
So far every row came from a `Sequence`: pulled one at a time, on the calling thread,
straight into SXSSF, which keeps 100 rows in memory and flushes the rest to temp files.
Real rows come from a database driver or an HTTP API that suspends. This extra makes the
DSL `suspend` without giving up the constant memory.
-->

---

# A `Flow` is not a `Sequence`

<InlineCompilerError :line="1" text="fetchInvoices()" message="Argument type mismatch: actual type is 'Flow<Invoice>',\nbut 'Sequence<uninferred T (of fun <T> generateExcel)>' was expected.">

```kotlin
import presentation.support.contexts.generateExcel
import presentation.support.flow.fetchInvoices

generateExcel("invoices.xlsx", fetchInvoices()) {
  val customer by column { it.customer }
}
```

</InlineCompilerError>

<!--
`fetchInvoices()` is a cold `flow { }`: a 100 ms `delay` per row, standing in for an R2DBC
or HTTP round trip, nothing happens until it is collected.

Kotlin 2.4.20 reports more than this one: `T` can no longer be inferred, so `column` is
ambiguous and `it.customer` unresolved. They all cascade from the mismatch, only that one is
on the slide.

`runBlocking { fetchInvoices().toList().asSequence() }` makes it compile and throws away the
streaming: every row in memory before the first cell is written.
-->

---

# `generateExcel` becomes `suspend`

<DrawnAnnotation text="suspend" label="Waits for rows without blocking a thread" />
<DrawnAnnotation text="rows: Flow<T>" label="Cold like `Sequence`, pulling a row may suspend" />
<DrawnAnnotation text="SheetBuilder<T>.() -> Unit" label="Not `suspend`: runs once, only declares columns" color="var(--fundamentals-blue)" />

```kotlin no-compile
suspend fun <T> generateExcel(
  path: String,
  rows: Flow<T>,
  block: SheetBuilder<T>.() -> Unit,
)
```

<!--
Only the rows change. The builder block defines the schema: it runs once, before the first
row, and never needs to wait on anything. Keeping it non-suspending keeps the DSL exactly
the same as in Part 5: same `column`, same `formula`, same `@OverloadResolutionByLambdaReturnType`.
-->

---
magic-move
---

# Apache POI blocks, so it runs on `Dispatchers.IO`

<DrawnAnnotation text="withContext(Dispatchers.IO)" label="Temp files and the `.xlsx` are blocking writes" />
<DrawnAnnotation text="rows.collect(sheet::write)" label="Asks for the next row once this one is written" color="var(--fundamentals-blue)" />

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import presentation.support.flow.SheetBuilder

suspend fun <T> generateExcel(
  path: String,
  rows: Flow<T>,
  block: SheetBuilder<T>.() -> Unit,
) {
  val columns = SheetBuilder<T>().apply(block)
  withContext(Dispatchers.IO) {
    streamSheet(path, columns) { sheet -> rows.collect(sheet::write) }
  }
}
```

<!--
The builder runs on the caller's dispatcher, it is pure. Everything that touches POI moves
to `Dispatchers.IO`: SXSSF flushes every 100 rows to a temp file, and `save` writes the zip.

`collect` inside `withContext` means the upstream runs on IO too, flows preserve the
collector's context. A driver that needs its own dispatcher brings its own `flowOn`.

Backpressure is free: `emit` only returns after `sheet.write` returned, so the source can
never run ahead of the writer. Memory stays at the SXSSF window, whatever the row count.
-->

---
magic-move
---

# `Sequence` and `Flow` share one writer

<DrawnAnnotation text="streamSheet" label="`inline`: its lambda may suspend when the caller can" />
<DrawnAnnotation text="rows.forEach(sheet::write)" label="Blocking, on the caller's thread" color="var(--fundamentals-blue)" />
<DrawnAnnotation text="rows.collect(sheet::write)" />

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import presentation.support.flow.SheetBuilder

fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  block: SheetBuilder<T>.() -> Unit,
) {
  val columns = SheetBuilder<T>().apply(block)
  streamSheet(path, columns) { sheet -> rows.forEach(sheet::write) }
}

suspend fun <T> generateExcel(
  path: String,
  rows: Flow<T>,
  block: SheetBuilder<T>.() -> Unit,
) {
  val columns = SheetBuilder<T>().apply(block)
  withContext(Dispatchers.IO) {
    streamSheet(path, columns) { sheet -> rows.collect(sheet::write) }
  }
}
```

<!--
One row-at-a-time writer in the core: `streamSheet` opens the SXSSF workbook, hands out a
`SheetStream<T>` with `write(row)`, saves, and closes it in `use` (POI 5.5 `close()` disposes). Because it is `inline`,
the lambda body is pasted into each caller: in the blocking overload it is plain code, in
the `suspend` one `collect` may suspend. No second writer, no `suspend` in the core.

Two overloads, not one: `Sequence` to `Flow` is free (`invoices.asFlow()`), so a suspending
caller can always use the `Flow` overload. The other way round needs `runBlocking`, so
blocking callers (a CLI, a JDBC service) keep the `Sequence` overload and never see a
coroutine. JVM signatures differ (`Sequence` vs `Flow` + `Continuation`), no `@JvmName`.
-->

---

# `collect` pulls one row at a time

<DrawnAnnotation text="fetched" label="Upstream suspends in `emit` until the row is written" />
<DrawnAnnotation text="wrote" color="var(--fundamentals-blue)" />

<TypeHint :line="4" receiver="SheetBuilder<Invoice>">

```kotlin
import kotlinx.coroutines.flow.onEach
import presentation.support.flow.fetchInvoices
import presentation.support.flow.generateExcel

val rows = fetchInvoices()
  .onEach { println("fetched ${it.customer}") }

generateExcel("invoices.xlsx", rows) {
  val customer by column {
    println("  wrote ${it.customer}")
    it.customer
  }
}
```
```console
fetched Ada
  wrote Ada
fetched Linus
  wrote Linus
fetched Grace
  wrote Grace
fetched Olivia
  wrote Olivia
fetched Linus
  wrote Linus
```

</TypeHint>

<!--
Never two `fetched` in a row: the flow is sequential, `emit` is a direct call into the
collector. That is the backpressure, no buffer anywhere. With a million rows SXSSF holds
100 of them and the driver holds whatever it fetches per round trip.

`buffer()` or `flowOn` upstream would decouple them: the driver can fetch ahead while POI
writes, bounded by the buffer's capacity. Opt in when the round trips dominate.
-->

---

# Cancellation still deletes the temp files

<DrawnAnnotation text="awaitCancellation()" label="The database stops answering" color="red" />
<DrawnAnnotation text="1 temp file" label="SXSSF creates the sheet's temp file up front" />
<DrawnAnnotation text="withTimeoutOrNull(1.seconds)" label="Cancels `collect` while it waits for row three" color="var(--fundamentals-blue)" />

<TypeHint :line="7" receiver="SheetBuilder<Invoice>">

```kotlin
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withTimeoutOrNull
import presentation.support.flow.fetchInvoices
import presentation.support.flow.generateExcel
import presentation.support.flow.sxssfTempFiles

val stalled = flow {
  emitAll(fetchInvoices().take(2))
  awaitCancellation()
}.onEach { println("${it.customer}, ${sxssfTempFiles().size} temp file") }

withTimeoutOrNull(1.seconds) {
  generateExcel("invoices.xlsx", stalled) {
    val customer by column { it.customer }
  }
} ?: println("Timed out, ${sxssfTempFiles().size} temp files")
```
```console
Ada, 1 temp file
Linus, 1 temp file
Timed out, 0 temp files
```

</TypeHint>

<!--
Two rows arrive, then the source hangs, as a stalled connection would. After one second
`withTimeoutOrNull` cancels: the `CancellationException` comes out of `collect`, through
the `inline` `streamSheet`, into `use`, which closes the workbook. In POI 5.5.1 `close()`
calls `dispose()` itself (and `dispose()` is deprecated), so closing deletes
`poifiles/poi-sxssf-sheet*.xml`. `save` never ran: no half-written `invoices.xlsx` either.

The cleanup is blocking file I/O that never suspends, on `Dispatchers.IO` where it belongs,
so it needs no `withContext(NonCancellable)`.

A plain 250 ms timeout against `fetchInvoices()` is racy on a cold JVM: loading POI can
take longer than the first two rows, hence the stalled source.
-->

---

# A column lambda cannot suspend

<InlineCompilerError :line="3" text="fetchEmail" message="Suspension functions can only be called within coroutine body.">
<TypeHint :line="1" receiver="SheetBuilder<Invoice>">

```kotlin
import presentation.support.flow.fetchEmail
import presentation.support.flow.fetchInvoices
import presentation.support.flow.generateExcel

generateExcel("invoices.xlsx", fetchInvoices()) {
  val customer by column { it.customer }
  val email by column { fetchEmail(it.customer) }
}
```

</TypeHint>
</InlineCompilerError>

<!--
`column` takes a `(T) -> String`, stored and run once per row, so it is not a coroutine body
even though `main` is `suspend`. Outside a suspend function the message reads
"Suspend function 'suspend fun fetchEmail(customer: String): String' can only be called
from a coroutine or another suspend function."

`inline` does not help. A `suspend inline fun generateExcel` would let the *block* call
`fetchEmail`, inline lambdas inherit the caller's suspend context. The column lambda is
stored, never inlined (Part 6), and Kotlin 2.4.20 reports the same diagnostic inside it.
-->

---

# `suspend` does not pick an overload

<DrawnAnnotation text="DelegatedColumns<T>()" label="Already has `column(value: (T) -> String)`" color="var(--fundamentals-blue)" />

<InlineCompilerError :line="9" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(value: suspend (Invoice) -> String): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Column<String>>>`\n`fun column(value: (Invoice) -> String): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Column<String>>>`" style="--inline-compiler-error-message-size: 1.05rem">
<InlineCompilerError :line="10" text="column" message="Overload resolution ambiguity between candidates:\n`fun column(value: suspend (Invoice) -> String): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Column<String>>>`\n`fun column(value: (Invoice) -> String): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Column<String>>>`" style="--inline-compiler-error-message-size: 1.05rem">

```kotlin
@ExcelDsl
class SheetBuilder<T> : DelegatedColumns<T>() {
  @OverloadResolutionByLambdaReturnType
  @JvmName("suspendTextColumn")
  fun column(value: suspend (T) -> String): ColumnDelegate<String>
}

generateExcel("invoices.xlsx", fetchInvoices()) {
  val customer by column { it.customer }
  val email by column { fetchEmail(it.customer) }
}
```

</InlineCompilerError>
</InlineCompilerError>

<!--
Every lambda literal fits both `(T) -> String` and `suspend (T) -> String`, and the return
type is the same, so `@OverloadResolutionByLambdaReturnType` has nothing to choose by. Even
the plain `{ it.customer }` breaks. The compiler prints `PropertyDelegateProvider<Any?,
ReadOnlyProperty<Any?, Column<String>>>`, the expanded `ColumnDelegate<String>`. Line 10
also still reports "Suspension functions can only be called within coroutine body."

Dropping the non-suspend overloads does compile: `suspend (T) -> String`,
`suspend (T) -> Number`, `suspend (T) -> LocalDate` resolve by return type like before.
The price: every cell becomes a suspend call, `ColumnSpec` and the writer have to be
`suspend`, the `Sequence` overload needs `runBlocking`, and each row does one round trip
per suspending column, one after the other, no batching, no concurrency.
-->

---

# Suspend in the `Flow`, not in the column

<DrawnAnnotation text=".map { Billed(it, fetchEmail(it.customer)) }" label="The lookup is part of the source" />
<DrawnAnnotation text="{ it.email }" label="The column only reads" color="var(--fundamentals-blue)" />

<TypeHint :line="6" receiver="SheetBuilder<Billed>">

```kotlin
import kotlinx.coroutines.flow.map
import presentation.support.flow.fetchEmail
import presentation.support.flow.fetchInvoices
import presentation.support.flow.generateExcel

data class Billed(val invoice: Invoice, val email: String)

val rows = fetchInvoices()
  .map { Billed(it, fetchEmail(it.customer)) }

generateExcel("invoices.xlsx", rows) {
  val customer by column { it.invoice.customer }
  val email by column { it.email }
}
```
```console
| customer | email             |
| Ada      | ada@example.com   |
| Linus    | linus@example.com |
```

</TypeHint>

<!--
The DSL stays a schema: pure functions from a row to a cell. Everything that waits is in
the pipeline, where Flow already has the operators: `buffer()` to fetch ahead,
`flatMapMerge(concurrency = 8)` (or Arrow's `parMap`) to look up several customers at
once, a cache keyed by customer, `retry` on the HTTP call. None of that fits inside a
`column { }`.

Recap:
- `suspend fun generateExcel(rows: Flow<T>)`: `collect` pulls, SXSSF keeps memory constant
- `withContext(Dispatchers.IO)` around POI, the builder stays pure and non-suspending
- `use` inside an `inline` writer: cancellation closes, and so disposes, the workbook
- Keep the `Sequence` overload for blocking callers, `asFlow()` bridges the other way
- Column lambdas don't suspend, enrich the rows in the `Flow`
-->
