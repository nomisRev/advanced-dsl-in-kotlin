---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 1</div>

# Function shapes

## Every DSL is built from a few of them

---

# Goal: a home automation DSL

<DrawnAnnotation text="home {" />
<DrawnAnnotation text="room(&quot;hallway&quot;) {" label="Nested scopes, one builder each" :geometry="{ label: { x: 0.62, y: 0.27, width: 0.3 } }" />
<DrawnAnnotation text="by light()" label="Devices are delegated properties" :geometry="{ label: { x: 0.62, y: 0.36, width: 0.3 } }" />
<DrawnAnnotation text="then {" label="An infix function reads like a sentence" :geometry="{ label: { x: 0.62, y: 0.53, width: 0.3 } }" />

```kotlin
home {
  room("hallway") {
    val mainLight by light()
    val motionSensor by motionSensor()

    automation("welcome-home") {
      motionSensor.onMotion() then {
        mainLight.turnOn(brightness = 60)
      }
    }
  }
}
```

---

# Function shapes

<DrawnAnnotation text="HomeBuilder.() -> Unit" label="Lambda with receiver" />

```kotlin
fun home(block: HomeBuilder.() -> Unit): Home
```

---
magic-move
---

# Function shapes

<DrawnAnnotation text="class HomeBuilder" label="The receiver is a builder that collects the calls" />
<DrawnAnnotation text="fun build(): Home" label="Turns what was collected into the immutable result" />

```kotlin
class HomeBuilder {
  fun build(): Home
}

fun home(block: HomeBuilder.() -> Unit): Home
```

---
magic-move
---

# Function shapes

<DrawnAnnotation text="apply(block)" label="`inline fun <T> T.apply(block: T.() -> Unit): T`" />

```kotlin
class HomeBuilder {
  fun build(): Home
}

fun home(block: HomeBuilder.() -> Unit): Home =
  HomeBuilder().apply(block).build()
```

---
magic-move
---

# Function shapes

<DrawnAnnotation text="builder.block()" label="`block` runs with `builder` as `this`" />

```kotlin
class HomeBuilder {
  fun build(): Home
}

fun home(block: HomeBuilder.() -> Unit): Home {
  val builder = HomeBuilder()
  builder.block()
  return builder.build()
}
```

---

# Home automation

<DrawnAnnotation text="home {" label="Trailing lambda, `this` is `HomeBuilder`" :geometry="{ label: { x: 0.45, y: 0.66, width: 0.4 } }" />

```kotlin
class HomeBuilder {
  fun build(): Home
}

fun home(block: HomeBuilder.() -> Unit): Home =
  HomeBuilder().apply(block).build()

val home = home {

}
```

---
magic-move
---

# Home automation

<DrawnAnnotation text="room(&quot;hallway&quot;) {" label="What we want to write next" :geometry="{ label: { x: 0.45, y: 0.72, width: 0.3 } }" />

```kotlin
class HomeBuilder {
  fun build(): Home
}

fun home(block: HomeBuilder.() -> Unit): Home =
  HomeBuilder().apply(block).build()

val home = home {
  room("hallway") {

  }
}
```

---
magic-move
---

# Home automation

<DrawnAnnotation text="class RoomBuilder" />
<DrawnAnnotation text="RoomBuilder.() -> Unit" label="A nested scope gets its own receiver" :geometry="{ label: { x: 0.74, y: 0.42, width: 0.24 } }" />

```kotlin
class RoomBuilder

class HomeBuilder {
  fun room(name: String, block: RoomBuilder.() -> Unit): Unit
  fun build(): Home
}

fun home(block: HomeBuilder.() -> Unit): Home =
  HomeBuilder().apply(block).build()

val home = home {
  room("hallway") {

  }
}
```
