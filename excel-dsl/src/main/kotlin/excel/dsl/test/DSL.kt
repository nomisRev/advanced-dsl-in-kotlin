@file:OptIn(ExperimentalTypeInference::class)
package excel.dsl.test

import excel.dsl.Column
import excel.dsl.Formula
import org.apache.poi.ss.formula.functions.T
import kotlin.experimental.ExperimentalTypeInference

class WorkbookBuilder

fun workbook(block: context(WorkbookBuilder) () -> Unit) {}

class SheetBuilder<T> {
  fun add() {}
}

context(builder: WorkbookBuilder)
fun <T> sheet(values: Sequence<T>, block: context(SheetBuilder<T>) () -> Unit) {}

@OverloadResolutionByLambdaReturnType
context(builder: SheetBuilder<T>)
fun <T> column(name: String, block: (T) -> String): Column<String> = TODO()

context(builder: SheetBuilder<T>)
fun <T> column(name: String, block: (T) -> Number): Column<Number> = TODO()

context(builder: SheetBuilder<*>)
fun <A> formula(name: String, block: context(FormulaBuilder) () -> Formula<A>) {}


class FormulaBuilder internal constructor()

context(builder: FormulaBuilder)
operator fun Column<Number>.times(other: Column<Number>): Formula<Number> = TODO()

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "ERROR_SUPPRESSION")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated(
  """
    |
    |Cannot be called outside a formula { } block
  """.trimMargin(),
  level = DeprecationLevel.ERROR
)
operator fun Column<Number>.times(other: Column<Number>): Formula<Number> = TODO()

data class NonEmptyList<T, B>(val head: T, val tail: B) {
  companion {
//    operator fun <T> of(): NonEmptyList<T> = TODO()
//    operator fun <T> of(head: T): NonEmptyList<T> = TODO()

    operator fun <T, B> of(head: T, other: B): NonEmptyList<T,B> = TODO()

    @Deprecated(message = "Odd")
    operator fun <T, B> of(head: T, vararg tail: T): NonEmptyList<T, B> = TODO()
  }
}

fun test() {
  val x: NonEmptyList<Int> = [1, 2]
  val xx: NonEmptyList<Int> = [1, 2, 3]

}