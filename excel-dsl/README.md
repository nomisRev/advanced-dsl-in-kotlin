# Excel DSL

A sheet is a reusable schema. Declare it once, then write lists, sequences or flows.

```kotlin
import excel.dsl.*
import java.nio.file.Path

data class Invoice(val customer: String, val hours: Int, val rate: Double, val paid: Boolean)

val invoices = sheet<Invoice> {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column(CellFormat.currency("€")) { it.rate }
  val paid by column { it.paid }
  val amount = formula("Invoice total", CellFormat.currency("€")) { hours * rate }
  val large: Column<Boolean> by formula { amount gt 1000 }
  val status: Column<String> by formula { paid then "Paid" otherwise "Open" }
  val attention by formula { large and !paid }
  totals { customer.label("Total"); amount.sum() }
}

invoices.write(Path.of("a-long-report-filename.xlsx"), listOf(Invoice("Ada", 12, 90.0, false)), name = "Invoices")
```

Formula expressions carry their result type: arithmetic returns `Expr<Double>`, comparisons
return `Expr<Boolean>`, and text branches return `Expr<String>`. Numeric branches share
`Expr<Number>`. Both branches must have the same cell type. Numeric truthiness and Boolean
arithmetic require explicit `.asBoolean()` and `.asNumber()` conversions.

Arithmetic supports expressions, columns and numeric literals on either side, plus unary
minus. Use `column.ref()` to declare an alias formula and `literal(...)` for constant
number, text or Boolean formulas. Text comparisons ignore case. Boolean and text aliases
normalize blank source cells to `false` and `""`, respectively. Date columns remain data
columns; formula references support number, text and Boolean columns.

Named columns register immediately: `column("Customer") { it.customer }`, just like
`formula("Total") { hours * rate }`. The string shorthand `"Customer" { it.customer }`
is equivalent. Their handles may be ignored. Unnamed `column` and `formula` return
delegates: bind them with `by` to register the column.

The library is compiled with `-Xreturn-value-checker=full`, exporting checking metadata.
Consumers enable `-Xreturn-value-checker=check` or `full` to warn about discarded delegates.
Immediate registration APIs use `@IgnorableReturnValue`. Saving a delegate in an unused
variable does not trigger this checker; it still needs `by` to register.

Number literals, evaluation and writing use the same finite-number, precision and range checks.
`BigDecimal` and `BigInteger` have explicit column overloads that preserve their input types
in typed evaluation. They remain numeric Excel cells: arbitrary precision cannot be preserved
in a numeric cell. Use a string column for exact digits, or explicitly round the input before
passing it to a numeric column. Formats change appearance, not numeric precision.
Unsupported `Long`, `BigInteger` and `BigDecimal` values fail before silently losing digits.
`Double` and `Float` inputs already carry floating-point rounding; they must be finite.

## Evaluation

`sheet.evaluate(row)` returns a map keyed by header. Data columns preserve their values and
blanks. Formula errors appear as `ExcelError`, including errors in comparisons, logical
operations, aggregates and conditions. `IF` evaluates only its selected branch.

For typed lookup, retain a column handle:

```kotlin
lateinit var total: Column<Double>
val schema = sheet<Int> {
  val n by column { it }
  total = formula("Total") { 2 * n }
}
val result: CellResult<Double> = schema.evaluateTyped(3)[total]
check(result == CellResult.Value(6.0))
```

`CellResult.Value` holds a value or blank; `CellResult.Error` holds an `ExcelError`.
Looking up a foreign column fails. Raw map evaluation, expression nodes and rendering are
internal; the public API builds expressions through typed operations and evaluates schemas.
`sheet.totals(rows)` traverses rows once, evaluates only dependencies of
the requested totals, and keeps fixed-size accumulators per referenced column. Empty
inputs have no totals row.

`total { }` has explicit numeric, text and Boolean overloads matching the destination column's
cell category. `label(...)` explicitly places text under any column. `count()` uses `COUNT`
for numeric columns, ignoring blanks and errors. `countNonBlank()` uses `COUNTA` for any
column, including text, booleans, empty strings and errors. Use the same names inside a
custom totals formula: `count(hours)` or `countNonBlank(customer)`.

Written formulas include their computed numeric, text, Boolean or error result. This is
Excel's serialized formula result, so readers without a formula engine see the correct value.
Schemas and expression trees remain immutable. Each extractor runs once per written row;
only the current evaluated row and fixed-size totals accumulators are retained. The workbook
also requests recalculation when opened, to support later edits in Excel.

## Workbooks

`workbook { sheet("Q1", schema, rows) }` accepts lists and sequences and writes synchronously.
Use `flowWorkbook` for a suspending write; it accepts flows, lists and sequences, including
different row types:

```kotlin
val report = flowWorkbook {
  sheet("Current", invoices, currentInvoices) // Flow<Invoice>
  sheet("Archive", invoices, archivedInvoices) // List<Invoice>
}
report.write(outputStream) // suspend
```

Flow workbooks collect sheets sequentially on the caller's dispatcher, with backpressure and
cancellation checks between rows. Apache POI blocks, so wrap `write` in
`withContext(Dispatchers.IO)` or another blocking-friendly dispatcher of your choice; the
library never switches dispatchers itself. POI temporary files close on success, failure and
cancellation. The final ZIP write is blocking. Caller-provided streams stay open. Sources
are consumed on every write; reuse requires replayable sources.

Path overloads default to the filename without its extension. Pass `name` to choose a
sheet name independently. Invalid names fail before opening the destination. `generateExcel`
also accepts lists, sequences and flows with an optional `name` before the trailing lambda.

File overloads accept `vararg options: OpenOption`, following `Files.newOutputStream`.
By default they create or truncate the destination; failure can leave an empty or partial
file. Pass `options = arrayOf(StandardOpenOption.CREATE_NEW)` to refuse replacement.
For atomic replacement, write a sibling temporary file, close it successfully, then move
it to the destination with the desired replacement/atomicity options. The caller chooses
that policy; caller-provided streams remain open.

## API migration

- Replace raw `Expr` types with `Expr<Number>`, `Expr<Boolean>`, `Expr<String>` or `Expr<*>`.
- Boolean formula columns are `Column<Boolean>`; use `.asNumber()` before arithmetic.
- Replace numeric conditions with `.asBoolean()` and mixed-type branches with matching types.
- Use `evaluateTyped` for `CellResult`, or handle `ExcelError` in header maps. Raw map-based
  expression evaluation is internal.
- Build references with `.ref()`, comparisons with the infix operators, and conditionals
  with `then` / `otherwise`. Expression nodes and rendering are internal to preserve type constraints.
- Replace old `count()` / `count(column)` calls with `countNonBlank()` / `countNonBlank(column)`
  to retain `COUNTA` behavior. `count` now explicitly means numeric `COUNT`.
- Custom totals must return the destination column's cell category. Use the explicit
  `label` and `countNonBlank` operations when a footer intentionally holds another type.

Run `./gradlew :excel-dsl:test`. The tests include POI agreement, streaming lifecycle and
compilation of valid and invalid client programs against the public API.
