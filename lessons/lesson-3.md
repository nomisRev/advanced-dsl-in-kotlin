---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 3</div>

# Columns as values

## Delegated properties name the column

---

# The total is a number, not a formula

<DrawnAnnotation text="it.hours * it.rate" label="Computed in Kotlin, the sheet never recalculates" color="red"  :geometry="{ label: { x: 0.6908, y: 0.4437 }, connector: { type: 'quadratic', start: { x: 0.3755, y: 0.4214 }, control: { x: 0.4046, y: 0.4528 }, end: { x: 0.4576, y: 0.4443 } } }"/>

```kotlin
import presentation.support.typed.generateExcel

generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  column("Hours") { it.hours }
  column("Rate") { it.rate }
  column("Total") { it.hours * it.rate }
}
```
```console
| Customer | Hours | Rate | Total |
| Ada      | 12    | 90   | 1080  |
```

---
magic-move
---

# The total is a number, not a formula

<DrawnAnnotation text="hours * rate" label="`hours` & `rate` are `Column<Number>`, not an `Int`" on="0" :geometry="{ label: { x: 0.7082, y: 0.4433 }, connector: { type: 'quadratic', start: { x: 0.3950, y: 0.4240 }, control: { x: 0.4231, y: 0.4535 }, end: { x: 0.4793, y: 0.4467 } } }"/>

```kotlin
import presentation.support.typed.generateExcel

generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  val hours = column("Hours") { it.hours }
  val rate = column("Rate") { it.rate }
  column("Total") { hours * rate }
}
```
```console
| Customer | Hours | Rate | Total |
| Ada      | 12    | 90   | 1080  |
```

---
magic-move
---

# The total is a number, not a formula

<DrawnAnnotation text="formula" on="0" />
<DrawnAnnotation text="*" on="0" label="Sub DSL `FormulaScope`"  :geometry="{ label: { x: 0.7029, y: 0.4449 }, connector: { type: 'quadratic', start: { x: 0.4963, y: 0.4232 }, control: { x: 0.5375, y: 0.4573 }, end: { x: 0.5858, y: 0.4483 } } }"/>

<DrawnAnnotation text="hours" on="1" />
<DrawnAnnotation text="&quot;Hours&quot;" on="1" />
<DrawnAnnotation text="rate" on="1" occurrence="2" />
<DrawnAnnotation text="&quot;Rate&quot;" />
<DrawnAnnotation text="total" on="1" />
<DrawnAnnotation text="&quot;Total&quot;" label="Redundant redefinition of column name" on="1" :geometry="{ label: { x: 0.5912, y: 0.4466 }, connector: { type: 'quadratic', start: { x: 0.3437, y: 0.4195 }, control: { x: 0.3569, y: 0.4647 }, end: { x: 0.4002, y: 0.4468 } } }"/>

```kotlin
import presentation.support.typed.generateExcel

generateExcel("invoices.xlsx", invoices) {
  column("Customer") { it.customer }
  val hours = column("Hours") { it.hours }
  val rate = column("Rate") { it.rate }
  val total = formula("Total") { hours * rate }
}
```
```console
| Customer | Hours | Rate | Total |
| Ada      | 12    | 90   | 1080  |
```

---
magic-move
---

# Columns become values we can reference

<DrawnAnnotation text="val hours by column" label="The header comes from the property name"  :geometry="{ label: { x: 0.6550, y: 0.2964 }, connector: { type: 'quadratic', start: { x: 0.2941, y: 0.3162 }, control: { x: 0.4298, y: 0.3270 }, end: { x: 0.4520, y: 0.3063 } } }"/>

```kotlin
import presentation.support.delegated.generateExcel

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
}
```
```console
| customer | hours | rate | total  |
| Ada      | 12    | 90   | =B2*C2 |
```

---
magic-move
---

# Columns become values we can reference

```kotlin
import presentation.support.delegated.generateExcel

data class ExcelConfig(val columnNameStyle: (String) -> String)

companion val ExcelConfig.Default =
  ExcelConfig { it.replaceFirstChar(Char::uppercase) }

// Example
inline fun <T> generateExcel(
  path: String,
  rows: Sequence<T>,
  config: ExcelConfig = ExcelConfig.Default,
  block: SheetBuilder<T>.() -> Unit,
)

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  column("Total") { hours * rate }
}
```
```console
| Customer | Hours | Rate | Total  |
| Ada      | 12    | 90   | =B2*C2 |
```

---

# A delegate is just `getValue`

<DrawnAnnotation text="by entries" label="The `Map` is the delegate"  :geometry="{ label: { x: 0.3227, y: 0.3385 }, connector: { type: 'quadratic', start: { x: 0.2143, y: 0.2767 }, control: { x: 0.2181, y: 0.3061 }, end: { x: 0.2335, y: 0.3205 } } }"/>

```kotlin
val entries = mapOf("one" to 1, "two" to 2)
val one by entries

println(one)
```
```console
1
```

---
magic-move
---

# A delegate is just `getValue`

<DrawnAnnotation text="entries.getValue(null, ::one)" label="What the compiler generates for every read of `one`"  :geometry="{ label: { x: 0.6336, y: 0.3123, width: 0.4015 }, connector: { type: 'quadratic', start: { x: 0.4714, y: 0.3625 }, control: { x: 0.5023, y: 0.3609 }, end: { x: 0.5210, y: 0.3266 } } }"/>

```kotlin
val entries = mapOf("one" to 1, "two" to 2)
val one by entries

println(entries.getValue(null, ::one))
```
```console
1
```

