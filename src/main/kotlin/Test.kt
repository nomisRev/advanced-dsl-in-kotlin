import excel.dsl.CellFormat
import excel.dsl.Column
import excel.dsl.ColumnDelegate
import excel.dsl.SheetBuilder
import excel.dsl.column
import kotlin.properties.ReadOnlyProperty
import kotlin.experimental.ExperimentalTypeInference

data class Custom(val raw: String)

data class Invoice(
  val name: String,
  val custom: Custom
)

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("textColumn")
context(_: SheetBuilder<T>)
fun <T> customColumn(
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> Custom
): ColumnDelegate<String> =
  column(format, width) { value(it).raw }

val x = excel.dsl.sheet<Invoice> {
  val name by column { it.name }
  val custom by customColumn { it.custom }
}

sealed interface Formula<out T> {
  data class Ref<T>(val column: Column<T>) : Formula<T>
  data class Times(val left: Formula<Number>, val right: Formula<Number>) : Formula<Double>
  data class Eq<T>(val left: Formula<T>, val right: Formula<T>) : Formula<Boolean>
  data class In<T>(val target: Formula<T>, val values: Iterable<T>) : Formula<Boolean>
  data class Contains(val target: Formula<String>, val substring: String) : Formula<Boolean>
}

class FormulaBuilder internal constructor() {
  fun test() {}
}

context(_: FormulaBuilder)
infix fun Column<String>.contains(other: String): Formula<Boolean> =
  Formula.Contains(Formula.Ref(this), other)

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "ERROR_SUPPRESSION")
context(_: FormulaBuilder)
infix fun <@kotlin.internal.OnlyInputTypes A> Iterable<A>.contains(other: Column<A>): Formula<Boolean> =
  Formula.In(Formula.Ref(other), this)

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "ERROR_SUPPRESSION")
context(_: FormulaBuilder)
infix fun <A> Iterable<@kotlin.internal.Exact A>.contains2(other: Column<A>): Formula<Boolean> =
  Formula.In(Formula.Ref(other), this)

fun <A> formula(
  block: context(FormulaBuilder) () -> Formula<A>
): Formula<A> = block(FormulaBuilder())

val company: Column<String> = TODO()
val hours: Column<Long> = TODO()

//val rate: Column<Double> = TODO()
//
//val xx = formula { company contains "ABC" }
//val xxx = formula { ["ABC", "DEF"] contains company }
//// Rejected by OnlyInputTypes: formula { [1, 2, 3] contains company }
//
////context(_: FormulaBuilder)
////operator fun Column<Long>.compareTo(other: Column<Long>): Formula<Long> = TODO()
//
val y = formula<Number> {
//  test()
//  hours < 9
//  [1, 2] contains company
//  [1, 2] contains2 company


  TODO()
}

val t = t()

open class Poop {
  val tt by t
}

class Test2 : Poop() {
  val ttt by t
}

fun t(): ReadOnlyProperty<Poop, Int> = ReadOnlyProperty { _, prop ->
  println("Prop ${prop.name}")
  1
}

fun main() {
  val p = Poop()
  val t = Test2()
  p.tt
  t.ttt
  t.ttt
  t.ttt
}




