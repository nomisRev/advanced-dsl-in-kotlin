---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 2</div>

# Building a simple DSL

## Scopes, markers and delegates

---

# Nested scopes leak the outer receiver

<DrawnAnnotation text="room(&quot;hallway&quot;) {" label="`this: RoomBuilder`, and `this@home: HomeBuilder` is still in scope" :geometry="{ label: { x: 0.45, y: 0.3, width: 0.45 } }" />

```kotlin
val home = home {
  room("hallway") {

  }
}
```

---
magic-move
---

# Nested scopes leak the outer receiver

<DrawnAnnotation text="room(&quot;kitchen&quot;)" label="Resolves to `HomeBuilder.room` through the outer receiver" color="red" :geometry="{ label: { x: 0.45, y: 0.4, width: 0.45 } }" />

```kotlin
val home = home {
  room("hallway") {
    room("kitchen") {

    }
  }
}
```

---

# `@DslMarker` closes the scope

```kotlin
class RoomBuilder

class HomeBuilder {
  fun room(name: String, block: RoomBuilder.() -> Unit): Unit
  fun build(): Home
}
```

---
magic-move
---

# `@DslMarker` closes the scope

<DrawnAnnotation text="@DslMarker" label="`@Target(ANNOTATION_CLASS) annotation class DslMarker`" :geometry="{ label: { x: 0.35, y: 0.62, width: 0.5 } }" />

```kotlin
class RoomBuilder

class HomeBuilder {
  fun room(name: String, block: RoomBuilder.() -> Unit): Unit
  fun build(): Home
}

@DslMarker
annotation class HomeAutomationDsl
```

---
magic-move
---

# `@DslMarker` closes the scope

<DrawnAnnotation text="@HomeAutomationDsl" label="Mark every builder that belongs to the DSL" :geometry="{ label: { x: 0.6, y: 0.3, width: 0.35 } }" />
<DrawnAnnotation text="@HomeAutomationDsl" :occurrence="2" />

```kotlin
@HomeAutomationDsl
class RoomBuilder

@HomeAutomationDsl
class HomeBuilder {
  fun room(name: String, block: RoomBuilder.() -> Unit): Unit
  fun build(): Home
}

@DslMarker
annotation class HomeAutomationDsl
```

---

# Implicit receivers stay in their own scope

> Compile-time guarantee: an outer marked receiver
>
> cannot be called implicitly from an inner one

<InlineCompilerError text="room(&quot;kitchen&quot;)" message="'fun room(name: String, block: RoomBuilder.() -> Unit): Unit' can't be called in this context by implicit receiver. Use the explicit one if necessary">

```kotlin
@DslMarker
annotation class HomeAutomationDsl

val home = home {
  room("hallway") {
    room("kitchen") {

    }
  }
}
```

</InlineCompilerError>

---

# Devices belong to a room

<DrawnAnnotation text="light(&quot;main&quot;)" label="The name is written twice, once for Kotlin, once for the DSL" :geometry="{ label: { x: 0.5, y: 0.62, width: 0.45 } }" />

```kotlin
@HomeAutomationDsl
class RoomBuilder {
  fun light(name: String): Light
  fun motionSensor(name: String): MotionSensor
}

val home = home {
  room("hallway") {
    val main = light("main")
    val motion = motionSensor("motion")
  }
}
```

---
magic-move
---

# Devices belong to a room

<DrawnAnnotation text="by light()" label="Kotlin property delegates" :geometry="{ label: { x: 0.5, y: 0.62, width: 0.3 } }" />
<DrawnAnnotation text="by motionSensor()" />

```kotlin
@HomeAutomationDsl
class RoomBuilder {
  fun light(name: String): Light
  fun motionSensor(name: String): MotionSensor
}

val home = home {
  room("hallway") {
    val main by light()
    val motion by motionSensor()
  }
}
```

---

# A delegate is just `getValue`

<DrawnAnnotation text="by entries" label="The `Map` is the delegate" :geometry="{ label: { x: 0.5, y: 0.25, width: 0.3 } }" />

```kotlin
val entries = mapOf("one" to 1, "two" to 2)
val one by entries

println(one) // 1
```

---
magic-move
---

# A delegate is just `getValue`

<DrawnAnnotation text="entries.getValue(null, ::one)" label="What the compiler generates for every read of `one`" :geometry="{ label: { x: 0.6, y: 0.5, width: 0.35 } }" />

```kotlin
val entries = mapOf("one" to 1, "two" to 2)
val one by entries

println(entries.getValue(null, ::one)) // 1
```

---
magic-move
---

# A delegate is just `getValue`

<DrawnAnnotation text="operator fun" />
<DrawnAnnotation text="this[property.name]" label="`entries[&quot;one&quot;]`" :geometry="{ label: { x: 0.5, y: 0.7, width: 0.3 } }" />

