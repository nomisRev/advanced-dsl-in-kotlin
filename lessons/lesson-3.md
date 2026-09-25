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

<DrawnAnnotation text="it.hours * it.rate" label="Computed in Kotlin, the sheet never recalculates" color="red" />

```kotlin
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

# Columns become values we can reference

<DrawnAnnotation text="val hours by column" label="The header comes from the property name"  :geometry="{ label: { x: 0.6972, y: 0.2906 }, connector: { type: 'quadratic', start: { x: 0.2925, y: 0.3177 }, control: { x: 0.3922, y: 0.3448 }, end: { x: 0.4918, y: 0.3002 } } }"/>
<DrawnAnnotation text="hours * rate" label="`hours` is a `Column<Number>`, not an `Int`" color="var(--fundamentals-pink)" />

```kotlin
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

# A delegate is just `getValue`

<DrawnAnnotation text="by entries" label="The `Map` is the delegate" />

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

<DrawnAnnotation text="entries.getValue(null, ::one)" label="What the compiler generates for every read of `one`" />

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
<DrawnAnnotation text="this[property.name]" label="`entries[&quot;one&quot;]`"  :geometry="{ label: { x: 0.5418, y: 0.4300 }, connector: { type: 'quadratic', start: { x: 0.3480, y: 0.4982 }, control: { x: 0.4084, y: 0.4997 }, end: { x: 0.4641, y: 0.4509 } } }"/>

```kotlin
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
fun interface ReadOnlyProperty<in T, out V> {
  operator fun getValue(thisRef: T, property: KProperty<*>): V
}
```

---
magic-move
---

# `ReadOnlyProperty` names the contract

<DrawnAnnotation text="ReadOnlyProperty<Any?, Column<Number>>" label="`thisRef` is `Any?`, the property is a local inside a lambda" />

```kotlin
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

# Registering in `getValue` is too late

<DrawnAnnotation text="private val columns" label="The sheet has to know its columns up front" />

```kotlin
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

<DrawnAnnotation text="ReadOnlyProperty { _: Any?, property ->" label="Runs on every read, or never" color="red" />

```kotlin
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

<DrawnAnnotation text="PropertyDelegateProvider { _: Any?, property ->" label="Runs once, when `by column { }` is evaluated"  :geometry="{ label: { x: 0.7254, y: 0.3535 }, connector: { type: 'quadratic', start: { x: 0.6308, y: 0.4544 }, control: { x: 0.6556, y: 0.4501 }, end: { x: 0.6698, y: 0.3922 } } }"/>
<DrawnAnnotation text="ReadOnlyProperty { _: Any?, _ -> column }" label="Every read returns the registered column" color="var(--fundamentals-pink)" />

```kotlin
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