---
magic-move
---

# A delegate is just `getValue`

<DrawnAnnotation text="operator fun" />
<DrawnAnnotation text="this[property.name]" label="`entries[&quot;one&quot;]`"  :geometry="{ label: { x: 0.5422, y: 0.4392 }, connector: { type: 'quadratic', start: { x: 0.3485, y: 0.5043 }, control: { x: 0.4089, y: 0.5058 }, end: { x: 0.4646, y: 0.4570 } } }"/>

```kotlin
import kotlin.reflect.KProperty

val entries = mapOf("one" to 1, "two" to 2)
val one by entries

operator fun <V> Map<String, V>.getValue(
  thisRef: Any?,
  property: KProperty<*>,
): V = this[property.name]
  ?: throw NoSuchElementException("Key ${property.name} is missing")
```

---

# `ReadOnlyProperty` names the contract

<DrawnAnnotation text="fun interface" label="One abstract method, so a lambda implements it"  :geometry="{ label: { x: 0.3591, y: 0.3783 }, connector: { type: 'quadratic', start: { x: 0.1685, y: 0.2275 }, control: { x: 0.1711, y: 0.2862 }, end: { x: 0.1685, y: 0.3571 } } }"/>

```kotlin
import kotlin.reflect.KProperty

fun interface ReadOnlyProperty<in T, out V> {
  operator fun getValue(thisRef: T, property: KProperty<*>): V
}
```

---
magic-move
---

# `ReadOnlyProperty` names the contract

<DrawnAnnotation text="ReadOnlyProperty<Any?, Column<Number>>" label="`thisRef` is `Any?`, the property is a local inside a lambda" />

```kotlin no-compile
fun interface ReadOnlyProperty<in T, out V> {
  operator fun getValue(thisRef: T, property: KProperty<*>): V
}

class Column<V>(val name: String, val index: Int)

@ExcelDsl
class SheetBuilder<T> {
  fun column(value: (T) -> Number): ReadOnlyProperty<Any?, Column<Number>>
}
```

---

# Registering in `getValue` is dangerous

<DrawnAnnotation text="private val columns" label="The sheet has to know its columns up front"  :geometry="{ label: { x: 0.2732, y: 0.5695 }, connector: { type: 'quadratic', start: { x: 0.1160, y: 0.3255 }, control: { x: 0.1052, y: 0.4356 }, end: { x: 0.1160, y: 0.5466 } } }"/>

```kotlin no-compile
@ExcelDsl
class SheetBuilder<T> {
  private val columns: MutableList<Column<*>> = mutableListOf()

  fun column(value: (T) -> Number): ReadOnlyProperty<Any?, Column<Number>>
}
```

---
magic-move
---

# Registering in `getValue` is too late

<DrawnAnnotation text="ReadOnlyProperty { _: Any?, property ->" label="Runs on every read, or never" color="red" on="0" :geometry="{ label: { x: 0.7570, y: 0.6074 }, connector: { type: 'quadratic', start: { x: 0.7514, y: 0.4243 }, control: { x: 0.7340, y: 0.4893 }, end: { x: 0.7508, y: 0.5844 } } }" />
<DrawnAnnotation text="columns.add(column)" label="Not idempotent but might be called 0 or many times" color="red" on="1" :geometry="{ label: { x: 0.3306, y: 0.6230 }, connector: { type: 'quadratic', start: { x: 0.2057, y: 0.5139 }, control: { x: 0.2026, y: 0.5474 }, end: { x: 0.2001, y: 0.5913 } } }"/>

```kotlin
import kotlin.properties.ReadOnlyProperty

@ExcelDsl
class SheetBuilder<T> {
  private val columns: MutableList<Column<*>> = mutableListOf()

  fun column(value: (T) -> Number) = ReadOnlyProperty { _: Any?, property ->
    val column = Column<Number>(property.name, columns.size)
    columns.add(column)
    column
  }
}
```

---
magic-move
---

# `PropertyDelegateProvider` runs at the `by`

<DrawnAnnotation text="PropertyDelegateProvider { _: Any?, property ->" label="Runs once, when `by column { }` is evaluated" on="0" :geometry="{ label: { x: 0.7254, y: 0.3535 }, connector: { type: 'quadratic', start: { x: 0.6308, y: 0.4544 }, control: { x: 0.6556, y: 0.4501 }, end: { x: 0.6698, y: 0.3922 } } }"/>
<DrawnAnnotation text="ReadOnlyProperty { _: Any?, _ -> column }" label="Every read returns the registered column" on="1" :geometry="{ label: { x: 0.4175, y: 0.6770 } }"/>

```kotlin
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

@ExcelDsl
class SheetBuilder<T> {
  private val columns: MutableList<Column<*>> = mutableListOf()

  fun column(value: (T) -> Number) =
    PropertyDelegateProvider { _: Any?, property ->
      val column = Column<Number>(property.name, columns.size)
      columns.add(column)
      ReadOnlyProperty { _: Any?, _ -> column }
    }
}
```

---

# The lambda still picks the type

<TypeHint :line="2" text="customer" type="Column<String>">
<TypeHint :line="3" text="hours" type="Column<Number>">
<TypeHint :line="4" text="issued" type="Column<LocalDate>">

```kotlin
import presentation.support.delegated.generateExcel

generateExcel("invoices.xlsx", invoices) {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val issued by column { it.issuedOn }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

<!--
Same @OverloadResolutionByLambdaReturnType + @JvmName overloads as Part 2,
each returning a PropertyDelegateProvider for its own Column<V>. Verified on 2.4.20.
-->
