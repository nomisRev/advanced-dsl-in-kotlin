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

<DrawnAnnotation text="{ it }" label="Compiles, and writes `toString()` into the cell" color="red"  :geometry="{ label: { x: 0.5448, y: 0.3495 }, connector: { type: 'quadratic', start: { x: 0.2888, y: 0.3244 }, control: { x: 0.3027, y: 0.3508 }, end: { x: 0.3200, y: 0.3491 } } }"/>

```kotlin
generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  column("Invoice") { it }
}
```
```console
| Customer | Invoice                                                       |
| Ada      | Invoice(customer=Ada, hours=12, rate=90, issuedOn=2026-09-01) |
```

---

# One overload per cell type

<DrawnAnnotation text="-> String" />
<DrawnAnnotation text="-> Number" />
<DrawnAnnotation text="-> LocalDate" label="Every type a cell can hold" />

```kotlin
import java.time.LocalDate

class SheetBuilder<T> {
  fun column(name: String, value: (T) -> String)
  fun column(name: String, value: (T) -> Number)
  fun column(name: String, value: (T) -> LocalDate)
}
```

---

# The overload is chosen before the lambda

<InlineCompilerError :line="2" text="column" message="Overload resolution ambiguity between candidates:\nfun column(name: String, value: (Invoice) -> String): Unit\nfun column(name: String, value: (Invoice) -> Number): Unit\nfun column(name: String, value: (Invoice) -> LocalDate): Unit" style="--inline-compiler-error-message-size: 1.1rem">

```kotlin
generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
}
```

</InlineCompilerError>

<!--
The compiler needs a candidate to know the lambda's parameter types, and the lambda's
return type is the only difference between the candidates. So it gives up.
-->

---

# The lambda picks the overload

> Experimental: `@OptIn(ExperimentalTypeInference::class)`

<DrawnAnnotation text="@OverloadResolutionByLambdaReturnType" label="Analyse the lambda first, then choose" />
<DrawnAnnotation text="@OverloadResolutionByLambdaReturnType" occurrence="2" />
<DrawnAnnotation text="@OverloadResolutionByLambdaReturnType" occurrence="3" />

```kotlin
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

<InlineCompilerError :line="3" text="fun column(name: String, value: (T) -> String)" message="Platform declaration clash: The following declarations have the same JVM signature\n(column(Ljava/lang/String;Lkotlin/jvm/functions/Function1;)V)" style="--inline-compiler-error-message-size: 1.1rem">

```kotlin
class SheetBuilder<T> {
  @OverloadResolutionByLambdaReturnType
  fun column(name: String, value: (T) -> String)
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

<DrawnAnnotation text="@JvmName(&quot;textColumn&quot;)" label="Only Java and the bytecode see this name"  :geometry="{ label: { x: 0.7194, y: 0.2376 }, connector: { type: 'quadratic', start: { x: 0.3285, y: 0.3166 }, control: { x: 0.4276, y: 0.3131 }, end: { x: 0.5262, y: 0.2584 } } }"/>
<DrawnAnnotation text="@JvmName(&quot;numberColumn&quot;)" />
<DrawnAnnotation text="@JvmName(&quot;dateColumn&quot;)" />

```kotlin
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

<DrawnAnnotation text="{ it.customer }" label="`String`"  :geometry="{ label: { x: 0.5385, y: 0.2410 }, connector: { type: 'quadratic', start: { x: 0.4603, y: 0.2636 }, control: { x: 0.4861, y: 0.2703 }, end: { x: 0.5028, y: 0.2439 } } }"/>
<DrawnAnnotation text="{ it.hours }" label="`Number`"  :geometry="{ label: { x: 0.5431, y: 0.3004 }, connector: { type: 'quadratic', start: { x: 0.3943, y: 0.3131 }, control: { x: 0.4503, y: 0.3218 }, end: { x: 0.5074, y: 0.2965 } } }"/>
<DrawnAnnotation text="{ it.issuedOn }" label="`LocalDate`"  :geometry="{ label: { x: 0.5681, y: 0.3599 }, connector: { type: 'quadratic', start: { x: 0.4379, y: 0.3606 }, control: { x: 0.4773, y: 0.3674 }, end: { x: 0.5167, y: 0.3493 } } }"/>

```kotlin
generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  column("Hours") { it.hours }
  column("Issued") { it.issuedOn }
}
```

---
magic-move
---

# Every column has a cell type

<InlineCompilerError :line="5" text="column" message="Overload resolution ambiguity between candidates:\nfun column(name: String, value: (Invoice) -> String): Unit\nfun column(name: String, value: (Invoice) -> Number): Unit\nfun column(name: String, value: (Invoice) -> LocalDate): Unit" style="--inline-compiler-error-message-size: 1.1rem">

```kotlin
generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  column("Hours") { it.hours }
  column("Issued") { it.issuedOn }
  column("Invoice") { it }
}
```

</InlineCompilerError>

<!--
The message is still "ambiguity", but now it means: none of the cell types fit an Invoice.
-->
