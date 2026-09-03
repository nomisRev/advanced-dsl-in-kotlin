---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 3</div>

# Context parameters

## The environment a DSL call needs

---

# Context parameters are resolved from scope

<DrawnAnnotation text="context(automation: AutomationBuilder)" label="Callable when an `AutomationBuilder` is in scope, as `this` or as a context" :geometry="{ label: { x: 0.5, y: 0.34, width: 0.45 } }" />

```kotlin
context(automation: AutomationBuilder)
fun MotionSensor.onTriggered(detected: Boolean = true)
```

---
magic-move
---

# Context parameters are resolved from scope

<DrawnAnnotation text="automation.registerTrigger(this, detected)" label="Both the context parameter and the extension receiver are in the body" :geometry="{ label: { x: 0.5, y: 0.4, width: 0.45 } }" />

```kotlin
context(automation: AutomationBuilder)
fun MotionSensor.onTriggered(detected: Boolean = true) =
  automation.registerTrigger(this, detected)
```

---

# Context parameters let users extend the DSL

<DrawnAnnotation text="MyCustomSensor" label="Your own domain type, outside the library" :geometry="{ label: { x: 0.5, y: 0.34, width: 0.35 } }" />
<DrawnAnnotation text="automation.register(CustomEvent(this))" />

```kotlin
context(automation: AutomationBuilder)
fun MyCustomSensor.onCustomEvent() =
  automation.register(CustomEvent(this))
```

---

# The trigger needs an action

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

# The trigger needs an action

<DrawnAnnotation text="then {" label="Not defined yet" :geometry="{ label: { x: 0.55, y: 0.5, width: 0.25 } }" />

```kotlin
val home = home {
  room("hallway") {
    val main by light()
    val motion by motionSensor()

    automation("welcome-home") {
      motion.onTriggered() then {
        main.turnOn(brightness = 60)
      }
    }
  }
}
```

---

# Fluent syntax is an extension on the event

<DrawnAnnotation text="onTriggered()" />
<DrawnAnnotation text="AutomationEvent.then" />
<DrawnAnnotation text="AutomationActionBuilder.() -> Unit" label="`this: AutomationActionBuilder` inside the block" :geometry="{ label: { x: 0.5, y: 0.7, width: 0.4 } }" />

```kotlin
automation("welcome-home") {
  motion.onTriggered().then {

  }
}

context(automation: AutomationBuilder)
fun AutomationEvent.then(block: AutomationActionBuilder.() -> Unit)
```

---
magic-move
---

# `infix` drops the dot and the parentheses

<DrawnAnnotation text="infix" />
<DrawnAnnotation text="onTriggered() then {" label="One receiver, one argument, so it reads like a sentence" :geometry="{ label: { x: 0.6, y: 0.38, width: 0.35 } }" />

```kotlin
automation("welcome-home") {
  motion.onTriggered() then {

  }
}

context(automation: AutomationBuilder)
infix fun AutomationEvent.then(block: AutomationActionBuilder.() -> Unit)
```

---

# Actions are context-aware too

```kotlin
val home = home {
  room("hallway") {
    val main by light()
    val motion by motionSensor()

    automation("welcome-home") {
      motion.onTriggered() then {
        main.turnOn(brightness = 60)
      }
    }
  }
}
```

---
magic-move
---

# Actions are context-aware too

<DrawnAnnotation text="context(automation: AutomationActionBuilder)" label="Only callable inside `then { }`" :geometry="{ label: { x: 0.55, y: 0.72, width: 0.3 } }" />

```kotlin
val home = home {
  room("hallway") {
    val main by light()
    val motion by motionSensor()

    automation("welcome-home") {
      motion.onTriggered() then {
        main.turnOn(brightness = 60)
      }
    }
  }
}

context(automation: AutomationActionBuilder)
fun Light.turnOn(brightness: Int = 100)
```