```kotlin
val entries = mapOf("one" to 1, "two" to 2)
val one by entries

println(one) // 1

operator fun <V> Map<String, V>.getValue(
  thisRef: Any?,
  property: KProperty<*>,
): V = this[property.name]
  ?: throw NoSuchElementException("Key ${property.name} is missing in the map.")
```

---

# `ReadOnlyProperty` names the contract

<DrawnAnnotation text="fun interface" label="One abstract method, so a lambda implements it" :geometry="{ label: { x: 0.55, y: 0.14, width: 0.4 } }" />

```kotlin
fun interface ReadOnlyProperty<in T, out V> {
  operator fun getValue(thisRef: T, property: KProperty<*>): V
}
```

---
magic-move
---

# `ReadOnlyProperty` names the contract

<DrawnAnnotation text="ReadOnlyProperty<Any?, Light>" label="`thisRef` is `Any?`, the property lives inside a lambda" :geometry="{ label: { x: 0.55, y: 0.5, width: 0.4 } }" />

```kotlin
fun interface ReadOnlyProperty<in T, out V> {
  operator fun getValue(thisRef: T, property: KProperty<*>): V
}

@HomeAutomationDsl
class RoomBuilder {
  fun light(): ReadOnlyProperty<Any?, Light>
}
```

---

# Registering in `getValue` is too late

<DrawnAnnotation text="private val lights" label="The room has to know its devices" :geometry="{ label: { x: 0.6, y: 0.22, width: 0.3 } }" />

```kotlin
@HomeAutomationDsl
class RoomBuilder {
  private val lights: MutableList<Light> = mutableListOf()

  fun light(): ReadOnlyProperty<Any?, Light>
}
```

---
magic-move
---

# Registering in `getValue` is too late

<DrawnAnnotation text="ReadOnlyProperty { _: Any?, prop ->" label="Only runs on `getValue`, potentially never" color="red" :geometry="{ label: { x: 0.6, y: 0.5, width: 0.35 } }" />

```kotlin
@HomeAutomationDsl
class RoomBuilder {
  private val lights: MutableList<Light> = mutableListOf()

  fun light() = ReadOnlyProperty { _: Any?, prop ->
    val light = Light(prop.name)
    lights.add(light)
    light
  }
}
```

---
magic-move
---

# `PropertyDelegateProvider` runs at the `by`

<DrawnAnnotation text="PropertyDelegateProvider { _: Any?, prop ->" label="Inlined by the compiler, executes once when `light()` is called" :geometry="{ label: { x: 0.78, y: 0.36, width: 0.2 } }" />
<DrawnAnnotation text="ReadOnlyProperty { _: Any?, _ -> light }" label="The delegate only hands back the registered light" :geometry="{ label: { x: 0.78, y: 0.6, width: 0.2 } }" />

```kotlin
@HomeAutomationDsl
class RoomBuilder {
  private val lights: MutableList<Light> = mutableListOf()

  fun light() = PropertyDelegateProvider { _: Any?, prop ->
    val light = Light(prop.name)
    lights.add(light)
    ReadOnlyProperty { _: Any?, _ -> light }
  }
}
```

---

# Automations live in a room

```kotlin
val home = home {
  room("hallway") {
    val main by light()
    val motion by motionSensor()
  }
}
```

---
magic-move
---

# Automations live in a room

<DrawnAnnotation text="automation(&quot;welcome-home&quot;) {" label="`this: AutomationBuilder`" :geometry="{ label: { x: 0.55, y: 0.42, width: 0.3 } }" />

```kotlin
val home = home {
  room("hallway") {
    val main by light()
    val motion by motionSensor()

    automation("welcome-home") {

    }
  }
}
```

---
magic-move
---

# Automations live in a room

<DrawnAnnotation text="motion.onTriggered()" label="An extension on `MotionSensor`, but it needs the automation" :geometry="{ label: { x: 0.55, y: 0.5, width: 0.4 } }" />

```kotlin
val home = home {
  room("hallway") {
    val main by light()
    val motion by motionSensor()

    automation("welcome-home") {
      motion.onTriggered()
    }
  }
}
```

---
magic-move
---

# Automations live in a room

<DrawnAnnotation text="context(automation: AutomationBuilder)" label="A context parameter, resolved from the enclosing `AutomationBuilder`" :geometry="{ label: { x: 0.55, y: 0.66, width: 0.4 } }" />

```kotlin
val home = home {
  room("hallway") {
    val main by light()
    val motion by motionSensor()

    automation("welcome-home") {
      motion.onTriggered()
    }
  }
}

context(automation: AutomationBuilder)
fun MotionSensor.onTriggered(detected: Boolean = true)
```
