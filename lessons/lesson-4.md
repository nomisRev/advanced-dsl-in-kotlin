---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 4</div>

# Advanced tricks & tips

## Patterns from real DSLs

---

# `reified` recovers the type at the call site

<DrawnAnnotation text="reified A" label="Available as a real type inside the inlined body" :geometry="{ label: { x: 0.62, y: 0.46, width: 0.35 } }" />
<DrawnAnnotation text="typeOf<A>()" />
<DrawnAnnotation text="serializer<A>()" />

```kotlin
inline fun <reified A> describe(): KType = typeOf<A>()

inline fun <reified A> schema(): SerialDescriptor =
  serializer<A>().descriptor
```

<!--
The Keynote source only listed the shapes this unlocks: KType, and KSerializer<A> plus its SerialDescriptor.
-->

---

# Properties take context parameters too

<DrawnAnnotation text="context(auth: AuthScheme<C, P>)" />
<DrawnAnnotation text="val <C, P> RoutingContext.principal: P" label="A context-dependent extension property" :geometry="{ label: { x: 0.55, y: 0.4, width: 0.4 } }" />

```kotlin
context(auth: AuthScheme<C, P>)
val <C, P> RoutingContext.principal: P
  get() = auth.getPrincipal(call)
```

---

# Intermediate types shape the sentence

<DrawnAnnotation text="OnEventWithAction" :occurrence="1" label="The return type decides which word may come next" :geometry="{ label: { x: 0.55, y: 0.22, width: 0.4 } }" />
<DrawnAnnotation text="infix fun and" />

```kotlin
class OnEvent(private val event: AutomationEvent) {
  infix fun then(action: Action): OnEventWithAction
  infix fun then(action: ActionBuilder.() -> Unit): OnEventWithAction
}

class OnEventWithAction(private val steps: List<Action>) {
  infix fun and(action: Action): OnEventWithAction
}
```

---

# Group smart constructors in an `object`

<DrawnAnnotation text="nodeRequestLLM()" label="Koog names every node constructor `nodeXXX` for discoverability" :geometry="{ label: { x: 0.7, y: 0.64, width: 0.27 } }" />
<DrawnAnnotation text="Node.requestLLM()" label="The `object` is the namespace, the context parameter is the scope" :geometry="{ label: { x: 0.7, y: 0.86, width: 0.27 } }" />

```kotlin
object Node {
  context(builder: AIAgentGraphStrategyBuilder<*, *>)
  fun <Input> requestLLM(): NodeDelegateBuilder<Input, List<Message.Response>> =
    TODO()

  context(builder: StrategyBuilder<*, *>)
  fun requestToolCall(): NodeDelegateBuilder<ToolCall, ToolCallResult> = TODO()
}

val x = strategy<String, String>("x") {
  val requestLLM by nodeRequestLLM()
}

val y = strategy<String, String>("y") {
  val requestLLM by Node.requestLLM()
}
```

<!--
The Keynote source flagged this slide "Drop or keep? Probably drop".
-->

---
class: dense-code
---

# `ReadWriteProperty` for typed settings

<DrawnAnnotation text="ReadWriteProperty<SdkSettings, A?>" label="`thisRef` is the owner, so the delegate can reach `raw`" :geometry="{ label: { x: 0.68, y: 0.3, width: 0.3 } }" />
<DrawnAnnotation text="var userId by SdkProperty(" label="Each setting declares how it is encoded" :geometry="{ label: { x: 0.6, y: 0.88, width: 0.3 } }" />

```kotlin
class SdkSettings(private val raw: MutableMap<String, String>) {
  inner class SdkProperty<A : Any>(
    private val decode: (String) -> A?,
    private val encode: (A) -> String,
  ) : ReadWriteProperty<SdkSettings, A?> {
    override fun getValue(thisRef: SdkSettings, property: KProperty<*>): A? =
      thisRef.raw[property.name]?.let(decode)

    override fun setValue(thisRef: SdkSettings, property: KProperty<*>, value: A?) {
      if (value == null) raw.remove(property.name)
      else raw[property.name] = encode(value)
    }
  }

  var userId by SdkProperty(decode = { it.toIntOrNull() }, encode = { it.toString() })

  var userProfile by SdkProperty(
    decode = { Json.decodeFromString<UserProfile>(it) }, encode = { Json.encodeToString(it) },
  )
}
```

<!--
The Keynote source flagged this slide "Drop or keep?".
-->

---
class: dense-code
---

# Hide the old name, keep the binary

<DrawnAnnotation text="DeprecationLevel.HIDDEN" label="Invisible to new code, still resolved by compiled callers" :geometry="{ label: { x: 0.7, y: 0.5, width: 0.27 } }" />
<DrawnAnnotation text="fun auto(block: AutoScope.() -> Unit)" label="The grouped replacement" :geometry="{ label: { x: 0.72, y: 0.74, width: 0.25 } }" />

```kotlin
class ConsumerConfigScope(
  private val entries: MutableMap<String, String>,
) : CommonConfigScope(entries) {
  @Deprecated(
    message = "Grouped in the consumer DSL: use auto.includeJmxReporter.",
    replaceWith = ReplaceWith("auto.includeJmxReporter"),
    level = DeprecationLevel.HIDDEN,
  )
  override var autoIncludeJmxReporter: Boolean? by
    KafkaProperty(entries, KafkaKey.autoIncludeJmxReporter)

  inner class AutoScope {
    var includeJmxReporter: Boolean? by
      KafkaProperty(entries, KafkaKey.autoIncludeJmxReporter)
    var commitInterval: Duration? by
      KafkaProperty(entries, KafkaKey.autoCommitIntervalMs)
  }

  fun auto(block: AutoScope.() -> Unit) = block(AutoScope())
}
```

---

# Compose receivers with interfaces

<DrawnAnnotation text="ChannelScope<A> : ProducerScope<A>, ReceiverScope<A>" label="One receiver exposes both vocabularies" :geometry="{ label: { x: 0.55, y: 0.4, width: 0.35 } }" />

```kotlin
interface ProducerScope<A>
interface ReceiverScope<A>
interface ChannelScope<A> : ProducerScope<A>, ReceiverScope<A>
```

---

# Context parameter or receiver?

| | Extension receiver | Context parameter |
| --- | --- | --- |
| How many | one | any number |
| Inside the body | `this`, implicit member calls | by name only |
| At the call site | `sensor.onTriggered()` | resolved from scope |
| Reads as | the subject of the sentence | the ambient environment |

> **The receiver is what the sentence is about, the context is where it is said.**
>
> `serranofp.com/blog/context-params.html`
