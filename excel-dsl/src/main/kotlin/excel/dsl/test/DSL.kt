@file:OptIn(ExperimentalTypeInference::class)
package excel.dsl.test

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
fun <T> column(name: String, block: (T) -> String) {}

context(builder: SheetBuilder<T>)
fun <T> column(name: String, block: (T) -> Number) {}
